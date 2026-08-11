/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2023-03-07 00:03:27                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-11 19:44:18                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

/**
 * This sample code is an example of how to use the Business Objects APIs.
 * Because the sample code is designed for demonstration only, it is
 * unsupported.  You are free to modify and distribute the sample code as
 * needed.
 */
package com.da.crystal.report.CR;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.crystaldecisions.sdk.occa.report.application.DBOptions;
import com.crystaldecisions.sdk.occa.report.application.DataDefController;
import com.crystaldecisions.sdk.occa.report.application.DatabaseController;
import com.crystaldecisions.sdk.occa.report.application.ISubreportClientDocument;
import com.crystaldecisions.sdk.occa.report.application.ParameterFieldController;
import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.data.Connection;
import com.crystaldecisions.sdk.occa.report.data.ConnectionInfo;
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
import com.crystaldecisions.sdk.occa.report.exportoptions.ExcelExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.ExportOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.PDFExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.RTFWordExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.exportoptions.ReportExportFormat;
import com.crystaldecisions.sdk.occa.report.exportoptions.XMLExportFormatOptions;
import com.crystaldecisions.sdk.occa.report.lib.IStrings;
import com.crystaldecisions.sdk.occa.report.lib.PropertyBag;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKExceptionBase;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKPrinterException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.log4j.Log4j2;

/**
 * Crystal Reports Java Helper Sample
 * ************************
 * Please note that you need to define a runtime server in order for this class
 * to compile.
 * ************************
 *
 * @author Business Objects
 */
@Log4j2
public class CRJavaHelper {

