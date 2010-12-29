package org.apache.archiva.audit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface AuditManager
{
    List<AuditEvent> getMostRecentAuditEvents( MetadataRepository metadataRepository, List<String> repositoryIds )
        throws MetadataRepositoryException;

    void addAuditEvent( MetadataRepository repository, AuditEvent event )
        throws MetadataRepositoryException;

    void deleteAuditEvents( MetadataRepository metadataRepository, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get all audit events from the given repositories that match a certain range
     *
     * @param metadataRepository
     * @param repositoryIds      the repositories to retrieve events for
     * @param startTime          find events only after this time
     * @param endTime            find events only before this time
     * @return the list of events found
     */
    List<AuditEvent> getAuditEventsInRange( MetadataRepository metadataRepository, Collection<String> repositoryIds,
                                            Date startTime, Date endTime )
        throws MetadataRepositoryException;

    /**
     * Get all audit events from the given repositories that match a certain range and resource pattern
     *
     * @param metadataRepository
     * @param repositoryIds      the repositories to retrieve events for
     * @param resourcePattern    find all events whose resources start with this string
     * @param startTime          find events only after this time
     * @param endTime            find events only before this time
     * @return the list of events found
     */
    List<AuditEvent> getAuditEventsInRange( MetadataRepository metadataRepository, Collection<String> repositoryIds,
                                            String resourcePattern, Date startTime, Date endTime )
        throws MetadataRepositoryException;
}
