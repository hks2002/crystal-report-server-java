/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2022-03-25 15:19:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2023-06-28 10:58:43                                                                      *
 * @FilePath              : src/main/java/com/da/crystal/report/SrvInfoController.java                               *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.crystal.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class SrvInfoController {

    @Value("${project.name}")
    private String name;

    @Value("${project.version}")
    private String version;

    @Value("${spring.boot.version}")
    private String springBootVersion;

    @Value("${project.dependencies}")
    private String dependencies;

    @GetMapping("/Report/SrvVersion")
    public String getVersion() {
        return version;
    }

    @GetMapping("/Report/SrvName")
    public String getName() {
        return name;
    }

    @GetMapping("/Report/SrvInfo")
    public String getInfo() {
        return """
            {
                "name": "%s",
                "version": "%s",
                "springBootVersion": "%s"
            }
            """.formatted(
                name,
                version,
                springBootVersion
            );
    }

    @GetMapping("/Report/SrvProjectDependencies")
    public String getDependencies() {
        String str = "";
        str = dependencies.replace("Dependency", "");
        str = str.replaceAll("=", ":");
        str = str.replaceAll(":([^,}]+)", ":\"$1\"");
        return str;
    }
}