  /**
   * Logs on to an existing datasource
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
   * Changes the DataSource for each Table
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param connectionURL The connection URL
   * @param driverName    The driver Name
   * @param jndiName      The JNDI name
   * @throws ReportSDKException
   */
  public static boolean isSameDataSource(ReportClientDocument clientDoc, String connectionURL, String driverName,
      String jndiName)
      throws ReportSDKException {
    boolean useJNDI = jndiName != null && !jndiName.isEmpty();
    String SERVER_TYPE = useJNDI ? "JDBC (JNDI)" : "JDBC";

    // Check all main report tables
    Tables tables = clientDoc.getDatabaseController().getDatabase().getTables();
    for (int i = 0; i < tables.size(); i++) {
      if (!isTableConnectionMatch(tables.getTable(i), SERVER_TYPE, connectionURL, driverName, jndiName, useJNDI)) {
        return false;
      }
    }

    // Check all subreport tables
    IStrings subNames = clientDoc.getSubreportController().getSubreportNames();
    for (int subNum = 0; subNum < subNames.size(); subNum++) {
      ISubreportClientDocument subDoc = clientDoc.getSubreportController()
          .getSubreport(subNames.getString(subNum));
      Tables subTables = subDoc.getDatabaseController().getDatabase().getTables();
      for (int i = 0; i < subTables.size(); i++) {
        if (!isTableConnectionMatch(subTables.getTable(i), SERVER_TYPE, connectionURL, driverName, jndiName, useJNDI)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Checks if a single table's connection matches the target data source.
   */
  private static boolean isTableConnectionMatch(ITable table, String serverType, String connectionURL,
      String driverName, String jndiName, boolean useJNDI) {
    IConnectionInfo oldConnectionInfo = table.getConnectionInfo();
    PropertyBag oldPropertyBag = oldConnectionInfo.getAttributes();

    if (useJNDI) {
      return jndiName.equals(oldPropertyBag.getStringValue("JNDI Datasource Name"));
    } else {
      return serverType.equals(oldPropertyBag.getStringValue("Server Type")) &&
          connectionURL.equals(oldPropertyBag.getStringValue("Connection URL")) &&
          driverName.equals(oldPropertyBag.getStringValue("Database Class Name"));
    }
  }

  /**
   * Changes the DataSource for each Table
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param username      The DB logon user name
   * @param password      The DB logon password
   * @param connectionURL The connection URL
   * @param driverName    The driver Name
   * @param jndiName      The JNDI name
   * @param reportName    The new document name (with extension) to save as
   * @param reportPath    The folder path to save the document
   * @throws ReportSDKException
   */
  public static void changeDataSource(ReportClientDocument clientDoc, String username, String password,
      String connectionURL, String driverName, String jndiName, String reportName, String reportPath)
      throws ReportSDKException {

    // Replace connection for all main report tables
    DatabaseController dbc = clientDoc.getDatabaseController();
    Tables tables = dbc.getDatabase().getTables();
    for (int i = 0; i < tables.size(); i++) {
      replaceTableConnection(dbc, tables.getTable(i), jndiName, connectionURL, driverName, username, password);
    }

    // Next loop through all the subreports and pass in the same information.
    IStrings subNames = clientDoc.getSubreportController().getSubreportNames();
    for (int subNum = 0; subNum < subNames.size(); subNum++) {
      ISubreportClientDocument subRepotDoc = clientDoc.getSubreportController()
          .getSubreport(subNames.getString(subNum));

      DatabaseController subDbc = subRepotDoc.getDatabaseController();
      Tables subtables = subDbc.getDatabase().getTables();

      for (int i = 0; i < subtables.size(); i++) {
        replaceTableConnection(subDbc, subtables.getTable(i), jndiName, connectionURL, driverName, username, password);
      }
    }

    // Save the report with updated connection info (overwrite if exists)
    Path path = Path.of(reportPath, reportName + ".JDBC.rpt");
    try {
      clientDoc.saveAs(reportName + ".JDBC.rpt", reportPath, 1);
      log.info("Report saved: {}", path);
    } catch (ReportSDKExceptionBase | IOException e) {
      log.error("Failed to save report after connection change: {}", e.getMessage());
    }
  }

  /**
   * Builds connection info and replaces the table connection.
   */
  private static void replaceTableConnection(DatabaseController dbController, ITable table,
      String jndiName, String connectionURL, String driverName, String username, String password)
      throws ReportSDKException {
    IConnectionInfo connectionInfo = buildConnectionInfo(jndiName, connectionURL, driverName, username, password);

    Connection newConnection = new Connection();
    newConnection.setConnectionInfo(connectionInfo);

    dbController.replaceConnection(table.getConnection(), newConnection,
        DBOptions._doNotVerifyDB | DBOptions._ignoreCurrentTableQualifiers);
  }

  /**
   * Builds a new connection info with the specified JDBC properties.
   */
  private static IConnectionInfo buildConnectionInfo(String jndiName, String connectionURL, String driverName,
      String username, String password) {
    IConnectionInfo connectionInfo = new ConnectionInfo();
    PropertyBag propertyBag = buildConnectionPropertyBag(jndiName, connectionURL, driverName, username, password);

    connectionInfo.setAttributes(propertyBag);
    connectionInfo.setUserName(username);
    connectionInfo.setPassword(password);
    return connectionInfo;
  }

  /**
   * Builds a new connection info with the specified JDBC properties.
   */
  private static PropertyBag buildConnectionPropertyBag(String jndiName, String connectionURL, String driverName,
      String username, String password) {
    boolean useJNDI = jndiName != null && !jndiName.isEmpty();

    PropertyBag propertyBag = new PropertyBag();
    propertyBag.put("Server Type", useJNDI ? "JDBC (JNDI)" : "JDBC");
    propertyBag.put("Connection URL", connectionURL);
    propertyBag.put("Database Class Name", driverName);
    propertyBag.put("Database DLL", "crdb_jdbc.dll");
    if (useJNDI) {
      propertyBag.put("JNDI Datasource Name", jndiName);
    }
    return propertyBag;
  }

  /**
   * Disconnect from the database by closing all matching connections.
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param username      The DB logon user name
   * @param password      The DB logon password
   * @param connectionURL The connection URL
   * @param driverName    The driver Name
   * @param jndiName      The JNDI name
   * @throws ReportSDKException
   */
  public static void closeDatabaseConnection(ReportClientDocument clientDoc, String username, String password,
      String connectionURL, String driverName, String jndiName) throws ReportSDKException {

    DatabaseController dbc = clientDoc.getDatabaseController();
    IConnectionInfo connectionInfo = buildConnectionInfo(jndiName, connectionURL, driverName, username, password);

    Connection conn = new Connection();
    conn.setConnectionInfo(connectionInfo);
    if (dbc.isConnectionOpen(conn)) {
      dbc.closeConnection(conn);
      log.info("Closed main report connection");
    }

    IStrings subNames = clientDoc.getSubreportController().getSubreportNames();
    for (int subNum = 0; subNum < subNames.size(); subNum++) {
      DatabaseController subDbc = clientDoc.getSubreportController()
          .getSubreport(subNames.getString(subNum))
          .getDatabaseController();
      Connection subConn = new Connection();
      subConn.setConnectionInfo(connectionInfo);
      if (subDbc.isConnectionOpen(subConn)) {
        subDbc.closeConnection(subConn);
        log.info("Closed subreport connection: {}", subNames.getString(subNum));
      }
    }
  }

  /**
   * Passes a populated java.sql.Resultset object to a Table object
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param rs            The java.sql.Resultset used to populate the Table
   * @param tableName     The name of the table
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @throws ReportSDKException
   */
  public static void passResultSet(ReportClientDocument clientDoc, java.sql.ResultSet rs, String tableName,
      String subreportName) throws ReportSDKException {
    if (subreportName.equals("")) {
      clientDoc.getDatabaseController().setDataSource(rs, tableName, tableName + "_ResultSet");
    } else {
      clientDoc.getSubreportController().getSubreport(subreportName).getDatabaseController().setDataSource(
          rs, tableName, tableName + "_ResultSet");
    }
  }

  /**
   * Passes a populated collection of a Java class to a Table object
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param dataSet       The data used to populate the Table
   * @param className     The fully-qualified class name of the POJO objects being
   *                      passed
   * @param tableName     The name of the table
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @throws ReportSDKException
   */
  public static void passPOJO(ReportClientDocument clientDoc, Collection<?> dataSet, String className,
      String tableName, String subreportName) throws ReportSDKException,
      ClassNotFoundException {
    if (subreportName.equals("")) {
      clientDoc.getDatabaseController().setDataSource(dataSet, Class.forName(className), tableName,
          tableName + "_POJO");
    } else {
      clientDoc.getSubreportController().getSubreport(subreportName).getDatabaseController().setDataSource(
          dataSet, Class.forName(className), tableName, tableName + "_POJO");
    }
  }

  /**
   * Passes a single discrete parameter value to a report parameter
   *
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param parameterName The name of the parameter
   * @throws ReportSDKException
   */
  public static boolean isParameterExists(
      ReportClientDocument clientDoc,
      String parameterName)
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
   * 
   * @param clientDoc
   * @return List of parameters name
   * @throws ReportSDKException
   */
  public static List<String> getTopParams(ReportClientDocument clientDoc)
      throws ReportSDKException {
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
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param parameterName The name of the parameter
   * @param newValue      The new value of the parameter
   * @throws ReportSDKException
   */
  public static void setTopParameter(ReportClientDocument clientDoc,
      String parameterName, Object newValue)
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
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @param parameterName The name of the parameter
   * @param newValue      The new value of the parameter
   * @throws ReportSDKException
   */
  public static void addDiscreteParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object newValue) throws ReportSDKException {
    DataDefController dataDefController = null;
    if (subreportName.equals("")) {
      dataDefController = clientDoc.getDataDefController();
    } else {
      dataDefController = clientDoc.getSubreportController().getSubreport(subreportName)
          .getDataDefController();
    }

    ParameterFieldDiscreteValue newDiscValue = new ParameterFieldDiscreteValue();
    newDiscValue.setValue(newValue);

    ParameterField paramField = (ParameterField) dataDefController.getDataDefinition().getParameterFields()
        .findField(parameterName, FieldDisplayNameType.fieldName, Locale.getDefault());
    boolean multiValue = paramField.getAllowMultiValue();

    if (multiValue) {
      Values newVals = (Values) paramField.getCurrentValues().clone(true);
      newVals.add(newDiscValue);
      clientDoc.getDataDefController().getParameterFieldController().setCurrentValue(subreportName,
          parameterName, newVals);
    } else {
      clientDoc.getDataDefController().getParameterFieldController().setCurrentValue(subreportName,
          parameterName, newValue);
    }
  }

  /**
   * Passes multiple discrete parameter values to a report parameter
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @param parameterName The name of the parameter
   * @param newValues     An array of new values to get set on the parameter
   * @throws ReportSDKException
   */
  public static void addDiscreteParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object[] newValues) throws ReportSDKException {
    clientDoc.getDataDefController().getParameterFieldController().setCurrentValues(subreportName, parameterName,
        newValues);
  }

  /**
   * Passes a single range parameter value to a report parameter. The range is
   * assumed to
   * be inclusive on beginning and end.
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @param parameterName The name of the parameter
   * @param beginValue    The value of the beginning of the range
   * @param endValue      The value of the end of the range
   * @throws ReportSDKException
   */
  public static void addRangeParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object beginValue, Object endValue)
      throws ReportSDKException {
    addRangeParameterValue(clientDoc, subreportName, parameterName, beginValue, RangeValueBoundType.inclusive, endValue,
        RangeValueBoundType.inclusive);
  }

  /**
   * Passes multiple range parameter values to a report parameter.
   *
   * This overload of the addRangeParameterValue will only work if the
   * parameter is setup to accept multiple values.
   * 
   * If the Parameter does not accept multiple values then it is expected that
   * this version of the method will return an error
   * 
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param subreportName The name of the subreport. If tables in the main report
   *                      is to be used, "" should be passed
   * @param parameterName The name of the parameter
   * @param beginValues   Array of beginning values. Must be same length as
   *                      endValues.
   * @param endValues     Array of ending values. Must be same length as
   *                      beginValues.
   * @throws ReportSDKException
   */
  public static void addRangeParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object[] beginValues, Object[] endValues)
      throws ReportSDKException {
    addRangeParameterValue(clientDoc, subreportName, parameterName, beginValues, RangeValueBoundType.inclusive,
        endValues, RangeValueBoundType.inclusive);
  }

  /**
   * Passes a single range parameter value to a report parameter
   * 
   * @param clientDoc      The reportClientDocument representing the report being
   *                       used
   * @param subreportName  The name of the subreport. If tables in the main report
   *                       is to be used, "" should be passed
   * @param parameterName  The name of the parameter
   * @param beginValue     The value of the beginning of the range
   * @param lowerBoundType The inclusion/exclusion range of the start of range.
   * @param endValue       The value of the end of the range
   * @param upperBoundType The inclusion/exclusion range of the end of range.
   * @throws ReportSDKException
   */
  public static void addRangeParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object beginValue,
      RangeValueBoundType lowerBoundType, Object endValue,
      RangeValueBoundType upperBoundType) throws ReportSDKException {
    DataDefController dataDefController = null;
    if (subreportName.equals("")) {
      dataDefController = clientDoc.getDataDefController();
    } else {
      dataDefController = clientDoc.getSubreportController().getSubreport(subreportName).getDataDefController();
    }

    ParameterFieldRangeValue newRangeValue = new ParameterFieldRangeValue();
    newRangeValue.setBeginValue(beginValue);
    newRangeValue.setLowerBoundType(lowerBoundType);
    newRangeValue.setEndValue(endValue);
    newRangeValue.setUpperBoundType(upperBoundType);

    ParameterField paramField = (ParameterField) dataDefController.getDataDefinition().getParameterFields()
        .findField(parameterName, FieldDisplayNameType.fieldName, Locale.getDefault());
    boolean multiValue = paramField.getAllowMultiValue();

    if (multiValue) {
      Values newVals = (Values) paramField.getCurrentValues().clone(true);
      newVals.add(newRangeValue);
      clientDoc.getDataDefController().getParameterFieldController().setCurrentValue(subreportName, parameterName,
          newVals);
    } else {
      clientDoc.getDataDefController().getParameterFieldController().setCurrentValue(subreportName, parameterName,
          newRangeValue);
    }
  }

