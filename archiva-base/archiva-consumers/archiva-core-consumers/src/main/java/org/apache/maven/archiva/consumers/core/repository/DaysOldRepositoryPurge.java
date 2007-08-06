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

import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.database.ArtifactDAO;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.File;

/**
 * Purge repository for snapshots older than the specified days in the repository configuration.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class DaysOldRepositoryPurge
    extends AbstractRepositoryPurge
{       
    private RepositoryConfiguration repoConfig;

    public DaysOldRepositoryPurge( ArchivaRepository repository,
                                   BidirectionalRepositoryLayout layout, ArtifactDAO artifactDao,
                                   RepositoryConfiguration repoConfig)
    {
        super( repository, layout, artifactDao );
        this.repoConfig = repoConfig;
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getUrl().getPath(), path );

            if( !artifactFile.exists() )
            {
                return;
            }

            FilenameParts parts = getFilenameParts( path );

            if ( VersionUtil.isSnapshot( parts.version ) )
            {
                Calendar olderThanThisDate = Calendar.getInstance();
                olderThanThisDate.add( Calendar.DATE, ( -1 * repoConfig.getDaysOlder() ) );

                if ( artifactFile.lastModified() < olderThanThisDate.getTimeInMillis() )
                {
                    String[] fileParts = artifactFile.getName().split( "." + parts.extension );

                    File[] artifactFiles = getFiles( artifactFile.getParentFile(), fileParts[0] );

                    purge( artifactFiles );
                }
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage() );
        }
    }
    
}
