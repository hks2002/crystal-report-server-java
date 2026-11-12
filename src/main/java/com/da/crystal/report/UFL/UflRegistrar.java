/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-11-02 09:09:55                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 00:02:04                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
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
* <p>
* The UFL class names can be configured externally via the application config
* file, under the {@code ufl.classNames} entry. If it is absent it falls back
* to the default {@link #JAVA_UFL_CLASS_NAME}.
* </p>
*
*/

package com.da.crystal.report.UFL;

import java.util.ArrayList;
import java.util.List;

import com.crystaldecisions12.reports.common.engine.ConfigurationManager;
import com.crystaldecisions12.reports.common.engine.Engine;
import com.da.crystal.report.AppConfig;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UflRegistrar {

  private static final String CONFIG_EXTERNAL_FUNCTION_LIBS_CLASSNAME = "ExternalFunctionLibraryClassNames.classname";
  private static final String JAVA_UFL_CLASS_NAME = "com.da.crystal.report.UFL.JavaUFL";

  private static final String CONFIG_KEY = "ufl";
  private static final String CLASS_NAMES_KEY = "classNames";

  public static synchronized void registerUFLs() {
    registerUFLs(configuredClassNames());
  }

  public static synchronized void registerUFLs(String... classNames) {
    if (classNames == null || classNames.length == 0) {
      log.warn("No UFL class names provided, nothing to register");
      return;
    }

    try {
      ConfigurationManager cm = Engine.getDefault().getConfigurationManager();

      for (String className : classNames) {
        if (className == null || className.trim().isEmpty()) {
          continue;
        }

        String name = className.trim();

        try {
          Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
          log.warn("UFL class not found on classpath: {}", name);
        }

        cm.addProperty(CONFIG_EXTERNAL_FUNCTION_LIBS_CLASSNAME, name);

        log.info("  UFL class '{}' registered", name);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to register Java UFL: " + e.getMessage(), e);
    }
  }

  private static String[] configuredClassNames() {
    JsonObject ufl = AppConfig.config == null ? null : AppConfig.config.getJsonObject(CONFIG_KEY);
    JsonArray classNames = ufl == null ? null : ufl.getJsonArray(CLASS_NAMES_KEY);

    if (classNames == null || classNames.isEmpty()) {
      log.debug("No UFL class names configured, falling back to default: {}", JAVA_UFL_CLASS_NAME);
      return new String[] { JAVA_UFL_CLASS_NAME };
    }

    List<String> result = new ArrayList<>();
    for (Object item : classNames) {
      if (item instanceof String && !((String) item).trim().isEmpty()) {
        result.add(((String) item).trim());
      }
    }

    if (result.isEmpty()) {
      log.debug("Configured UFL class names are all empty, falling back to default: {}", JAVA_UFL_CLASS_NAME);
      return new String[] { JAVA_UFL_CLASS_NAME };
    }

    return result.toArray(new String[0]);
  }
}