  /**
   * Passes multiple range parameter values to a report parameter.
   *
   * This overload of the addRangeParameterValue will only work if the
   * parameter is setup to accept multiple values.
   * 
   * If the Parameter does not accept multiple values then it is expected that
   * this version of the method will return an error
   * 
   * @param clientDoc      The reportClientDocument representing the report being
   *                       used
   * @param subreportName  The name of the subreport. If tables in the main report
   *                       is to be used, "" should be passed
   * @param parameterName  The name of the parameter
   * @param beginValues    Array of beginning values. Must be same length as
   *                       endValues.
   * @param lowerBoundType The inclusion/exclusion range of the start of range.
   * @param endValues      Array of ending values. Must be same length as
   *                       beginValues.
   * @param upperBoundType The inclusion/exclusion range of the end of range.
   * 
   * @throws ReportSDKException
   */
  public static void addRangeParameterValue(ReportClientDocument clientDoc, String subreportName,
      String parameterName, Object[] beginValues,
      RangeValueBoundType lowerBoundType, Object[] endValues,
      RangeValueBoundType upperBoundType) throws ReportSDKException {
    // it is expected that the beginValues array is the same size as the endValues
    // array
    ParameterFieldRangeValue[] newRangeValues = new ParameterFieldRangeValue[beginValues.length];
    for (int i = 0; i < beginValues.length; i++) {
      newRangeValues[i] = new ParameterFieldRangeValue();
      newRangeValues[i].setBeginValue(beginValues[i]);
      newRangeValues[i].setLowerBoundType(lowerBoundType);
      newRangeValues[i].setEndValue(endValues[i]);
      newRangeValues[i].setUpperBoundType(upperBoundType);
    }

    clientDoc.getDataDefController().getParameterFieldController().setCurrentValues(subreportName, parameterName,
        newRangeValues);
  }

