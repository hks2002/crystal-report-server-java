# Crystal Report Server Java

![Github Version](https://img.shields.io/github/v/release/hks2002/crystal-report-server-java?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/crystal-report-server-java/Build-Test-Release.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/crystal-report-server-java)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079??style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

[English](./README.md) | [简体中文](./README.zh-cn.md)

A Crystal Report server run in java.

### Linux System Prepare

1. Copy fonts to`$JAVA_HOME/lib/fonts`, and set permission to this folder
    ```
    sudo chmod -R $JAVA_HOME/lib/fonts 755
    ```
2. Create fonts index, and update fonts cache (As Required)
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
3. Allow the TLS disabled algorithms (As Required)
   If the connect database version is too old, and the running Linux system is new, you maybe will have the TLS connection issue by disabled algorithms.

   Edit`JAVA_HOME/conf/security/java.security`, Delete `jdk.tls.disabledAlgorithms`disabled algorithms value；
   Edit`/etc/crypto-policies/back-ends/java.config`, Delete `jdk.tls.disabledAlgorithms`disabled algorithms value；   

### How to Use

1. It contains `postgres`，`mysql`， `sql server` driver by default, Add your db driver in `pom.xml` as necessary.
2. Upload `crystal-report-server-java.jar` to `SERVER_FOLDER`.
3. Upload `lib` (crystal report dependencies) to `SERVER_FOLDER/lib`.
4. Upload `*.rpt` (report template files) to `SERVER_FOLDER/reports`.
5. Upload `config-prod.json` (config file) to `SERVER_FOLDER`, configure database connection info and report template location.
6. Run `java -cp "./crystal-report-server-java.jar:lib/*" com.da.crystal.report.VertxApp --conf=config-prod.json --options=vertx-options.json` in `SERVER_FOLDER`.
7. Open `http://server:port/Report/{ReportTemplateName}/{format}?param0=val0&param1=val1` in browser, you can see your report.

> Note: `{ReportTemplateName}` is the name of your report template file without extension, `{format}` is the format you want, such as `pdf` `doc` `xls`, `param0` and `param1` are the parameters of your report, give a param named with `filename`, this param value will be used as the file name.
> Suggestion: Using `Command SQL` instead of `Table Link` for better performance, reduce the report generation duration.