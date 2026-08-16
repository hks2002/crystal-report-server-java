/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-16 11:51:49                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-16 18:06:42                                *
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
import com.da.crystal.report.AppConfig;
import com.da.crystal.report.CR.CRJavaHelper;
import com.da.crystal.report.JNDI.DataSourceConfig;

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
  DataSourceConfig dbConfig;

  ReportClientDocument clientDoc = new ReportClientDocument();

  public ReportHandler() {
    JsonObject reportConfig = AppConfig.config.getJsonObject("handler").getJsonObject("report");
    JsonObject databaseConfig = AppConfig.config.getJsonObject("database");

    this.REPORTS_PATH = Utils.isWindows()
        ? reportConfig.getJsonObject("windows").getString("reportsPath", REPORTS_PATH)
        : reportConfig.getJsonObject("linux").getString("reportsPath", REPORTS_PATH);
    this.dbConfig = new DataSourceConfig(
        databaseConfig.getString("url", "jdbc:mysql://localhost:3306/crystal_report"),
        databaseConfig.getString("driverClassName", "com.mysql.cj.jdbc.Driver"),
        databaseConfig.getString("jndiName", "jdbc/crystal_report"),
        databaseConfig.getString("user", "crystal_report"),
        databaseConfig.getString("password", "crystal_report"));
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

      // 2. Check report file exists
      File reportFile = findReportFile(report);
      if (reportFile == null) {
        resp.setStatusCode(404).end("<H1>Report template {" + report + "} not found!</H1>");
        return;
      }

      // 3. Copy report to temp file to avoid locking the original
      Path tempFile = Files.createTempFile("cr_", ".rpt");
      Files.copy(reportFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
      clientDoc = ReportClientDocument.openReport(tempFile.toFile());

      // 4. Process and export report
      processReport(clientDoc, context, report, format);

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

  private boolean isFormatAllowed(ReportClientDocument clientDoc, String format) {
    List<String> allowedFormat = Arrays.asList("pdf", "xls", "xlsx", "doc", "docx", "csv");
    return allowedFormat.contains(format);
  }

  private File findReportFile(String report) {
    log.debug(REPORTS_PATH);
    File JDBC_rpt = new File(REPORTS_PATH + '/' + report + ".JDBC.rpt");
    File original_rpt = new File(REPORTS_PATH + '/' + report + ".rpt");

    // If the original .rpt is newer than the cached .JDBC.rpt, the original
    // report was modified after the JDBC version was generated. Delete the
    // stale JDBC file so changeDataSource() regenerates it with fresh content.
    if (JDBC_rpt.exists() && original_rpt.exists() && original_rpt.lastModified() > JDBC_rpt.lastModified()) {
      log.info("Original report newer than JDBC cache, deleting stale cache: {}", JDBC_rpt.getName());
      if (!JDBC_rpt.delete()) {
        log.warn("Failed to delete stale JDBC report cache: {}", JDBC_rpt.getPath());
      }
      return original_rpt;
    }

    if (JDBC_rpt.exists()) {
      log.debug("Using cached report: {}", JDBC_rpt.getPath());
      return JDBC_rpt;
    }
    return original_rpt.exists() ? original_rpt : null;

  }

  private ReportInfo extractReportInfo(RoutingContext context) {
    ReportInfo info = new ReportInfo();
    var reqParams = context.request().params();
    for (String key : reqParams.names()) {
      String value = reqParams.get(key);
      log.debug("Request Parameter: {} ; Value: {}", key, value);
      if ("FILENAME".equalsIgnoreCase(key)) {
        info.reportFileName = value;
      }
      if ("AUTHOR".equalsIgnoreCase(key)) {
        info.author = value;
      }
    }
    return info;
  }

  private void processReport(ReportClientDocument clientDoc, RoutingContext context,
      String report, String format) {
    var resp = context.response();
    var reqParams = context.request().params();

    ReportInfo info = extractReportInfo(context);
    String reportFileName = info.reportFileName;
    String author = info.author;

    if (!isFormatAllowed(clientDoc, format)) {
      resp.setStatusCode(200).end("<H1>Document format {" + format + "} is not supported!</H1>");
      return;
    }

    context.vertx().executeBlocking(() -> {
      try {
        setDatabaseConnection(clientDoc, report);
        setReportParameters(clientDoc, reqParams);

        if (log.isDebugEnabled()) {
          logSelectionFormula(clientDoc, "");
        }

        setSummaryInfo(clientDoc, author, reportFileName);
        resp.setChunked(true);
        exportReport(clientDoc, resp, format, reportFileName);
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
    if (!CRJavaHelper.isSameDataSource(clientDoc, dbConfig)) {
      CRJavaHelper.changeDataSource(clientDoc, dbConfig, report, REPORTS_PATH);
    }
    CRJavaHelper.logonDataSource(clientDoc, dbConfig.getUsername(), dbConfig.getPassword());
  }

  private void logSelectionFormula(ReportClientDocument clientDoc, String phase) {
    try {
      Object dataDef = clientDoc.getDataDefController().getDataDefinition();
      Object recordFilter = dataDef.getClass().getMethod("getRecordFilter").invoke(dataDef);
      if (recordFilter == null) {
        log.debug("Selection formula {}: <null>", phase);
        return;
      }
      String formula = (String) recordFilter.getClass()
          .getMethod("getFreeEditingText").invoke(recordFilter);
      log.debug("Selection formula {}: \n{}", phase, formula);
      if (formula != null && formula.contains(":=")) {
        log.warn(
            "Too complex formula will cause performance issue!, [WHERE] clause will probably not generated in the SQL statements");
      }

    } catch (Exception e) {
      log.warn("Selection formula {}: <error: {}>", phase, e.getMessage());
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

  private void exportReport(ReportClientDocument clientDoc, HttpServerResponse resp, String format, String fileName)
      throws ReportSDKExceptionBase, IOException {
    switch (format) {
      case "pdf":
        CRJavaHelper.exportPDF(clientDoc, resp, fileName.isEmpty() ? false : true);
        break;
      case "rtf":
      case "doc":
      case "docx":
        CRJavaHelper.exportMSWord(clientDoc, resp, fileName.isEmpty() ? false : true);
        break;
      case "xls":
      case "xlsx":
        CRJavaHelper.exportExcel(clientDoc, resp, fileName.isEmpty() ? false : true);
        break;
      case "csv":
        CRJavaHelper.exportCSV(clientDoc, resp, fileName.isEmpty() ? false : true);
        break;
    }
  }

  private void closeClientDoc(ReportClientDocument clientDoc, String report) {
    try {
      log.debug("Closing report {}", report);
      clientDoc.close();
    } catch (ReportSDKExceptionBase e) {
      log.error("Error closing report document", e);
    }
  }

  private static class ReportInfo {
    String reportFileName = "";
    String author = "Crystal Report Server Java";
  }
}