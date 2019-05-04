package org.apache.archiva.checksum;

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
 * Status of checksum update for specific algorithm.
 */
public class UpdateStatus {

    /**
     * Checksum file did not exist before and was created
     */
    public static final int CREATED = 1;
    /**
     * Checksum file existed, but content differed
     */
    public static final int UPDATED = 2;
    /**
     * Nothing changed
     */
    public static final int NONE = 0;
    /**
     * Error occured during update/creation of the checksum file
     */
    public static final int ERROR = -1;

    private final ChecksumAlgorithm algorithm;
    private final int status;
    private final Throwable error;

    public UpdateStatus(ChecksumAlgorithm algorithm) {
        this.algorithm = algorithm;
        status = NONE;
        error = null;
    }

    public UpdateStatus(ChecksumAlgorithm algorithm, int status) {
        this.algorithm = algorithm;
        this.status = status;
        error = null;
    }

    public UpdateStatus(ChecksumAlgorithm algorithm, Throwable error) {
        this.algorithm = algorithm;
        this.status = ERROR;
        this.error = error;
    }

    /**
     * Return the status value.
     * @return The value
     */
    public int getValue() {
        return status;
    }

    /**
     * Return error, if exists, otherwise <code>null</code> will be returned.
     * @return
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Return the algorithm, this status is assigned to.
     * @return The checksum algorithm
     */
    public ChecksumAlgorithm getAlgorithm() {
        return algorithm;
    }
}
