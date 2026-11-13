/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-11-13 22:16:23                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.crystal.report.handler;

import com.da.crystal.report.AppConfig;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerDepsHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    // "dependencies": "[Dependency {groupId=io.vertx,
    // artifactId=vertx-launcher-application, version=5.1.5, type=jar}, Dependency
    // {groupId=io.vertx, artifactId=vertx-config, version=5.1.5, type=jar}]";
    String dependencies = AppConfig.config.getString("dependencies");
    // 解析dependencies字符串为JsonArray
    // Maven格式: [Dependency {groupId=io.vertx, artifactId=vertx-config,
    // version=5.1.5, type=jar}, ...]
    dependencies = dependencies.replaceAll("Dependency", "");
    dependencies = dependencies.replaceAll("\\s+", "");
    // 单条正则: key=value → "key":"value"，value匹配到逗号或}为止
    dependencies = dependencies.replaceAll("(\\w+)=([^,}]+)", "\"$1\":\"$2\"");

    context.response()
        .putHeader("Content-Type", "application/json")
        .end(JsonObject.of("success", true, "data", new JsonArray(dependencies)).encode());
  }

}