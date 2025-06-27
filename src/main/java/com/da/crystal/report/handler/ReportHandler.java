/******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                     *
 * @CreatedDate           : 2025-03-16 11:51:49                               *
 * @LastEditors           : Robert Huang<56649783@qq.com>                     *
 * @LastEditDate          : 2025-06-26 09:10:33                               *
 * @CopyRight             : Dedienne Aerospace Zhuhai of China                *
 *****************************************************************************/

package com.da.crystal.report.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.document.ISummaryInfo;
import com.crystaldecisions.sdk.occa.report.document.SummaryInfo;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;
import com.da.crystal.report.CRJavaHelper;

import io.vertx.core.Handler;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReportHandler implements Handler<RoutingContext> {
  String reportsPath = "/usr/share/java/crystal-report-server/reports";
  String url = "jdbc:mysql://localhost:3306/crystal_report";
  String driverClassName = "com.mysql.cj.jdbc.Driver";
  String user = "crystal_report";
  String password = "crystal_report";
  String jndiName = "java:comp/env/jdbc/crystal_report";

  ReportClientDocument clientDoc = new ReportClientDocument();

  public ReportHandler(JsonObject config) {
    JsonObject reportConfig = config.getJsonObject("report");

    this.reportsPath = Utils.isWindows()
        ? reportConfig.getJsonObject("windows").getString("reportsPath", reportsPath)
        : reportConfig.getJsonObject("linux").getString("reportsPath", reportsPath);
    this.url = reportConfig.getString("url", url);
    this.driverClassName = reportConfig.getString("driverClassName", driverClassName);
    this.user = reportConfig.getString("user", user);
    this.password = reportConfig.getString("password", password);
  }

  @Override
  public void handle(RoutingContext context) {
    var req = context.request();
    var resp = context.response();

    try {
      String params[] = req.uri().split("/");
      if (params.length != 4) {
        log.info("Wrong request url:{}", req.uri());
        resp.setStatusCode(400).end("<H1>Wrong request url!</H1>");
        return;
      }

      String report = params[2];
      String format = params[3].split("\\?")[0].toLowerCase();
      log.debug("Report: " + report + " Format: " + format);

      // Check format, it doesn't list all formats supported by Crystal Reports,
      // just listed we want
      List<String> allowedFormat = Arrays.asList("pdf", "xls", "doc", "rtf", "csv");
      if (!allowedFormat.contains(format)) {
        resp.setStatusCode(200).end("<H1>Document format {" + format + "} is not supported!</H1>");
        return;
      }

      log.debug(reportsPath);
      File file = new File(reportsPath + '/' + report + ".rpt");
      if (!file.exists()) {
        resp.setStatusCode(404).end("<H1>Report template {" + report + "} not found!</H1>");
        return;
      }

      // Set additions parameters
      String reportNO = "";
      String author = "Crystal Report Server Java";
      var reqParams = req.params();
      for (String key : reqParams.names()) {
        String value = reqParams.get(key);
        log.debug("Request Parameter: {} ; Value: {}", key, value);

        if (key.toUpperCase().equals("FILENAME")) {
          reportNO = value;
        }
        if (key.toUpperCase().equals("AUTHOR")) {
          author = value;
        }
      }

      // open report
      clientDoc = ReportClientDocument.openReport(file);

      // set Database connection
      CRJavaHelper.changeDataSource(
          clientDoc,
          user,
          password,
          url,
          driverClassName,
          report + ".rpt",
          reportsPath);

      // Check/Set report param
      List<String> reportParams = CRJavaHelper.getTopParams(clientDoc);
      for (String param : reportParams) {
        log.debug("Report Parameter: {}", param);
        if (!reqParams.contains(param)) {
          log.warn("Request Parameter: {} missing", param);
          resp.end("<H1>Report param [" + param + "] not found in request!</H1>");
          return;
        } else {
          String value = reqParams.getAll(param).get(0);

          // ⚠️❗❗❗ top level parameter only, make sure all parameters are top
          // level
          if (value != null && !value.isEmpty()) {
            CRJavaHelper.setTopParameter(clientDoc, param, value);
          }
        }
      }

      // set summary info
      ISummaryInfo summaryInfo = new SummaryInfo();
      summaryInfo.setAuthor(author);
      summaryInfo.setTitle(reportNO);
      clientDoc.setSummaryInfo(summaryInfo);

      // export report
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
        default:
          break;
      }
    } catch (ReportSDKExceptionBase | IOException e) {
      resp.setStatusCode(500).end("<H1>Server Error!</H1><br>" + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        clientDoc.close();
      } catch (ReportSDKExceptionBase e) {
        /* ignore */
      }
    }
  }
}
