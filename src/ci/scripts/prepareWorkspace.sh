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
#  Date:   2017-05-24
#
#  Removes directories that are not used anymore.
##
ATTIC_DIRS="archiva-modules/archiva-base/archiva-indexer\
 archiva-modules/archiva-base/archiva-proxy-common\
 archiva-modules/archiva-base/archiva-maven2-common\
 archiva-modules/archiva-base/archiva-maven2-indexer\
 archiva-modules/archiva-base/archiva-maven2-metadata\
 archiva-modules/archiva-base/archiva-maven2-model\
 archiva-modules/archiva-base/archiva-proxy-maven\
 archiva-modules/archiva-scheduler/archiva-scheduler-indexing-maven2\
 archiva-modules/metadata/metadata-model-maven2\
 archiva-modules/plugins/maven2-repository\
 archiva-modules/archiva-base/archiva-converter\
 archiva-modules/archiva-base/archiva-consumers/archiva-lucene-consumer\
 archiva-modules/maven/archiva-converter\
"
REMOVE_DIRS=".indexer"
TMP_DIRECTORY=".tmp"

while [ ! -z "$1" ]; do
  case "$1" in
    -d)
      shift
      REPO_DIR=$1
      shift
      ;;
    *)
      shift
      ;;
  esac
done

if [ -e "${TMP_DIRECTORY}" ]; then
  rm -rf "${TMP_DIRECTORY}"
fi
mkdir -p "${TMP_DIRECTORY}"

if [ ! -z "${REPO_DIR}" ]; then
  if [ -d "${REPO_DIR}" ]; then
    rm -rf "${REPO_DIR}"
  else
    echo "WARNING: Directory not found ${REPO_DIR}"
  fi
fi

for i in ${ATTIC_DIRS}; do
 if [ "X${i}" != "X" -a -d ${i} ]; then
   echo "Deleting directory ${i}"
   rm -rf ${i}
 fi
done

for i in ${REMOVE_DIRS}; do
  find . -type d -name "${i}" -print0 | xargs -0 rm -rvf
done

TMP_DIRS="/tmp/archiva /var/tmp/archiva"
for MY_TMP in $TMP_DIRS; do
  if [ -e ${MY_TMP} ]; then
    echo "Trying to delete ${MY_TMP}"
    rm -rf ${MY_TMP} >/dev/null 2>&1
  fi
  if [ -e ${MY_TMP} ]; then
    echo "Trying to move ${MY_TMP} away"
    mv ${MY_TMP} ${MY_TMP}.$$
  fi
  if [ -e ${MY_TMP} ]; then
    echo "Warning there exists a temporary directory, that cannot be cleaned ${MY_TMP}"
    ls -latr ${MY_TMP}
    ls -latr $(dirname ${MY_TMP})
  fi
done

exit 0
