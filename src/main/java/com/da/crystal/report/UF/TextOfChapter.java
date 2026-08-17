/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-11 18:27:43                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-17 18:40:22                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.UF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TextOfChapter implements FormulaFunction {

  // Crystal Reports requires Java UFL identifiers to be lower-case only.
  // FormulaParser converts function names to lower-case before lookup
  private static final String FUNCTION_NAME = "textofchapter";

  @SuppressWarnings("null")
  private static final Cache<String, String> cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofDays(1))
      .build();

  private final FormulaFunctionArgumentDefinition[] arguments;
  private final CrystalResourcesFactory message = new CrystalResourcesFactory("message");

  public TextOfChapter() {
    this.arguments = new FormulaFunctionArgumentDefinition[] {
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.number,
        SimpleFormulaFunctionArgumentDefinition.number
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
      String lanChapter = ArgUtils.getArgString(args, 2);
      String lanNum = ArgUtils.getArgString(args, 3);

      String text = getValFromDB(lang, lanChapter, lanNum);
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

  @SuppressWarnings("null")
  private String getValFromDB(String lang, String lanChapter, String lanNum) throws Exception {
    String key = lang + "|" + lanChapter + "|" + lanNum;
    String cached = cache.getIfPresent(key);

    if (cached != null) {
      return cached;
    }

    DataSource ds = (DataSource) JNDIManager.lookup("jdbc/crystal_report");
    try (Connection conn = ds.getConnection()) {
      String sql = "SELECT TOP 1 LANMES_0 FROM EXPLOIT.APLSTD WHERE LAN_0 = ? AND LANCHP_0 = ? AND LANNUM_0 = ?";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, lang);
        ps.setString(2, lanChapter);
        ps.setString(3, lanNum);
        try (ResultSet rs = ps.executeQuery()) {
          String result = rs.next() ? rs.getString(1) : "";
          cache.put(key, result);
          log.debug("getValFromDB: lang={}, lanChapter={}, lanNum={}, result={}", lang, lanChapter, lanNum, result);
          return result;
        }
      }
    }
  }

}