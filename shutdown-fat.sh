#!/usr/bin/bash

ps aux | grep crystal-report-server-java-fat.jar | head -n 1 | awk '{print $2}' | xargs kill -9
