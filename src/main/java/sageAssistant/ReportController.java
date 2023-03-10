/*************************************************************************************************
 * @Author                : hks2002<56649783@qq.com>                                             *
 * @CreatedDate           : 2023-03-06 21:22:42                                                  *
 * @LastEditors           : hks2002<56649783@qq.com>                                             *
 * @LastEditDate          : 2023-03-10 10:10:39                                                  *
 * @FilePath              : rptSrv/src/main/java/sageAssistant/ReportController.java             *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                      *
 ************************************************************************************************/

package sageAssistant;

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

    ReportController() {
        log.debug("ReportController() is called!");
    }

    @GetMapping("/Report/*/*")
    public void handReportRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String report = req.getServletPath().split("/")[2];
            String format = req.getServletPath().split("/")[3];
            log.debug("Report: " + report + " Format: " + format);

            // Check format
            List<String> allowedFormat = Arrays.asList("pdf", "xls", "doc", "rtf", "csv");
            if (!allowedFormat.contains(format)) {
                resp.getWriter().write("<H1>Format {" + format + "} not supported!</H1>");
                return;
            }

            // Check report
            File file = new File("reports/" + report + ".rpt");
            if (!file.exists()) {
                resp.getWriter().write("<H1>Report template {" + report + "} not found!</H1>");
                return;
            }

            // open report
            rptDoc = ReportClientDocument.openReport(file);

            // set Database connection
            CRJavaHelper.changeDataSource(rptDoc, username, password, url, driverClassName, "");

            // set parameters
            String reportNO = "";
            var reqParams = req.getParameterMap();
            for (String key : reqParams.keySet()) {
                String value = reqParams.get(key)[0];
                log.debug("Parameter: " + key + "; Value: " + value);

                if (key.toLowerCase().equals("filename")) {
                    reportNO = value;
                }

                // ⚠️❗❗❗ top level parameter only, make sure all parameters are top level
                if (value != null && !value.isEmpty()) {
                    CRJavaHelper.setParameterValue(rptDoc, key, value);
                }
            }

            // set summary info
            ISummaryInfo summaryInfo = new com.crystaldecisions.sdk.occa.report.document.SummaryInfo();
            summaryInfo.setAuthor("Sage Assistant");
            if (reportNO.isEmpty()) {
                log.warn("Please provide a parameter named {FileName}, the value will be the file name.");
            }
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
            rptDoc.close();
        } catch (ReportSDKExceptionBase e) {
            resp.getWriter().write("<H1>Server Error!</H1><br>" + e.getMessage());
            e.printStackTrace();
        }
    }
}
