/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-08-17 00:00:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-12 10:49:01                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.crystal.report.JNDI;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Log4j2
public class DataSourceManager {
  private static volatile DataSource ds;

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

    log.info("HikariConfig: {}", hikariConfig);

    boolean sqlLog = dsConfig.getBoolean("sqlLog", false);
    if (sqlLog) {
      log.info("SQL logging ENABLED");
      ProxyDataSourceBuilder builder = ProxyDataSourceBuilder.create(new HikariDataSource(hikariConfig))
          .name("CR")
          .logQueryBySlf4j()
          .logSlowQueryBySlf4j(5, TimeUnit.SECONDS)
          .multiline();

      ds = builder.build();
    } else {
      log.info("SQL logging disabled, set it to true in the config if you want to observe the SQL statements");
      ds = new HikariDataSource(hikariConfig);
    }

    return ds;
  }

  public static DataSource getDs() {
    return ds;
  }

  public static void shutdown() {
    if (ds != null) {
      try {
        if (ds instanceof AutoCloseable) {
          ((AutoCloseable) ds).close();
          log.info("DataSource closed");
        } else {
          log.warn("DataSource is not AutoCloseable, nothing to close");
        }
      } catch (Exception e) {
        log.error("Failed to close DataSource: {}", e.getMessage(), e);
      }
      ds = null;
    }
  }

}