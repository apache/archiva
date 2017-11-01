package org.apache.archiva.repository.features;

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

import java.time.Period;

/**
 *
 * This feature provides settings for artifact cleanup. This is meant mainly for snapshot artifacts,
 * that should be deleted after a time period.
 *
 */
public class ArtifactCleanupFeature implements RepositoryFeature<ArtifactCleanupFeature> {

    private boolean deleteReleasedSnapshots = false;
    private Period retentionPeriod = Period.ofDays(100);
    private int retentionCount = 2;

    public ArtifactCleanupFeature() {

    }

    public ArtifactCleanupFeature( boolean deleteReleasedSnapshots, Period retentionPeriod, int retentionCount) {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
        this.retentionPeriod = retentionPeriod;
        this.retentionCount = retentionCount;
    }

    @Override
    public ArtifactCleanupFeature get() {
        return this;
    }

    /**
     * Returns true, if snapshot artifacts should be deleted, when artifacts with release version
     * exist in one of the managed repositories.
     * @return True, if artifacts should be deleted after release, otherwise false.
     */
    public boolean isDeleteReleasedSnapshots() {
        return deleteReleasedSnapshots;
    }

    /**
     * Sets the flag for the deletion of released snapshot artifacts.
     * @param deleteReleasedSnapshots
     */
    public void setDeleteReleasedSnapshots(boolean deleteReleasedSnapshots) {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    }

    /**
     * Returns the amount of time after that, the (snapshot) artifacts can be deleted.
     *
     * @return The time period after that the artifacts can be deleted.
     */
    public Period getRetentionPeriod() {
        return retentionPeriod;
    }

    /**
     * Sets time period, after that artifacts can be deleted.
     * @param retentionPeriod
     */
    public void setRetentionPeriod( Period retentionPeriod ) {
        this.retentionPeriod = retentionPeriod;
    }

    /**
     * Sets the number of (snapshot) artifacts that should be kept, even if they are older
     * than the retention time.
     * @return The number of artifacts for a version that should be kept
     */
    public int getRetentionCount() {
        return retentionCount;
    }

    /**
     * Sets the number of artifacts that should be kept and not be deleted.
     *
     * @param retentionCount
     */
    public void setRetentionCount(int retentionCount) {
        this.retentionCount = retentionCount;
    }
}
