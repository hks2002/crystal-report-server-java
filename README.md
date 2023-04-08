# Crystal Report Server Java

![Github Version](https://img.shields.io/github/v/release/hks2002/crystal-report-server-java?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/crystal-report-server-java/Build.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/crystal-report-server-java)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079??style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

[English](./README.md) | [简体中文](./README.zh-cn.md)

An Crystal Report server run in java.

### Linux System Prepare

1. Copy fonts to`JAVA_HOME/lib/fonts`, and set permission to this folder。
    ```
    sudo chmod -R $JAVA_HOME/lib/fonts 755
    ```
2. Create fonts index, and update fonts cache。
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

### How to Use

1. It contains `postgres`，`mysql`， `sql server` driver by default, Add your db driver in `pom.xml` as necessary.
2. Config your database url, driverClassName, user, password in `application.properties`.
3. Run `maven package` to get `war` package.
4. Copy `war` to tomcat, and run tomcat.
5. Copy your crystal report files to `WEB-INF/reports` folder, you can add them at anytime.
6. Open `http://server:port/Report/{ReportTemplateName}/{format}?param0=val0&param1=val1` in browser, you can see your report.

> Note: `{ReportTemplateName}` is the name of your report template file without extension, `{format}` is the format you want, such as `pdf` `doc` `xls`, `param0` and `param1` are the parameters of your report, give a param named with `filename`, this param value will be used as the file name.
