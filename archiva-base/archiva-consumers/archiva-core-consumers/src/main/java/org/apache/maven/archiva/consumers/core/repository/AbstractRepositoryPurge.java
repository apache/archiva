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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all repository purge tasks.
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public abstract class AbstractRepositoryPurge
    implements RepositoryPurge
{
    protected ManagedRepositoryContent repository;

    protected ArtifactDAO artifactDao;

    private Map<String, RepositoryContentIndex> indices;

    public AbstractRepositoryPurge( ManagedRepositoryContent repository, ArtifactDAO artifactDao,
                                    Map<String, RepositoryContentIndex> indices )
    {
        this.repository = repository;
        this.artifactDao = artifactDao;
        this.indices = indices;
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
        List<LuceneRepositoryContentRecord> fileContentRecords = new ArrayList<LuceneRepositoryContentRecord>();
        List<LuceneRepositoryContentRecord> hashcodeRecords = new ArrayList<LuceneRepositoryContentRecord>();
        List<LuceneRepositoryContentRecord> bytecodeRecords = new ArrayList<LuceneRepositoryContentRecord>();

        for ( ArtifactReference reference : references )
        {
            File artifactFile = repository.toFile( reference );

            ArchivaArtifact artifact =
                new ArchivaArtifact( reference.getGroupId(), reference.getArtifactId(), reference.getVersion(),
                                     reference.getClassifier(), reference.getType() );

            FileContentRecord fileContentRecord = new FileContentRecord();
            fileContentRecord.setFilename( repository.toPath( artifact ) );
            fileContentRecords.add( fileContentRecord );

            HashcodesRecord hashcodesRecord = new HashcodesRecord();
            hashcodesRecord.setArtifact( artifact );
            hashcodeRecords.add( hashcodesRecord );

            BytecodeRecord bytecodeRecord = new BytecodeRecord();
            bytecodeRecord.setArtifact( artifact );
            bytecodeRecords.add( bytecodeRecord );

            artifactFile.delete();
            purgeSupportFiles( artifactFile );

            // intended to be swallowed
            // continue updating the database for all artifacts
            try
            {
                String artifactPath = toRelativePath( artifactFile );
                updateDatabase( artifactPath );
            }
            catch ( ArchivaDatabaseException ae )
            {
                // TODO: determine logging to be used
            }
            catch ( LayoutException le )
            {
                // Ignore
            }
        }

        try
        {
            updateIndices( fileContentRecords, hashcodeRecords, bytecodeRecords );
        }
        catch ( RepositoryIndexException e )
        {
            // Ignore
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
                // TODO: log that it was deleted?
            }
        }
    }

    private void updateDatabase( String path )
        throws ArchivaDatabaseException, LayoutException
    {
        ArtifactReference artifact = repository.toArtifactReference( path );
        ArchivaArtifact queriedArtifact =
            artifactDao.getArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                     artifact.getClassifier(), artifact.getType() );

        artifactDao.deleteArtifact( queriedArtifact );

        // TODO [MRM-37]: re-run the database consumers to clean up
    }

    private void updateIndices( List<LuceneRepositoryContentRecord> fileContentRecords,
                                List<LuceneRepositoryContentRecord> hashcodeRecords,
                                List<LuceneRepositoryContentRecord> bytecodeRecords )
        throws RepositoryIndexException
    {
        RepositoryContentIndex index = indices.get( "filecontent" );
        index.deleteRecords( fileContentRecords );

        index = indices.get( "hashcodes" );
        index.deleteRecords( hashcodeRecords );

        index = indices.get( "bytecode" );
        index.deleteRecords( bytecodeRecords );
    }
}
