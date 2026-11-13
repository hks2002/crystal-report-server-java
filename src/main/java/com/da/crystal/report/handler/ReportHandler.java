/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-13 11:10:36                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

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

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReportHandler implements Handler<RoutingContext> {
  ReportClientDocument clientDoc = new ReportClientDocument();

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
    log.debug(AppConfig.report_path);
    File JNDI_rpt = new File(AppConfig.report_path + '/' + report + ".JNDI.rpt");
    File original_rpt = new File(AppConfig.report_path + '/' + report + ".rpt");

    // If the original .rpt is newer than the cached .JNDI.rpt, the original
    // report was modified after the JNDI version was generated. Delete the
    // stale JNDI file so changeDataSource() regenerates it with fresh content.
    if (JNDI_rpt.exists() && original_rpt.exists() && original_rpt.lastModified() > JNDI_rpt.lastModified()) {
      log.info("Original report newer than JNDI cache, deleting stale cache: {}", JNDI_rpt.getName());
      if (!JNDI_rpt.delete()) {
        log.warn("Failed to delete stale JNDI report cache: {}", JNDI_rpt.getPath());
      }
      return original_rpt;
    }

    if (JNDI_rpt.exists()) {
      log.debug("Using cached report: {}", JNDI_rpt.getPath());
      return JNDI_rpt;
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
        File JNDI_rpt = new File(AppConfig.report_path + '/' + report + ".JNDI.rpt");
        if (!JNDI_rpt.exists()) {
          setDatabaseConnection(clientDoc, report);
        }
        setReportParameters(clientDoc, reqParams);

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
    if (!CRJavaHelper.isSameDataSource(clientDoc, AppConfig.dbConfig)) {
      CRJavaHelper.changeDataSource(clientDoc, AppConfig.dbConfig, report, AppConfig.report_path);
    }
    CRJavaHelper.logonDataSource(clientDoc, AppConfig.dbConfig.getUsername(), AppConfig.dbConfig.getPassword());
  }

  private void setReportParameters(ReportClientDocument clientDoc, MultiMap reqParams)
      throws ReportSDKExceptionBase {
    List<String> reportParams = CRJavaHelper.getTopParams(clientDoc);
    for (String param : reportParams) {
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