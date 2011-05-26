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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

/**
 * plexus.component role="org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager" role-hint="default"
 */
@Service("repositoryStatisticsManager#default")
public class DefaultRepositoryStatisticsManager
    implements RepositoryStatisticsManager
{
    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryStatisticsManager.class );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    public RepositoryStatistics getLastStatistics( MetadataRepository metadataRepository, String repositoryId )
        throws MetadataRepositoryException
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

    private void walkRepository( MetadataRepository metadataRepository, RepositoryStatistics stats, String repositoryId,
                                 String ns )
        throws MetadataResolutionException
    {
        for ( String namespace : metadataRepository.getNamespaces( repositoryId, ns ) )
        {
            walkRepository( metadataRepository, stats, repositoryId, ns + "." + namespace );
        }

        Collection<String> projects = metadataRepository.getProjects( repositoryId, ns );
        if ( !projects.isEmpty() )
        {
            stats.setTotalGroupCount( stats.getTotalGroupCount() + 1 );
            stats.setTotalProjectCount( stats.getTotalProjectCount() + projects.size() );

            for ( String project : projects )
            {
                for ( String version : metadataRepository.getProjectVersions( repositoryId, ns, project ) )
                {
                    for ( ArtifactMetadata artifact : metadataRepository.getArtifacts( repositoryId, ns, project,
                                                                                       version ) )
                    {
                        stats.setTotalArtifactCount( stats.getTotalArtifactCount() + 1 );
                        stats.setTotalArtifactFileSize( stats.getTotalArtifactFileSize() + artifact.getSize() );

                        MavenArtifactFacet facet = (MavenArtifactFacet) artifact.getFacet(
                            MavenArtifactFacet.FACET_ID );
                        if ( facet != null )
                        {
                            String type = facet.getType();
                            stats.setTotalCountForType( type, stats.getTotalCountForType( type ) + 1 );
                        }
                    }
                }
            }
        }
    }

    public void addStatisticsAfterScan( MetadataRepository metadataRepository, String repositoryId, Date startTime,
                                        Date endTime, long totalFiles, long newFiles )
        throws MetadataRepositoryException
    {
        RepositoryStatistics repositoryStatistics = new RepositoryStatistics();
        repositoryStatistics.setScanStartTime( startTime );
        repositoryStatistics.setScanEndTime( endTime );
        repositoryStatistics.setTotalFileCount( totalFiles );
        repositoryStatistics.setNewFileCount( newFiles );

        // TODO
        // In the future, instead of being tied to a scan we might want to record information in the fly based on
        // events that are occurring. Even without these totals we could query much of the information on demand based
        // on information from the metadata content repository. In the mean time, we lock information in at scan time.
        // Note that if new types are later discoverable due to a code change or new plugin, historical stats will not
        // be updated and the repository will need to be rescanned.

        long startGather = System.currentTimeMillis();

        if ( metadataRepository.canObtainAccess( Session.class ) )
        {
            // TODO: this is currently very raw and susceptible to changes in content structure. Should we instead
            //   depend directly on the plugin and interrogate the JCR repository's knowledge of the structure?
            populateStatisticsFromJcr( (Session) metadataRepository.obtainAccess( Session.class ), repositoryId,
                                       repositoryStatistics );
        }
        else
        {
            // TODO:
            //   if the file repository is used more permanently, we may seek a more efficient mechanism - e.g. we could
            //   build an index, or store the aggregate information and update it on the fly. We can perhaps even walk
            //   but retrieve less information to speed it up. In the mean time, we walk the repository using the
            //   standard APIs
            populateStatisticsFromRepositoryWalk( metadataRepository, repositoryId, repositoryStatistics );
        }

        log.info( "Gathering statistics executed in " + ( System.currentTimeMillis() - startGather ) + "ms" );

        metadataRepository.addMetadataFacet( repositoryId, repositoryStatistics );
    }

    private void populateStatisticsFromJcr( Session session, String repositoryId,
                                            RepositoryStatistics repositoryStatistics )
        throws MetadataRepositoryException
    {
        // TODO: these may be best as running totals, maintained by observations on the properties in JCR

        try
        {
            QueryManager queryManager = session.getWorkspace().getQueryManager();

            // TODO: JCR-SQL2 query will not complete on a large repo in Jackrabbit 2.2.0 - see JCR-2835
            //    Using the JCR-SQL2 variants gives
            //      "org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024"
//            String whereClause = "WHERE ISDESCENDANTNODE([/repositories/" + repositoryId + "/content])";
//            Query query = queryManager.createQuery( "SELECT size FROM [archiva:artifact] " + whereClause,
//                                                    Query.JCR_SQL2 );
            String whereClause = "WHERE jcr:path LIKE '/repositories/" + repositoryId + "/content/%'";
            Query query = queryManager.createQuery( "SELECT size FROM archiva:artifact " + whereClause, Query.SQL );

            QueryResult queryResult = query.execute();

            Map<String, Integer> totalByType = new HashMap<String, Integer>();
            long totalSize = 0, totalArtifacts = 0;
            for ( Row row : JcrUtils.getRows( queryResult ) )
            {
                Node n = row.getNode();
                totalSize += row.getValue( "size" ).getLong();

                String type;
                if ( n.hasNode( MavenArtifactFacet.FACET_ID ) )
                {
                    Node facetNode = n.getNode( MavenArtifactFacet.FACET_ID );
                    type = facetNode.getProperty( "type" ).getString();
                }
                else
                {
                    type = "Other";
                }
                Integer prev = totalByType.get( type );
                totalByType.put( type, prev != null ? prev + 1 : 1 );

                totalArtifacts++;
            }

            repositoryStatistics.setTotalArtifactCount( totalArtifacts );
            repositoryStatistics.setTotalArtifactFileSize( totalSize );
            for ( Map.Entry<String, Integer> entry : totalByType.entrySet() )
            {
                repositoryStatistics.setTotalCountForType( entry.getKey(), entry.getValue() );
            }

            // The query ordering is a trick to ensure that the size is correct, otherwise due to lazy init it will be -1
//            query = queryManager.createQuery( "SELECT * FROM [archiva:project] " + whereClause, Query.JCR_SQL2 );
            query = queryManager.createQuery( "SELECT * FROM archiva:project " + whereClause + " ORDER BY jcr:score",
                                              Query.SQL );
            repositoryStatistics.setTotalProjectCount( query.execute().getRows().getSize() );

//            query = queryManager.createQuery(
//                "SELECT * FROM [archiva:namespace] " + whereClause + " AND namespace IS NOT NULL", Query.JCR_SQL2 );
            query = queryManager.createQuery(
                "SELECT * FROM archiva:namespace " + whereClause + " AND namespace IS NOT NULL ORDER BY jcr:score",
                Query.SQL );
            repositoryStatistics.setTotalGroupCount( query.execute().getRows().getSize() );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void populateStatisticsFromRepositoryWalk( MetadataRepository metadataRepository, String repositoryId,
                                                       RepositoryStatistics repositoryStatistics )
        throws MetadataRepositoryException
    {
        try
        {
            for ( String ns : metadataRepository.getRootNamespaces( repositoryId ) )
            {
                walkRepository( metadataRepository, repositoryStatistics, repositoryId, ns );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public void deleteStatistics( MetadataRepository metadataRepository, String repositoryId )
        throws MetadataRepositoryException
    {
        metadataRepository.removeMetadataFacets( repositoryId, RepositoryStatistics.FACET_ID );
    }

    public List<RepositoryStatistics> getStatisticsInRange( MetadataRepository metadataRepository, String repositoryId,
                                                            Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        List<RepositoryStatistics> results = new ArrayList<RepositoryStatistics>();
        List<String> list = metadataRepository.getMetadataFacets( repositoryId, RepositoryStatistics.FACET_ID );
        Collections.sort( list, Collections.reverseOrder() );
        for ( String name : list )
        {
            try
            {
                Date date = createNameFormat().parse( name );
                if ( ( startTime == null || !date.before( startTime ) ) && ( endTime == null || !date.after(
                    endTime ) ) )
                {
                    RepositoryStatistics stats = (RepositoryStatistics) metadataRepository.getMetadataFacet(
                        repositoryId, RepositoryStatistics.FACET_ID, name );
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

    private static SimpleDateFormat createNameFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( RepositoryStatistics.SCAN_TIMESTAMP_FORMAT );
        fmt.setTimeZone( UTC_TIME_ZONE );
        return fmt;
    }
}
