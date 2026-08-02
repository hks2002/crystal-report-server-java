/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-16 11:51:49                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-07-31 21:20:28                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.handler;

import com.da.crystal.report.AppConfig;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerInfoHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    var srvInfo = AppConfig.config.getJsonObject("srvInfo");
    context.response().end(srvInfo.encode());
  }

}
