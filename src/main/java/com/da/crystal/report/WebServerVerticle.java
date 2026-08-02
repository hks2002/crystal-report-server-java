/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-08 19:11:51                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-07-31 21:16:55                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebServerVerticle extends VerticleBase {
  @Override
  public Future<?> start() {
    JsonObject config = config();

    return vertx.createHttpServer(new HttpServerOptions(config.getJsonObject("http")))
        .requestHandler(RootRouter.create(vertx))
        .listen().onSuccess(http -> {
          log.info("HTTP server started on port {}", http.actualPort());
        });
  }
}
