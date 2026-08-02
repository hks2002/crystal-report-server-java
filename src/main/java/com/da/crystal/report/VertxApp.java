/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2025-03-27 16:01:51                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-01 03:06:22                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxApp {
  static public String[] appArgs = null;

  public static void main(String[] args) {

    List<String> list = new ArrayList<>();
    list.add("com.da.crystal.report.WebServerVerticle");
    list.addAll(Arrays.asList(args));
    appArgs = list.toArray(new String[0]);

    log.info("Starting VertxApplication with args: {}", list);

    VertxApplicationHooks hooks = new VertxAppHooks();
    VertxApplication app = new VertxApplication(appArgs, hooks);
    app.launch();
  }
}
