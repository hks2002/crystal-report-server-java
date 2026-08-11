package com.da.crystal.report.UFL;

import com.crystaldecisions.reports.common.engine.ConfigurationManager;
import com.crystaldecisions.reports.common.engine.Engine;

import lombok.extern.log4j.Log4j2;

/**
 * UflRegistrar
 *
 * Registers Java UFL functions with Crystal Reports engine programmatically
 *
 * <p>
 * Crystal Reports loads Java UFL class names from the ConfigurationManager
 * under the key {@code ExternalFunctionLibraryClassNames.classname}. This is
 * the same mechanism that CRConfig.xml uses, but done in code so no XML file
 * is needed.
 * </p>
 *
 */
@Log4j2
public class UflRegistrar {

  private static final String CONFIG_EXTERNAL_FUNCTION_LIBS_CLASSNAME = "ExternalFunctionLibraryClassNames.classname";
  private static final String JAVA_UFL_CLASS_NAME = "com.da.crystal.report.UFL.JavaUFL";

  private static volatile boolean libraryRegistered = false;

  public static synchronized void registerUfls() throws UflRegistrationException {
    if (libraryRegistered) {
      log.debug("UFL already registered, skipping");
      return;
    }
    try {
      // Phase 1: Register Java UFL class in ConfigurationManager
      ConfigurationManager cm = Engine.getDefault().getConfigurationManager();

      // Check BEFORE adding
      // java.util.List<String> beforeList =
      // cm.getList(CONFIG_EXTERNAL_FUNCTION_LIBS_CLASSNAME);
      // log.debug(" UFL before adding: {} entries", beforeList.size());
      // for (String s : beforeList) {
      // log.debug(" existing entry: {}", s);
      // }

      cm.addProperty(CONFIG_EXTERNAL_FUNCTION_LIBS_CLASSNAME, JAVA_UFL_CLASS_NAME);

      // Phase 2: Force-load JavaUFL class so its static initializer runs now,
      // scanning the UF package and caching functions at startup rather than
      // waiting for the first report request to trigger class loading.
      try {
        Class.forName(JAVA_UFL_CLASS_NAME, true, Thread.currentThread().getContextClassLoader());
      } catch (ClassNotFoundException e) {
        log.warn("JavaUFL class not found on classpath: {}", JAVA_UFL_CLASS_NAME);
      }

      libraryRegistered = true;
      log.info("  UFL class '{}' registered", JAVA_UFL_CLASS_NAME);
    } catch (Exception e) {
      throw new UflRegistrationException("Failed to register Java UFL: " + e.getMessage(), e);
    }
  }

  public static boolean isLibraryRegistered() {
    return libraryRegistered;
  }

  public static class UflRegistrationException extends Exception {
    public UflRegistrationException(String message) {
      super(message);
    }

    public UflRegistrationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
