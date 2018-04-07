#!/bin/bash

nohup fluxbox >/dev/null 2>&1 &
sleep 3
exec java ${JAVA_OPTS} -jar /opt/bin/selenium-server-standalone.jar ${SE_OPTS}