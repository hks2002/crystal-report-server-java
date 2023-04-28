/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2023-04-12 19:43:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2023-04-28 10:39:12                                                                      *
 * @FilePath              : src/test/java/com/da/crystal/report/ReportFunctionTests.java                             *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.crystal.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.data.Fields;
import com.crystaldecisions.sdk.occa.report.data.IField;
import com.crystaldecisions.sdk.occa.report.data.TableJoins;
import com.crystaldecisions.sdk.occa.report.definition.IParagraph;
import com.crystaldecisions.sdk.occa.report.definition.IParagraphElement;
import com.crystaldecisions.sdk.occa.report.definition.IReportObject;
import com.crystaldecisions.sdk.occa.report.definition.ParagraphElements;
import com.crystaldecisions.sdk.occa.report.definition.Paragraphs;
import com.crystaldecisions.sdk.occa.report.definition.ReportObjects;
import com.crystaldecisions.sdk.occa.report.document.ISummaryInfo;
import com.crystaldecisions.sdk.occa.report.lib.ReportObjectKind;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class ReportFunctionTests {

    private ReportClientDocument clientDoc = null;
    String reportsPath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "../reports/";

    @Test
    void saveRptTest() throws ReportSDKException, IOException {
        File file = new File(reportsPath + "TEST.rpt");
        clientDoc = ReportClientDocument.openReport(file);

        // set summary info
        ISummaryInfo summaryInfo = new com.crystaldecisions.sdk.occa.report.document.SummaryInfo();
        summaryInfo.setAuthor("Author");
        summaryInfo.setTitle("Title");
        clientDoc.setSummaryInfo(summaryInfo);

        clientDoc.saveAs("Modified.rpt", reportsPath, 1); // 1 over write

        file = new File(reportsPath + "Modified.rpt"); // open it to verify
        clientDoc = ReportClientDocument.openReport(file);

        summaryInfo = clientDoc.getReportDocument().getSummaryInfo();

        assertEquals("Author", summaryInfo.getAuthor());
        assertEquals("Title", summaryInfo.getTitle());

        clientDoc.close();
    }

    @Test
    void getResultFieldTest() throws ReportSDKException, IOException {
        File file = new File(reportsPath + "TEST.rpt");
        ReportClientDocument clientDoc = ReportClientDocument.openReport(file);

        Fields<IField> fields = clientDoc.getDataDefinition().getResultFields();
        for (var field : fields) {
            log.debug("{} : {}", field.getName(), field.getLongName(null));
        }

        clientDoc.close();
    }

    @Test
    void getTableLinksTest() throws ReportSDKException, IOException {
        File file = new File(reportsPath + "TEST.rpt");
        ReportClientDocument clientDoc = ReportClientDocument.openReport(file);

        TableJoins tableJoins = clientDoc.getDatabase().getTableJoins();
        for (var tableJoin : tableJoins) {
            for (var fieldLink : tableJoin.getFieldLinks()) log.debug(
                "{} {} {} {} {} {}",
                tableJoin.getSourceTableAlias(),
                tableJoin.getJoinOperator().toString(),
                tableJoin.getTargetTableAlias(),
                fieldLink.getFromField().getLongName(null),
                fieldLink.getLinkOperator().toString(),
                fieldLink.getToField().getLongName(null)
            );
        }

        clientDoc.close();
    }

    @Test
    void getReportTextTest()
        throws ReportSDKException, IOException, NoSuchFieldException, NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        File file = new File(reportsPath + "TEST.rpt");
        ReportClientDocument clientDoc = ReportClientDocument.openReport(file);

        ReportObjects rptObjects = clientDoc.getReportDefController().getReportObjectController().getAllReportObjects();
        for (IReportObject obj : rptObjects) {
            if (obj.getKind().equals(ReportObjectKind.text)) {
                Class clazzObjectText = obj.getClass();
                Method getParagraphs = clazzObjectText.getDeclaredMethod("getParagraphs");
                Paragraphs paragraphs = (Paragraphs) getParagraphs.invoke(obj);

                for (IParagraph paragraph : paragraphs) {
                    Class clazzParagraph = paragraph.getClass();
                    Method getParagraphElements = clazzParagraph.getDeclaredMethod("getParagraphElements");
                    ParagraphElements paragraphElements = (ParagraphElements) getParagraphElements.invoke(paragraph);

                    for (IParagraphElement paragraphElement : paragraphElements) {
                        Class clazzParagraphElement = paragraphElement.getClass();
                        Method[] methods = clazzParagraphElement.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.getName().equals("getText")) {
                                String text = (String) method.invoke(paragraphElement);
                                log.debug("Text: {}", text);
                            }
                        }
                    }
                }
            }
        }

        clientDoc.close();
    }

    @Test
    void setReportTextTest()
        throws ReportSDKException, IOException, NoSuchFieldException, NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        File file = new File(reportsPath + "TEST.rpt");
        ReportClientDocument clientDoc = ReportClientDocument.openReport(file);

        ReportObjects rptObjects = clientDoc.getReportDefController().getReportObjectController().getAllReportObjects();
        for (IReportObject obj : rptObjects) {
            if (obj.getKind().equals(ReportObjectKind.text)) {
                Class clazzObjectText = obj.getClass();
                Method getParagraphs = clazzObjectText.getDeclaredMethod("getParagraphs");
                Paragraphs paragraphs = (Paragraphs) getParagraphs.invoke(obj);

                for (IParagraph paragraph : paragraphs) {
                    Class clazzParagraph = paragraph.getClass();
                    Method getParagraphElements = clazzParagraph.getDeclaredMethod("getParagraphElements");
                    ParagraphElements paragraphElements = (ParagraphElements) getParagraphElements.invoke(paragraph);

                    for (IParagraphElement paragraphElement : paragraphElements) {
                        Class clazzParagraphElement = paragraphElement.getClass();
                        Method[] methods = clazzParagraphElement.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.getName().equals("setText")) { // didn't work
                                method.invoke(paragraphElement, "TEST");
                            }
                        }
                    }
                }
            }
        }
        clientDoc.saveAs("Modified.rpt", reportsPath, 1);
        clientDoc.close();
    }
}
