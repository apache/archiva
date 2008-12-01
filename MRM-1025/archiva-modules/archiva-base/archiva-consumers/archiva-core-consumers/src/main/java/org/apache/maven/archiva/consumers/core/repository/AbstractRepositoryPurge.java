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

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;

/**
 * Base class for all repository purge tasks.
 * 
 */
public abstract class AbstractRepositoryPurge
    implements RepositoryPurge
{
    protected final ManagedRepositoryContent repository;
    
	protected final List<RepositoryListener> listeners;

    public AbstractRepositoryPurge( ManagedRepositoryContent repository, List<RepositoryListener> listeners )
    {
        this.repository = repository;
        this.listeners = listeners;
    }

    /**
     * Get all files from the directory that matches the specified filename.
     * 
     * @param dir the directory to be scanned
     * @param filename the filename to be matched
     * @return
     */
    protected File[] getFiles( File dir, String filename )
    {
        FilenameFilter filter = new ArtifactFilenameFilter( filename );

        File[] files = dir.listFiles( filter );

        return files;
    }

    protected String toRelativePath( File artifactFile )
    {
        String artifactPath = artifactFile.getAbsolutePath();
        if ( artifactPath.startsWith( repository.getRepoRoot() ) )
        {
            artifactPath = artifactPath.substring( repository.getRepoRoot().length() );
        }

        return artifactPath;
    }

    /**
     * Purge the repo. Update db and index of removed artifacts.
     * 
     * @param artifactFiles
     * @throws RepositoryIndexException
     */
    protected void purge( Set<ArtifactReference> references )
    {        
        if( references != null && !references.isEmpty() )
        {
            for ( ArtifactReference reference : references )
            {   
                File artifactFile = repository.toFile( reference );
                
                ArchivaArtifact artifact =
                    new ArchivaArtifact( reference.getGroupId(), reference.getArtifactId(), reference.getVersion(),
                                         reference.getClassifier(), reference.getType() );
    
                for ( RepositoryListener listener : listeners )
                {
                    listener.deleteArtifact( repository, artifact );
                }
                
                // TODO: this needs to be logged
                artifactFile.delete();
                purgeSupportFiles( artifactFile );
            }
        }
    }

    /**
     * <p>
     * This find support files for the artifactFile and deletes them.
     * </p>
     * <p>
     * Support Files are things like ".sha1", ".md5", ".asc", etc.
     * </p>
     * 
     * @param artifactFile the file to base off of.
     */
    private void purgeSupportFiles( File artifactFile )
    {
        File parentDir = artifactFile.getParentFile();

        if ( !parentDir.exists() )
        {
            return;
        }

        FilenameFilter filter = new ArtifactFilenameFilter( artifactFile.getName() );

        File[] files = parentDir.listFiles( filter );

        for ( File file : files )
        {
            if ( file.exists() && file.isFile() )
            {
                file.delete();
                // TODO: log that it was deleted
            }
        }
    }
}
