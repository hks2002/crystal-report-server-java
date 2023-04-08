# Crystal Report Server Java (水晶报表 Java 服务器)

![Github Version](https://img.shields.io/github/v/release/hks2002/crystal-report-server-java?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/crystal-report-server-java/Build.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/crystal-report-server-java)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079??style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

[English](./README.md) | [简体中文](./README.zh-cn.md)

一个基于 java 的水晶报表服务器。

### Linux 系统环境准备

1. 将需要到字体复制到`JAVA_HOME/lib/fonts`目录中, 并设置好权限。
    ```
    sudo chmod -R $JAVA_HOME/lib/fonts 755
    ```
2. 建立字体索引信息，更新字体缓存。
    - CentOS
    ```
    yum install -y fontconfig mkfontscale
    mkfontscale && mkfontdir && fc-cache -fv
    ```
    - Ubuntu
    ```
    sudo apt-get -y install fontconfig xfonts-utils
    mkfontscale && mkfontdir && fc-cache -fv
    ```

### 如何使用

1. 默认包含`postgres`，`mysql`， `sql server`驱动, 其他数据库需要在 `pom.xml` 中添加数据库驱动。
2. 在 `application.properties` 中配置数据库连接信息。
3. 运行 `maven package` 生成 `war` 包，将 `war` 包拷贝到 tomcat（或者其他容器） 中运行, `war`包是为了更新报表更便捷。
4. 将水晶报表`rpt`文件拷贝到 `WEB-INF/reports` 文件夹中，可以随时添加。
5. 在浏览器中打开 `http://server:port/Report/{ReportTemplateName}/{format}?param0=val0&param1=val1`，即可看到报表。

> 说明：`{ReportTemplateName}` 是报表模板文件名，不包含扩展名；`{format}` 是报表格式，如 `pdf` `doc` `xls`；`param0` 和 `param1` 是报表参数，如果有一个参数名包含 `filename`，则该参数值将作为文件名。
