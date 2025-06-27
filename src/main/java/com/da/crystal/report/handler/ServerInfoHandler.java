/******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                     *
 * @CreatedDate           : 2025-03-16 11:51:49                               *
 * @LastEditors           : Robert Huang<56649783@qq.com>                     *
 * @LastEditDate          : 2025-06-26 09:11:08                               *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                   *
 *****************************************************************************/

package com.da.crystal.report.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerInfoHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    context.response().sendFile("META-INF/MANIFEST.MF");
  }

}
