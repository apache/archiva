#!/usr/bin/env bash
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
#  Builds and runs a container
#
#  Tries to find the docker configuration in ../docker/${DOCKER_CFG}
#

# Always change the version, if your Dockerfile or scripts of the container change
CONTAINER_VERSION="1.0"
CONTAINER_NAME="archiva/selenium"
DOCKER_CFG="webtest"
INSTANCE_NAME="archiva-webtest"
PORT_MAPPING="4444:4444"
NETWORK_TYPE="host"

# Using high screen resolution to avoid scrolling bar in browser
export SCREEN_WIDTH="3840"
export SCREEN_HEIGHT="2160"

HERE=`dirname $0`

TAG="${CONTAINER_NAME}:${CONTAINER_VERSION}"

START_ARG="$1"

docker -v

function stop_instance() {
  CONT=`docker ps -q --filter=name=${INSTANCE_NAME}`
  if [ "${CONT}" != "" ]; then
    echo "Stopping container ${INSTANCE_NAME}"
    docker stop "${INSTANCE_NAME}"
  fi
  # We remove the instance always
  CONT=`docker ps -a -q --filter=name=${INSTANCE_NAME}`
  if [ "${CONT}" != "" ]; then
    echo "Removing container ${INSTANCE_NAME}"
    docker rm "${INSTANCE_NAME}"
  fi
}

function start_instance() {
  echo "Starting container ${INSTANCE_NAME}"
  docker run -d --net="${NETWORK_TYPE}" -p "${PORT_MAPPING}" --name "${INSTANCE_NAME}" "${TAG}"
}

function print_usage() {
  echo "container_webtest start|stop"
  echo "Starts or stops the container. Builds the images if necessary"
}

if [ "${START_ARG}" == "start" ]; then
  IMG=`docker images -q ${TAG}`
  # Build the image, if it does not exist
  if [ "${IMG}" == "" ]; then
    echo "Building image ${TAG}"
    DOCKER_DIR="${HERE}/../docker/${DOCKER_CFG}"
    MY_PWD=`pwd`
    cd ${DOCKER_DIR} || { echo "Could not change to docker directory ${DOCKER_CFG}"; exit 1; }
    docker build --force-rm -t "${TAG}" .
    if [ $? -ne 0 ]; then
      cd ${MY_PWD}
      echo "Could not build docker image"
      exit 1
    fi
    cd ${MY_PWD}
    IMG=`docker images -q ${TAG}`
  fi
  # Removing old instances
  stop_instance
  # Starting
  start_instance
elif [ "${START_ARG}" == "stop" ]; then
  stop_instance
else
  print_usage
fi

