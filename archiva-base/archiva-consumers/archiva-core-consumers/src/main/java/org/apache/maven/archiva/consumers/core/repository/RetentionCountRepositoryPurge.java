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

import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Purge the repository by retention count. Retain only the specified number of snapshots.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RetentionCountRepositoryPurge
    extends AbstractRepositoryPurge
{
    private int retentionCount;

    public RetentionCountRepositoryPurge( ManagedRepositoryConfiguration repository, BidirectionalRepositoryLayout layout,
                                          ArtifactDAO artifactDao, int retentionCount )
    {
        super( repository, layout, artifactDao );
        this.retentionCount = retentionCount;
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getLocation(), path );

            if ( !artifactFile.exists() )
            {
                return;
            }

            FilenameParts parts = getFilenameParts( path );

            if ( VersionUtil.isSnapshot( parts.version ) )
            {
                File parentDir = artifactFile.getParentFile();

                if ( parentDir.isDirectory() )
                {
                    File[] files = parentDir.listFiles();
                    List uniqueVersionFilenames = getUniqueVersions( files );
                    Collections.sort( uniqueVersionFilenames );

                    if ( uniqueVersionFilenames.size() > retentionCount )
                    {
                        int count = uniqueVersionFilenames.size();
                        for ( Iterator iter = uniqueVersionFilenames.iterator(); iter.hasNext(); )
                        {
                            String filename = (String) iter.next();
                            if ( count > retentionCount )
                            {
                                File[] artifactFiles = getFiles( parentDir, filename );
                                purge( artifactFiles );
                                count--;
                            }
                        }
                    }
                }
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage() );
        }
    }

    private List getUniqueVersions( File[] files )
    {
        List uniqueVersions = new ArrayList();

        for ( int i = 0; i < files.length; i++ )
        {
            if ( !( files[i].getName().toUpperCase() ).endsWith( "SHA1" ) &&
                !( files[i].getName().toUpperCase() ).endsWith( "MD5" ) )
            {
                FilenameParts filenameParts = null;

                // skip those files that have layout exception (no artifact id/no version/no extension)
                try
                {
                    filenameParts = getFilenameParts( files[i].getAbsolutePath() );
                }
                catch ( LayoutException le )
                {

                }

                if ( filenameParts != null &&
                    !uniqueVersions.contains( filenameParts.artifactId + "-" + filenameParts.version ) )
                {
                    uniqueVersions.add( filenameParts.artifactId + "-" + filenameParts.version );
                }
            }
        }

        return uniqueVersions;
    }
}
