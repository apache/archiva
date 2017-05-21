package org.apache.archiva.repository.content.maven2;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.metadata.repository.storage.maven2.DefaultArtifactMappingProvider;
import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ManagedDefaultRepositoryContent
 */
@Service ("managedRepositoryContent#default")
@Scope ("prototype")
public class ManagedDefaultRepositoryContent
    extends AbstractDefaultRepositoryContent
    implements ManagedRepositoryContent
{
    @Inject
    @Named ( "fileTypes" )
    private FileTypes filetypes;

    private ManagedRepository repository;

    public ManagedDefaultRepositoryContent()
    {
        // default to use if there are none supplied as components
        this.artifactMappingProviders = Collections.singletonList( new DefaultArtifactMappingProvider() );
    }

    @Override
    public void deleteVersion( VersionedReference reference )
    {
        String path = toMetadataPath( reference );
        File projectPath = new File( getRepoRoot(), path );

        File projectDir = projectPath.getParentFile();
        if ( projectDir.exists() && projectDir.isDirectory() )
        {
            FileUtils.deleteQuietly( projectDir );
        }
    }

    @Override
    public void deleteProject( String namespace, String projectId )
        throws RepositoryException, ContentNotFoundException
    {
        ArtifactReference artifactReference = new ArtifactReference();
        artifactReference.setGroupId( namespace );
        artifactReference.setArtifactId( projectId );
        String path = toPath( artifactReference );
        File directory = new File( getRepoRoot(), path );
        if ( !directory.exists() )
        {
            throw new ContentNotFoundException( "cannot found project " + namespace + ":" + projectId );
        }
        if ( directory.isDirectory() )
        {
            try
            {
                FileUtils.deleteDirectory( directory );
            }
            catch ( IOException e )
            {
                throw new RepositoryException( e.getMessage(), e );
            }
        }
        else
        {
            log.warn( "project {}:{} is not a directory", namespace, projectId );
        }

    }

    @Override
    public void deleteArtifact( ArtifactReference artifactReference )
    {
        String path = toPath( artifactReference );
        File filePath = new File( getRepoRoot(), path );

        if ( filePath.exists() )
        {
            FileUtils.deleteQuietly( filePath );
        }

        File filePathmd5 = new File( getRepoRoot(), path + ".md5" );

        if ( filePathmd5.exists() )
        {
            FileUtils.deleteQuietly( filePathmd5 );
        }

        File filePathsha1 = new File( getRepoRoot(), path + ".sha1" );

        if ( filePathsha1.exists() )
        {
            FileUtils.deleteQuietly( filePathsha1 );
        }
    }

    @Override
    public void deleteGroupId( String groupId )
        throws ContentNotFoundException
    {

        String path = StringUtils.replaceChars( groupId, '.', '/' );

        File directory = new File( getRepoRoot(), path );

        if ( directory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( directory );
            }
            catch ( IOException e )
            {
                log.warn( "skip error deleting directory {}:", directory.getPath(), e );
            }
        }
    }

    @Override
    public String getId()
    {
        return repository.getId();
    }

    @Override
    public Set<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException
    {
        File artifactFile = toFile( reference );
        File repoDir = artifactFile.getParentFile();

        if ( !repoDir.exists() )
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-existant directory: " + repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-directory: " + repoDir.getAbsolutePath() );
        }

        Set<ArtifactReference> foundArtifacts = new HashSet<>();

        // First gather up the versions found as artifacts in the managed repository.
        File repoFiles[] = repoDir.listFiles();
        for (File repoFile : repoFiles) 
        {
            if (repoFile.isDirectory()) {
                // Skip it. it's a directory.
                continue;
            }
            String relativePath = PathUtil.getRelative(repository.getLocation(), repoFile);
            if ( filetypes.matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );

                    // Test for related, groupId / artifactId / version must match.
                    if ( artifact.getGroupId().equals( reference.getGroupId() ) && artifact.getArtifactId().equals(
                            reference.getArtifactId() ) && artifact.getVersion().equals( reference.getVersion() ) )
                    {
                        foundArtifacts.add( artifact );
                    }
                }
                catch ( LayoutException e )
                {
                    log.debug( "Not processing file that is not an artifact: {}", e.getMessage() );
                }
            }
        }

        return foundArtifacts;
    }

    @Override
    public String getRepoRoot()
    {
        return repository.getLocation();
    }

    @Override
    public ManagedRepository getRepository()
    {
        return repository;
    }

    /**
     * Gather the Available Versions (on disk) for a specific Project Reference, based on filesystem
     * information.
     *
     * @return the Set of available versions, based on the project reference.
     * @throws org.apache.archiva.repository.layout.LayoutException
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Override
    public Set<String> getVersions( ProjectReference reference )
        throws ContentNotFoundException, LayoutException
    {
        String path = toMetadataPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( repository.getLocation(), path );

        if ( !repoDir.exists() )
        {
            throw new ContentNotFoundException(
                "Unable to get Versions on a non-existant directory: " + repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new ContentNotFoundException(
                "Unable to get Versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<>();
        VersionedReference versionRef = new VersionedReference();
        versionRef.setGroupId( reference.getGroupId() );
        versionRef.setArtifactId( reference.getArtifactId() );

        File repoFiles[] = repoDir.listFiles();
        for (File repoFile : repoFiles) 
        {
            if (!repoFile.isDirectory()) {
                // Skip it. not a directory.
                continue;
            }
            // Test if dir has an artifact, which proves to us that it is a valid version directory.
            String version = repoFile.getName();
            versionRef.setVersion( version );
            if ( hasArtifact( versionRef ) )
            {
                // Found an artifact, must be a valid version.
                foundVersions.add( version );
            }
        }

        return foundVersions;
    }

    @Override
    public Set<String> getVersions( VersionedReference reference )
        throws ContentNotFoundException
    {
        String path = toMetadataPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( repository.getLocation(), path );

        if ( !repoDir.exists() )
        {
            throw new ContentNotFoundException(
                "Unable to get versions on a non-existant directory: " + repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new ContentNotFoundException(
                "Unable to get versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<>();

        // First gather up the versions found as artifacts in the managed repository.
        File repoFiles[] = repoDir.listFiles();
        for (File repoFile : repoFiles) 
        {
            if (repoFile.isDirectory()) {
                // Skip it. it's a directory.
                continue;
            }
            String relativePath = PathUtil.getRelative(repository.getLocation(), repoFile);
            if ( filetypes.matchesDefaultExclusions( relativePath ) )
            {
                // Skip it, it's metadata or similar
                continue;
            }
            if ( filetypes.matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );

                    foundVersions.add( artifact.getVersion() );
                }
                catch ( LayoutException e )
                {
                    log.debug( "Not processing file that is not an artifact: {}", e.getMessage() );
                }
            }
        }

        return foundVersions;
    }

    @Override
    public boolean hasContent( ArtifactReference reference )
    {
        File artifactFile = toFile( reference );
        return artifactFile.exists() && artifactFile.isFile();
    }

    @Override
    public boolean hasContent( ProjectReference reference )
    {
        try
        {
            Set<String> versions = getVersions( reference );
            return !versions.isEmpty();
        }
        catch ( ContentNotFoundException | LayoutException e )
        {
            return false;
        }
    }

    @Override
    public boolean hasContent( VersionedReference reference )
    {
        try
        {
            return ( getFirstArtifact( reference ) != null );
        }
        catch ( IOException | LayoutException e )
        {
            return false;
        }
    }

    @Override
    public void setRepository( ManagedRepository repository )
    {
        this.repository = repository;
    }

    /**
     * Convert a path to an artifact reference.
     *
     * @param path the path to convert. (relative or full location path)
     * @throws org.apache.archiva.repository.layout.LayoutException if the path cannot be converted to an artifact reference.
     */
    @Override
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( ( path != null ) && path.startsWith( repository.getLocation() ) && repository.getLocation().length() > 0 )
        {
            return super.toArtifactReference( path.substring( repository.getLocation().length() + 1 ) );
        }

        return super.toArtifactReference( path );
    }

    @Override
    public File toFile( ArtifactReference reference )
    {
        return new File( repository.getLocation(), toPath( reference ) );
    }

    @Override
    public File toFile( ArchivaArtifact reference )
    {
        return new File( repository.getLocation(), toPath( reference ) );
    }

    /**
     * Get the first Artifact found in the provided VersionedReference location.
     *
     * @param reference the reference to the versioned reference to search within
     * @return the ArtifactReference to the first artifact located within the versioned reference. or null if
     *         no artifact was found within the versioned reference.
     * @throws java.io.IOException     if the versioned reference is invalid (example: doesn't exist, or isn't a directory)
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    private ArtifactReference getFirstArtifact( VersionedReference reference )
        throws LayoutException, IOException
    {
        String path = toMetadataPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( repository.getLocation(), path );

        if ( !repoDir.exists() )
        {
            throw new IOException( "Unable to gather the list of snapshot versions on a non-existant directory: "
                                       + repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new IOException(
                "Unable to gather the list of snapshot versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        File repoFiles[] = repoDir.listFiles();
        for (File repoFile : repoFiles) 
        {
            if (repoFile.isDirectory()) {
                // Skip it. it's a directory.
                continue;
            }
            String relativePath = PathUtil.getRelative(repository.getLocation(), repoFile);
            if ( filetypes.matchesArtifactPattern( relativePath ) )
            {
                ArtifactReference artifact = toArtifactReference( relativePath );

                return artifact;
            }
        }

        // No artifact was found.
        return null;
    }

    private boolean hasArtifact( VersionedReference reference )
        throws LayoutException
    {
        try
        {
            return ( getFirstArtifact( reference ) != null );
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    public void setFiletypes( FileTypes filetypes )
    {
        this.filetypes = filetypes;
    }
}
