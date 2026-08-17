/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-17 00:00:00                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-17 19:01:00                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.JNDI;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DataSourceManager {

  private static volatile HikariDataSource hikariDataSource;
  private static volatile DataSource dataSource;

  private DataSourceManager() {
  }

  public static DataSource setup(JsonObject dsConfig) {
    HikariConfig hikariConfig = new HikariConfig();

    hikariConfig.setJdbcUrl(dsConfig.getString("url"));
    hikariConfig.setDriverClassName(dsConfig.getString("driverClassName"));
    hikariConfig.setUsername(dsConfig.getString("user"));
    hikariConfig.setPassword(dsConfig.getString("password"));

    JsonObject pool = dsConfig.getJsonObject("pool");
    if (pool != null) {
      if (pool.containsKey("maximumPoolSize")) {
        hikariConfig.setMaximumPoolSize(pool.getInteger("maximumPoolSize"));
      }
      if (pool.containsKey("minimumIdle")) {
        hikariConfig.setMinimumIdle(pool.getInteger("minimumIdle"));
      }
      if (pool.containsKey("connectionTimeout")) {
        hikariConfig.setConnectionTimeout(pool.getLong("connectionTimeout"));
      }
      if (pool.containsKey("idleTimeout")) {
        hikariConfig.setIdleTimeout(pool.getLong("idleTimeout"));
      }
      if (pool.containsKey("maxLifetime")) {
        hikariConfig.setMaxLifetime(pool.getLong("maxLifetime"));
      }
      if (pool.containsKey("leakDetectionThreshold")) {
        hikariConfig.setLeakDetectionThreshold(pool.getLong("leakDetectionThreshold"));
      }
      if (pool.containsKey("poolName")) {
        hikariConfig.setPoolName(pool.getString("poolName"));
      }
    }

    hikariDataSource = new HikariDataSource(hikariConfig);

    boolean sqlIntercept = dsConfig.getBoolean("sqlIntercept", false);
    if (sqlIntercept) {
      dataSource = new DataSourceProxy(hikariDataSource, sql -> sql);
      log.info("SQL intercept ENABLED");
    } else {
      dataSource = hikariDataSource;
      log.info("SQL intercept disabled");
    }

    log.info("HikariCP DataSource created");
    log.info("URL: {}", hikariConfig.getJdbcUrl());
    log.info("MaxPoolSize: {}", hikariConfig.getMaximumPoolSize());
    log.info("MinIdle: {}", hikariConfig.getMinimumIdle());

    return dataSource;
  }

  public static DataSource getDataSource() {
    return dataSource;
  }

  public static void shutdown() {
    if (hikariDataSource != null) {
      hikariDataSource.close();
      log.info("HikariCP DataSource closed");
    }
    hikariDataSource = null;
    dataSource = null;
  }

}