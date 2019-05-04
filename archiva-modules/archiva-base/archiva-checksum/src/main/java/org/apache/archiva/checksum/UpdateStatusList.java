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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Container for a list of update status objects.
 *
 * If there is a overall error that is not specific to a algorithm, the total status
 * flag is set to error.
 */
public class UpdateStatusList {

    private int totalStatus = UpdateStatus.NONE;
    private Throwable error;
    private Map<ChecksumAlgorithm, UpdateStatus> statusList = new TreeMap<>();

    public UpdateStatusList() {

    }

    public void addStatus(UpdateStatus status) {
        statusList.put(status.getAlgorithm(), status);
    }

    public static UpdateStatusList INITIALIZE(List<ChecksumAlgorithm> algorithms) {
        final UpdateStatusList list = new UpdateStatusList();
        for(ChecksumAlgorithm algorithm : algorithms) {
            list.addStatus(new UpdateStatus(algorithm));
        }
        return list;
    }

    public int getTotalStatus() {
        return totalStatus;
    }

    public void setTotalError(Throwable e) {
        this.error = e;
        this.totalStatus = UpdateStatus.ERROR;
    }

    public Throwable getTotalError() {
        return error;
    }

    public List<UpdateStatus> getStatusList() {
        return new ArrayList(statusList.values());
    }

    public void setStatus(ChecksumAlgorithm algorithm, UpdateStatus status) {
        statusList.put(algorithm, status);
    }

    public void setStatus(ChecksumAlgorithm algorithm, int status) {
        statusList.put(algorithm, new UpdateStatus(algorithm, status));
    }

    public void setErrorStatus(ChecksumAlgorithm algorithm, Throwable e) {
        statusList.put(algorithm, new UpdateStatus(algorithm,e));
    }

    public UpdateStatus getStatus(ChecksumAlgorithm algorithm) {
        return statusList.get(algorithm);
    }
}
