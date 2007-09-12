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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.util.Calendar;

/**
 * Purge repository for snapshots older than the specified days in the repository configuration.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DaysOldRepositoryPurge
    extends AbstractRepositoryPurge
{
    private int daysOlder;

    public DaysOldRepositoryPurge( ArchivaRepository repository, BidirectionalRepositoryLayout layout,
                                   ArtifactDAO artifactDao, int daysOlder )
    {
        super( repository, layout, artifactDao );
        this.daysOlder = daysOlder;
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getUrl().getPath(), path );

            if ( !artifactFile.exists() )
            {
                return;
            }

            FilenameParts parts = getFilenameParts( path );

            Calendar olderThanThisDate = Calendar.getInstance();
            olderThanThisDate.add( Calendar.DATE, -daysOlder );

            if ( VersionUtil.isGenericSnapshot( parts.version ) )
            {
                if ( artifactFile.lastModified() < olderThanThisDate.getTimeInMillis() )
                {
                    doPurge( artifactFile, parts.extension );
                }
            }
            else if ( VersionUtil.isUniqueSnapshot( parts.version ) )
            {
                String[] versionParts = StringUtils.split( parts.version, '-' );
                String timestamp = StringUtils.remove( versionParts[1], '.' );
                int year = Integer.parseInt( StringUtils.substring( timestamp, 0, 4 ) );
                int month = Integer.parseInt( StringUtils.substring( timestamp, 4, 6 ) ) - 1;
                int day = Integer.parseInt( StringUtils.substring( timestamp, 6, 8 ) );
                int hour = Integer.parseInt( StringUtils.substring( timestamp, 8, 10 ) );
                int min = Integer.parseInt( StringUtils.substring( timestamp, 10, 12 ) );
                int sec = Integer.parseInt( StringUtils.substring( timestamp, 12 ) );

                Calendar timestampDate = Calendar.getInstance();
                timestampDate.set( year, month, day, hour, min, sec );

                if ( timestampDate.getTimeInMillis() < olderThanThisDate.getTimeInMillis() )
                {
                    doPurge( artifactFile, parts.extension );
                }
                else
                {
                    if ( artifactFile.lastModified() < olderThanThisDate.getTimeInMillis() )
                    {
                        doPurge( artifactFile, parts.extension );
                    }
                }
            }

        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage() );
        }
    }

    private void doPurge( File artifactFile, String extension )
    {
        String[] fileParts = artifactFile.getName().split( "." + extension );

        File[] artifactFiles = getFiles( artifactFile.getParentFile(), fileParts[0] );

        purge( artifactFiles );
    }
}

