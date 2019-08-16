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

import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 *
 */
@Service("auditManager#default")
public class DefaultAuditManager
    implements AuditManager
{
    private static final int NUM_RECENT_EVENTS = 10;

    private static final Logger log = LoggerFactory.getLogger( DefaultAuditManager.class );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    @Inject
    RepositorySessionFactory repositorySessionFactory;

    @Override
    public List<AuditEvent> getMostRecentAuditEvents( MetadataRepository metadataRepository,
                                                      List<String> repositoryIds )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            // TODO: consider a more efficient implementation that directly gets the last ten from the content repository
            List<AuditRecord> records = new ArrayList<>();
            for (String repositoryId : repositoryIds) {
                List<String> names = metadataRepository.getMetadataFacets(session, repositoryId, AuditEvent.FACET_ID);
                for (String name : names) {
                    records.add(new AuditRecord(repositoryId, name));
                }
            }
            Collections.sort(records);
            records = records.subList(0, records.size() < NUM_RECENT_EVENTS ? records.size() : NUM_RECENT_EVENTS);

            List<AuditEvent> events = new ArrayList<>(records.size());
            for (AuditRecord record : records) {
                AuditEvent auditEvent = (AuditEvent) metadataRepository.getMetadataFacet(session,
                        record.repositoryId,
                        AuditEvent.FACET_ID, record.name);
                events.add(auditEvent);
            }
            return events;
        }
    }

    @Override
    public void addAuditEvent( MetadataRepository repository, AuditEvent event )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            // ignore those with no repository - they will still be logged to the textual audit log
            if (event.getRepositoryId() != null) {
                repository.addMetadataFacet(session, event.getRepositoryId(), event);
            }
        }
    }

    @Override
    public void deleteAuditEvents( MetadataRepository metadataRepository, String repositoryId )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            metadataRepository.removeMetadataFacets(session, repositoryId, AuditEvent.FACET_ID);
        }
    }

    @Override
    public List<AuditEvent> getAuditEventsInRange( MetadataRepository metadataRepository,
                                                   Collection<String> repositoryIds, Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        return getAuditEventsInRange( metadataRepository, repositoryIds, null, startTime, endTime );
    }

    @Override
    public List<AuditEvent> getAuditEventsInRange( MetadataRepository metadataRepository,
                                                   Collection<String> repositoryIds, String resource, Date startTime,
                                                   Date endTime )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            List<AuditEvent> results = new ArrayList<>();
            for (String repositoryId : repositoryIds) {
                List<String> list = metadataRepository.getMetadataFacets(session, repositoryId, AuditEvent.FACET_ID);
                for (String name : list) {
                    try {
                        Date date = createNameFormat().parse(name);
                        if ((startTime == null || !date.before(startTime)) && (endTime == null || !date.after(
                                endTime))) {
                            AuditEvent event = (AuditEvent) metadataRepository.getMetadataFacet(session,
                                    repositoryId,
                                    AuditEvent.FACET_ID, name);

                            if (resource == null || event.getResource().startsWith(resource)) {
                                results.add(event);
                            }
                        }
                    } catch (ParseException e) {
                        log.error("Invalid audit event found in the metadata repository: {}", e.getMessage());
                        // continue and ignore this one
                    }
                }
            }
            Collections.sort(results, new Comparator<AuditEvent>() {
                @Override
                public int compare(AuditEvent o1, AuditEvent o2) {
                    return o2.getTimestamp().compareTo(o1.getTimestamp());
                }
            });
            return results;
        }
    }

    private static SimpleDateFormat createNameFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( AuditEvent.TIMESTAMP_FORMAT );
        fmt.setTimeZone( UTC_TIME_ZONE );
        return fmt;
    }

    private static final class AuditRecord
        implements Comparable<AuditRecord>
    {
        private String repositoryId;

        private String name;

        public AuditRecord( String repositoryId, String name )
        {
            this.repositoryId = repositoryId;
            this.name = name;
        }

        @Override
        public int compareTo( AuditRecord other )
        {
            // reverse ordering
            return other.name.compareTo( name );
        }
    }

    public RepositorySessionFactory getRepositorySessionFactory( )
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }
}
