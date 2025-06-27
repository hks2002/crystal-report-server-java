#!/usr/bin/bash

ps aux | grep crystal-report-server-java.jar | head -n 1 | awk '{print $2}' | xargs kill -9
