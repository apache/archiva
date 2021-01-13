#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Just a simple script to generate users on the local archiva instance for interactive UI testing

BASE_URL="http://localhost:8080/archiva"
USER_NAME="admin"
PASSWD="admin456"
USERS=50

#Authenticate
TOKEN=$(curl -s -X POST "${BASE_URL}/api/v2/redback/auth/authenticate" -H  "accept: application/json" -H  "Content-Type: application/json" \
 -d "{\"grant_type\":\"authorization_code\",\"client_id\":\"test-bash\",\"client_secret\":\"string\",\"code\":\"string\",\"scope\":\"string\",\"state\":\"string\",\"user_id\":\"${USER_NAME}\",\
 \"password\":\"${PASSWD}\",\"redirect_uri\":\"string\"}"|sed -n -e '/access_token/s/.*"access_token":"\([^"]\+\)".*/\1/gp')
if [ "${TOKEN}" == "" ]; then
    echo "Authentication failed!"
    exit 1
fi


NUM=$USERS
while [ $NUM -ge 0 ]; do
  SUFFIX=$(printf "%03d" $NUM)
  echo "User: test${SUFFIX}"
  curl -s -w ' - %{http_code}' -X POST "${BASE_URL}/api/v2/redback/users" -H  "accept: application/json" \
   -H  "Authorization: Bearer ${TOKEN}" \
   -H  "Content-Type: application/json" \
   -d "{\"user_id\":\"test${SUFFIX}\",\"full_name\":\"Test User ${SUFFIX}\",\"email\":\"test${SUFFIX}@test.org\",\"validated\":true,\"locked\":false,\"password_change_required\":false,\"password\":\"test123\"}"
  NUM=$((NUM-1))
  echo " "
  sleep 0.2 # Sleeping to get different creation timestamps
done