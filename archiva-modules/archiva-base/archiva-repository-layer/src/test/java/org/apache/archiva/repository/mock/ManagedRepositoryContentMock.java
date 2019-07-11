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
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.content.StorageAsset;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("managedRepositoryContent#mock")
public class ManagedRepositoryContentMock implements ManagedRepositoryContent
{
    private ManagedRepository repository;

    @Override
    public void deleteVersion( VersionedReference reference ) throws ContentNotFoundException
    {

    }

    @Override
    public void deleteArtifact( ArtifactReference artifactReference ) throws ContentNotFoundException
    {

    }

    @Override
    public void deleteGroupId( String groupId ) throws ContentNotFoundException
    {

    }

    @Override
    public void deleteProject( String namespace, String projectId ) throws RepositoryException
    {

    }

    @Override
    public String getId( )
    {
        return null;
    }

    @Override
    public Set<ArtifactReference> getRelatedArtifacts( ArtifactReference reference ) throws ContentNotFoundException
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
    public Set<String> getVersions( ProjectReference reference ) throws ContentNotFoundException, LayoutException
    {
        return null;
    }

    @Override
    public Set<String> getVersions( VersionedReference reference ) throws ContentNotFoundException
    {
        return null;
    }

    @Override
    public boolean hasContent( ArtifactReference reference )
    {
        return false;
    }

    @Override
    public boolean hasContent( ProjectReference reference )
    {
        return false;
    }

    @Override
    public boolean hasContent( VersionedReference reference )
    {
        return false;
    }

    @Override
    public void setRepository( ManagedRepository repo )
    {
        this.repository = repo;
    }

    @Override
    public ArtifactReference toArtifactReference( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public Path toFile( ArtifactReference reference )
    {
        return null;
    }

    @Override
    public Path toFile( ArchivaArtifact reference )
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
    public String toPath( ArchivaArtifact reference )
    {
        return null;
    }

}
