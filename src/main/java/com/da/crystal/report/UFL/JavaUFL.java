package com.da.crystal.report.UFL;

import java.util.List;

import com.crystaldecisions.reports.formulas.FormulaFunction;
import com.crystaldecisions.reports.formulas.FormulaFunctionLibrary;
import com.crystaldecisions.reports.formulas.FormulaFunctionSetupException;

import lombok.extern.log4j.Log4j2;

/**
 * JavaUFL Library
 *
 * <p>
 * Auto-discovers all {@link FormulaFunction} implementations in the
 * {@code com.da.crystal.report.UF} package and registers them as UFL functions.
 * No manual registration needed — just create a class that implements
 * {@link FormulaFunction} in the UF package and it will be picked up
 * automatically.
 * </p>
 *
 * <p>
 * This class is statically initialized — scanning happens once at class-load
 * time and all subsequent constructor calls return the same cached function
 * array. Crystal Reports may instantiate this class multiple times when
 * opening reports; the static cache avoids redundant scanning.
 * </p>
 */
@Log4j2
public class JavaUFL implements FormulaFunctionLibrary {

  private static final String UF_PACKAGE = "com.da.crystal.report.UF";
  private static final String UF_RESOURCE_PATH = "com/da/crystal/report/UF";

  /** Cached functions — initialized once at class-load time. */
  private static final FormulaFunction[] CACHED_FUNCTIONS;

  static {
    List<FormulaFunction> discovered = FunctionUtils.scanFunctions(UF_PACKAGE, UF_RESOURCE_PATH);
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
