/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-11 18:27:57                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-17 01:13:27                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.UF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import com.crystaldecisions.reports.common.CrystalResourcesFactory;
import com.crystaldecisions.reports.common.value.FormulaValue;
import com.crystaldecisions.reports.common.value.FormulaValueType;
import com.crystaldecisions.reports.common.value.StringValue;
import com.crystaldecisions.reports.formulas.FormulaFunction;
import com.crystaldecisions.reports.formulas.FormulaFunctionArgumentDefinition;
import com.crystaldecisions.reports.formulas.FormulaFunctionCallException;
import com.crystaldecisions.reports.formulas.FormulaValueReference;
import com.crystaldecisions.reports.formulas.SimpleFormulaFunctionArgumentDefinition;
import com.da.crystal.report.JNDI.JNDIManager;
import com.da.crystal.report.UFL.ArgUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class X3TranslatedText implements FormulaFunction {

  // Crystal Reports requires Java UFL identifiers to be lower-case only.
  // FormulaParser converts function names to lower-case before lookup,
  // so 'TextOfChapter' in the report will match 'textofchapter' here.
  private static final String FUNCTION_NAME = "x3translatedtext";

  private final FormulaFunctionArgumentDefinition[] arguments;
  private final CrystalResourcesFactory message = new CrystalResourcesFactory("message");

  public X3TranslatedText() {
    this.arguments = new FormulaFunctionArgumentDefinition[] {
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.string
    };
  }

  @Override
  public String getIdentifier() {
    return FUNCTION_NAME;
  }

  @Override
  public FormulaFunctionArgumentDefinition[] getArguments() {
    return arguments;
  }

  @Override
  public FormulaValueType getReturnType() {
    return FormulaValueType.string;
  }

  @Override
  public void validateArgumentValues(FormulaValueReference[] args)
      throws FormulaFunctionCallException {
    // Accept any arguments
  }

  public FormulaValueType validate(FormulaValueReference[] args,
      com.crystaldecisions.reports.formulas.FormulaEnvironment env)
      throws FormulaFunctionCallException {
    return FormulaValueType.string;
  }

  @Override
  public FormulaValue evaluate(FormulaValueReference[] args)
      throws FormulaFunctionCallException {
    try {

      String lang = ArgUtils.getArgString(args, 1);
      String oriText = ArgUtils.getArgString(args, 2);

      String text = getValFromDB(lang, oriText);
      return StringValue.fromString(text);
    } catch (Exception e) {
      throw new FormulaFunctionCallException(
          FUNCTION_NAME,
          "JavaUFL",
          message,
          ArgUtils.argsToString(args),
          e);
    }
  }

  private String getValFromDB(String lang, String oriText) throws Exception {
    DataSource ds = (DataSource) JNDIManager.lookup("jdbc/crystal_report");
    try (Connection conn = ds.getConnection()) {
      String sql = "SELECT TOP 1 TEXTE_0 FROM EXPLOIT.ATEXTRA WHERE CODFIC_0 = 'ITMMASTER' AND ZONE_0 = 'YITM_CMUTXT' AND LANGUE_0 = ? AND IDENT1_0 = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        String[] oriTextArray = oriText.split(",");
        String ident1 = oriTextArray[2]; // PN
        ps.setString(1, lang);
        ps.setString(2, ident1);
        try (ResultSet rs = ps.executeQuery()) {
          return rs.next() ? rs.getString(1) : "";
        }
      }
    }
  }

}
