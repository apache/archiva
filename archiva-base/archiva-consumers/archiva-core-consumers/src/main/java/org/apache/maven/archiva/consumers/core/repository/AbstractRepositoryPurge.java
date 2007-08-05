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

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.RepositoryLayoutUtils;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public abstract class AbstractRepositoryPurge
    implements RepositoryPurge
{
    private ArchivaRepository repository;

    private BidirectionalRepositoryLayout layout;

    private RepositoryContentIndex index;

    private ArtifactDAO artifactDao;

    /**
     * Get all files from the directory that matches the specified filename.
     *
     * @param dir      the directory to be scanned
     * @param filename the filename to be matched
     * @return
     */
    protected File[] getFiles( File dir, String filename )
        throws RepositoryPurgeException
    {
        FilenameFilter filter = new ArtifactFilenameFilter( filename );

        if ( !dir.isDirectory() )
        {
            System.out.println( "File is not a directory." );
        }

        File[] files = dir.listFiles( filter );

        return files;
    }

    public abstract void process( String path, Configuration configuration )
        throws RepositoryPurgeException;

    /**
     * Purge the repo. Update db and index of removed artifacts.
     *
     * @param artifactFiles
     * @throws RepositoryIndexException
     */
    protected void purge( File[] artifactFiles )
        throws RepositoryIndexException
    {
        List records = new ArrayList();
                
        for ( int i = 0; i < artifactFiles.length; i++ )
        {
            artifactFiles[i].delete();

            String[] artifactPathParts = artifactFiles[i].getAbsolutePath().split( getRepository().getUrl().getPath() );
            String artifactPath = artifactPathParts[artifactPathParts.length - 1];
            if ( !artifactPath.toUpperCase().endsWith( "SHA1" ) && !artifactPath.toUpperCase().endsWith( "MD5" ) )
            {
                updateDatabase( artifactPath );
            }

            FileContentRecord record = new FileContentRecord();
            record.setRepositoryId( this.repository.getId() );
            record.setFilename( artifactPath );
            records.add( record );
        }

        //index.deleteRecords( records );
    }

    private void updateDatabase( String path )
    {
        try
        {
            ArchivaArtifact artifact = layout.toArtifact( path );
            ArchivaArtifact queriedArtifact = artifactDao.getArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                       artifact.getVersion(), artifact.getClassifier(),
                                                                       artifact.getType() );
                        
            artifactDao.deleteArtifact( queriedArtifact );
        }
        catch ( ArchivaDatabaseException ae )
        {

        }
        catch ( LayoutException le )
        {

        }
    }

    /**
     * Get the artifactId, version, extension and classifier from the path parameter
     *
     * @param path
     * @return
     * @throws LayoutException
     */
    protected FilenameParts getFilenameParts( String path )
        throws LayoutException
    {
        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        FilenameParts parts = RepositoryLayoutUtils.splitFilename( pathParts[pathParts.length - 1], null );

        return parts;
    }

    public void setRepository( ArchivaRepository repository )
    {
        this.repository = repository;
    }

    public void setLayout( BidirectionalRepositoryLayout layout )
    {
        this.layout = layout;
    }

    public void setIndex( RepositoryContentIndex index )
    {
        this.index = index;
    }

    public void setArtifactDao( ArtifactDAO artifactDao )
    {
        this.artifactDao = artifactDao;
    }

    protected ArchivaRepository getRepository()
    {
        return repository;
    }

    protected BidirectionalRepositoryLayout getLayout()
    {
        return layout;
    }

}
