package org.apache.archiva.admin.model.admin;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RepositoryTaskInfo;
import org.apache.archiva.admin.model.beans.ScanStatus;
import org.springframework.util.StopWatch;

import java.util.List;

/**
 * Interface for managing repository scan tasks.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public interface RepositoryTaskAdministration
{
    /**
     * Schedules a full repository scan for the given repository. Metadata and Index are updated.
     * All files are scanned, even if they were not modified since the last scan.
     *
     * @param repositoryId the repository identifier
     * @throws RepositoryAdminException if it was not possible to schedule the scan
     */
    void scheduleFullScan( String repositoryId ) throws RepositoryAdminException;

    /**
     * Schedules a scan that rebuilds the fulltext index of the repository.
     *
     * @param repositoryId the repository identifier
     * @throws RepositoryAdminException if it was not possible to schedule the index scan
     */
    void scheduleIndexFullScan( String repositoryId ) throws RepositoryAdminException;

    /**
     * Schedules a scan that rebuilds the fulltext index of the repository.
     *
     * @param repositoryId the repository identifier
     * @param relativePath the path to the file to add to the index
     * @throws RepositoryAdminException if it was not possible to schedule the index scan
     */
    void scheduleIndexScan( String repositoryId, String relativePath ) throws RepositoryAdminException;

    /**
     * Schedules a scan that rebuilds metadata of the repository
     *
     * @param repositoryId the repository identifier
     * @throws RepositoryAdminException if it was not possible to schedule the index scan
     */
    void scheduleMetadataFullScan( String repositoryId ) throws RepositoryAdminException;

    /**
     * Schedules a scan that rebuilds metadata of the repository but only for updated files.
     *
     * @param repositoryId the repository identifier
     * @throws RepositoryAdminException if it was not possible to schedule the index scan
     */
    void scheduleMetadataUpdateScan( String repositoryId ) throws RepositoryAdminException;

    /**
     * Returns information about currently running scans for the given repository.
     *
     * @return the status information
     * @param repositoryId the repository identifier
     * @throws RepositoryAdminException if there was an error retrieving the scan status
     */
    ScanStatus getCurrentScanStatus(String repositoryId) throws RepositoryAdminException;


    /**
     * Returns information about currently running scans for all repositories.
     *
     * @return the status information
     * @throws RepositoryAdminException if there was an error retrieving the scan status
     */
    ScanStatus getCurrentScanStatus() throws RepositoryAdminException;

    /**
     * Cancels the tasks either running or queued for the given repository.
     * @param repositoryId the repository identifier
     * @return a list of canceled tasks.
     */
    List<RepositoryTaskInfo> cancelTasks(String repositoryId) throws RepositoryAdminException;

    /**
     * Cancels the metadata scan tasks either running or queued for the given repository.
     * @param repositoryId the repository identifier
     * @return a list of canceled tasks.
     */
    List<RepositoryTaskInfo> cancelScanTasks(String repositoryId) throws RepositoryAdminException;

    /**
     * Cancels the indexing tasks either running or queued for the given repository.
     * @param repositoryId the repository identifier
     * @return a list of canceled tasks.
     */
    List<RepositoryTaskInfo> cancelIndexTasks(String repositoryId) throws RepositoryAdminException;
}