  /**
   * Exports a report to PDF
   *
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServletResponse object
   * @param startPage  Starting page
   * @param endPage    Ending page
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportPDF(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServletResponse object
   * @param startPage  Starting page
   * @param endPage    Ending page
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportPDF(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      int startPage,
      int endPage,
      boolean attachment)
      throws ReportSDKExceptionBase, IOException {
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportRTF(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServletResponse object
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportMSWord(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param startPage  Starting page
   * @param endPage    Ending page.
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportRTF(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      int startPage,
      int endPage,
      boolean attachment)
      throws ReportSDKExceptionBase, IOException {
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportRTFEditable(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
      throws ReportSDKExceptionBase, IOException {
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param startPage  Starting page
   * @param endPage    Ending page.
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportRTFEditable(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      int startPage,
      int endPage,
      boolean attachment)
      throws ReportSDKExceptionBase, IOException {
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
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportExcelDataOnly(ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
      throws ReportSDKExceptionBase, IOException {
    DataOnlyExcelExportFormatOptions excelOptions = new DataOnlyExcelExportFormatOptions();
    ExportOptions exportOptions = new ExportOptions();
    exportOptions.setExportFormatType(ReportExportFormat.recordToMSExcel);
    exportOptions.setFormatOptions(excelOptions);

    export(clientDoc, exportOptions, response, attachment, "application/excel", "xls");
  }

  /**
   * Exports a report to CSV
   *
   * @param clientDoc  The reportClientDocument representing the report being
   *                   used
   * @param response   The HttpServerResponse object
   * @param attachment true to prompts for open or save; false opens the report
   *                   in the specified format after exporting.
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  public static void exportCSV(
      ReportClientDocument clientDoc,
      HttpServerResponse response,
      boolean attachment)
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
   * Exports a report to Excel
   * 
   * @param clientDoc The reportClientDocument representing the report being used
   * @return An <code>InputStream</code> object containing the report document
   *         exported to the
   *         specified format.
   * @throws ReportSDKException
   */
  public static InputStream exportExcel(ReportClientDocument clientDoc) throws ReportSDKException {
    ExportOptions exportOptions = new ExportOptions();
    exportOptions.setExportFormatType(ReportExportFormat.MSExcel);
    exportOptions.setFormatOptions(new ExcelExportFormatOptions());

    // Export the report using the export options.
    return clientDoc.getPrintOutputController().export(exportOptions);
  }

