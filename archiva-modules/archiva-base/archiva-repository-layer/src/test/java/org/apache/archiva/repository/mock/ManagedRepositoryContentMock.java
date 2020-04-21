package org.apache.archiva.repository.mock;

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

import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentAccessException;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.storage.StorageAsset;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("managedRepositoryContent#mock")
public class ManagedRepositoryContentMock implements ManagedRepositoryContent
{
    private ManagedRepository repository;

    @Override
    public VersionedReference toVersion( String groupId, String artifactId, String version )
    {
        return null;
    }

    @Override
    public VersionedReference toVersion( ArtifactReference artifactReference )
    {
        return null;
    }

    @Override
    public void deleteItem( ContentItem item ) throws ItemNotFoundException, ContentAccessException
    {

    }

    @Override
    public ContentItem getItem( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }

    @Override
    public Namespace getNamespace( ItemSelector namespaceSelector ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }

    @Override
    public Project getProject( ItemSelector projectSelector ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }


    @Override
    public void deleteVersion( VersionedReference reference ) throws ContentNotFoundException, ContentAccessException
    {

    }


    @Override
    public Version getVersion( ItemSelector versionCoordinates ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }

    @Override
    public void deleteArtifact( ArtifactReference artifactReference ) throws ContentNotFoundException, ContentAccessException
    {

    }


    @Override
    public Artifact getArtifact( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public List<? extends Artifact> getArtifacts( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public Stream<? extends Artifact> newArtifactStream( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public List<? extends Project> getProjects( Namespace namespace ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public List<? extends Project> getProjects( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }

    @Override
    public List<? extends Version> getVersions( Project project ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public List<? extends Version> getVersions( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }

    @Override
    public List<? extends Artifact> getArtifacts( ContentItem item ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public Stream<? extends Artifact> newArtifactStream( ContentItem item ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public boolean hasContent( ItemSelector selector )
    {
        return false;
    }

    @Override
    public void addArtifact( Path sourceFile, Artifact destination ) throws IllegalArgumentException
    {

    }

    @Override
    public ContentItem toItem( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public ContentItem toItem( StorageAsset assetPath ) throws LayoutException
    {
        return null;
    }


    @Override
    public void deleteGroupId( String groupId ) throws ContentNotFoundException, ContentAccessException
    {

    }


    @Override
    public void deleteProject( String namespace, String projectId ) throws ContentNotFoundException, ContentAccessException
    {

    }

    @Override
    public void deleteProject( ProjectReference reference ) throws ContentNotFoundException, ContentAccessException
    {

    }

    @Override
    public String getId( )
    {
        return null;
    }

    @Override
    public List<ArtifactReference> getRelatedArtifacts( VersionedReference reference ) throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        return null;
    }

    @Override
    public List<ArtifactReference> getArtifacts( VersionedReference reference ) throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        return null;
    }

    @Override
    public String getRepoRoot( )
    {
        return null;
    }

    @Override
    public ManagedRepository getRepository( )
    {
        return repository;
    }

    @Override
    public boolean hasContent( ArtifactReference reference ) throws ContentAccessException
    {
        return false;
    }

    @Override
    public boolean hasContent( VersionedReference reference ) throws ContentAccessException
    {
        return false;
    }

    @Override
    public void setRepository( ManagedRepository repo )
    {
        this.repository = repo;
    }

    @Override
    public StorageAsset toFile( VersionedReference reference )
    {
        return null;
    }

    @Override
    public ArtifactReference toArtifactReference( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public StorageAsset toFile( ArtifactReference reference )
    {
        return null;
    }

    @Override
    public StorageAsset toFile( ArchivaArtifact reference )
    {
        return null;
    }

    @Override
    public String toMetadataPath( ProjectReference reference )
    {
        return null;
    }

    @Override
    public String toMetadataPath( VersionedReference reference )
    {
        return null;
    }

    @Override
    public String toPath( ArtifactReference reference )
    {
        return null;
    }

    @Override
    public String toPath( ItemSelector selector )
    {
        return null;
    }

    @Override
    public ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public String toPath( ArchivaArtifact reference )
    {
        return null;
    }

}
