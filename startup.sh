#!/usr/bin/bash

./shutdown.sh
java -jar crystal-report-server-java.jar --conf=config-prod.json --options=vertx-options.json &
