#!/usr/bin/bash

./shutdown-fat.sh
java -jar crystal-report-server-java-fat.jar --conf=config-prod.json --options=vertx-options.json

