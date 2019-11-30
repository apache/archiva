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
#  Date:   2018-11-15
#
#  Publishes the site content and generated reports to the web content repository.
#  It stops after the staging and let you check the content before pushing to the repository
#

THIS_DIR=$(dirname $0)
THIS_DIR=$(readlink -f ${THIS_DIR})
CONTENT_DIR=".site-content"
BRANCH="asf-staging-3.0"
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
PUBLISH_PATH=$(mvn help:evaluate -Dexpression=scmPublishPath -q -DforceStdout)
BRANCH=$(mvn help:evaluate -Dexpression=scmPublishBranch -q -DforceStdout)
CONTENT_DIR=$(mvn help:evaluate -Dexpression=scmPubCheckoutDirectory -q -DforceStdout)

if [ -d "${CONTENT_DIR}/.git" ]; then
  git -C "${CONTENT_DIR}" fetch origin
  git -C "${CONTENT_DIR}" checkout ${BRANCH}
  git -C "${CONTENT_DIR}" reset --hard origin/${BRANCH}
  git -C "${CONTENT_DIR}" clean -f -d
fi

echo ">>>> Creating site and reports <<<<" 
mvn clean site "$@"
mvn site:stage "$@"

echo "*****************************************"
echo ">>>> Finished the site stage process <<<<"
echo "> You can check the content in the folder target/staging or by opening the following url"
echo "> file://${THIS_DIR}/target/staging${PUBLISH_PATH}/index.html"
echo "> "
echo "> If everything is fine enter yes. After that the publish process will be started."
echo -n "Do you want to publish (yes/no)? "
read ANSWER

if [ "${ANSWER}" == "yes" -o "${ANSWER}" == "YES" -o "${ANSWER}" == "y" -o "${ANSWER}" == "Y" ]; then
  echo "> Starting publish process"
  mvn scm-publish:publish-scm "$@"
else
  echo "> Aborting now"
  echo "> Running git reset in .site-content directory" 
  git -C "${CONTENT_DIR}" fetch origin
  git -C "${CONTENT_DIR}" reset --hard origin/${BRANCH}
  git -C "${CONTENT_DIR}" clean -f -d
  echo ">>>> Finished <<<<"
fi

