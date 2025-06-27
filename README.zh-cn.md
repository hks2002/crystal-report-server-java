# Crystal Report Server Java (水晶报表 Java 服务器)

![Github Version](https://img.shields.io/github/v/release/hks2002/crystal-report-server-java?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/crystal-report-server-java/Release-Please.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/crystal-report-server-java)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079??style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

[English](./README.md) | [简体中文](./README.zh-cn.md)

一个基于 java 的水晶报表服务器。

### Linux 系统环境准备

1. 将需要到字体复制到`$JAVA_HOME/lib/fonts`目录中, 并设置好权限。
    ```
    sudo chmod -R $JAVA_HOME/lib/fonts 755
    ```
2. 建立字体索引信息，更新字体缓存(按需）。
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
3. 放开TLS传输层加密算法限制（按需）。
   如果你连接的数据库版本比较旧，而Linux系统比较新，可能会遇到TLS传输层算法限制的问题，

   编辑`JAVA_HOME/conf/security/java.security`, 删掉`jdk.tls.disabledAlgorithms`后面的特定的限制算法；
   编辑`/etc/crypto-policies/back-ends/java.config`, 删掉`jdk.tls.disabledAlgorithms`后面的特定的限制算法；

### 如何使用

1. 默认包含`postgres`，`mysql`， `sql server`驱动, 其他数据库需要在 `pom.xml` 中添加数据库驱动。
2. 目标文件夹下，上传`target/lib`(此lib包含所有的依赖)， 上传`*.rpt`报表文件到到`reports`文件夹。
3. 可在 `config-prod` 中配置数据库连接信息和报表模板位置。
4. 目标文件夹下运行 `java -jar crystal-report-server-java.jar --conf=config-prod.json`, 或者运行 `java -jar crystal-report-server-java-fat.jar --conf=config-prod.json`。
5. 在浏览器中打开 `http://server:port/Report/{ReportTemplateName}/{format}?param0=val0&param1=val1`，即可看到报表。

> 说明：`{ReportTemplateName}` 是报表模板文件名，不包含扩展名；`{format}` 是报表格式，如 `pdf` `doc` `xls`；`param0` 和 `param1` 是报表参数，如果有一个参数名包含 `filename`，则该参数值将作为文件名。
> 建议：使用`Command SQL`代替`Table Link`,可以获得更快的报表生成速度。
