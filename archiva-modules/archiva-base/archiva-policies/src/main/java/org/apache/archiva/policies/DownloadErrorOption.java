package org.apache.archiva.policies;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Options for download error update policies
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public enum DownloadErrorOption implements PolicyOption {

    STOP("stop"),QUEUE("queue-error"),IGNORE("ignore"),ALWAYS("always"), NOT_PRESENT("not-present");

    private final String id;

    DownloadErrorOption(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public PolicyOption ofId(String id) {
        for (StandardOption option : StandardOption.values()) {
            if (option.getId().equals(id)) {
                return option;
            }
        }
        return StandardOption.NOOP;
    }

    @Override
    public String toString() {
        return id;
    }
}
