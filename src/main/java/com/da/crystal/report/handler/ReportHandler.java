/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-16 11:51:49                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-11 19:44:08                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.document.ISummaryInfo;
import com.crystaldecisions.sdk.occa.report.document.SummaryInfo;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;
import com.da.crystal.report.CR.CRJavaHelper;
import com.da.crystal.report.JNDI.JNDIManager;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReportHandler implements Handler<RoutingContext> {
  String REPORTS_PATH = "/usr/share/java/crystal-report-server/reports";
  String JDBC_URL = "jdbc:mysql://localhost:3306/crystal_report";
  String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  String USER = "crystal_report";
  String PASSWORD = "crystal_report";
  String JNDI_NAME = "java:comp/env/jdbc/crystal_report";

  ReportClientDocument clientDoc = new ReportClientDocument();

  public ReportHandler(JsonObject config) {
    JsonObject reportConfig = config.getJsonObject("report");

    this.REPORTS_PATH = Utils.isWindows()
        ? reportConfig.getJsonObject("windows").getString("reportsPath", REPORTS_PATH)
        : reportConfig.getJsonObject("linux").getString("reportsPath", REPORTS_PATH);
    this.JDBC_URL = reportConfig.getString("url", JDBC_URL);
    this.DRIVER_CLASS_NAME = reportConfig.getString("driverClassName", DRIVER_CLASS_NAME);
    this.USER = reportConfig.getString("user", USER);
    this.PASSWORD = reportConfig.getString("password", PASSWORD);
    this.JNDI_NAME = reportConfig.getString("jndiName", JNDI_NAME);
  }

  @Override
  public void handle(RoutingContext context) {
    var resp = context.response();

    try {
      // 1. Parse and validate URL
      String[] pathInfo = parseReportPath(context.request().uri());
      if (pathInfo == null) {
        resp.setStatusCode(400).end("<H1>Wrong request url!</H1>");
        return;
      }
      String report = pathInfo[0];
      String format = pathInfo[1];

      // 2. Validate format
      if (!isFormatAllowed(format)) {
        resp.setStatusCode(200).end("<H1>Document format {" + format + "} is not supported!</H1>");
        return;
      }

      // 3. Check report file exists
      File reportFile = findReportFile(report);
      if (reportFile == null) {
        resp.setStatusCode(404).end("<H1>Report template {" + report + "} not found!</H1>");
        return;
      }

      // 4. Extract query parameters
      ReportParams params = extractQueryParams(context);
      String reportFileName = params.reportFileName;
      String author = params.author;

      // 5. Copy report to temp file to avoid locking the original
      Path tempFile = Files.createTempFile("cr_", ".rpt");
      Files.copy(reportFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
      clientDoc = ReportClientDocument.openReport(tempFile.toFile());

      // 6. Process and export report
      processReport(clientDoc, context, report, format, author, reportFileName);

    } catch (ReportSDKExceptionBase | IOException e) {
      if (!resp.closed()) {
        resp.setStatusCode(500).end("<H1>Server Error!</H1><br>" + e.getMessage());
      }
      e.printStackTrace();
    }
  }

  private String[] parseReportPath(String uri) {
    String[] pathParams = uri.split("/");
    if (pathParams.length != 4) {
      log.info("Wrong request url:{}", uri);
      return null;
    }
    String report = pathParams[2];
    String format = pathParams[3].split("\\?")[0].toLowerCase();
    log.debug("Report: {} Format: {}", report, format);
    return new String[] { report, format };
  }

  private boolean isFormatAllowed(String format) {
    List<String> allowedFormat = Arrays.asList("pdf", "xls", "doc", "rtf", "csv");
    return allowedFormat.contains(format);
  }

  private File findReportFile(String report) {
    log.debug(REPORTS_PATH);
    File JDBC_rpt = new File(REPORTS_PATH + '/' + report + ".JDBC.rpt");
    File original_rpt = new File(REPORTS_PATH + '/' + report + ".rpt");

    // If the original .rpt is newer than the cached .JDBC.rpt, the original
    // report was modified after the JDBC version was generated. Delete the
    // stale JDBC file so changeDataSource() regenerates it with fresh content.
    if (JDBC_rpt.exists() && original_rpt.exists()
        && original_rpt.lastModified() > JDBC_rpt.lastModified()) {
      log.info("Original report newer than JDBC cache, deleting stale cache: {}", JDBC_rpt.getName());
      if (!JDBC_rpt.delete()) {
        log.warn("Failed to delete stale JDBC report cache: {}", JDBC_rpt.getPath());
      }
      return original_rpt;
    }

    if (JDBC_rpt.exists()) {
      log.debug("Using report: {}", JDBC_rpt.getPath());
      return JDBC_rpt;
    }
    return original_rpt.exists() ? original_rpt : null;
  }

  private ReportParams extractQueryParams(RoutingContext context) {
    ReportParams params = new ReportParams();
    var reqParams = context.request().params();
    for (String key : reqParams.names()) {
      String value = reqParams.get(key);
      log.debug("Request Parameter: {} ; Value: {}", key, value);
      if ("FILENAME".equalsIgnoreCase(key)) {
        params.reportFileName = value;
      }
      if ("AUTHOR".equalsIgnoreCase(key)) {
        params.author = value;
      }
    }
    return params;
  }

  private void processReport(ReportClientDocument clientDoc, RoutingContext context,
      String report, String format, String author, String reportFileName) {
    var resp = context.response();
    var reqParams = context.request().params();

    context.vertx().executeBlocking(() -> {
      try {
        setDatabaseConnection(clientDoc, report);
        setReportParameters(clientDoc, reqParams);
        setSummaryInfo(clientDoc, author, reportFileName);
        resp.setChunked(true);
        exportReport(clientDoc, resp, format);
        return null;
      } finally {
        closeClientDoc(clientDoc, report);
      }
    }, false).onFailure(e -> {
      log.error("Error processing report", e);
      if (!resp.closed()) {
        resp.setStatusCode(500).end("<H1>Server Error!</H1><br>" + e.getMessage());
      }
    });
  }

  private void setDatabaseConnection(ReportClientDocument clientDoc, String report)
      throws ReportSDKExceptionBase {
    boolean isSameDataSource = CRJavaHelper.isSameDataSource(clientDoc, JDBC_URL, DRIVER_CLASS_NAME, JNDI_NAME);
    if (!isSameDataSource) {
      CRJavaHelper.changeDataSource(clientDoc, USER, PASSWORD, JDBC_URL, DRIVER_CLASS_NAME, JNDI_NAME, report,
          REPORTS_PATH);
    }
    boolean useJNDI = JNDI_NAME != null && !JNDI_NAME.isEmpty();
    if (!useJNDI) {
      CRJavaHelper.logonDataSource(clientDoc, USER, PASSWORD);
    }
  }

  private void setReportParameters(ReportClientDocument clientDoc, MultiMap reqParams)
      throws ReportSDKExceptionBase {
    List<String> reportParams = CRJavaHelper.getTopParams(clientDoc);
    for (String param : reportParams) {
      log.debug("Report Parameter: {}", param);
      if (!reqParams.contains(param)) {
        log.warn("Request Parameter: {} missing", param);
        throw new IllegalArgumentException("Report param [" + param + "] not found in request!");
      }
      String value = reqParams.getAll(param).get(0);
      if (value != null && !value.isEmpty()) {
        CRJavaHelper.setTopParameter(clientDoc, param, value);
      }
    }
  }

  private void setSummaryInfo(ReportClientDocument clientDoc, String author, String title)
      throws ReportSDKExceptionBase {
    ISummaryInfo summaryInfo = new SummaryInfo();
    summaryInfo.setAuthor(author);
    summaryInfo.setTitle(title);
    clientDoc.setSummaryInfo(summaryInfo);
  }

  private void exportReport(ReportClientDocument clientDoc, HttpServerResponse resp, String format)
      throws ReportSDKExceptionBase, IOException {
    switch (format) {
      case "pdf":
        CRJavaHelper.exportPDF(clientDoc, resp, false);
        break;
      case "doc":
        CRJavaHelper.exportMSWord(clientDoc, resp, false);
        break;
      case "rtf":
        CRJavaHelper.exportRTF(clientDoc, resp, false);
        break;
      case "csv":
        CRJavaHelper.exportCSV(clientDoc, resp, false);
        break;
    }
  }

  private void closeClientDoc(ReportClientDocument clientDoc, String report) {
    try {
      log.debug("Closing report {}", report);
      clientDoc.close();
    } catch (ReportSDKExceptionBase e) {
      log.error("Error closing report document", e);
    } finally {
      JNDIManager.releaseConnections();
    }
  }

  private static class ReportParams {
    String reportFileName = "";
    String author = "Crystal Report Server Java";
  }
}