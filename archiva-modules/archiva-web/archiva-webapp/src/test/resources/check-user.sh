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

# Just a simple script that checks, if a user exists by using the REST Api

BASE_URL="http://localhost:8080/archiva"
USER_NAME="admin"
PASSWD="admin456"

CHECK_USER=$1


#Authenticate
TOKEN=$(curl -s -X POST "${BASE_URL}/api/v2/redback/auth/authenticate" -H  "accept: application/json" -H  "Content-Type: application/json" \
 -d "{\"grant_type\":\"authorization_code\",\"client_id\":\"test-bash\",\"client_secret\":\"string\",\"code\":\"string\",\"scope\":\"string\",\"state\":\"string\",\"user_id\":\"${USER_NAME}\",\
 \"password\":\"${PASSWD}\",\"redirect_uri\":\"string\"}"|sed -n -e '/access_token/s/.*"access_token":"\([^"]\+\)".*/\1/gp')
if [ "${TOKEN}" == "" ]; then
    echo "Authentication failed!"
    exit 1
fi

echo curl -I -w ' - %{http_code}' "${BASE_URL}/api/v2/redback/users/${CHECK_USER}" -H  "accept: application/json" \
   -H  "Authorization: Bearer ${TOKEN}" \
   -H  "Content-Type: application/json"

curl -I -w ' - %{http_code}' "${BASE_URL}/api/v2/redback/users/${CHECK_USER}" -H  "accept: application/json" \
   -H  "Authorization: Bearer ${TOKEN}" \
   -H  "Content-Type: application/json"