  /**
   * Exports a report to XML
   * 
   * @param clientDoc The reportClientDocument representing the report being used
   * @return An <code>InputStream</code> object containing the report document
   *         exported to the
   *         specified format.
   * @throws ReportSDKException
   */
  public static InputStream exportXML(ReportClientDocument clientDoc) throws ReportSDKException {
    ExportOptions exportOptions = new ExportOptions();
    exportOptions.setExportFormatType(ReportExportFormat.XML);
    exportOptions.setFormatOptions(new XMLExportFormatOptions());

    // Export the report using the export options.
    return clientDoc.getPrintOutputController().export(exportOptions);
  }

  /**
   * Exports a report to XML
   * 
   * @param clientDoc The reportClientDocument representing the report being used
   * @return An <code>InputStream</code> object containing the report document
   *         exported to the
   *         specified format.
   * @throws ReportSDKException
   */
  public static InputStream exportXML(ReportClientDocument clientDoc, int indexOfXmlFormats) throws ReportSDKException {
    ExportOptions exportOptions = new ExportOptions();
    exportOptions.setExportFormatType(ReportExportFormat.XML);
    exportOptions.setFormatOptions(new XMLExportFormatOptions());
    XMLExportFormatOptions formatOptions = new XMLExportFormatOptions(indexOfXmlFormats);
    exportOptions.setFormatOptions(formatOptions);
    // Export the report using the export options.
    return clientDoc.getPrintOutputController().export(exportOptions);
  }

