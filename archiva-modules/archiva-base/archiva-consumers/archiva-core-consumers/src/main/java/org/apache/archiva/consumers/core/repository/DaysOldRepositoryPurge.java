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
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.commons.lang.time.DateUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Purge from repository all snapshots older than the specified days in the repository configuration.
 */
public class DaysOldRepositoryPurge
    extends AbstractRepositoryPurge
{
    private SimpleDateFormat timestampParser;

    private int retentionTime;

    private int retentionCount;

    public DaysOldRepositoryPurge( ManagedRepositoryContent repository, int retentionTime, int retentionCount,
                                   RepositorySession repositorySession, List<RepositoryListener> listeners )
    {
        super( repository, repositorySession, listeners );
        this.retentionTime = retentionTime;
        this.retentionCount = retentionCount;
        timestampParser = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        timestampParser.setTimeZone( DateUtils.UTC_TIME_ZONE );
    }

    @Override
    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            Path artifactFile = Paths.get( repository.getRepoRoot( ), path );

            if ( !Files.exists(artifactFile) )
            {
                return;
            }

            ArtifactReference artifact = repository.toArtifactReference( path );

            Calendar olderThanThisDate = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
            olderThanThisDate.add( Calendar.DATE, -retentionTime );

            // respect retention count
            VersionedReference reference = new VersionedReference( );
            reference.setGroupId( artifact.getGroupId( ) );
            reference.setArtifactId( artifact.getArtifactId( ) );
            reference.setVersion( artifact.getVersion( ) );

            List<String> versions = new ArrayList<>( repository.getVersions( reference ) );

            Collections.sort( versions, VersionComparator.getInstance( ) );

            if ( retentionCount > versions.size( ) )
            {
                // Done. nothing to do here. skip it.
                return;
            }

            int countToPurge = versions.size( ) - retentionCount;

            Set<ArtifactReference> artifactsToDelete = new HashSet<>( );
            for ( String version : versions )
            {
                if ( countToPurge-- <= 0 )
                {
                    break;
                }

                ArtifactReference newArtifactReference = repository.toArtifactReference(
                    artifactFile.toAbsolutePath( ).toString() );
                newArtifactReference.setVersion( version );

                Path newArtifactFile = repository.toFile( newArtifactReference );

                // Is this a generic snapshot "1.0-SNAPSHOT" ?
                if ( VersionUtil.isGenericSnapshot( newArtifactReference.getVersion( ) ) )
                {
                    if ( Files.getLastModifiedTime( newArtifactFile ).toMillis() < olderThanThisDate.getTimeInMillis( ) )
                    {
                        artifactsToDelete.addAll( repository.getRelatedArtifacts( newArtifactReference ) );
                    }
                }
                // Is this a timestamp snapshot "1.0-20070822.123456-42" ?
                else if ( VersionUtil.isUniqueSnapshot( newArtifactReference.getVersion( ) ) )
                {
                    Calendar timestampCal = uniqueSnapshotToCalendar( newArtifactReference.getVersion( ) );

                    if ( timestampCal.getTimeInMillis( ) < olderThanThisDate.getTimeInMillis( ) )
                    {
                        artifactsToDelete.addAll( repository.getRelatedArtifacts( newArtifactReference ) );
                    }
                }
            }
            purge( artifactsToDelete );
        }
        catch ( ContentNotFoundException | IOException e )
        {
            throw new RepositoryPurgeException( e.getMessage( ), e );
        }
        catch ( LayoutException e )
        {
            log.debug( "Not processing file that is not an artifact: {}", e.getMessage( ) );
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
                    Calendar cal = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
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
