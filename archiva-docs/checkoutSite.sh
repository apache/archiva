#!/bin/bash
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
#  Date:   2018-11-03
#
# This script runs a sparse git clone of a remote repository and
# initializes the git configuration.
#
# It is mainly used for site content creation, because the main archiva-web-content repository
# is rather large and we don't want to checkout the complete data.
#

SITE_DIR=".site-content"
GIT_REMOTE=""

GIT_USER=$(git config user.name)
GIT_EMAIL=$(git config user.email)

GIT_PATTERN_FILE="git-sparse-checkout-pattern"
GIT_PATTERN_DEST=".git/info/sparse-checkout"

MY_PWD=$(pwd)

CLONE=1
FORCE=1
MODULE_DIR="${MY_PWD}"
PATTERN=""
BRANCH="master"
while [ ! -z "$1" ]; do
  case "$1" in
    -f) 
      FORCE=0
      shift
      ;;
    -d)
      shift
      SITE_DIR="$1"
      shift
      ;;
    -p)
      shift
      if [ -z "${PATTERN}" ]; then
        PATTERN="${1}"
      else
        PATTERN="${PATTERN}\n${1}"
      fi
      shift
      ;;
    -m)
      shift
      MODULE_DIR="$1"
      shift
      ;;
    -b)
      shift
      BRANCH="$1"
      shift
      ;;
    *)
      GIT_REMOTE="$1"
      shift
      ;; 
  esac
done

print_usage() {
  echo "checkoutRepo [-m MODULE_DIR] [-d SITE_DIR]  [-f] GIT_URL"
  echo " -m: The module directory where the pattern file can be found and the site dir will be created."
  echo " -d SITE_DIR: Use the given directory for checkout"
  echo " -f: Force clone, even if directory exists"
}

if [ ! -f "${MODULE_DIR}/pom.xml" ]; then
  echo "Looks like the working directory is not a valid dir. No pom.xml found."
  exit 1
fi

cd "${MODULE_DIR}" || { echo "Could not change to module directory ${MODULE_DIR}"; exit 1; }

if [ -z "$GIT_REMOTE" ]; then
  print_usage
  exit 1
fi

if [ "${GIT_REMOTE:0:8}" == "scm:git:" ]; then
  GIT_REMOTE="${GIT_REMOTE:8}"
fi


if [ -d "${SITE_DIR}" ]; then
  if [ ! -d "${SITE_DIR}/.git" ]; then
    echo "Directory ${SITE_DIR} exist already, but is not a git clone. Aborting."
    exit 1
  elif [ "$FORCE" -eq 0 ]; then
    CLONE=0
  fi
else
  CLONE=0
fi

if [ $CLONE -eq 0 ]; then
  git clone "${GIT_REMOTE}" "${SITE_DIR}" --no-checkout
  if [ $? -ne 0 ]; then
    echo "Git clone failed"
    exit 1
  fi
fi

cd "${SITE_DIR}" || { echo "Could not change to site dir ${SITE_DIR}"; exit 1; }

git checkout "${BRANCH}"

git config core.sparsecheckout true
git config user.name "${GIT_USER}"
git config user.email "${GIT_EMAIL}"

if [ ! -z "${PATTERN}" ]; then
    echo -e "${PATTERN}" >"${GIT_PATTERN_DEST}"
elif [ -f "../${GIT_PATTERN_FILE}" ]; then
  cp "../${GIT_PATTERN_FILE}" "${GIT_PATTERN_DEST}"
fi

git checkout --

cd "${MY_PWD}"

