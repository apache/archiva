package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.commons.lang.time.DateUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Purge from repository all snapshots older than the specified days in the repository configuration.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DaysOldRepositoryPurge
    extends AbstractRepositoryPurge
{
    private SimpleDateFormat timestampParser;

    private int daysOlder;
    
    public DaysOldRepositoryPurge( ManagedRepositoryContent repository, ArtifactDAO artifactDao,
                                   int daysOlder )
    {
        super( repository, artifactDao );
        this.daysOlder = daysOlder;
        timestampParser = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        timestampParser.setTimeZone( DateUtils.UTC_TIME_ZONE );
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getRepoRoot(), path );

            if ( !artifactFile.exists() )
            {
                return;
            }

            ArtifactReference artifact = repository.toArtifactReference( path );

            Calendar olderThanThisDate = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
            olderThanThisDate.add( Calendar.DATE, -daysOlder );

            // Is this a generic snapshot "1.0-SNAPSHOT" ?
            if ( VersionUtil.isGenericSnapshot( artifact.getVersion() ) )
            {
                if ( artifactFile.lastModified() < olderThanThisDate.getTimeInMillis() )
                {
                    doPurgeAllRelated( artifactFile );
                }
            }
            // Is this a timestamp snapshot "1.0-20070822.123456-42" ?
            else if ( VersionUtil.isUniqueSnapshot( artifact.getVersion() ) )
            {
                Calendar timestampCal = uniqueSnapshotToCalendar( artifact.getVersion() );

                if ( timestampCal.getTimeInMillis() < olderThanThisDate.getTimeInMillis() )
                {
                    doPurgeAllRelated( artifactFile );
                }
                else if ( artifactFile.lastModified() < olderThanThisDate.getTimeInMillis() )
                {
                    doPurgeAllRelated( artifactFile );
                }
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage() );
        }
    }

    private Calendar uniqueSnapshotToCalendar( String version )
    {
        // The latestVersion will contain the full version string "1.0-alpha-5-20070821.213044-8"
        // This needs to be broken down into ${base}-${timestamp}-${build_number}

        Matcher m = VersionUtil.UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if ( m.matches() )
        {
            Matcher mtimestamp = VersionUtil.TIMESTAMP_PATTERN.matcher( m.group( 2 ) );
            if ( mtimestamp.matches() )
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

    private void doPurgeAllRelated( File artifactFile ) throws LayoutException
    {
        ArtifactReference reference = repository.toArtifactReference( artifactFile.getAbsolutePath() );
        
        try
        {
            Set<ArtifactReference> related = repository.getRelatedArtifacts( reference );
            purge( related );
        }
        catch ( ContentNotFoundException e )
        {
            // Nothing to do here.
            // TODO: Log this?
        }
    }
}
