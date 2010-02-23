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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.repository.audit.AuditEvent;

public interface AuditManager
{
    List<AuditEvent> getMostRecentAuditEvents( List<String> repositoryIds );

    void addAuditEvent( AuditEvent event );

    void deleteAuditEvents( String repositoryId );

    /**
     * Get all audit events from the given repositories that match a certain range
     *
     * @param repositoryIds the repositories to retrieve events for
     * @param startTime     find events only after this time
     * @param endTime       find events only before this time
     * @return the list of events found
     */
    List<AuditEvent> getAuditEventsInRange( Collection<String> repositoryIds, Date startTime, Date endTime );

    /**
     * Get all audit events from the given repositories that match a certain range and resource pattern
     *
     * @param repositoryIds   the repositories to retrieve events for
     * @param resourcePattern find all events whose resources start with this string
     * @param startTime       find events only after this time
     * @param endTime         find events only before this time
     * @return the list of events found
     */
    List<AuditEvent> getAuditEventsInRange( Collection<String> repositoryIds, String resourcePattern, Date startTime,
                                            Date endTime );
}