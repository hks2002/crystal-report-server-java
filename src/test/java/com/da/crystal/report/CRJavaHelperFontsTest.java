/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-11-13 00:00:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-13 11:08:29                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;
import com.da.crystal.report.CR.CRJavaHelper;

/**
 * Prints the distinct fonts used by an existing report, including subreports.
 */
class CRJavaHelperFontsTest {

  @Test
  void collectFontsTest() throws ReportSDKExceptionBase {
    File reportFile = new File("reports/TEST.rpt");
    ReportClientDocument clientDoc = ReportClientDocument.openReport(reportFile);
    try {
      java.util.Set<String> fontNames = CRJavaHelper.collectFontNames(clientDoc);
      System.out.println("=== fonts used by " + reportFile.getName() + " ===");
      for (String name : fontNames) {
        System.out.println("  " + name);
      }
      System.out.println("=== total fonts: " + fontNames.size() + " ===");
      assertTrue(!fontNames.isEmpty());
    } finally {
      clientDoc.close();
    }
  }
}