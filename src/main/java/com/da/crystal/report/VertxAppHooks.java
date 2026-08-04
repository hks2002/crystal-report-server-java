/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-05-19 16:54:08                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-07-31 22:31:23                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report;

import com.da.crystal.report.JNDI.JNDIManager;

import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxAppHooks implements VertxApplicationHooks {

  @Override
  public JsonObject afterVertxOptionsParsed(JsonObject options) {
    if (options != null) {
      log.debug("afterVertxOptionsParsed: Your VertxOptions\n{}", options.encodePrettily());
    }
    return options;
  }

  @Override
  public JsonObject afterDeploymentOptionsParsed(JsonObject options) {
    if (options != null) {
      log.debug("afterDeploymentOptionsParsed:\n{}", options.encodePrettily());
    }
    return options;
  }

  @Override
  public JsonObject afterConfigParsed(JsonObject config) {
    log.debug("afterConfigParsed");

    if (config != null) {
      AppConfig.setup(config);
      AppConfig.printConfig();
    }

    return config;
  }

  @Override
  public void beforeStartingVertx(HookContext context) {
    log.debug("BeforeStartingVertx");
  }

  @Override
  public VertxBuilder createVertxBuilder(VertxOptions options) {
    if (options != null) {
      log.debug("createVertxBuilder: Final VertxOptions\n{}", options.toJson().encodePrettily());
    }

    return Vertx.builder().with(options);
  }

  @Override
  public void afterVertxStarted(HookContext context) {
    log.debug("afterVertxStarted");

    try {

      boolean hasConfigArg = false;
      String configArg = null;
      for (String s : VertxApp.appArgs) {
        if (s.startsWith("-conf=") || s.startsWith("--conf=")) {
          hasConfigArg = true;
          configArg = s.replaceFirst("-{1,2}conf=", "");
          break;
        }
      }

      if (hasConfigArg && configArg != null) {
        AppConfig.setupPeriod(context.vertx(), configArg);
      }

      initJNDI();

    } catch (Exception e) {
      log.error("{}", e.getMessage());
    }
  }

  private void initJNDI() {
    try {
      JsonObject handlerConfig = AppConfig.config.getJsonObject("handler");
      if (handlerConfig != null) {
        JsonObject reportConfig = handlerConfig.getJsonObject("report");
        if (reportConfig != null) {
          JNDIManager.init(reportConfig);
        }
      }
    } catch (Exception e) {
      log.error("Failed to initialize JNDI", e);
    }
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    log.debug("beforeDeployingVerticle");
  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    log.debug("afterVertxStarted");
  }

  @Override
  public void afterFailureToStartVertx(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
  }

  @Override
  public void afterFailureToDeployVerticle(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
    context.vertx().close();
  }
}