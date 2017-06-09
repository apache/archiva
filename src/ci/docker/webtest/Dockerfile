#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
#  Author: Martin Stockhammer <martin_s@apache.org>
#  Date:   2017-04-16
#
#  Dockerfile for ci testing of the web modules.
#  Currently only chrome browser is installed into the image.
#  Uses selenium version 2.53.1

FROM ubuntu:yakkety
MAINTAINER Apache Archiva <dev@archiva.apache.org>

ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_NONINTERACTIVE_SEEN true

ARG CHROME_VERSION=google-chrome-stable
RUN apt-get -qqy update 
RUN apt-get -qqy install apt-utils >/dev/null 2>&1
RUN apt-get -qqy install wget unzip >/dev/null
RUN apt-get -qqy upgrade
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
  && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
  && apt-get -qqy update \
  && apt-get -qqy install \
    $CHROME_VERSION \
  >/dev/null


ARG CHROME_DRIVER_VERSION=2.29
RUN wget --no-verbose -O /tmp/chromedriver_linux64.zip https://chromedriver.storage.googleapis.com/$CHROME_DRIVER_VERSION/chromedriver_linux64.zip \
  && rm -rf /opt/selenium/chromedriver \
  && unzip /tmp/chromedriver_linux64.zip -d /opt/selenium \
  && rm /tmp/chromedriver_linux64.zip \
  && mv /opt/selenium/chromedriver /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION \
  && chmod 755 /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION \
  && ln -fs /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION /usr/bin/chromedriver \
  >/dev/null

RUN apt-get -qqy install openjdk-8-jdk xvfb dbus locales >/dev/null \
  && rm -rf /var/lib/apt/lists/* /var/cache/apt/* \
  >/dev/null

ARG SELENIUM_VERSION=2.53.1
RUN mkdir -p /opt/bin && wget --no-verbose -O /opt/bin/selenium-server-standalone.jar https://selenium-release.storage.googleapis.com/2.53/selenium-server-standalone-$SELENIUM_VERSION.jar \
  && chmod 644 /opt/bin/selenium-server-standalone.jar

COPY entry_point.sh /opt/bin/entry_point.sh
COPY chrome_launcher.sh /usr/bin/google-chrome
RUN chmod +x /opt/bin/entry_point.sh
RUN chmod +x /usr/bin/google-chrome

ENV SCREEN_WIDTH 1600
ENV SCREEN_HEIGHT 1200
ENV SCREEN_DEPTH 24
ENV X_START_NUM=3

RUN echo "DBUS_SESSION_BUS_ADDRESS=/dev/null" >> /etc/environment

CMD ["/opt/bin/entry_point.sh"]


