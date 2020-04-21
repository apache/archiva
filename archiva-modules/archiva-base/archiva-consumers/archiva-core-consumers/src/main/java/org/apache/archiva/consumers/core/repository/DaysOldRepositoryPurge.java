package org.apache.archiva.consumers.core.repository;

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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Purge from repository all snapshots older than the specified days in the repository configuration.
 */
public class DaysOldRepositoryPurge
    extends AbstractRepositoryPurge
{
    private SimpleDateFormat timestampParser;

    private int retentionPeriod;

    private int retentionCount;

    public DaysOldRepositoryPurge( ManagedRepositoryContent repository, int retentionPeriod, int retentionCount,
                                   RepositorySession repositorySession, List<RepositoryListener> listeners )
    {
        super( repository, repositorySession, listeners );
        this.retentionPeriod = retentionPeriod;
        this.retentionCount = retentionCount;
        timestampParser = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        timestampParser.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    }

    @Override
    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {

            ContentItem item = repository.toItem( path );
            if ( item instanceof Artifact )
            {
                Artifact artifactItem = (Artifact) item;

                if ( !artifactItem.exists( ) )
                {
                    return;
                }

                // ArtifactReference artifact = repository.toArtifactReference( path );

                Calendar olderThanThisDate = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                olderThanThisDate.add( Calendar.DATE, -retentionPeriod );

                ArchivaItemSelector selector = ArchivaItemSelector.builder( )
                    .withNamespace( artifactItem.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) )
                    .withProjectId( artifactItem.getVersion( ).getProject( ).getId( ) )
                    .withVersion( artifactItem.getVersion( ).getVersion( ) )
                    .withClassifier( "*" )
                    .includeRelatedArtifacts( )
                    .build( );

                List<String> artifactVersions;
                try( Stream<? extends Artifact> stream = repository.newArtifactStream( selector )){
                     artifactVersions = stream.map( a -> a.getArtifactVersion( ) )
                         .filter( StringUtils::isNotEmpty )
                         .distinct()
                         .collect( Collectors.toList( ) );
                }

                Collections.sort( artifactVersions, VersionComparator.getInstance( ) );

                if ( retentionCount > artifactVersions.size( ) )
                {
                    // Done. nothing to do here. skip it.
                    return;
                }

                int countToPurge = artifactVersions.size( ) - retentionCount;


                ArchivaItemSelector.Builder artifactSelectorBuilder = ArchivaItemSelector.builder( )
                    .withNamespace( artifactItem.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) )
                    .withProjectId( artifactItem.getVersion( ).getProject( ).getId( ) )
                    .withVersion( artifactItem.getVersion( ).getVersion( ) )
                    .withArtifactId( artifactItem.getId() )
                    .withClassifier( "*" )
                    .includeRelatedArtifacts( );

                Set<Artifact> artifactsToDelete = new HashSet<>( );
                for ( String version : artifactVersions )
                {
                    if ( countToPurge-- <= 0 )
                    {
                        break;
                    }

                    ArchivaItemSelector artifactSelector = artifactSelectorBuilder.withArtifactVersion( version ).build( );
                    try
                    {


                        // Is this a generic snapshot "1.0-SNAPSHOT" ?
                        if ( VersionUtil.isGenericSnapshot( version ) )
                        {
                            List<? extends Artifact> artifactList = repository.getArtifacts( artifactSelector );
                            if ( artifactList.size()>0 && artifactList.get(0).getAsset().getModificationTime( ).toEpochMilli( ) < olderThanThisDate.getTimeInMillis( ) )
                            {
                                artifactsToDelete.addAll( artifactList );
                            }
                        }
                        // Is this a timestamp snapshot "1.0-20070822.123456-42" ?
                        else if ( VersionUtil.isUniqueSnapshot( version ) )
                        {
                            Calendar timestampCal = uniqueSnapshotToCalendar( version );

                            if ( timestampCal.getTimeInMillis( ) < olderThanThisDate.getTimeInMillis( ) )
                            {
                                artifactsToDelete.addAll( repository.getArtifacts( artifactSelector ) );
                            }
                        }
                    } catch ( IllegalArgumentException e ) {
                        log.error( "Bad selector for artifact: {}", e.getMessage( ), e );
                        // continue
                    }
                }
                purge( artifactsToDelete );
            }
        }
        catch ( LayoutException e )
        {
            log.debug( "Not processing file that is not an artifact: {}", e.getMessage( ) );
        }
        catch ( org.apache.archiva.repository.ContentAccessException e )
        {
            e.printStackTrace( );
        }
    }

    private Calendar uniqueSnapshotToCalendar( String version )
    {
        // The latestVersion will contain the full version string "1.0-alpha-5-20070821.213044-8"
        // This needs to be broken down into ${base}-${timestamp}-${build_number}

        Matcher m = VersionUtil.UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if ( m.matches( ) )
        {
            Matcher mtimestamp = VersionUtil.TIMESTAMP_PATTERN.matcher( m.group( 2 ) );
            if ( mtimestamp.matches( ) )
            {
                String tsDate = mtimestamp.group( 1 );
                String tsTime = mtimestamp.group( 2 );

                Date versionDate;
                try
                {
                    versionDate = timestampParser.parse( tsDate + "." + tsTime );
                    Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                    cal.setTime( versionDate );

                    return cal;
                }
                catch ( ParseException e )
                {
                    // Invalid Date/Time
                    return null;
                }
            }
        }
        return null;
    }

}
