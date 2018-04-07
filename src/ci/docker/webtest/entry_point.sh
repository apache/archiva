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
#  This is the start script for the docker image that runs the
#  selenium standalone server.
#  Starts a Xvfb framebuffer server for the browser display.
#  Uses the automatic display number determination from the xvfb-run script.


# Set the number for the xserver display to start
: ${X_START_NUM:-1}

export GEOMETRY="$SCREEN_WIDTH""x""$SCREEN_HEIGHT""x""$SCREEN_DEPTH"

function shutdown {
  kill -s SIGTERM $NODE_PID
  wait $NODE_PID
}

if [ ! -z "$SE_OPTS" ]; then
  echo "appending selenium options: ${SE_OPTS}"
fi

rm -f /tmp/.X*lock

export JAVA_OPTS
export SE_OPTS

xvfb-run -a -n $X_START_NUM --server-args="-screen 0 $GEOMETRY -ac +extension RANDR" \
  /opt/bin/x_run.sh &
NODE_PID=$!

trap shutdown SIGTERM SIGINT
wait $NODE_PID
