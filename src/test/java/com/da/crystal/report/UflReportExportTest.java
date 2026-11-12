/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-11-12 11:30:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 13:54:44                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.exportoptions.ExportOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.PDFExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.ReportExportFormat;
import com.da.crystal.report.JNDI.JNDIManager;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

/**
 * Verifies that the UFL.rpt report can be loaded and exported to PDF without
 * errors, exercising the Java UFL function registration path.
 */
@Log4j2
class UflReportExportTest {

  private static final String REPORTS_PATH = "reports/";
  private static final String UFL_RPT = "UFL.rpt";

  @BeforeAll
  static void registerUfls() throws Exception {
    // Register Java UFL functions before opening any report so that the
    // Crystal Reports engine can resolve UFL function calls in UFL.rpt.
    // assertDoesNotThrow(() -> UflRegistrar.registerUFLs(),
    // "UFL registration should not throw exception");
    // log.info("UFL functions registered for UFL.rpt export test");

    // UFL functions such as TextOfChapter query the database through the
    // jdbc/crystal_report JNDI binding, so the datasource must be initialized
    // before the report is opened and exported.
    JsonObject config = new JsonObject(
        new String(Files.readAllBytes(Path.of("src/main/resources/config-dev.json"))));
    JNDIManager.init(config.getJsonObject("database"));
    log.info("JNDI DataSource initialized for UFL.rpt export test");
  }

  @Test
  @DisplayName("Load UFL.rpt and export to PDF without errors")
  void exportUflReportToPdf() throws Exception {
    File reportFile = new File(REPORTS_PATH, UFL_RPT);
    assertTrue(reportFile.exists(), "UFL.rpt should exist in reports/ directory: " + reportFile.getAbsolutePath());

    ReportClientDocument clientDoc = ReportClientDocument.openReport(reportFile);
    try {
      assertNotNull(clientDoc, "ReportClientDocument should not be null after opening UFL.rpt");

      PDFExportFormatOptions pdfOptions = new PDFExportFormatOptions();
      ExportOptions exportOptions = new ExportOptions();
      exportOptions.setExportFormatType(ReportExportFormat.PDF);
      exportOptions.setFormatOptions(pdfOptions);

      byte[] pdfBytes;
      try (InputStream is = new BufferedInputStream(
          clientDoc.getPrintOutputController().export(exportOptions));
          ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
          baos.write(buffer, 0, read);
        }
        pdfBytes = baos.toByteArray();
      }

      assertNotNull(pdfBytes, "PDF export output should not be null");
      assertTrue(pdfBytes.length > 0, "PDF export output should not be empty");

      // A valid PDF document always starts with the "%PDF" magic header.
      String header = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
      assertTrue(header.equals("%PDF"),
          "PDF output should start with %PDF header, got: " + header);

      log.info("UFL.rpt exported to PDF successfully, size={} bytes", pdfBytes.length);
    } finally {
      if (clientDoc != null) {
        try {
          clientDoc.close();
        } catch (Exception e) {
          log.warn("Failed to close ReportClientDocument: {}", e.getMessage());
        }
      }
    }
  }
}