/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-09 23:29:08                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-01 11:00:25                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AppConfig {

  static public JsonObject config = null;

  public static void setup(JsonObject config) {
    AppConfig.config = config;
  }

  public static void setupPeriod(Vertx vertx, String configPath) {

    ConfigStoreOptions storeOptions = new ConfigStoreOptions()
        .setOptional(true)
        .setType("file").setFormat("json")
        .setConfig(JsonObject.of("path", configPath));

    ConfigRetrieverOptions retrieveOptions = new ConfigRetrieverOptions()
        .addStore(storeOptions)
        .setScanPeriod(3000);

    ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, retrieveOptions);
    // The first time to load the config
    cfgRetriever.getConfig().onSuccess(conf -> {
      setup(conf);
    });

    cfgRetriever.listen(change -> {
      JsonObject newConf = change.getNewConfiguration();
      if (newConf.equals(AppConfig.config)) {
        return;
      }
      setup(newConf);
    });
  }

  public static void printConfig() {
    if (AppConfig.config == null) {
      log.info("Config is not loaded yet");
      return;
    }
    String safeStr = AppConfig.config
        .encodePrettily()
        .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2");
    log.info("Current Config: \n{}", safeStr);
  }

}