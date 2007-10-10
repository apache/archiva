package org.apache.maven.archiva.repository.content;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ManagedLegacyRepositoryContent 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.repository.ManagedRepositoryContent"
 *      role-hint="legacy"
 *      instantiation-strategy="per-lookup"
 */
public class ManagedLegacyRepositoryContent
    extends AbstractLegacyRepositoryContent
    implements ManagedRepositoryContent, Initializable
{
    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    private ManagedRepositoryConfiguration repository;

    private List<String> artifactPatterns;

    public void deleteVersion( VersionedReference reference )
        throws ContentNotFoundException
    {
        File groupDir = new File( repository.getLocation(), reference.getGroupId() );

        if ( !groupDir.exists() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-existant groupId directory: "
                + groupDir.getAbsolutePath() );
        }

        if ( !groupDir.isDirectory() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-directory: "
                + groupDir.getAbsolutePath() );
        }

        // First gather up the versions found as artifacts in the managed repository.
        File typeDirs[] = groupDir.listFiles();
        for ( File typeDir : typeDirs )
        {
            if ( !typeDir.isDirectory() )
            {
                // Skip it, we only care about directories.
                continue;
            }

            if ( !typeDir.getName().endsWith( "s" ) )
            {
                // Skip it, we only care about directories that end in "s".
            }

            deleteVersions( typeDir, reference );
        }
    }

    private void deleteVersions( File typeDir, VersionedReference reference )
    {
        File repoFiles[] = typeDir.listFiles();
        for ( File repoFile : repoFiles )
        {
            if ( repoFile.isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( repository.getLocation(), repoFile );

            if ( matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );
                    if ( StringUtils.equals( artifact.getArtifactId(), reference.getArtifactId() )
                        && StringUtils.equals( artifact.getVersion(), reference.getVersion() ) )
                    {
                        repoFile.delete();
                        deleteSupportFiles( repoFile );
                    }
                }
                catch ( LayoutException e )
                {
                    /* don't fail the process if there is a bad artifact within the directory. */
                }
            }
        }
    }

    private void deleteSupportFiles( File repoFile )
    {
        deleteSupportFile( repoFile, ".sha1" );
        deleteSupportFile( repoFile, ".md5" );
        deleteSupportFile( repoFile, ".asc" );
        deleteSupportFile( repoFile, ".gpg" );
    }

    private void deleteSupportFile( File repoFile, String supportExtension )
    {
        File supportFile = new File( repoFile.getAbsolutePath() + supportExtension );
        if ( supportFile.exists() && supportFile.isFile() )
        {
            supportFile.delete();
        }
    }

    public String getId()
    {
        return repository.getId();
    }

    public Set<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException, LayoutException
    {
        File artifactFile = toFile( reference );
        File repoDir = artifactFile.getParentFile();

        if ( !repoDir.exists() )
        {
            throw new ContentNotFoundException( "Unable to get related artifacts using a non-existant directory: "
                + repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new ContentNotFoundException( "Unable to get related artifacts using a non-directory: "
                + repoDir.getAbsolutePath() );
        }

        Set<ArtifactReference> foundArtifacts = new HashSet<ArtifactReference>();

        // First gather up the versions found as artifacts in the managed repository.
        File projectParentDir = repoDir.getParentFile();
        File typeDirs[] = projectParentDir.listFiles();
        for ( File typeDir : typeDirs )
        {
            if ( !typeDir.isDirectory() )
            {
                // Skip it, we only care about directories.
                continue;
            }

            if ( !typeDir.getName().endsWith( "s" ) )
            {
                // Skip it, we only care about directories that end in "s".
            }

            getRelatedArtifacts( typeDir, reference, foundArtifacts );
        }

        return foundArtifacts;
    }

    public String getRepoRoot()
    {
        return repository.getLocation();
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public Set<String> getVersions( ProjectReference reference )
        throws ContentNotFoundException
    {
        File groupDir = new File( repository.getLocation(), reference.getGroupId() );

        if ( !groupDir.exists() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-existant groupId directory: "
                + groupDir.getAbsolutePath() );
        }

        if ( !groupDir.isDirectory() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-directory: "
                + groupDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<String>();

        // First gather up the versions found as artifacts in the managed repository.
        File typeDirs[] = groupDir.listFiles();
        for ( File typeDir : typeDirs )
        {
            if ( !typeDir.isDirectory() )
            {
                // Skip it, we only care about directories.
                continue;
            }

            if ( !typeDir.getName().endsWith( "s" ) )
            {
                // Skip it, we only care about directories that end in "s".
            }

            getProjectVersions( typeDir, reference, foundVersions );
        }

        return foundVersions;
    }

    public Set<String> getVersions( VersionedReference reference )
        throws ContentNotFoundException
    {
        File groupDir = new File( repository.getLocation(), reference.getGroupId() );

        if ( !groupDir.exists() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-existant groupId directory: "
                + groupDir.getAbsolutePath() );
        }

        if ( !groupDir.isDirectory() )
        {
            throw new ContentNotFoundException( "Unable to get versions using a non-directory: "
                + groupDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<String>();

        // First gather up the versions found as artifacts in the managed repository.
        File typeDirs[] = groupDir.listFiles();
        for ( File typeDir : typeDirs )
        {
            if ( !typeDir.isDirectory() )
            {
                // Skip it, we only care about directories.
                continue;
            }

            if ( !typeDir.getName().endsWith( "s" ) )
            {
                // Skip it, we only care about directories that end in "s".
            }

            getVersionedVersions( typeDir, reference, foundVersions );
        }

        return foundVersions;
    }

    public boolean hasContent( ArtifactReference reference )
    {
        File artifactFile = toFile( reference );
        return artifactFile.exists() && artifactFile.isFile();
    }

    public boolean hasContent( ProjectReference reference )
    {
        try
        {
            Set<String> versions = getVersions( reference );
            return CollectionUtils.isNotEmpty( versions );
        }
        catch ( ContentNotFoundException e )
        {
            return false;
        }
    }

    public boolean hasContent( VersionedReference reference )
    {
        try
        {
            Set<String> versions = getVersions( reference );
            return CollectionUtils.isNotEmpty( versions );
        }
        catch ( ContentNotFoundException e )
        {
            return false;
        }
    }

    public void initialize()
        throws InitializationException
    {
        this.artifactPatterns = new ArrayList<String>();
        initVariables();
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    /**
     * Convert a path to an artifact reference.
     * 
     * @param path the path to convert. (relative or full location path)
     * @throws LayoutException if the path cannot be converted to an artifact reference.
     */
    @Override
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( ( path != null ) && path.startsWith( repository.getLocation() ) )
        {
            return super.toArtifactReference( path.substring( repository.getLocation().length() ) );
        }

        return super.toArtifactReference( path );
    }

    public File toFile( ArtifactReference reference )
    {
        return new File( repository.getLocation(), toPath( reference ) );
    }

    public String toMetadataPath( ProjectReference reference )
    {
        // No metadata present in legacy repository.
        return null;
    }

    public String toMetadataPath( VersionedReference reference )
    {
        // No metadata present in legacy repository.
        return null;
    }

    private void getProjectVersions( File typeDir, ProjectReference reference, Set<String> foundVersions )
    {
        File repoFiles[] = typeDir.listFiles();
        for ( File repoFile : repoFiles )
        {
            if ( repoFile.isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( repository.getLocation(), repoFile );

            if ( matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );
                    if ( StringUtils.equals( artifact.getArtifactId(), reference.getArtifactId() ) )
                    {
                        foundVersions.add( artifact.getVersion() );
                    }
                }
                catch ( LayoutException e )
                {
                    /* don't fail the process if there is a bad artifact within the directory. */
                }
            }
        }
    }

    private void getRelatedArtifacts( File typeDir, ArtifactReference reference, Set<ArtifactReference> foundArtifacts )
    {
        File repoFiles[] = typeDir.listFiles();
        for ( int i = 0; i < repoFiles.length; i++ )
        {
            if ( repoFiles[i].isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( repository.getLocation(), repoFiles[i] );

            if ( matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );
                    if ( StringUtils.equals( artifact.getArtifactId(), reference.getArtifactId() )
                        && artifact.getVersion().startsWith( reference.getVersion() ) )
                    {
                        foundArtifacts.add( artifact );
                    }
                }
                catch ( LayoutException e )
                {
                    /* don't fail the process if there is a bad artifact within the directory. */
                }
            }
        }
    }

    private void getVersionedVersions( File typeDir, VersionedReference reference, Set<String> foundVersions )
    {
        File repoFiles[] = typeDir.listFiles();
        for ( int i = 0; i < repoFiles.length; i++ )
        {
            if ( repoFiles[i].isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( repository.getLocation(), repoFiles[i] );

            if ( matchesArtifactPattern( relativePath ) )
            {
                try
                {
                    ArtifactReference artifact = toArtifactReference( relativePath );
                    if ( StringUtils.equals( artifact.getArtifactId(), reference.getArtifactId() )
                        && artifact.getVersion().startsWith( reference.getVersion() ) )
                    {
                        foundVersions.add( artifact.getVersion() );
                    }
                }
                catch ( LayoutException e )
                {
                    /* don't fail the process if there is a bad artifact within the directory. */
                }
            }
        }
    }

    private void initVariables()
    {
        synchronized ( this.artifactPatterns )
        {
            this.artifactPatterns.clear();

            this.artifactPatterns.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
        }
    }

    private boolean matchesArtifactPattern( String relativePath )
    {
        // Correct the slash pattern.
        relativePath = relativePath.replace( '\\', '/' );

        Iterator<String> it = this.artifactPatterns.iterator();
        while ( it.hasNext() )
        {
            String pattern = it.next();

            if ( SelectorUtils.matchPath( pattern, relativePath, false ) )
            {
                // Found match
                return true;
            }
        }

        // No match.
        return false;
    }
}
