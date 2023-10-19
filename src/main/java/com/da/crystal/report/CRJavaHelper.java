/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2023-03-07 00:03:27                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2023-10-19 10:46:00                                                                       *
 * @FilePath              : src/main/java/com/da/crystal/report/CRJavaHelper.java                                     *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

/**
 * This sample code is an example of how to use the Business Objects APIs.
 * Because the sample code is designed for demonstration only, it is
 * unsupported.  You are free to modify and distribute the sample code as needed.
 */
package com.da.crystal.report;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.crystaldecisions.sdk.occa.report.application.DataDefController;
import com.crystaldecisions.sdk.occa.report.application.ParameterFieldController;
import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.data.FieldDisplayNameType;
import com.crystaldecisions.sdk.occa.report.data.Fields;
import com.crystaldecisions.sdk.occa.report.data.IConnectionInfo;
import com.crystaldecisions.sdk.occa.report.data.IParameterField;
import com.crystaldecisions.sdk.occa.report.data.ITable;
import com.crystaldecisions.sdk.occa.report.data.ParameterField;
import com.crystaldecisions.sdk.occa.report.data.ParameterFieldDiscreteValue;
import com.crystaldecisions.sdk.occa.report.data.ParameterFieldRangeValue;
import com.crystaldecisions.sdk.occa.report.data.RangeValueBoundType;
import com.crystaldecisions.sdk.occa.report.data.Tables;
import com.crystaldecisions.sdk.occa.report.data.Values;
import com.crystaldecisions.sdk.occa.report.document.PaperSize;
import com.crystaldecisions.sdk.occa.report.document.PaperSource;
import com.crystaldecisions.sdk.occa.report.document.PrintReportOptions;
import com.crystaldecisions.sdk.occa.report.document.PrinterDuplex;
import com.crystaldecisions.sdk.occa.report.exportoptions.CharacterSeparatedValuesExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.DataOnlyExcelExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.EditableRTFExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.ExportOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.PDFExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.RTFWordExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.ReportExportFormat;
import com.crystaldecisions.sdk.occa.report.lib.IStrings;
import com.crystaldecisions.sdk.occa.report.lib.PropertyBag;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Crystal Reports Java Helper Sample ************************ Please note that
 * you need to define a runtime server in order for this class to compile.
 * ************************
 *
 * @author Business Objects
 */
@Slf4j
public class CRJavaHelper {

    /**
     * Logs on to all existing datasource
     *
     * @param clientDoc The reportClientDocument representing the report being used
     * @param username  The DB logon user name
     * @param password  The DB logon password
     * @throws ReportSDKException
     */
    public static void logonDataSource(ReportClientDocument clientDoc, String username, String password)
        throws ReportSDKException {
        clientDoc.getDatabaseController().logon(username, password);
    }

