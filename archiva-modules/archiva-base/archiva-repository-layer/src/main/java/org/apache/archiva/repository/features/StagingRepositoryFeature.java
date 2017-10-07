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


import org.apache.archiva.repository.ManagedRepository;

/**
 * This feature provides some information about staging repositories.
 *
 */
public class StagingRepositoryFeature implements RepositoryFeature<StagingRepositoryFeature> {

    public static final String STAGING_REPO_POSTFIX = "-stage";

    private ManagedRepository stagingRepository = null;
    private boolean stageRepoNeeded = false;

    public StagingRepositoryFeature() {

    }

    public StagingRepositoryFeature(ManagedRepository stagingRepository, boolean stageRepoNeeded) {
        this.stagingRepository = stagingRepository;
        this.stageRepoNeeded = stageRepoNeeded;
    }

    @Override
    public StagingRepositoryFeature get() {
        return this;
    }

    /**
     * Returns the staging repository, if it exists.
     *
     * @return The staging repository, null if not set.
     *
     */
    public ManagedRepository getStagingRepository() {
        return stagingRepository;
    }

    /**
     * Sets the staging repository.
     *
     * @param stagingRepository
     */
    public void setStagingRepository(ManagedRepository stagingRepository) {
        this.stagingRepository = stagingRepository;
    }

    /**
     * Returns true, if a staging repository is needed by this repository.
     * @return True, if staging repository is needed, otherwise false.
     */
    public boolean isStageRepoNeeded() {
        return stageRepoNeeded;
    }

    /**
     * Sets the flag for needed staging repository.
     *
     * @param stageRepoNeeded
     */
    public void setStageRepoNeeded(boolean stageRepoNeeded) {
        this.stageRepoNeeded = stageRepoNeeded;
    }
}
