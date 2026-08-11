/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-11 18:27:02                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-11 18:27:23                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.UF;

import com.crystaldecisions.reports.common.value.FormulaValue;
import com.crystaldecisions.reports.common.value.FormulaValueType;
import com.crystaldecisions.reports.common.value.StringValue;
import com.crystaldecisions.reports.formulas.FormulaFunction;
import com.crystaldecisions.reports.formulas.FormulaFunctionArgumentDefinition;
import com.crystaldecisions.reports.formulas.FormulaFunctionCallException;
import com.crystaldecisions.reports.formulas.FormulaValueReference;
import com.crystaldecisions.reports.formulas.SimpleFormulaFunctionArgumentDefinition;
import com.da.crystal.report.UFL.ArgUtils;

/**
 * SampleFn UFL Function.
 * 
 * <pre>
 * SampleFn(myparam)
 * </pre>
 * 
 * <p>
 * It return the first argument as a string.
 * </p>
 */
public class SampleFn implements FormulaFunction {

  // Crystal Reports requires Java UFL identifiers to be lower-case only.
  // FormulaParser converts function names to lower-case before lookup,
  // so 'SampleFn' in the report will match 'samplefn' here.
  private static final String FUNCTION_NAME = "samplefn";

  private final FormulaFunctionArgumentDefinition[] arguments;

  public SampleFn() {
    this.arguments = new FormulaFunctionArgumentDefinition[] {
        SimpleFormulaFunctionArgumentDefinition.string,
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

  /**
   * The core of the function.
   * This sample function is to return the first argument as a string.
   */
  @Override
  public FormulaValue evaluate(FormulaValueReference[] args)
      throws FormulaFunctionCallException {
    try {
      return StringValue.fromString(ArgUtils.getArgString(args, 0));
    } catch (Exception e) {
      throw new FormulaFunctionCallException(
          FUNCTION_NAME,
          "JavaUFL",
          new com.crystaldecisions.reports.common.CrystalResourcesFactory("Messages"),
          FUNCTION_NAME + "(" + ArgUtils.argsToString(args) + ")",
          e);
    }
  }

}
