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
#  Date:   2017-04-16
#
#  Starts the chrome browser from the chrome installation path.
#

export GNOME_DISABLE_CRASH_DIALOG=SET_BY_GOOGLE_CHROME
export DBUS_SESSION_BUS_ADDRESS=/dev/null

# Make sure that the profile directory specified in the environment, if any,
# overrides the default.
if [[ -n "$CHROME_USER_DATA_DIR" ]]; then
  PROFILE_DIRECTORY_FLAG="--user-data-dir=$CHROME_USER_DATA_DIR"
fi

# Sanitize std{in,out,err} because they'll be shared with untrusted child
# processes (http://crbug.com/376567).
exec < /dev/null
exec > >(exec cat)
exec 2> >(exec cat >&2)

exec /opt/google/chrome/chrome --no-sandbox "$@"
