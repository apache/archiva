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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.audit.AuditManager"
 */
public class DefaultAuditManager
    implements AuditManager
{
    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    private static final int NUM_RECENT_REVENTS = 10;

    private static final Logger log = LoggerFactory.getLogger( DefaultAuditManager.class );

    public List<AuditEvent> getMostRecentAuditEvents( List<String> repositoryIds )
    {
        // TODO: consider a more efficient implementation that directly gets the last ten from the content repository
        List<AuditRecord> records = new ArrayList<AuditRecord>();
        for ( String repositoryId : repositoryIds )
        {
            List<String> timestamps = metadataRepository.getMetadataFacets( repositoryId, AuditEvent.FACET_ID );
            for ( String timestamp : timestamps )
            {
                records.add( new AuditRecord( repositoryId, timestamp ) );
            }
        }
        Collections.sort( records );
        records = records.subList( 0, records.size() < NUM_RECENT_REVENTS ? records.size() : NUM_RECENT_REVENTS );

        List<AuditEvent> events = new ArrayList<AuditEvent>( records.size() );
        for ( AuditRecord record : records )
        {
            AuditEvent auditEvent =
                (AuditEvent) metadataRepository.getMetadataFacet( record.repositoryId, AuditEvent.FACET_ID,
                                                                  record.name );
            events.add( auditEvent );
        }
        return events;
    }

    public void addAuditEvent( AuditEvent event )
    {
        // ignore those with no repository - they will still be logged to the textual audit log
        if ( event.getRepositoryId() != null )
        {
            metadataRepository.addMetadataFacet( event.getRepositoryId(), event );
        }
    }

    public void deleteAuditEvents( String repositoryId )
    {
        metadataRepository.removeMetadataFacets( repositoryId, AuditEvent.FACET_ID );
    }

    public List<AuditEvent> getAuditEventsInRange( Collection<String> repositoryIds, Date startTime, Date endTime )
    {
        List<AuditEvent> results = new ArrayList<AuditEvent>();
        for ( String repositoryId : repositoryIds )
        {
            List<String> list = metadataRepository.getMetadataFacets( repositoryId, AuditEvent.FACET_ID );
            for ( String name : list )
            {
                try
                {
                    Date date = new SimpleDateFormat( AuditEvent.TIMESTAMP_FORMAT ).parse( name );
                    if ( ( startTime == null || !date.before( startTime ) ) &&
                        ( endTime == null || !date.after( endTime ) ) )
                    {
                        AuditEvent event =
                            (AuditEvent) metadataRepository.getMetadataFacet( repositoryId, AuditEvent.FACET_ID, name );
                        results.add( event );
                    }
                }
                catch ( ParseException e )
                {
                    log.error( "Invalid audit event found in the metadata repository: " + e.getMessage() );
                    // continue and ignore this one
                }
            }
        }
        Collections.sort( results, new Comparator<AuditEvent>()
        {
            public int compare( AuditEvent o1, AuditEvent o2 )
            {
                return o2.getTimestamp().compareTo( o1.getTimestamp() );
            }
        } );
        return results;
    }

    public void setMetadataRepository( MetadataRepository metadataRepository )
    {
        this.metadataRepository = metadataRepository;
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

        public int compareTo( AuditRecord other )
        {
            // reverse ordering
            return other.name.compareTo( name );
        }
    }
}
