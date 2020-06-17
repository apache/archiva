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

import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ItemDeleteStatus;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.content.BaseRepositoryContentLayout;
import org.apache.archiva.repository.content.ManagedRepositoryContentLayout;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.BaseDataItemTypes;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.DataItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.ArchivaContentItem;
import org.apache.archiva.repository.content.base.ArchivaDataItem;
import org.apache.archiva.repository.content.base.ArchivaNamespace;
import org.apache.archiva.repository.content.base.ArchivaProject;
import org.apache.archiva.repository.content.base.ArchivaVersion;
import org.apache.archiva.repository.storage.StorageAsset;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("managedRepositoryContent#mock")
public class ManagedRepositoryContentMock implements BaseRepositoryContentLayout, ManagedRepositoryContent
{
    private ManagedRepository repository;

    @Override
    public void deleteAllItems( ItemSelector selector, Consumer<ItemDeleteStatus> consumer ) throws ContentAccessException, IllegalArgumentException
    {

    }

    @Override
    public <T extends ContentItem> T adaptItem( Class<T> clazz, ContentItem item ) throws LayoutException
    {
        if (clazz.isAssignableFrom( Version.class ))
        {
            if ( !item.hasCharacteristic( Version.class ) )
            {
                item.setCharacteristic( Version.class, createVersionFromPath( item.getAsset() ) );
            }
            return (T) item.adapt( Version.class );
        } else if ( clazz.isAssignableFrom( Project.class )) {
            if ( !item.hasCharacteristic( Project.class ) )
            {
                item.setCharacteristic( Project.class, createProjectFromPath( item.getAsset() ) );
            }
            return (T) item.adapt( Project.class );
        } else if ( clazz.isAssignableFrom( Namespace.class )) {
            if ( !item.hasCharacteristic( Namespace.class ) )
            {
                item.setCharacteristic( Namespace.class, createNamespaceFromPath( item.getAsset() ) );
            }
            return (T) item.adapt( Namespace.class );
        }
        throw new LayoutException( "Could not convert item to class " + clazz);
    }

    private Version createVersionFromPath( StorageAsset asset )
    {
        Project proj = createProjectFromPath( asset.getParent( ) );
        return ArchivaVersion.withRepository( this ).withAsset( asset )
            .withProject( proj ).withVersion( asset.getName( ) ).build();
    }

    private Project createProjectFromPath( StorageAsset asset)  {
        Namespace ns = createNamespaceFromPath( asset );
        return ArchivaProject.withRepository( this ).withAsset( asset )
            .withNamespace( ns ).withId( asset.getName( ) ).build( );
    }

    private Namespace createNamespaceFromPath( StorageAsset asset) {
        String namespace = asset.getPath( ).replace( "/", "." );
        return ArchivaNamespace.withRepository( this )
            .withAsset( asset ).withNamespace( namespace ).build();
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
    public Version getVersion( ItemSelector versionCoordinates ) throws ContentAccessException, IllegalArgumentException
    {
        return null;
    }


    @Override
    public Artifact getArtifact( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    @Override
    public Artifact getArtifact( String path ) throws LayoutException, ContentAccessException
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
    public Stream<? extends ContentItem> newItemStream( ItemSelector selector, boolean parallel ) throws ContentAccessException, IllegalArgumentException
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
    public List<String> getArtifactVersions( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
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
    public ContentItem getParent( ContentItem item )
    {
        try
        {
            return toItem( item.getAsset( ).getParent( ) );
        }
        catch ( LayoutException e )
        {
            throw new RuntimeException( "Bad layout error " + e.getMessage( ) );
        }
    }

    @Override
    public List<? extends ContentItem> getChildren( ContentItem item )
    {
        return null;
    }

    @Override
    public <T extends ContentItem> T applyCharacteristic( Class<T> clazz, ContentItem item ) throws LayoutException
    {
        return null;
    }

    @Override
    public <T extends ManagedRepositoryContentLayout> T getLayout( Class<T> clazz ) throws LayoutException
    {
        return null;
    }

    @Override
    public <T extends ManagedRepositoryContentLayout> boolean supportsLayout( Class<T> clazz )
    {
        return false;
    }

    @Override
    public List<Class<? extends ManagedRepositoryContentLayout>> getSupportedLayouts( )
    {
        return null;
    }

    @Override
    public void addArtifact( Path sourceFile, Artifact destination ) throws IllegalArgumentException
    {

    }

    @Override
    public DataItem getMetadataItem( Version version )
    {
        return ArchivaDataItem.withAsset( version.getAsset( ).resolve( "maven-metadata.xml" ) ).withId( "maven-metadata.xml" )
            .withDataType( BaseDataItemTypes.METADATA ).build();
    }

    @Override
    public DataItem getMetadataItem( Project project )
    {
        return ArchivaDataItem.withAsset( project.getAsset( ).resolve( "maven-metadata.xml" ) ).withId( "maven-metadata.xml" )
            .withDataType( BaseDataItemTypes.METADATA ).build( );
    }


    @Override
    public ContentItem toItem( String path ) throws LayoutException
    {
        StorageAsset asset = repository.getRoot().resolve( path );
        return toItem( asset );
    }

    @Override
    public ContentItem toItem( StorageAsset asset ) throws LayoutException
    {
        if (asset.isLeaf()) {
            return ArchivaDataItem.withAsset( asset ).withId( asset.getName() ).build();
        } else
        {
            return ArchivaContentItem.withRepository( this )
                .withAsset( asset ).build( );
        }
    }


    @Override
    public String toPath( ContentItem item )
    {
        return null;
    }

    @Override
    public String getId( )
    {
        return null;
    }

    @Override
    public ManagedRepository getRepository( )
    {
        return repository;
    }

    @Override
    public void setRepository( ManagedRepository repo )
    {
        this.repository = repo;
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
    public ManagedRepositoryContent getGenericContent( )
    {
        return null;
    }
}
