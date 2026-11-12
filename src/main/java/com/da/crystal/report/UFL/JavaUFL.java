/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-11-12 09:14:55                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 14:18:00                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report.UFL;

import java.util.List;

import com.crystaldecisions.reports.formulas.FormulaFunction;
import com.crystaldecisions.reports.formulas.FormulaFunctionLibrary;
import com.crystaldecisions.reports.formulas.FormulaFunctionSetupException;

import lombok.extern.log4j.Log4j2;

/**
 * JavaUFL Library
 *
 * </p>
 */
@Log4j2
public class JavaUFL implements FormulaFunctionLibrary {
  /** Cached functions — initialized once at class-load time. */
  private static final FormulaFunction[] CACHED_FUNCTIONS;

  static {
    String ufPackage = "com.da.crystal.report.UF";
    String ufResourcePath = ufPackage.replace('.', '/');
    List<FormulaFunction> discovered = FunctionUtils.scanFunctions(ufPackage, ufResourcePath);
    CACHED_FUNCTIONS = discovered.toArray(new FormulaFunction[0]);

    log.info("JavaUFL initialized with {} function(s):", CACHED_FUNCTIONS.length);
    for (FormulaFunction func : CACHED_FUNCTIONS) {
      log.info("  - {} (returnType={}, args={})",
          func.getIdentifier(),
          func.getReturnType(),
          func.getArguments() != null ? func.getArguments().length : 0);
    }
  }

  /**
   * Constructor — required by Crystal Reports SDK.
   * Functions are accessed from the static cache directly.
   */
  public JavaUFL() throws FormulaFunctionSetupException {
  }

  /**
   * Returns the number of functions in this library.
   *
   * @return The count of custom UFL functions
   */
  @Override
  public int size() {
    return CACHED_FUNCTIONS.length;
  }

  /**
   * Returns the function at the specified index.
   *
   * @param index The zero-based index of the function
   * @return The FormulaFunction at the specified index
   * @throws IndexOutOfBoundsException if index is out of range
   */
  @Override
  public FormulaFunction getFunction(int index) {
    if (index < 0 || index >= CACHED_FUNCTIONS.length) {
      throw new IndexOutOfBoundsException(
          "Function index " + index + " out of range. Library has " + CACHED_FUNCTIONS.length + " functions.");
    }
    return CACHED_FUNCTIONS[index];
  }

  /**
   * Returns the cached function array — useful for diagnostics.
   */
  public static FormulaFunction[] getCachedFunctions() {
    return CACHED_FUNCTIONS.clone();
  }
}