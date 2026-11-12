/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 11:11:24                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/
package com.da.crystal.report;

import com.da.crystal.report.JNDI.DataSourceConfig;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AppConfig {

  static public JsonObject config = null;
  static public String report_path;
  static public DataSourceConfig dbConfig;

  private static final String DEFAULT_REPORTS_PATH = "/usr/share/java/crystal-report-server/reports";

  public static void setup(JsonObject config) {
    AppConfig.config = config;

    JsonObject reportConfig = config.getJsonObject("handler").getJsonObject("report");
    AppConfig.report_path = Utils.isWindows()
        ? reportConfig.getJsonObject("windows").getString("reportsPath", DEFAULT_REPORTS_PATH)
        : reportConfig.getJsonObject("linux").getString("reportsPath", DEFAULT_REPORTS_PATH);

    JsonObject databaseConfig = config.getJsonObject("database");
    AppConfig.dbConfig = new DataSourceConfig(
        databaseConfig.getString("url", "jdbc:mysql://localhost:3306/crystal_report"),
        databaseConfig.getString("driverClassName", "com.mysql.cj.jdbc.Driver"),
        databaseConfig.getString("user", "crystal_report"),
        databaseConfig.getString("password", "crystal_report"));
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