/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-05-19 16:54:08                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-06-25 20:11:05                                                                      *
 * @FilePath              : src/main/java/com/da/crystal/report/VertxAppHooks.java                                   *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.crystal.report;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
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
  public VertxBuilder createVertxBuilder(VertxOptions options) {
    log.info("VertxOptions:\n{}", options.toJson().encodePrettily());
    return Vertx.builder().with(options);
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    Vertx vertx = context.vertx();
    ConfigRetrieverOptions retrieveOptions = new ConfigRetrieverOptions();

    if (vertx.fileSystem().existsBlocking("config.json")) {
      retrieveOptions.addStore(new ConfigStoreOptions().setType("file")
          .setConfig(JsonObject.of("path", "config.json")));
    }
    if (vertx.fileSystem().existsBlocking("config-prod.json")) {
      retrieveOptions.addStore(new ConfigStoreOptions().setType("file")
          .setConfig(JsonObject.of("path", "config-prod.json")));
    }
    if (vertx.fileSystem().existsBlocking("config-test.json")) {
      retrieveOptions.addStore(new ConfigStoreOptions().setType("file")
          .setConfig(JsonObject.of("path", "config-test.json")));
    }
    ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, retrieveOptions);

    try {
      log.info("Loading config for verticle");
      // Load default config, ❗️❗️❗️ blocking call ❗️❗️❗️
      JsonObject defaultConfig = cfgRetriever.getConfig().toCompletionStage().toCompletableFuture().get();
      // this config could passing by command line with args
      // -config=#{absolutePath.Config}
      JsonObject deploymentConfig = context.deploymentOptions().getConfig();

      defaultConfig.getMap().forEach((k, v) -> {
        deploymentConfig.put(k, v);
      });
      log.info("Final Config: \n{}",
          deploymentConfig.encodePrettily()
              // .replaceAll("(\\\"user\\\" : \\\").*(\\\",)", "$1******$2")
              .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2"));

    } catch (Exception e) {
      log.error("{}", e);
    }

  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    // log.info("Hooray! VertxApplication Started! Running background tasks...");
    // FSUtils.setFolderInfo(context.vertx().fileSystem(), "Z:/", "Y:/", 2, 3,
    // "COPY");
    // FSUtils.cleanDBDoc(context.vertx().fileSystem(), "Y:/");
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