  /**
   * Exports a report to a specified format
   *
   * @param clientDoc     The reportClientDocument representing the report being
   *                      used
   * @param exportOptions Export options
   * @param response      The response object to write to
   * @param attachment    True to prompts for open or save; false opens the
   *                      report
   *                      in the specified format after exporting.
   * @param mimeType      MIME type of the format being exported
   * @param extension     file extension of the format (e.g., "pdf" for Acrobat)
   * @throws ReportSDKExceptionBase
   * @throws IOException
   */
  private static void export(
      ReportClientDocument clientDoc,
      ExportOptions exportOptions,
      HttpServerResponse response,
      boolean attachment,
      String mimeType,
      String extension)
      throws ReportSDKExceptionBase, IOException {
    InputStream is = null;
    try {
      is = new BufferedInputStream(clientDoc.getPrintOutputController().export(exportOptions));

      byte[] data = new byte[1024 * 2000];
      response.putHeader("Content-Type", mimeType);

      String name = clientDoc.getReportSource().getReportTitle();
      name = name == null ? "CrystalReport" : name.replaceAll("\"", "");

      String contentDisposition = attachment ? "attachment" : "inline";
      response.putHeader("Content-Disposition", contentDisposition +
          "; filename=\"" + name +
          "." + extension + "\"");

      Buffer buffer = Buffer.buffer();
      while (is.read(data) > -1) {
        buffer.appendBytes(data);
      }
      response.end(buffer);
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * Prints to the server printer
   * 
   * @param clientDoc   The reportClientDocument representing the report being
   *                    used
   * @param printerName Name of printer used to print the report
   * @throws ReportSDKPrinterException
   */
  public static void printToServer(ReportClientDocument clientDoc, String printerName) throws ReportSDKException {
    PrintReportOptions printOptions = new PrintReportOptions();
    // Note: Printer with the <printer name> below must already be
    // configured.
    printOptions.setPrinterName(printerName);
    printOptions.setJobTitle("Sample Print Job from JRC.");
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
   * @param clientDoc   The reportClientDocument representing the report being
   *                    used
   * @param printerName Name of printer used to print the report
   * @param startPage   Starting page
   * @param endPage     Ending page.
   * @throws ReportSDKPrinterException
   */
  public static void printToServer(ReportClientDocument clientDoc, String printerName, int startPage, int endPage)
      throws ReportSDKException {
    PrintReportOptions printOptions = new PrintReportOptions();
    // Note: Printer with the <printer name> below must already be
    // configured.
    printOptions.setPrinterName(printerName);
    printOptions.setJobTitle("Sample Print Job from JRC.");
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