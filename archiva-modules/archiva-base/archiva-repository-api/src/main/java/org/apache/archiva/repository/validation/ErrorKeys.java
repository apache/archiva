package org.apache.archiva.repository.validation;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.components.registry.Registry;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ErrorKeys
{
    String ISNULL = "isnull";
    String ISEMPTY = "empty";
    String EXISTS = "exists";
    String MANAGED_REPOSITORY_EXISTS = "managed_repo_exists";
    String REMOTE_REPOSITORY_EXISTS = "remote_repo_exists";
    String REPOSITORY_GROUP_EXISTS = "group_exists";
    String MAX_LENGTH_EXCEEDED = "max_length";
    String INVALID_CHARS = "invalid_chars";
    String BELOW_MIN = "min";
    String INVALID_SCHEDULING_EXPRESSION = "invalid_scheduling_exp";
    String INVALID_LOCATION = "invalid_location";
}