    /**
     * Changes the DataSource for all table, include sub reports also.
     * If it doesn't need to change the DataSource, it will do logon directly,
     * If changed the DataSource, it will save the datasource to the report
     * ⚠️Suggest using Command sql instead of tables⚠️
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param username      The DB logon user name
     * @param password      The DB logon password
     * @param connectionURL The connection URL
     * @param driverName    The driver Name
     * @param fileName      The file name
     * @param filePath      The file path
     * // How to use a JNDI data source with the Crystal Reports Java SDK on Tomcat https://userapps.support.sap.com/sap/support/knowledge/en/1343290
     * @throws ReportSDKException
     * @throws IOException
     */
    public static void changeDataSource(
        ReportClientDocument clientDoc,
        String username,
        String password,
        String connectionURL,
        String driverName,
        String fileName,
        String filePath
    ) {
        try {
            // Set new table connection property attributes
            PropertyBag newPropertyBag = new PropertyBag();

            // Below is the list of values required to switch to use a JDBC/JNDI connection
            // How to use a JNDI data source with the Crystal Reports Java SDK on Tomcat
            // JNDI name for Crystal Report must start with 'jdbc/'
            // https://userapps.support.sap.com/sap/support/knowledge/en/1343290

            newPropertyBag.put("Server Type", "JDBC (JNDI)");
            newPropertyBag.put("Use JDBC", "true");
            newPropertyBag.put("Trusted_Connection", "false");
            newPropertyBag.put("Connection URL", connectionURL);
            newPropertyBag.put("Database Class Name", driverName);
            newPropertyBag.put("Database DLL", "crdb_jdbc.dll");

            // If same jdbc info, just do login
            IConnectionInfo oldConnectionInfo = clientDoc.getDatabaseController().getConnectionInfos(null).get(0);
            PropertyBag oldPropertyBag = oldConnectionInfo.getAttributes();
            if (
                oldPropertyBag.getStringValue("Server Type").equals("JDBC (JNDI)") &&
                oldPropertyBag.getStringValue("Connection URL").equals(connectionURL) &&
                oldPropertyBag.getStringValue("Database Class Name").equals(driverName)
            ) {
                logonDataSource(clientDoc, username, password);
                return;
            }

            Tables tables = null;
            ITable table = null;
            ITable newTable = null;

            // Obtain collection of tables from this database controller
            tables = clientDoc.getDatabaseController().getDatabase().getTables();
            for (int i = 0; i < tables.size(); i++) {
                table = tables.getTable(i);
                // ⚠️⚠️⚠️ Must Update the table with new table, It's seems a bug of Crystal Report
                newTable = changeDataSource(table, newPropertyBag, username, password);
                clientDoc.getDatabaseController().setTableLocation(table, newTable);
            }

            // Next loop through all the subReports.
            IStrings subReportNames = clientDoc.getSubreportController().getSubreportNames();
            for (int subNum = 0; subNum < subReportNames.size(); subNum++) {
                String subReportName = subReportNames.getString(subNum);

                tables =
                    clientDoc
                        .getSubreportController()
                        .getSubreport(subReportName)
                        .getDatabaseController()
                        .getDatabase()
                        .getTables();
                for (int i = 0; i < tables.size(); i++) {
                    table = tables.getTable(i);

                    // ⚠️⚠️⚠️ Must Update the table with new table, It's seems a bug of Crystal Report
                    newTable = changeDataSource(table, newPropertyBag, username, password);
                    clientDoc
                        .getSubreportController()
                        .getSubreport(subReportName)
                        .getDatabaseController()
                        .setTableLocation(table, newTable);
                }
            }

            // if modified, save it, so that could directly login, change data source is slowly.
            clientDoc.saveAs(fileName, filePath, 1);
        } catch (ReportSDKExceptionBase e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Changes the DataSource for table.
     *
     * @param table     The table need to changed
     * @param propertyBag   connection info properties
     * @param username      The DB logon user name
     * @param password      The DB logon password
     * @throws ReportSDKException
     */
    public static ITable changeDataSource(ITable table, PropertyBag propertyBag, String username, String password) {
        if (log.isDebugEnabled()) {
            log.debug("{} {} {}", table.getName(), table.getAlias(), table.getQualifiedName());
        }
        // We set the Fully qualified name to the Table Alias to keep the method generic
        // This workflow may not work in all scenarios and should likely be customized to work
        // in the developer's specific situation. The end result of this statement will be to strip
        // the existing table of it's db specific identifiers. For example
        // Xtreme.dbo.Customer becomes just Customer
        // table.setQualifiedName(table.getAlias());

        // Change properties that are different from the original datasource
        // For example, if the table name has changed you will be required to change it during this routine
        // table.setQualifiedName(TABLE_NAME_QUALIFIER);

        // Change connection information properties
        IConnectionInfo connectionInfo = table.getConnectionInfo();
        if (log.isDebugEnabled()) {
            log.debug(table.getName() + ":" + connectionInfo.getAttributes().toString());
        }
        connectionInfo.setAttributes(propertyBag);

        // Set database username and password
        // NOTE: Even if the username and password properties do not change
        // when switching databases, the database password is ⚠️*not*⚠️ saved in the report and must be set at
        // runtime if the database is secured.
        if (!propertyBag.containsKey("Connection Name (Optional)")) {
            connectionInfo.setUserName(username);
            connectionInfo.setPassword(password);
        }

        if (log.isDebugEnabled()) {
            log.debug(table.getName() + ":" + connectionInfo.getAttributes().toString());
        }
        // ⚠️⚠️⚠️ Must Update the table with new table, It's seems a bug of Crystal Report
        return (ITable) table.clone(true);
    }

    /**
     * Passes a populated java.sql.ResultSet object to a Table object
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param rs         The java.sql.ResultSet used to populate the Table
     * @param tableAlias The alias of the table
     * @param subReportName The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @throws ReportSDKException
     */
    public static void passResultSet(
        ReportClientDocument clientDoc,
        java.sql.ResultSet rs,
        String tableAlias,
        String subReportName
    ) throws ReportSDKException {
        if (subReportName == null || subReportName.isEmpty()) {
            clientDoc.getDatabaseController().setDataSource(rs, tableAlias, tableAlias);
        } else {
            clientDoc
                .getSubreportController()
                .getSubreport(subReportName)
                .getDatabaseController()
                .setDataSource(rs, tableAlias, tableAlias);
        }
    }

    /**
     * Passes a populated collection of a Java class to a Table object
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param dataSet    The java.sql.ResultSet used to populate the Table
     * @param className  The fully-qualified class name of the POJO objects being passed
     * @param tableAlias The alias of the table
     * @param subReportName The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @throws ReportSDKException
     */
    public static void passPOJO(
        ReportClientDocument clientDoc,
        @SuppressWarnings("rawtypes") Collection dataSet,
        String className,
        String tableAlias,
        String subReportName
    ) throws ReportSDKException, ClassNotFoundException {
        if (subReportName == null || subReportName.isEmpty()) {
            clientDoc.getDatabaseController().setDataSource(dataSet, Class.forName(className), tableAlias, tableAlias);
        } else {
            clientDoc
                .getSubreportController()
                .getSubreport(subReportName)
                .getDatabaseController()
                .setDataSource(dataSet, Class.forName(className), tableAlias, tableAlias);
        }
    }

    /**
     * Passes a populated collection of a Java class to a Table object, ⚠️⚠️⚠️ only one Table in the report
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param dataSet    The java.sql.ResultSet used to populate the Table
     * @param className  The fully-qualified class name of the POJO objects being passed
     * @param subReportName The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @throws ReportSDKException
     */
    public static void passPOJO(
        ReportClientDocument clientDoc,
        @SuppressWarnings("rawtypes") Collection dataSet,
        String className,
        String subReportName
    ) throws ReportSDKException, ClassNotFoundException {
        String tableAlias = null;
        if (subReportName == null || subReportName.isEmpty()) {
            tableAlias = clientDoc.getDatabase().getTables().get(0).getName();
            clientDoc.getDatabaseController().setDataSource(dataSet, Class.forName(className), tableAlias, tableAlias);
        } else {
            tableAlias =
                clientDoc
                    .getSubreportController()
                    .getSubreport(subReportName)
                    .getDatabaseController()
                    .getDatabase()
                    .getTables()
                    .get(0)
                    .getName();
            clientDoc
                .getSubreportController()
                .getSubreport(subReportName)
                .getDatabaseController()
                .setDataSource(dataSet, Class.forName(className), tableAlias, tableAlias);
        }
    }

    /**
     * Passes a single discrete parameter value to a report parameter
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param parameterName The name of the parameter
     * @throws ReportSDKException
     */
    public static boolean isParameterExists(ReportClientDocument clientDoc, String parameterName)
        throws ReportSDKException {
        Fields<IParameterField> parameterFields = clientDoc.getDataDefinition().getParameterFields();
        for (int i = 0; i < parameterFields.size(); i++) {
            IParameterField paramToChange = (IParameterField) parameterFields.getField(i);
            String field = paramToChange.getName();
            if (field.equals(parameterName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all top level parameters name.
     * @param clientDoc
     * @return List of parameters name
     * @throws ReportSDKException
     */
    public static List<String> getTopParams(ReportClientDocument clientDoc) throws ReportSDKException {
        List<String> topParams = new ArrayList<String>();
        Fields<IParameterField> parameterFields = clientDoc.getDataDefinition().getParameterFields();
        for (int i = 0; i < parameterFields.size(); i++) {
            IParameterField paramToChange = (IParameterField) parameterFields.getField(i);
            topParams.add(paramToChange.getName());
        }
        return topParams;
    }

    /**
     * Passes a single discrete parameter value to a report parameter,
     * ⚠️❗❗❗ top level parameter only, make sure all parameters are top level
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param parameterName The name of the parameter
     * @param newValue      The new value of the parameter
     * @throws ReportSDKException
     */
    public static void setTopParameter(ReportClientDocument clientDoc, String parameterName, Object newValue)
        throws ReportSDKException {
        ParameterFieldController paramFieldController = clientDoc.getDataDefController().getParameterFieldController();
        Fields<IParameterField> parameterFields = clientDoc.getDataDefinition().getParameterFields();

        for (int i = 0; i < parameterFields.size(); i++) {
            IParameterField paramField = (IParameterField) parameterFields.getField(i);
            String field = paramField.getName();

            if (field.equals(parameterName)) {
                paramFieldController.setCurrentValue("", parameterName, newValue);
            }
        }
    }

    /**
     * Passes a single discrete parameter value to a report parameter
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param reportName    The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName The name of the parameter
     * @param newValue      The new value of the parameter
     * @throws ReportSDKException
     */
    public static void setDiscreteParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object newValue
    ) throws ReportSDKException {
        DataDefController dataDefController = null;
        if (reportName == null || reportName.isEmpty()) {
            dataDefController = clientDoc.getDataDefController();
        } else {
            dataDefController = clientDoc.getSubreportController().getSubreport(reportName).getDataDefController();
        }

        ParameterFieldDiscreteValue newDiscValue = new ParameterFieldDiscreteValue();
        newDiscValue.setValue(newValue);

        ParameterField paramField = (ParameterField) dataDefController
            .getDataDefinition()
            .getParameterFields()
            .findField(parameterName, FieldDisplayNameType.fieldName, Locale.getDefault());
        boolean multiValue = paramField.getAllowMultiValue();

        if (multiValue) {
            Values newVals = (Values) paramField.getCurrentValues().clone(true);
            newVals.add(newDiscValue);
            clientDoc
                .getDataDefController()
                .getParameterFieldController()
                .setCurrentValue(reportName, parameterName, newVals);
        } else {
            clientDoc
                .getDataDefController()
                .getParameterFieldController()
                .setCurrentValue(reportName, parameterName, newValue);
        }
    }

    /**
     * Passes multiple discrete parameter values to a report parameter
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param reportName    The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName The name of the parameter
     * @param newValues     An array of new values to get set on the parameter
     * @throws ReportSDKException
     */
    public static void setDiscreteParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object[] newValues
    ) throws ReportSDKException {
        clientDoc
            .getDataDefController()
            .getParameterFieldController()
            .setCurrentValues(reportName, parameterName, newValues);
    }

    /**
     * Passes a single range parameter value to a report parameter. The range is
     * assumed to be inclusive on beginning and end.
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param reportName    The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName The name of the parameter
     * @param beginValue    The value of the beginning of the range
     * @param endValue      The value of the end of the range
     * @throws ReportSDKException
     */
    public static void setRangeParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object beginValue,
        Object endValue
    ) throws ReportSDKException {
        setRangeParameterValue(
            clientDoc,
            reportName,
            parameterName,
            beginValue,
            RangeValueBoundType.inclusive,
            endValue,
            RangeValueBoundType.inclusive
        );
    }

    /**
     * Passes multiple range parameter values to a report parameter.
     *
     * This overload of the addRangeParameterValue will only work if the parameter
     * is setup to accept multiple values.
     *
     * If the Parameter does not accept multiple values then it is expected that
     * this version of the method will return an error
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param reportName    The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName The name of the parameter
     * @param beginValues   Array of beginning values. Must be same length as endValues.
     * @param endValues     Array of ending values. Must be same length as beginValues.
     * @throws ReportSDKException
     */
    public static void setRangeParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object[] beginValues,
        Object[] endValues
    ) throws ReportSDKException {
        setRangeParameterValue(
            clientDoc,
            reportName,
            parameterName,
            beginValues,
            RangeValueBoundType.inclusive,
            endValues,
            RangeValueBoundType.inclusive
        );
    }

    /**
     * Passes a single range parameter value to a report parameter
     *
     * @param clientDoc      The reportClientDocument representing the report being used
     * @param reportName     The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName  The name of the parameter
     * @param beginValue     The value of the beginning of the range
     * @param lowerBoundType The inclusion/exclusion range of the start of range.
     * @param endValue       The value of the end of the range
     * @param upperBoundType The inclusion/exclusion range of the end of range.
     * @throws ReportSDKException
     */
    public static void setRangeParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object beginValue,
        RangeValueBoundType lowerBoundType,
        Object endValue,
        RangeValueBoundType upperBoundType
    ) throws ReportSDKException {
        DataDefController dataDefController = null;
        if (reportName == null || reportName.isEmpty()) {
            dataDefController = clientDoc.getDataDefController();
        } else {
            dataDefController = clientDoc.getSubreportController().getSubreport(reportName).getDataDefController();
        }

        ParameterFieldRangeValue newRangeValue = new ParameterFieldRangeValue();
        newRangeValue.setBeginValue(beginValue);
        newRangeValue.setLowerBoundType(lowerBoundType);
        newRangeValue.setEndValue(endValue);
        newRangeValue.setUpperBoundType(upperBoundType);

        ParameterField paramField = (ParameterField) dataDefController
            .getDataDefinition()
            .getParameterFields()
            .findField(parameterName, FieldDisplayNameType.fieldName, Locale.getDefault());
        boolean multiValue = paramField.getAllowMultiValue();

        if (multiValue) {
            Values newVals = (Values) paramField.getCurrentValues().clone(true);
            newVals.add(newRangeValue);
            clientDoc
                .getDataDefController()
                .getParameterFieldController()
                .setCurrentValue(reportName, parameterName, newVals);
        } else {
            clientDoc
                .getDataDefController()
                .getParameterFieldController()
                .setCurrentValue(reportName, parameterName, newRangeValue);
        }
    }

    /**
     * Passes multiple range parameter values to a report parameter.
     *
     * This overload of the setRangeParameterValue will only work if the parameter
     * is setup to accept multiple values.
     *
     * If the Parameter does not accept multiple values then it is expected that
     * this version of the method will return an error
     *
     * @param clientDoc      The reportClientDocument representing the report being used
     * @param reportName     The name of the subReport. If tables in the main report is to be used, "" should be passed
     * @param parameterName  The name of the parameter
     * @param beginValues    Array of beginning values. Must be same length as endValues.
     * @param lowerBoundType The inclusion/exclusion range of the start of range.
     * @param endValues      Array of ending values. Must be same length as beginValues.
     * @param upperBoundType The inclusion/exclusion range of the end of range.
     *
     * @throws ReportSDKException
     */
    public static void setRangeParameterValue(
        ReportClientDocument clientDoc,
        String reportName,
        String parameterName,
        Object[] beginValues,
        RangeValueBoundType lowerBoundType,
        Object[] endValues,
        RangeValueBoundType upperBoundType
    ) throws ReportSDKException {
        // it is expected that the beginValues array is the same size as the
        // endValues array
        ParameterFieldRangeValue[] newRangeValues = new ParameterFieldRangeValue[beginValues.length];
        for (int i = 0; i < beginValues.length; i++) {
            newRangeValues[i] = new ParameterFieldRangeValue();
            newRangeValues[i].setBeginValue(beginValues[i]);
            newRangeValues[i].setLowerBoundType(lowerBoundType);
            newRangeValues[i].setEndValue(endValues[i]);
            newRangeValues[i].setUpperBoundType(upperBoundType);
        }
        clientDoc
            .getDataDefController()
            .getParameterFieldController()
            .setCurrentValues(reportName, parameterName, newRangeValues);
    }

    /**
     * Exports a report to PDF
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param startPage  Starting page
     * @param endPage    Ending page
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportPDF(ReportClientDocument clientDoc, HttpServletResponse response, boolean attachment)
        throws ReportSDKExceptionBase, IOException {
        // PDF export allows page range export. The following routine ensures
        // that the requested page range is valid
        PDFExportFormatOptions pdfOptions = new PDFExportFormatOptions();
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.PDF);
        exportOptions.setFormatOptions(pdfOptions);

        export(clientDoc, exportOptions, response, attachment, "application/pdf", "pdf");
    }

    /**
     * Exports a report to PDF for a range of pages
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param startPage  Starting page
     * @param endPage    Ending page
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportPDF(
        ReportClientDocument clientDoc,
        HttpServletResponse response,
        int startPage,
        int endPage,
        boolean attachment
    ) throws ReportSDKExceptionBase, IOException {
        // PDF export allows page range export. The following routine ensures
        // that the requested page range is valid
        PDFExportFormatOptions pdfOptions = new PDFExportFormatOptions();
        pdfOptions.setStartPageNumber(startPage);
        pdfOptions.setEndPageNumber(endPage);
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.PDF);
        exportOptions.setFormatOptions(pdfOptions);

        export(clientDoc, exportOptions, response, attachment, "application/pdf", "pdf");
    }

    /**
     * Exports a report to RTF
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportRTF(ReportClientDocument clientDoc, HttpServletResponse response, boolean attachment)
        throws ReportSDKExceptionBase, IOException {
        // RTF export allows page range export. The following routine ensures
        // that the requested page range is valid
        RTFWordExportFormatOptions rtfOptions = new RTFWordExportFormatOptions();
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.RTF);
        exportOptions.setFormatOptions(rtfOptions);

        export(clientDoc, exportOptions, response, attachment, "text/rtf", "rtf");
    }

    /**
     * Exports a report to RTF
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportMSWord(ReportClientDocument clientDoc, HttpServletResponse response, boolean attachment)
        throws ReportSDKExceptionBase, IOException {
        // RTF export allows page range export. The following routine ensures
        // that the requested page range is valid

        RTFWordExportFormatOptions rtfOptions = new RTFWordExportFormatOptions();
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.MSWord);
        exportOptions.setFormatOptions(rtfOptions);

        export(clientDoc, exportOptions, response, attachment, "application/word", "doc");
    }

    /**
     * Exports a report to RTF for a range of pages
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param startPage  Starting page
     * @param endPage    Ending page.
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportRTF(
        ReportClientDocument clientDoc,
        HttpServletResponse response,
        int startPage,
        int endPage,
        boolean attachment
    ) throws ReportSDKExceptionBase, IOException {
        // RTF export allows page range export. The following routine ensures
        // that the requested page range is valid
        RTFWordExportFormatOptions rtfOptions = new RTFWordExportFormatOptions();
        rtfOptions.setStartPageNumber(startPage);
        rtfOptions.setEndPageNumber(endPage);
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.RTF);
        exportOptions.setFormatOptions(rtfOptions);

        export(clientDoc, exportOptions, response, attachment, "text/rtf", "rtf");
    }

    /**
     * Exports a report to RTF
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportRTFEditable(
        ReportClientDocument clientDoc,
        HttpServletResponse response,
        boolean attachment
    ) throws ReportSDKExceptionBase, IOException {
        // RTF export allows page range export. The following routine ensures
        // that the requested page range is valid
        EditableRTFExportFormatOptions rtfOptions = new EditableRTFExportFormatOptions();
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.editableRTF);
        exportOptions.setFormatOptions(rtfOptions);

        export(clientDoc, exportOptions, response, attachment, "text/rtf", "rtf");
    }

    /**
     * Exports a report to RTF for a range of pages
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param startPage  Starting page
     * @param endPage    Ending page.
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportRTFEditable(
        ReportClientDocument clientDoc,
        HttpServletResponse response,
        ServletContext context,
        int startPage,
        int endPage,
        boolean attachment
    ) throws ReportSDKExceptionBase, IOException {
        // RTF export allows page range export. The following routine ensures
        // that the requested page range is valid
        EditableRTFExportFormatOptions rtfOptions = new EditableRTFExportFormatOptions();
        rtfOptions.setStartPageNumber(startPage);
        rtfOptions.setEndPageNumber(endPage);
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.editableRTF);
        exportOptions.setFormatOptions(rtfOptions);

        export(clientDoc, exportOptions, response, attachment, "text/rtf", "rtf");
    }

    /**
     * Exports a report to Excel (Data Only)
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportExcelDataOnly(
        ReportClientDocument clientDoc,
        HttpServletResponse response,
        boolean attachment
    ) throws ReportSDKExceptionBase, IOException {
        DataOnlyExcelExportFormatOptions excelOptions = new DataOnlyExcelExportFormatOptions();
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.recordToMSExcel);
        exportOptions.setFormatOptions(excelOptions);

        export(clientDoc, exportOptions, response, attachment, "application/excel", "xls");
    }

    /**
     * Exports a report to CSV
     *
     * @param clientDoc  The reportClientDocument representing the report being used
     * @param response   The HttpServletResponse object
     * @param attachment true to prompts for open or save; false opens the report in
     *                   the specified format after exporting.
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    public static void exportCSV(ReportClientDocument clientDoc, HttpServletResponse response, boolean attachment)
        throws ReportSDKExceptionBase, IOException {
        CharacterSeparatedValuesExportFormatOptions csvOptions = new CharacterSeparatedValuesExportFormatOptions();
        csvOptions.setSeparator(",");
        csvOptions.setDelimiter("\n");
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setExportFormatType(ReportExportFormat.characterSeparatedValues);
        exportOptions.setFormatOptions(csvOptions);

        export(clientDoc, exportOptions, response, attachment, "text/csv", "csv");
    }

    /**
     * Exports a report to a specified format
     *
     * @param clientDoc     The reportClientDocument representing the report being used
     * @param exportOptions Export options
     * @param response      The response object to write to
     * @param attachment    True to prompts for open or save; false opens the report
     *                      in the specified format after exporting.
     * @param mimeType      MIME type of the format being exported
     * @param extension     file extension of the format (e.g., "pdf" for Acrobat)
     * @throws ReportSDKExceptionBase
     * @throws IOException
     */
    private static void export(
        ReportClientDocument clientDoc,
        ExportOptions exportOptions,
        HttpServletResponse response,
        boolean attachment,
        String mimeType,
        String extension
    ) throws ReportSDKExceptionBase, IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(clientDoc.getPrintOutputController().export(exportOptions));

            byte[] data = new byte[1024];
            response.setContentType(mimeType);

            String name = clientDoc.getReportSource().getReportTitle();
            name = name == null ? "CrystalReport" : name.replaceAll("\"", "");

            String contentDisposition = attachment ? "attachment" : "inline";
            response.setHeader(
                "Content-Disposition",
                contentDisposition + "; filename=\"" + name + "." + extension + "\""
            );

            OutputStream os = response.getOutputStream();
            while (is.read(data) > -1) {
                os.write(data);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Prints to the server printer
     *
     * @param clientDoc   The reportClientDocument representing the report being used
     * @param printerName Name of printer used to print the report
     * @throws ReportSDKException
     */
    public static void printToServer(ReportClientDocument clientDoc, String printerName) throws ReportSDKException {
        PrintReportOptions printOptions = new PrintReportOptions();
        // Note: Printer with the <printer name> below must already be configured.
        printOptions.setPrinterName(printerName);
        printOptions.setJobTitle("Sample Print Job from Crystal Reports.");
        printOptions.setPrinterDuplex(PrinterDuplex.useDefault);
        printOptions.setPaperSource(PaperSource.auto);
        printOptions.setPaperSize(PaperSize.paperLetter);
        printOptions.setNumberOfCopies(1);
        printOptions.setCollated(false);

        // Print report
        clientDoc.getPrintOutputController().printReport(printOptions);
    }

    /**
     * Prints a range of pages to the server printer
     *
     * @param clientDoc   The reportClientDocument representing the report being used
     * @param printerName Name of printer used to print the report
     * @param startPage   Starting page
     * @param endPage     Ending page.
     * @throws ReportSDKException
     */
    public static void printToServer(ReportClientDocument clientDoc, String printerName, int startPage, int endPage)
        throws ReportSDKException {
        PrintReportOptions printOptions = new PrintReportOptions();
        // Note: Printer with the <printer name> below must already be configured.
        printOptions.setPrinterName(printerName);
        printOptions.setJobTitle("Sample Print Job from Crystal Reports.");
        printOptions.setPrinterDuplex(PrinterDuplex.useDefault);
        printOptions.setPaperSource(PaperSource.auto);
        printOptions.setPaperSize(PaperSize.paperLetter);
        printOptions.setNumberOfCopies(1);
        printOptions.setCollated(false);
        PrintReportOptions.PageRange printPageRange = new PrintReportOptions.PageRange(startPage, endPage);
        printOptions.addPrinterPageRange(printPageRange);

        // Print report
        clientDoc.getPrintOutputController().printReport(printOptions);
    }
}
