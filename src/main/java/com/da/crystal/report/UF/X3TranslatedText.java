/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-08-11 18:27:57                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 16:22:31                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.crystal.report.UF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

import javax.sql.DataSource;

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
public class X3TranslatedText implements FormulaFunction {
  @SuppressWarnings("null")
  private static final Cache<String, String> cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofDays(1))
      .build();

  private final FormulaFunctionArgumentDefinition[] arguments;

  public X3TranslatedText() {
    this.arguments = new FormulaFunctionArgumentDefinition[] {
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.string,
        SimpleFormulaFunctionArgumentDefinition.string
    };
  }

  @Override
  public String getIdentifier() {
    return this.getClass().getSimpleName().toLowerCase();
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
      log.debug("X3TranslatedText.evaluate: text: {}", text);
      return StringValue.fromString(text);
    } catch (Exception e) {
      log.warn("X3TranslatedText.evaluate failed: {}", e.getMessage());
      return StringValue.fromString("");
    }
  }

  private String getValFromDB(String lang, String oriText) throws Exception {
    String key = lang + "|" + oriText;
    String cached = cache.getIfPresent(key);

    if (cached != null) {
      return cached;
    }

    DataSource ds = (DataSource) JNDIManager.lookup("jdbc/crystal_report");
    try (Connection conn = ds.getConnection()) {
      String[] oriTextArray = oriText.split("~");
      String table = oriTextArray[0];
      String field = oriTextArray[1];
      String ident1 = oriTextArray[2];
      String sql = "SELECT TOP 1 TEXTE_0 FROM EXPLOIT.ATEXTRA WHERE CODFIC_0 = ? AND ZONE_0 = ? AND IDENT1_0 = ? AND LANGUE_0 = ?";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        // Start from 1, not 0
        ps.setString(1, table);
        ps.setString(2, field);
        ps.setString(3, ident1);
        ps.setString(4, lang);
        try (ResultSet rs = ps.executeQuery()) {
          String result = rs.next() ? rs.getString(1) : "";
          cache.put(key, result);
          log.debug("getValFromDB: lang={}, oriText={}, result={}", lang, oriText, result);
          return result;
        }
      }
    }
  }

}
