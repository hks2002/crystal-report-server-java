#!/usr/bin/bash

./shutdown.sh
java -cp "./crystal-report-server-java.jar:lib/*" com.da.crystal.report.VertxApp --conf=config-prod.json --options=vertx-options.json
