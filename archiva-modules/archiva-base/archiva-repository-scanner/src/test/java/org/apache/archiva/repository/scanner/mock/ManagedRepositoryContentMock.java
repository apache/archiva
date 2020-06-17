package org.apache.archiva.repository.scanner.mock;

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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.maven.model.MavenArtifactFacet;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.repository.content.BaseRepositoryContentLayout;
import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.ItemDeleteStatus;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
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
import org.apache.archiva.repository.content.base.ArchivaDataItem;
import org.apache.archiva.repository.content.base.ArchivaNamespace;
import org.apache.archiva.repository.content.base.ArchivaProject;
import org.apache.archiva.repository.content.base.ArchivaVersion;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ManagedRepositoryContentMock implements BaseRepositoryContentLayout, ManagedRepositoryContent
{
    private static final String PATH_SEPARATOR = "/";
    private static final String GROUP_SEPARATOR = ".";
    public static final String MAVEN_METADATA = "maven-metadata.xml";


    private ManagedRepository repository;
    private FilesystemStorage fsStorage;

    public ManagedRepositoryContentMock(ManagedRepository repo) {
        this.repository = repo;
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
    public void deleteAllItems( ItemSelector selector, Consumer<ItemDeleteStatus> consumer ) throws ContentAccessException, IllegalArgumentException
    {

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
            throw new RuntimeException( "Bad layout conversion " + e.getMessage( ) );
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
        return null;
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
        return null;
    }

    @Override
    public ContentItem toItem( StorageAsset assetPath ) throws LayoutException
    {
        return null;
    }

    @Override
    public String toPath( ContentItem item )
    {
        return null;
    }

    @Override
    public String getId( )
    {
        return repository.getId();
    }

    private StorageAsset getRepoRootAsset() {
        if (fsStorage==null) {
            try {
                fsStorage = new FilesystemStorage(Paths.get("", "target", "test-repository", "managed"), new DefaultFileLockManager());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fsStorage.getRoot();
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

    public ArtifactMetadata getArtifactForPath( String repoId, String relativePath )
    {
        String[] parts = relativePath.replace( '\\', '/' ).split( "/" );

        int len = parts.length;
        if ( len < 4 )
        {
            throw new IllegalArgumentException(
                    "Not a valid artifact path in a Maven 2 repository, not enough directories: " + relativePath );
        }

        String id = parts[--len];
        String baseVersion = parts[--len];
        String artifactId = parts[--len];
        StringBuilder groupIdBuilder = new StringBuilder();
        for ( int i = 0; i < len - 1; i++ )
        {
            groupIdBuilder.append( parts[i] );
            groupIdBuilder.append( '.' );
        }
        groupIdBuilder.append( parts[len - 1] );

        return getArtifactFromId( repoId, groupIdBuilder.toString(), artifactId, baseVersion, id );
    }

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile( "([0-9]{8}.[0-9]{6})-([0-9]+).*" );



    public ArtifactMetadata getArtifactFromId( String repoId, String namespace, String projectId, String projectVersion,
                                               String id )
    {
        if ( !id.startsWith( projectId + "-" ) )
        {
            throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                    + "' doesn't start with artifact ID '" + projectId + "'" );
        }

        MavenArtifactFacet facet = new MavenArtifactFacet();

        int index = projectId.length() + 1;
        String version;
        String idSubStrFromVersion = id.substring( index );
        if ( idSubStrFromVersion.startsWith( projectVersion ) && !VersionUtil.isUniqueSnapshot( projectVersion ) )
        {
            // non-snapshot versions, or non-timestamped snapshot versions
            version = projectVersion;
        }
        else if ( VersionUtil.isGenericSnapshot( projectVersion ) )
        {
            // timestamped snapshots
            try
            {
                int mainVersionLength = projectVersion.length() - 8; // 8 is length of "SNAPSHOT"
                if ( mainVersionLength == 0 )
                {
                    throw new IllegalArgumentException(
                            "Timestamped snapshots must contain the main version, filename was '" + id + "'" );
                }

                Matcher m = TIMESTAMP_PATTERN.matcher( idSubStrFromVersion.substring( mainVersionLength ) );
                m.matches();
                String timestamp = m.group( 1 );
                String buildNumber = m.group( 2 );
                facet.setTimestamp( timestamp );
                facet.setBuildNumber( Integer.parseInt( buildNumber ) );
                version = idSubStrFromVersion.substring( 0, mainVersionLength ) + timestamp + "-" + buildNumber;
            }
            catch ( IllegalStateException e )
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                        + "' doesn't contain a timestamped version matching snapshot '"
                        + projectVersion + "'", e);
            }
        }
        else
        {
            // invalid
            throw new IllegalArgumentException(
                    "Not a valid artifact path in a Maven 2 repository, filename '" + id + "' doesn't contain version '"
                            + projectVersion + "'" );
        }

        String classifier;
        String ext;
        index += version.length();
        if ( index == id.length() )
        {
            // no classifier or extension
            classifier = null;
            ext = null;
        }
        else
        {
            char c = id.charAt( index );
            if ( c == '-' )
            {
                // classifier up until '.'
                int extIndex = id.indexOf( '.', index );
                if ( extIndex >= 0 )
                {
                    classifier = id.substring( index + 1, extIndex );
                    ext = id.substring( extIndex + 1 );
                }
                else
                {
                    classifier = id.substring( index + 1 );
                    ext = null;
                }
            }
            else if ( c == '.' )
            {
                // rest is the extension
                classifier = null;
                ext = id.substring( index + 1 );
            }
            else
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                        + "' expected classifier or extension but got '"
                        + id.substring( index ) + "'" );
            }
        }

        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setId( id );
        metadata.setNamespace( namespace );
        metadata.setProject( projectId );
        metadata.setRepositoryId( repoId );
        metadata.setProjectVersion( projectVersion );
        metadata.setVersion( version );

        facet.setClassifier( classifier );

        // we use our own provider here instead of directly accessing Maven's artifact handlers as it has no way
        // to select the correct order to apply multiple extensions mappings to a preferred type
        // TODO: this won't allow the user to decide order to apply them if there are conflicts or desired changes -
        //       perhaps the plugins could register missing entries in configuration, then we just use configuration
        //       here?

        String type = null;


        // use extension as default
        if ( type == null )
        {
            type = ext;
        }

        // TODO: should we allow this instead?
        if ( type == null )
        {
            throw new IllegalArgumentException(
                    "Not a valid artifact path in a Maven 2 repository, filename '" + id + "' does not have a type" );
        }

        facet.setType( type );
        metadata.addFacet( facet );

        return metadata;
    }


    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
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
