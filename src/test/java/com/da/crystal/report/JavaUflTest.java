/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-08-11 18:23:59                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 13:56:29                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.crystaldecisions.reports.formulas.FormulaFunctionLibrary;
import com.crystaldecisions.reports.formulas.FormulaFunctionSetupException;
import com.da.crystal.report.UF.SampleFn;
import com.da.crystal.report.UFL.JavaUFL;

import lombok.extern.log4j.Log4j2;

@Log4j2
class JavaUflTest {

  @BeforeEach
  void setUp() {
    // No setup needed for unit tests
  }

  @Test
  void testFunctionBasic() throws FormulaFunctionSetupException {
    SampleFn function = new SampleFn();

    // Test identifier
    assertEquals("samplefn", function.getIdentifier());

    // Test arguments
    assertNotNull(function.getArguments());
    assertEquals(1, function.getArguments().length);

    // Test return type
    assertNotNull(function.getReturnType());

    log.info("samplefn basic functionality verified");
  }

  @Test
  void testJavaUflCreation() throws FormulaFunctionSetupException {
    FormulaFunctionLibrary library = new JavaUFL();

    assertNotNull(library, "JavaUFL library should not be null");
    assertTrue(library.size() > 0, "JavaUFL library should contain at least one function");

    log.info("JavaUFL library created successfully with {} function(s)", library.size());
  }

}