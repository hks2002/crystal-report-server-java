/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-03 00:00:00                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-17 11:57:56                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.crystal.report.JNDI.DataSourceManager;
import com.da.crystal.report.JNDI.JNDIManager;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class JNDIConnectionTest {

  @Test
  void jndiConnectionTest() throws Exception {
    JsonObject config = new JsonObject(
        new String(java.nio.file.Files.readAllBytes(
            java.nio.file.Path.of("src/main/resources/config-prod.json"))));

    JsonObject reportConfig = config.getJsonObject("handler").getJsonObject("report");

    JNDIManager.init(reportConfig);

    DataSource ds = DataSourceManager.getDataSource();
    assert ds != null : "DataSource should not be null";

    try (Connection conn = ds.getConnection()) {
      assert conn != null : "Connection should not be null";
      assert !conn.isClosed() : "Connection should be open";
      log.info("JNDI DataSource connected successfully");
      log.info("  URL: {}", conn.getMetaData().getURL());
      log.info("  DB Product: {}", conn.getMetaData().getDatabaseProductName());

      try (Statement stmt = conn.createStatement()) {
        stmt.execute("SELECT 1");
        log.info("  Query executed successfully");
      }
    }

    JNDIManager.shutdown();
  }
}