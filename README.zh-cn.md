# Crystal Report Server Java (水晶报表 Java 服务器)

![Github Version](https://img.shields.io/github/v/release/hks2002/crystal-report-server-java?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/crystal-report-server-java/Build.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/crystal-report-server-java)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079??style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

一个基于 java 的水晶报表服务器。

### 如何使用

1. 在 `application.properties` 中配置数据库连接信息。
2. 根据需要在 `pom.xml` 中添加数据库驱动。
3. 运行 `maven package` 生成 `war` 包。
4. 将 `war` 包拷贝到 tomcat 中运行。
5. 将水晶报表文件拷贝到 `WEB-INF/classes/reports` 文件夹中，可以随时添加。
6. 在浏览器中打开 `http://server:port/Report/{ReportTemplateName}/{format}?param0=val0&param1=val1`，即可看到报表。

> 说明：`{ReportTemplateName}` 是报表文件名，不包含扩展名；`{format}` 是报表格式，如 `pdf` `doc` `xls`；`param0` 和 `param1` 是报表参数，如果有一个参数名包含 `filename`，则该参数值将作为文件名。
