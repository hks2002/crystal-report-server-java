/*****************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                    *
 * @CreatedDate           : 2025-03-08 19:11:51                              *
 * @LastEditors           : Robert Huang<56649783@qq.com>                    *
 * @LastEditDate          : 2025-06-26 09:17:37                              *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                  *
 ****************************************************************************/

package com.da.crystal.report;

import com.da.crystal.report.handler.ReportHandler;
import com.da.crystal.report.handler.ServerInfoHandler;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebServerVerticle extends VerticleBase {
  @Override
  public Future<?> start() {
    JsonObject config = config();

    Router router = Router.router(vertx);
    router.route("/Report/*").handler(new ReportHandler(config.getJsonObject("handler")));
    router.route("/Report-api/server-info").handler(new ServerInfoHandler());

    return vertx.createHttpServer(new HttpServerOptions(config.getJsonObject("http")))
        .requestHandler(router)
        .listen().onSuccess(http -> {
          log.info("HTTP server started on port {}", http.actualPort());
        });
  }
}
