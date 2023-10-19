/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2023-03-06 21:22:42                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2023-10-19 14:16:56                                                                       *
 * @FilePath              : src/main/java/com/da/crystal/report/ReportController.java                                 *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.crystal.report;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.document.ISummaryInfo;
import com.crystaldecisions.sdk.occa.report.document.SummaryInfo;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class ReportController {

    ReportClientDocument clientDoc = new ReportClientDocument();

    @Value("${rpt.datasource.url}")
    private String url;

    @Value("${rpt.datasource.driverClassName}")
    private String driverClassName;

    @Value("${rpt.datasource.username}")
    private String username;

    @Value("${rpt.datasource.password}")
    private String password;

    @GetMapping("/Report/*/*")
    public void handReportRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String params[] = req.getServletPath().split("/");
            if (params.length != 4) {
                log.info("Wrong request url:{}", req.getServletPath());
                resp.getWriter().write("<H1>Wrong request url!</H1>");
                return;
            }

            String report = params[2];
            String format = params[3];
            if (log.isDebugEnabled()) {
                log.debug("Report: " + report + " Format: " + format);
            }
            // Check format, it doesn't list all formats supported by Crystal Reports, just listed we want
            List<String> allowedFormat = Arrays.asList("pdf", "xls", "doc", "rtf", "csv");
            if (!allowedFormat.contains(format)) {
                resp.getWriter().write("<H1>Document format {" + format + "} is not supported!</H1>");
                return;
            }

            // Check report template file
            String reportsPath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "reports/";
            log.debug(reportsPath);
            File file = new File(reportsPath + report + ".rpt");
            if (!file.exists()) {
                resp.getWriter().write("<H1>Report template {" + report + "} not found!</H1>");
                return;
            }

            // Set additions parameters
            String reportNO = "";
            String author = "Crystal Report Server Java";
            var reqParams = req.getParameterMap();
            for (String key : reqParams.keySet()) {
                String value = reqParams.get(key)[0];
                if (log.isDebugEnabled()) {
                    log.debug("Request Parameter: {} ; Value: {}", key, value);
                }

                if (key.toUpperCase().equals("FILENAME")) {
                    reportNO = value;
                }
                if (key.toUpperCase().equals("AUTHOR")) {
                    author = value;
                }
            }

            // open report
            clientDoc = ReportClientDocument.openReport(file);
            
            // Check/Set report param
            List<String> reportParams = CRJavaHelper.getTopParams(clientDoc);
            for (String param : reportParams) {
                if (log.isDebugEnabled()) {
                    log.debug("Report Parameter: {}", param);
                }
                if (!reqParams.containsKey(param)) {
                    log.warn("Request Parameter: {} missing", param);
                    resp.getWriter().write("<H1>Report param [" + param + "] not found in request!</H1>");
                    return;
                } else {
                    String value = reqParams.get(param)[0];

                    // ⚠️❗❗❗ top level parameter only, make sure all parameters are top level
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

            // set Database connection
            CRJavaHelper.changeDataSource(clientDoc, username, password, url, driverClassName, report + ".rpt", reportsPath);
           
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
        } catch (ReportSDKExceptionBase e) {
            resp.getWriter().write("<H1>Server Error!</H1><br>" + e.getMessage());
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
