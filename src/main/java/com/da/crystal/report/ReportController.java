/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2023-03-06 21:22:42                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2023-04-08 16:39:44                                                                      *
 * @FilePath              : src/main/java/com/da/crystal/report/ReportController.java                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.crystal.report;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.document.ISummaryInfo;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class ReportController {

    private static final Logger log = LogManager.getLogger();
    ReportClientDocument rptDoc = new ReportClientDocument();

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driverClassName}")
    private String driverClassName;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @GetMapping("/Report/*/*")
    public void handReportRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String report = req.getServletPath().split("/")[2];
            String format = req.getServletPath().split("/")[3];
            log.debug("Report: " + report + " Format: " + format);

            // Check format
            List<String> allowedFormat = Arrays.asList("pdf", "xls", "doc", "rtf", "csv");
            if (!allowedFormat.contains(format)) {
                resp.getWriter().write("<H1>Document format {" + format + "} is not supported!</H1>");
                return;
            }

            // Check report template file
            String reportsPath =
                Thread.currentThread().getContextClassLoader().getResource("").getPath() + "../reports/";
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
                log.debug("Request Parameter: {} ; Value: {}", key, value);

                if (key.toUpperCase().equals("FILENAME")) {
                    reportNO = value;
                }
                if (key.toUpperCase().equals("AUTHOR")) {
                    author = value;
                }
            }

            // open report
            rptDoc = ReportClientDocument.openReport(file);

            // set Database connection
            CRJavaHelper.changeDataSource(rptDoc, username, password, url, driverClassName, "");

            // Check/Set report param
            List<String> reportParams = CRJavaHelper.getTopParams(rptDoc);
            for (String param : reportParams) {
                log.debug("Report Parameter: {}", param);
                if (!reqParams.containsKey(param)) {
                    log.warn("Request Parameter: {} missing", param);
                    resp.getWriter().write("<H1>Report param [" + param + "] not found in request!</H1>");
                    return;
                } else {
                    String value = reqParams.get(param)[0];

                    // ⚠️❗❗❗ top level parameter only, make sure all parameters are top level
                    if (value != null && !value.isEmpty()) {
                        CRJavaHelper.setParameterValue(rptDoc, param, value);
                    }
                }
            }

            // set summary info
            ISummaryInfo summaryInfo = new com.crystaldecisions.sdk.occa.report.document.SummaryInfo();
            summaryInfo.setAuthor(author);
            summaryInfo.setTitle(reportNO);
            rptDoc.setSummaryInfo(summaryInfo);

            // export report
            switch (format) {
                case "pdf":
                    CRJavaHelper.exportPDF(rptDoc, resp, false);
                    break;
                case "doc":
                    CRJavaHelper.exportMSWord(rptDoc, resp, false);
                    break;
                case "rtf":
                    CRJavaHelper.exportRTF(rptDoc, resp, false);
                    break;
                case "csv":
                    CRJavaHelper.exportCSV(rptDoc, resp, false);
                    break;
                default:
                    break;
            }
        } catch (ReportSDKExceptionBase e) {
            resp.getWriter().write("<H1>Server Error!</H1><br>" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                rptDoc.close();
            } catch (ReportSDKExceptionBase e) {
                /* ignore */
            }
        }
    }
}
