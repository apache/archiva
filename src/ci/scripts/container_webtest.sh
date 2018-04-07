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
#  For consistent testing environment, it's the best to build the image locally and push it to
#  the Docker hub repository. There exists one repository on Dockerhub: apachearchiva.
#  The login data for the Dockerhub ID has to be given by the environment variable: DOCKER_HUB_PW
#
#  The script first checks, if a image with the given CONTAINER_VERSION exists. If not, it tries to
#  pull the image from Docker hub. If the pull fails, it tries to build a local one.
#
#  If you have changes on the image, you should change CONTAINER_VERSION in this file, create it locally
#  and push it to the Dockerhub repository: apachearchiva/build-webtest:${CONTAINER_VERSION}
#  After that, push the changes to the git repository.
#
#  The script tries to cleanup orphaned images from the local docker repository, if the CONTAINER_VERSION
#  has changed.
#

# Always change the version, if your Dockerfile or scripts of the container change
CONTAINER_VERSION="1.3"
CONTAINER_NAME="archiva/selenium"
DOCKER_CFG="webtest"
INSTANCE_NAME="archiva-webtest"
PORT_MAPPING="4444:4444"
NETWORK_TYPE="host"
DOCKER_HUB_ID="apachearchiva"
DOCKER_HUB_REPO="${DOCKER_HUB_ID}/build-webtest"
DOCKER_HUB_TAG="${DOCKER_HUB_REPO}:${CONTAINER_VERSION}"

# Using high screen resolution to avoid scrolling bar in browser
export SCREEN_WIDTH="3840"
export SCREEN_HEIGHT="2160"

HERE=`dirname $0`

TAG="${CONTAINER_NAME}:${CONTAINER_VERSION}"
VERBOSE=1
REMOVE_LOCALLY=1
while [ ! -z "$1" ]; do
  case "$1" in
    -v)
       VERBOSE=0
       ;;
    -r)
       REMOVE_LOCALLY=0
       ;;
    *)
       START_ARG=$1
       ;;
  esac
  shift
done

docker -v

if [ -z "${DOCKER_HUB_PW}" ]; then
  echo "WARNING: The docker hub password is not provided on the environment."
fi


function cleanup_orphaned() {
  echo "Checking for orphaned images:"
  while read IMG; do
    echo "Removing ${IMG}"
    docker rmi "${IMG}"
  done < <(docker images "${CONTAINER_NAME}" | awk -vVER="${CONTAINER_VERSION}" '$2 !~ VER && $2 !~ /TAG/ { printf("%s:%s\n",$1,$2) }')
  while read IMG; do
    echo "Removing ${IMG}"
    docker rmi "${IMG}"
  done < <(docker images "${DOCKER_HUB_REPO}" | awk -vVER="${CONTAINER_VERSION}" '$2 !~ VER && $2 !~ /TAG/ { printf("%s:%s\n",$1,$2) }')
}

cleanup() {
  cleanup_orphaned
  docker logout 1>/dev/null 2>&1
}

trap cleanup EXIT

function docker_login() {
  if  [ ! -z "${DOCKER_HUB_PW}" ]; then
    echo "${DOCKER_HUB_PW}" | docker login --username "${DOCKER_HUB_ID}" --password-stdin 2>/dev/null
    if [ $? -ne 0 ]; then
      echo "Seems to be older docker version."
      docker login --username "${DOCKER_HUB_ID}" --password "${DOCKER_HUB_PW}"
    fi
  fi
}

function build_image() {
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
    IMG_ID=`docker images -q ${TAG}`
}

function get_image() {
    IMG_ID=$(docker images -q "${DOCKER_HUB_TAG}")
    if [ -z "${IMG_ID}" ]; then
      docker_login
      docker pull "${DOCKER_HUB_TAG}"
      docker logout
      IMG_ID=$(docker images -q "${DOCKER_HUB_TAG}")
      if [ -z "${IMG_ID}" ]; then
        echo "Could not load docker image from remote. Trying to build a local one."
        build_image
      else
        docker tag "${DOCKER_HUB_TAG}" "${TAG}"
      fi
    fi
}


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
  echo "ARGS: -d -e SCREEN_WIDTH=${SCREEN_WIDTH} -e SCREEN_HEIGHT=${SCREEN_HEIGHT} --net=${NETWORK_TYPE} -p ${PORT_MAPPING} --name ${INSTANCE_NAME} ${TAG}"
  docker run -d -e "SCREEN_WIDTH=${SCREEN_WIDTH}" -e "SCREEN_HEIGHT=${SCREEN_HEIGHT}" --net="${NETWORK_TYPE}" -p "${PORT_MAPPING}" --name "${INSTANCE_NAME}" "${TAG}"
}

function print_usage() {
  echo "container_webtest [-v] [-r] start|stop"
  echo "Starts or stops the container. Builds the images if necessary"
  echo "  -v: Print verbose information about the docker container and environment."
  echo "  -r: Remove a local found image and try to re-pull the image"
}

echo "Date: $(date)"
if [ $VERBOSE -eq 0 ]; then
  docker ps
  echo "netstat"
  netstat -anp |grep 4444
fi


if [ "${START_ARG}" == "start" ]; then
  # Removing old instances
  stop_instance
  IMG_ID=`docker images -q ${TAG}`
  # Build the image, if it does not exist
  if [ -z "${IMG_ID}" ]; then
    get_image
  elif [ $REMOVE_LOCALLY -eq 0 ]; then
    docker rmi "${TAG}" 1>/dev/null 2>&1
    docker rmi "${DOCKER_HUB_TAG}" 1>/dev/null 2>&1
    get_image
  fi
  # Starting
  start_instance
  if [ $? -ne 0 ]; then
    echo "Error from docker run"
  fi
  if [ $VERBOSE -eq 0 ]; then
    docker ps
  fi
  TIMEOUT=20
  RES=1
  while [ $RES -gt 0 -a $TIMEOUT -gt 0 ]; do
    sleep 0.2
    TIMEOUT=$((TIMEOUT-1))
    docker logs "${INSTANCE_NAME}" | tail -5 |  grep -q "Selenium Server is up and running"
    RES=$?
  done
  if [ $VERBOSE -eq 0 ]; then
    docker logs "${INSTANCE_NAME}"
    echo "netstat: "
    netstat -anp |grep 4444
    echo "Trying curl on Webdriver port: "
    curl "http://localhost:4444/wd/hub"
    echo "Result: "$?
  fi
elif [ "${START_ARG}" == "stop" ]; then
  if [ $VERBOSE -eq 0 ]; then
    docker logs "${INSTANCE_NAME}"
  fi
  stop_instance
else
  print_usage
fi

