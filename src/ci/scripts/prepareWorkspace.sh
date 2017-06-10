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
ATTIC_DIRS=""
REMOVE_DIRS=".indexer"

for i in ${ATTIC_DIRS}; do
 if [ "X${i}" != "X" -a -d ${i} ]; then
   echo "Deleting directory ${i}"
   rm -rf ${i}
 fi
done

for i in ${REMOVE_DIRS}; do
  find . -type d -name "${i}" -print0 | xargs -0 rm -rvf
done
