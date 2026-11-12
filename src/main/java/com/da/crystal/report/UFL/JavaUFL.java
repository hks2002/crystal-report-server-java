/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-11-12 09:14:55                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 09:20:20                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report.UFL;

import java.util.List;

import com.crystaldecisions.reports.formulas.FormulaFunction;
import com.crystaldecisions.reports.formulas.FormulaFunctionLibrary;
import com.crystaldecisions.reports.formulas.FormulaFunctionSetupException;
import com.da.crystal.report.AppConfig;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

/**
 * JavaUFL Library
 *
 * </p>
 */
@Log4j2
public class JavaUFL implements FormulaFunctionLibrary {

  private static final String DEFAULT_UF_PACKAGE = "com.da.crystal.report.UF";

  private static final String CONFIG_KEY = "ufl";
  private static final String SCAN_PATH_KEY = "scanPath";

  /** Cached functions — initialized once at class-load time. */
  private static final FormulaFunction[] CACHED_FUNCTIONS;

  static {
    String ufPackage = configuredScanPath();
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
   * Resolves the package to scan for {@link FormulaFunction}s, reading it from
   * the application config under {@code ufl.scanPath}. Falls back to
   * {@link #DEFAULT_UF_PACKAGE} when absent.
   *
   * @return the configured (or default) package to scan
   */
  private static String configuredScanPath() {
    JsonObject ufl = AppConfig.config == null ? null : AppConfig.config.getJsonObject(CONFIG_KEY);
    String scanPath = ufl == null ? null : ufl.getString(SCAN_PATH_KEY);

    if (scanPath == null || scanPath.trim().isEmpty()) {
      log.info("No UFL scan path configured, falling back to default: {}", DEFAULT_UF_PACKAGE);
      return DEFAULT_UF_PACKAGE;
    }

    return scanPath.trim();
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