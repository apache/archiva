package org.apache.archiva.metadata.repository.stats;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager" role-hint="default"
 */
public class DefaultRepositoryStatisticsManager
    implements RepositoryStatisticsManager
{
    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryStatisticsManager.class );

    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    static final DateFormat SCAN_TIMESTAMP = new SimpleDateFormat( "yyyyMMdd.HHmmss.SSS" );

    public RepositoryStatistics getLastStatistics( String repositoryId )
    {
        // TODO: consider a more efficient implementation that directly gets the last one from the content repository
        List<String> scans = metadataRepository.getMetadataFacets( repositoryId, RepositoryStatistics.FACET_ID );
        Collections.sort( scans );
        if ( !scans.isEmpty() )
        {
            String name = scans.get( scans.size() - 1 );
            return (RepositoryStatistics) metadataRepository.getMetadataFacet( repositoryId,
                                                                               RepositoryStatistics.FACET_ID, name );
        }
        else
        {
            return null;
        }
    }

    public void addStatisticsAfterScan( String repositoryId, RepositoryStatistics repositoryStatistics )
    {
        // In the future, instead of being tied to a scan we might want to record information in the fly based on
        // events that are occurring. Even without these totals we could query much of the information on demand based
        // on information from the metadata content repository. In the mean time, we lock information in at scan time.
        // Note that if new types are later discoverable due to a code change or new plugin, historical stats will not
        // be updated and the repository will need to be rescanned.

        // TODO, populate these and also a count per artifact type
        // populate total artifact count from content repository
//        repositoryStatistics.setTotalArtifactCount(  );
        // populate total size from content repository
//        repositoryStatistics.setTotalArtifactFileSize(  );
        // populate total group count from content repository
//        repositoryStatistics.setTotalGroupCount(  );
        // populate total project count from content repository
//        repositoryStatistics.setTotalProjectCount(  );

        metadataRepository.addMetadataFacet( repositoryId, RepositoryStatistics.FACET_ID,
                                             SCAN_TIMESTAMP.format( repositoryStatistics.getScanStartTime() ),
                                             repositoryStatistics );
    }

    public void deleteStatistics( String repositoryId )
    {
        metadataRepository.removeMetadataFacets( repositoryId, RepositoryStatistics.FACET_ID );
    }

    public List<RepositoryStatistics> getStatisticsInRange( String repositoryId, Date startTime, Date endTime )
    {
        List<RepositoryStatistics> results = new ArrayList<RepositoryStatistics>();
        List<String> list = metadataRepository.getMetadataFacets( repositoryId, RepositoryStatistics.FACET_ID );
        Collections.sort( list, Collections.reverseOrder() );
        for ( String name : list )
        {
            try
            {
                Date date = SCAN_TIMESTAMP.parse( name );
                if ( !date.before( startTime ) && !date.after( endTime ) )
                {
                    RepositoryStatistics stats =
                        (RepositoryStatistics) metadataRepository.getMetadataFacet( repositoryId,
                                                                                    RepositoryStatistics.FACET_ID,
                                                                                    name );
                    results.add( stats );
                }
            }
            catch ( ParseException e )
            {
                log.error( "Invalid scan result found in the metadata repository: " + e.getMessage() );
                // continue and ignore this one
            }
        }
        return results;
    }

    public void setMetadataRepository( MetadataRepository metadataRepository )
    {
        this.metadataRepository = metadataRepository;
    }
}
