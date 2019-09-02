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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.model.DefaultRepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsProvider;
import org.apache.archiva.metadata.repository.stats.model.RepositoryWalkingStatisticsProvider;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 *
 */
@Service("repositoryStatisticsManager#default")
public class DefaultRepositoryStatisticsManager
    implements RepositoryStatisticsManager
{
    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryStatisticsManager.class );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private RepositoryWalkingStatisticsProvider walkingProvider = new RepositoryWalkingStatisticsProvider();



    @Inject
    RepositorySessionFactory repositorySessionFactory;

    @Override
    public boolean hasStatistics( String repositoryId )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            final MetadataRepository metadataRepository = session.getRepository( );
            return metadataRepository.hasMetadataFacet(session, repositoryId, DefaultRepositoryStatistics.FACET_ID);
        }
    }

    @Override
    public RepositoryStatistics getLastStatistics( String repositoryId )
        throws MetadataRepositoryException
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            final MetadataRepository metadataRepository = session.getRepository( );

            // TODO: consider a more efficient implementation that directly gets the last one from the content repository
            List<String> scans = metadataRepository.getMetadataFacets(session, repositoryId, DefaultRepositoryStatistics.FACET_ID);
            if (scans == null) {
                return null;
            }
            Collections.sort(scans);
            if (!scans.isEmpty()) {
                String name = scans.get(scans.size() - 1);
                RepositoryStatistics repositoryStatistics =
                        RepositoryStatistics.class.cast(metadataRepository.getMetadataFacet(session, repositoryId,
                                RepositoryStatistics.FACET_ID, name));
                stopWatch.stop();
                log.debug("time to find last RepositoryStatistics: {} ms", stopWatch.getTime());
                return repositoryStatistics;
            } else {
                return null;
            }
        }
    }

    @Override
    public void addStatisticsAfterScan( String repositoryId, Date startTime,
                                        Date endTime, long totalFiles, long newFiles )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            final MetadataRepository metadataRepository = session.getRepository( );

            DefaultRepositoryStatistics repositoryStatistics = new DefaultRepositoryStatistics();
            repositoryStatistics.setRepositoryId(repositoryId);
            repositoryStatistics.setScanStartTime(startTime);
            repositoryStatistics.setScanEndTime(endTime);
            repositoryStatistics.setTotalFileCount(totalFiles);
            repositoryStatistics.setNewFileCount(newFiles);

            // TODO
            // In the future, instead of being tied to a scan we might want to record information in the fly based on
            // events that are occurring. Even without these totals we could query much of the information on demand based
            // on information from the metadata content repository. In the mean time, we lock information in at scan time.
            // Note that if new types are later discoverable due to a code change or new plugin, historical stats will not
            // be updated and the repository will need to be rescanned.

            long startGather = System.currentTimeMillis();

            if (metadataRepository instanceof RepositoryStatisticsProvider) {
                ((RepositoryStatisticsProvider) metadataRepository).populateStatistics(session,
                        metadataRepository, repositoryId, repositoryStatistics);
            } else {
                walkingProvider.populateStatistics(session, metadataRepository, repositoryId, repositoryStatistics);
            }

            log.info("Gathering statistics executed in {} ms", (System.currentTimeMillis() - startGather));

            metadataRepository.addMetadataFacet(session, repositoryId, repositoryStatistics);
        }
    }

    @Override
    public void deleteStatistics( String repositoryId )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            final MetadataRepository metadataRepository = session.getRepository( );
            metadataRepository.removeMetadataFacets(session, repositoryId, DefaultRepositoryStatistics.FACET_ID);
        }
    }

    @Override
    public List<RepositoryStatistics> getStatisticsInRange( String repositoryId,
                                                            Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        try(RepositorySession session = repositorySessionFactory.createSession()) {
            final MetadataRepository metadataRepository = session.getRepository( );
            List<RepositoryStatistics> results = new ArrayList<>();
            List<String> list = metadataRepository.getMetadataFacets(session, repositoryId, DefaultRepositoryStatistics.FACET_ID);
            Collections.sort(list, Collections.reverseOrder());
            for (String name : list) {
                try {
                    Date date = createNameFormat().parse(name);
                    if ((startTime == null || !date.before(startTime)) && (endTime == null || !date.after(
                            endTime))) {
                        RepositoryStatistics stats =
                                (RepositoryStatistics) metadataRepository.getMetadataFacet(session,
                                        repositoryId,
                                        DefaultRepositoryStatistics.FACET_ID, name);
                        results.add(stats);
                    }
                } catch (ParseException e) {
                    log.error("Invalid scan result found in the metadata repository: {}", e.getMessage());
                    // continue and ignore this one
                }
            }
            return results;
        }
    }

    private static SimpleDateFormat createNameFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( DefaultRepositoryStatistics.SCAN_TIMESTAMP_FORMAT );
        fmt.setTimeZone( UTC_TIME_ZONE );
        return fmt;
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
