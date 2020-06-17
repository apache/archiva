package org.apache.archiva.repository.maven.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.ItemDeleteStatus;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.BaseArtifactTypes;
import org.apache.archiva.repository.content.BaseRepositoryContentLayout;
import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.DataItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.content.LayoutRuntimeException;
import org.apache.archiva.repository.content.ManagedRepositoryContentLayout;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.ArchivaContentItem;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.content.base.ArchivaNamespace;
import org.apache.archiva.repository.content.base.ArchivaProject;
import org.apache.archiva.repository.content.base.ArchivaVersion;
import org.apache.archiva.repository.content.base.builder.ArtifactOptBuilder;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.StorageUtil;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ManagedDefaultRepositoryContent
 */
public class ManagedDefaultRepositoryContent
    extends AbstractDefaultRepositoryContent
    implements ManagedRepositoryContent, BaseRepositoryContentLayout
{

    // attribute flag that marks version objects that point to a snapshot artifact version
    public static final String SNAPSHOT_ARTIFACT_VERSION = "maven.snav";

    private FileTypes filetypes;

    public void setFileTypes( FileTypes fileTypes )
    {
        this.filetypes = fileTypes;
    }

    private ManagedRepository repository;

    private FileLockManager lockManager;

    @Inject
    @Named( "metadataReader#maven" )
    MavenMetadataReader metadataReader;

    @Inject
    @Named( "MavenContentHelper" )
    MavenContentHelper mavenContentHelper;

    public static final String SNAPSHOT = "SNAPSHOT";

    public static final Pattern UNIQUE_SNAPSHOT_PATTERN = Pattern.compile( "^(SNAPSHOT|[0-9]{8}\\.[0-9]{6}-[0-9]+)(.*)" );
    public static final Pattern CLASSIFIER_PATTERN = Pattern.compile( "^-([^.]+)(\\..*)" );
    public static final Pattern COMMON_EXTENSIONS = Pattern.compile( "^(jar|war|ear|dar|tar|zip|pom|xml)$" );

    public static final Pattern TIMESTAMP_PATTERN = Pattern.compile( "^([0-9]{8})\\.([0-9]{6})$" );

    public static final Pattern GENERIC_SNAPSHOT_PATTERN = Pattern.compile( "^(.*)-" + SNAPSHOT );

    private static final List<Class<? extends ManagedRepositoryContentLayout>> LAYOUTS = Arrays.asList( BaseRepositoryContentLayout.class );

    /**
     * We are caching content items in a weak reference map. To avoid always recreating the
     * the hierarchical structure.
     * TODO: Better use a object cache? E.g. our spring cache implementation?
     */
    private ReferenceMap<StorageAsset, ContentItem> itemMap = new ReferenceMap<>( );
    private ReferenceMap<StorageAsset, DataItem> dataItemMap = new ReferenceMap<>( );

    public ManagedDefaultRepositoryContent( )
    {
        super(  );
    }

    public ManagedDefaultRepositoryContent( ManagedRepository repository, FileTypes fileTypes, FileLockManager lockManager )
    {
        super(  );
        setFileTypes( fileTypes );
        this.lockManager = lockManager;
        setRepository( repository );
    }

    private StorageAsset getAssetByPath( String assetPath )
    {
        return getStorage( ).getAsset( assetPath );
    }

    private StorageAsset getAsset( String namespace )
    {
        String namespacePath = formatAsDirectory( namespace.trim( ) );
        if ( StringUtils.isEmpty( namespacePath ) )
        {
            namespacePath = "";
        }
        return getAssetByPath( namespacePath );
    }

    private StorageAsset getAsset( String namespace, String project )
    {
        return getAsset( namespace ).resolve( project );
    }

    private StorageAsset getAsset( String namespace, String project, String version )
    {
        return getAsset( namespace, project ).resolve( version );
    }

    private StorageAsset getAsset( String namespace, String project, String version, String fileName )
    {
        return getAsset( namespace, project, version ).resolve( fileName );
    }


    /// ************* Start of new generation interface ******************


    @Override
    public <T extends ContentItem> T adaptItem( Class<T> clazz, ContentItem item ) throws LayoutException
    {
        try
        {
            if ( clazz.isAssignableFrom( Version.class ) )
            {
                if ( !item.hasCharacteristic( Version.class ) )
                {
                    item.setCharacteristic( Version.class, createVersionFromPath( item.getAsset( ) ) );
                }
                return (T) item.adapt( Version.class );
            }
            else if ( clazz.isAssignableFrom( Project.class ) )
            {
                if ( !item.hasCharacteristic( Project.class ) )
                {
                    item.setCharacteristic( Project.class, createProjectFromPath( item.getAsset( ) ) );
                }
                return (T) item.adapt( Project.class );
            }
            else if ( clazz.isAssignableFrom( Namespace.class ) )
            {
                if ( !item.hasCharacteristic( Namespace.class ) )
                {
                    item.setCharacteristic( Namespace.class, createNamespaceFromPath( item.getAsset( ) ) );
                }
                return (T) item.adapt( Namespace.class );
            }
            else if ( clazz.isAssignableFrom( Artifact.class ) )
            {
                if ( !item.hasCharacteristic( Artifact.class ) )
                {
                    item.setCharacteristic( Artifact.class, createArtifactFromPath( item.getAsset( ) ) );
                }
                return (T) item.adapt( Artifact.class );
            }
        } catch (LayoutRuntimeException e) {
            throw new LayoutException( e.getMessage( ), e );
        }
        throw new LayoutException( "Could not convert item to class " + clazz);
    }


    @Override
    public void deleteAllItems( ItemSelector selector, Consumer<ItemDeleteStatus> consumer ) throws ContentAccessException, IllegalArgumentException
    {
        try ( Stream<? extends ContentItem> stream = newItemStream( selector, false ) )
        {
            stream.forEach( item -> {
                try
                {
                    deleteItem( item );
                    consumer.accept( new ItemDeleteStatus( item ) );
                }
                catch ( ItemNotFoundException e )
                {
                    consumer.accept( new ItemDeleteStatus( item, ItemDeleteStatus.ITEM_NOT_FOUND, e ) );
                }
                catch ( Exception e )
                {
                    consumer.accept( new ItemDeleteStatus( item, ItemDeleteStatus.DELETION_FAILED, e ) );
                }
                catch ( Throwable e )
                {
                    consumer.accept( new ItemDeleteStatus( item, ItemDeleteStatus.UNKNOWN, e ) );
                }
            } );
        }
    }

    /**
     * Removes the item from the filesystem. For namespaces, projects and versions it deletes
     * recursively.
     * For namespaces you have to be careful, because maven repositories may have sub namespaces
     * parallel to projects. Which means deleting a namespaces also deletes the sub namespaces and
     * not only the projects of the given namespace. Better run the delete for each project of
     * a namespace.
     * <p>
     * Artifacts are deleted as provided. No related artifacts will be deleted.
     *
     * @param item the item that should be removed
     * @throws ItemNotFoundException  if the item does not exist
     * @throws ContentAccessException if some error occurred while accessing the filesystem
     */
    @Override
    public void deleteItem( ContentItem item ) throws ItemNotFoundException, ContentAccessException
    {
        final Path baseDirectory = getRepoDir( );
        final Path itemPath = item.getAsset( ).getFilePath( );
        if ( !Files.exists( itemPath ) )
        {
            throw new ItemNotFoundException( "The item " + item.toString( ) + "does not exist in the repository " + getId( ) );
        }
        if ( !itemPath.toAbsolutePath( ).startsWith( baseDirectory.toAbsolutePath( ) ) )
        {
            log.error( "The namespace {} to delete from repository {} is not a subdirectory of the repository base.", item, getId( ) );
            log.error( "Namespace directory: {}", itemPath );
            log.error( "Repository directory: {}", baseDirectory );
            throw new ContentAccessException( "Inconsistent directories found. Could not delete namespace." );
        }
        try
        {
            if ( Files.isDirectory( itemPath ) )
            {
                FileUtils.deleteDirectory( itemPath );
            }
            else
            {
                Files.deleteIfExists( itemPath );
            }
        }
        catch ( IOException e )
        {
            log.error( "Could not delete item from path {}: {}", itemPath, e.getMessage( ), e );
            throw new ContentAccessException( "Error occured while deleting item " + item + ": " + e.getMessage( ), e );
        }
    }

    @Override
    public ContentItem getItem( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if ( selector.hasVersion( ) && selector.hasArtifactId( ) )
        {
            return getArtifact( selector );
        } else if ( !selector.hasVersion() && selector.hasArtifactVersion() && selector.hasArtifactId() ) {
            String baseVersion = VersionUtil.getBaseVersion( selector.getArtifactVersion( ) );
            ItemSelector selector1 = ArchivaItemSelector.builder( ).withSelector( selector )
                .withVersion(baseVersion).build();
            return getArtifact( selector1 );
        }
        else if ( selector.hasProjectId( ) && selector.hasVersion( ) )
        {
            return getVersion( selector );
        }
        else if ( selector.hasProjectId( ) )
        {
            return getProject( selector );
        }
        else
        {
            return getNamespace( selector );
        }
    }

    @Override
    public Namespace getNamespace( final ItemSelector namespaceSelector ) throws ContentAccessException, IllegalArgumentException
    {
        StorageAsset nsPath = getAsset( namespaceSelector.getNamespace() );
        try
        {
            return getNamespaceFromPath( nsPath );
        }
        catch ( LayoutException e )
        {
            throw new IllegalArgumentException( "Not a valid selector " + e.getMessage( ), e );
        }
    }


    @Override
    public Project getProject( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if ( !selector.hasProjectId( ) )
        {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        final StorageAsset path = getAsset( selector.getNamespace( ), selector.getProjectId( ) );
        try
        {
            return getProjectFromPath( path );
        }
        catch ( LayoutException e )
        {
            throw new IllegalArgumentException( "Not a valid selector " + e.getMessage( ), e );
        }
    }


    @Override
    public Version getVersion( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if ( !selector.hasProjectId( ) )
        {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        if ( !selector.hasVersion( ) )
        {
            throw new IllegalArgumentException( "Version must be set" );
        }
        final StorageAsset path = getAsset( selector.getNamespace( ), selector.getProjectId( ), selector.getVersion( ) );
        try
        {
            return getVersionFromPath( path );
        }
        catch ( LayoutException e )
        {
            throw new IllegalArgumentException( "Not a valid selector " + e.getMessage( ), e );
        }
    }


    public Artifact createArtifact( final StorageAsset artifactPath, final ItemSelector selector,
                                    final String classifier )
    {
        Version version = getVersion( selector );
        ArtifactOptBuilder builder = org.apache.archiva.repository.content.base.ArchivaArtifact.withAsset( artifactPath )
            .withVersion( version )
            .withId( selector.getArtifactId( ) )
            .withArtifactVersion( mavenContentHelper.getArtifactVersion( artifactPath, selector ) )
            .withClassifier( classifier );
        if ( selector.hasType( ) )
        {
            builder.withType( selector.getType( ) );
        }
        return builder.build( );
    }

    public Namespace getNamespaceFromArtifactPath( final StorageAsset artifactPath ) throws LayoutException
    {
        if (artifactPath == null) {
            throw new LayoutException( "Path null is not valid for artifact" );
        }
        final StorageAsset namespacePath = artifactPath.getParent( ).getParent( ).getParent( );
        return getNamespaceFromPath( namespacePath );
    }

    public Namespace getNamespaceFromPath( final StorageAsset nsPath ) throws LayoutException
    {
        if (nsPath == null) {
            throw new LayoutException( "Path null is not valid for namespace" );
        }

        ContentItem item;
        try
        {
            item = itemMap.computeIfAbsent( nsPath,
                path -> createNamespaceFromPath( nsPath ) );
        }
        catch ( LayoutRuntimeException e )
        {
            throw new LayoutException( e.getMessage( ), e.getCause() );
        }
        if (!item.hasCharacteristic( Namespace.class )) {
            item.setCharacteristic( Namespace.class, createNamespaceFromPath( nsPath ) );
        }
        return item.adapt( Namespace.class );
    }

    public Namespace createNamespaceFromPath( final StorageAsset namespacePath) throws LayoutRuntimeException
    {
        if (namespacePath == null) {
            throw new LayoutRuntimeException( "Path null is not valid for namespace" );
        }
        final String namespace = MavenContentHelper.getNamespaceFromNamespacePath( namespacePath );
        return ArchivaNamespace.withRepository( this )
            .withAsset( namespacePath )
            .withNamespace( namespace )
            .build( );
    }

    private Project getProjectFromPath( final StorageAsset path ) throws LayoutException
    {
        if (path == null) {
            throw new LayoutException( "Path null is not valid for project" );
        }
        ContentItem item;
        try
        {
            item = itemMap.computeIfAbsent( path, this::createProjectFromPath );
        }
        catch ( LayoutRuntimeException e )
        {
            throw new LayoutException( e.getMessage( ), e.getCause( ) );
        }
        if (!item.hasCharacteristic( Project.class )) {
            item.setCharacteristic( Project.class, createProjectFromPath( path ) );
        }
        return item.adapt( Project.class );
    }

    private Project createProjectFromPath( final StorageAsset projectPath ) throws LayoutRuntimeException
    {
        if (projectPath==null) {
            throw new LayoutRuntimeException( "Path null is not valid for project" );
        }
        Namespace namespace;
        try
        {
            namespace = getNamespaceFromPath( projectPath.getParent( ) );
        }
        catch ( LayoutException e )
        {
            throw new LayoutRuntimeException( e.getMessage( ), e.getCause() );
        }
        return ArchivaProject.withRepository( this ).withAsset( projectPath )
            .withNamespace( namespace )
            .withId( projectPath.getName( ) ).build( );
    }

    private Project getProjectFromArtifactPath( final StorageAsset artifactPath ) throws LayoutException
    {
        if (artifactPath == null) {
            throw new LayoutException( "Path null is not valid for artifact" );
        }
        final StorageAsset projectPath = artifactPath.getParent( ).getParent( );
        return getProjectFromPath( projectPath );
    }

    private Version getVersionFromArtifactPath( final StorageAsset artifactPath ) throws LayoutException
    {
        if (artifactPath==null) {
            throw new LayoutException( "Path null is not valid for version" );
        }
        final StorageAsset versionPath = artifactPath.getParent( );
        return getVersionFromPath( versionPath );
    }

    private Version getVersionFromPath( StorageAsset path ) throws LayoutException
    {
        if (path==null) {
            throw new LayoutException( "Path null is not valid for version" );
        }
        ContentItem item;
        try
        {
            item = itemMap.computeIfAbsent( path, this::createVersionFromPath );
        }
        catch ( LayoutRuntimeException e )
        {
            throw new LayoutException( e.getMessage( ), e.getCause( ) );
        }
        if (!item.hasCharacteristic( Version.class )) {
            item.setCharacteristic( Version.class, createVersionFromPath( path ) );
        }
        return item.adapt( Version.class );
    }

    private Version createVersionFromPath(StorageAsset path) throws LayoutRuntimeException
    {
        if (path==null) {
            throw new LayoutRuntimeException( "Path null is not valid for version" );
        }
        Project proj;
        try
        {
            proj = getProjectFromPath( path.getParent( ) );
        }
        catch ( LayoutException e )
        {
            throw new LayoutRuntimeException( e.getMessage( ), e );
        }
        return ArchivaVersion.withRepository( this ).withAsset( path )
            .withProject( proj ).withVersion(path.getName()).build();
    }

    private Optional<Artifact> getOptionalArtifactFromPath( final StorageAsset artifactPath) {
        try
        {
            return Optional.of( getArtifactFromPath( artifactPath ) );
        }
        catch ( LayoutException e )
        {
            log.error( "Could not get artifact from path {}", artifactPath.getPath( ) );
            return Optional.empty( );
        }
    }

    private Artifact getArtifactFromPath( final StorageAsset artifactPath ) throws LayoutException
    {
        if (artifactPath==null) {
            throw new LayoutException( "Path null is not valid for artifact" );
        }
        DataItem item;
        try
        {
            item = dataItemMap.computeIfAbsent( artifactPath, this::createArtifactFromPath );
        }
        catch ( LayoutRuntimeException e )
        {
            throw new LayoutException( e.getMessage( ), e.getCause() );
        }
        if (!item.hasCharacteristic( Artifact.class )) {
            item.setCharacteristic( Artifact.class, createArtifactFromPath( artifactPath ) );
        }
        return item.adapt( Artifact.class );
    }

    private Artifact createArtifactFromPath( final StorageAsset artifactPath ) throws LayoutRuntimeException
    {
        if (artifactPath==null) {
            throw new LayoutRuntimeException( "Path null is not valid for artifact" );
        }
        final Version version;
        try
        {
            version = getVersionFromArtifactPath( artifactPath );
        }
        catch ( LayoutException e )
        {
            throw new LayoutRuntimeException( e.getMessage( ), e );
        }
        final ArtifactInfo info = getArtifactInfoFromPath( version.getId( ), artifactPath );
        return org.apache.archiva.repository.content.base.ArchivaArtifact.withAsset( artifactPath )
            .withVersion( version )
            .withId( info.id )
            .withClassifier( info.classifier )
            .withRemainder( info.remainder )
            .withType( info.type )
            .withArtifactVersion( info.version )
            .withContentType( info.contentType )
            .withArtifactType( info.artifactType )
            .build( );
    }

    private String getContentType(StorageAsset artifactPath) {
        try
        {
            return Files.probeContentType( artifactPath.getFilePath( ) );

        }
        catch ( IOException e )
        {
            return "";
        }
    }


    private DataItem getDataItemFromPath( final StorageAsset artifactPath )
    {
        final String contentType = getContentType( artifactPath );
        return dataItemMap.computeIfAbsent( artifactPath, myArtifactPath ->
            org.apache.archiva.repository.content.base.ArchivaDataItem.withAsset( artifactPath )
                .withId( artifactPath.getName( ) )
                .withContentType( contentType )
                .build( )
        );

    }

    private ContentItem getItemFromPath( final StorageAsset itemPath )
    {
        if ( itemPath.isLeaf( ) )
        {
            if (dataItemMap.containsKey( itemPath )) {
                return dataItemMap.get( itemPath );
            }
            return getDataItemFromPath( itemPath );
        }
        else
        {
            if (itemMap.containsKey( itemPath )) {
                return itemMap.get( itemPath );
            } else {
                return ArchivaContentItem.withRepository( this ).withAsset( itemPath ).build();
            }
        }
    }

    @Override
    public ManagedRepositoryContent getGenericContent( )
    {
        return this;
    }

    private ArtifactInfo getArtifactInfoFromPath( final String genericVersion, final StorageAsset path )
    {
        final ArtifactInfo info = new ArtifactInfo( );
        info.asset = path;
        info.id = path.getParent( ).getParent( ).getName( );
        final String fileName = path.getName( );
        if ( VersionUtil.isGenericSnapshot( genericVersion ) )
        {
            String baseVersion = StringUtils.substringBeforeLast( genericVersion, "-" + SNAPSHOT );
            String prefix = info.id + "-" + baseVersion + "-";
            if ( fileName.startsWith( prefix ) )
            {
                String versionPostfix = StringUtils.removeStart( fileName, prefix );
                Matcher matcher = UNIQUE_SNAPSHOT_PATTERN.matcher( versionPostfix );
                if ( matcher.matches( ) )
                {
                    info.version = baseVersion + "-" + matcher.group( 1 );
                    String newPrefix = info.id + "-" + info.version;
                    if ( fileName.startsWith( newPrefix ) )
                    {
                        String classPostfix = StringUtils.removeStart( fileName, newPrefix );
                        Matcher cMatch = CLASSIFIER_PATTERN.matcher( classPostfix );
                        if ( cMatch.matches( ) )
                        {
                            info.classifier = cMatch.group( 1 );
                            info.remainder = cMatch.group( 2 );
                        }
                        else
                        {
                            info.classifier = "";
                            info.remainder = classPostfix;
                        }
                    }
                    else
                    {
                        log.debug( "Artifact does not match the maven name pattern {}", path );
                        info.artifactType = BaseArtifactTypes.UNKNOWN;
                        info.classifier = "";
                        info.remainder = StringUtils.substringAfter( fileName, prefix );
                    }
                }
                else
                {
                    log.debug( "Artifact does not match the snapshot version pattern {}", path );

                    info.artifactType = BaseArtifactTypes.UNKNOWN;
                    // This is just a guess. No guarantee to the get a usable version.
                    info.version = StringUtils.removeStart( fileName, info.id + '-' );
                    String postfix = StringUtils.substringAfterLast( info.version, "." ).toLowerCase( );
                    while ( COMMON_EXTENSIONS.matcher( postfix ).matches( ) )
                    {
                        info.version = StringUtils.substringBeforeLast( info.version, "." );
                        postfix = StringUtils.substringAfterLast( info.version, "." ).toLowerCase( );
                    }
                    info.classifier = "";
                    info.remainder = StringUtils.substringAfter( fileName, prefix );
                }
            }
            else
            {
                log.debug( "Artifact does not match the maven name pattern: {}", path );
                if ( fileName.contains( "-" + baseVersion ) )
                {
                    info.id = StringUtils.substringBefore( fileName, "-" + baseVersion );
                }
                else
                {
                    info.id = fileName;
                }
                info.artifactType = BaseArtifactTypes.UNKNOWN;
                info.version = "";
                info.classifier = "";
                info.remainder = StringUtils.substringAfterLast( fileName, "." );
            }
        }
        else
        {
            String prefix = info.id + "-" + genericVersion;
            if ( fileName.startsWith( prefix + "-") )
            {
                info.version = genericVersion;
                String classPostfix = StringUtils.removeStart( fileName, prefix );
                Matcher cMatch = CLASSIFIER_PATTERN.matcher( classPostfix );
                if ( cMatch.matches( ) )
                {
                    info.classifier = cMatch.group( 1 );
                    info.remainder = cMatch.group( 2 );
                }
                else
                {
                    info.classifier = "";
                    info.remainder = classPostfix;
                }
            } else if (fileName.startsWith(prefix + ".")) {
                info.version = genericVersion;
                info.remainder = StringUtils.removeStart( fileName, prefix );
                info.classifier = "";
            } else if (fileName.startsWith(info.id+"-")) {
                String postFix = StringUtils.removeStart( fileName, info.id + "-" );
                String versionPart = StringUtils.substringBefore( postFix, "." );
                if (VersionUtil.isVersion(versionPart)) {
                    info.version = versionPart;
                    info.remainder = StringUtils.removeStart( postFix, versionPart );
                    info.classifier = "";
                } else {
                    info.version = "";
                    info.classifier = "";
                    int dotPos = fileName.indexOf( "." );
                    info.remainder = fileName.substring( dotPos );
                }

            }
            else
            {
                if ( fileName.contains( "-" + genericVersion ) )
                {
                    info.id = StringUtils.substringBefore( fileName, "-" + genericVersion );
                }
                else
                {
                    info.id = fileName;
                    info.version = "";
                }
                log.debug( "Artifact does not match the version pattern {}", path );
                info.artifactType = BaseArtifactTypes.UNKNOWN;
                info.classifier = "";
                info.remainder = StringUtils.substringAfterLast( fileName, "." );
            }
        }
        info.extension = StringUtils.substringAfterLast( fileName, "." );
        info.type = MavenContentHelper.getTypeFromClassifierAndExtension( info.classifier, info.extension );
        try
        {
            info.contentType = Files.probeContentType( path.getFilePath( ) );
        }
        catch ( IOException e )
        {
            info.contentType = "";
            //
        }
        if ( MavenContentHelper.METADATA_FILENAME.equalsIgnoreCase( fileName ) )
        {
            info.artifactType = BaseArtifactTypes.METADATA;
        }
        else if ( MavenContentHelper.METADATA_REPOSITORY_FILENAME.equalsIgnoreCase( fileName ) )
        {
            info.artifactType = MavenTypes.REPOSITORY_METADATA;
        }
        else if ( StringUtils.isNotEmpty( info.remainder ) && StringUtils.countMatches( info.remainder, "." ) >= 2 )
        {
            String mainFile = StringUtils.substringBeforeLast( fileName, "." );
            if ( path.getParent( ).resolve( mainFile ).exists( ) )
            {
                info.artifactType = BaseArtifactTypes.RELATED;
            }
        }
        return info;

    }

    @Override
    public Artifact getArtifact( final ItemSelector selectorArg ) throws ContentAccessException
    {
        ItemSelector selector = selectorArg;
        if ( !selectorArg.hasProjectId( ) )
        {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        if ( !selectorArg.hasVersion( ) )
        {
            if (selectorArg.hasArtifactVersion() && VersionUtil.isSnapshot( selectorArg.getArtifactVersion() )) {
                selector = ArchivaItemSelector.builder( ).withSelector( selectorArg )
                    .withVersion( VersionUtil.getBaseVersion( selectorArg.getArtifactVersion( ) ) ).build();
            } else if (selectorArg.hasArtifactVersion()) {
                selector = ArchivaItemSelector.builder( ).withSelector( selectorArg )
                    .withVersion( selectorArg.getArtifactVersion( ) ).build();

            } else
            {
                throw new IllegalArgumentException( "Version must be set" );
            }
        }
        if ( !selectorArg.hasArtifactId( ) )
        {
            throw new IllegalArgumentException( "Artifact id must be set" );
        }
        final StorageAsset artifactDir = getAsset( selector.getNamespace( ), selector.getProjectId( ),
            selector.getVersion( ) );
        final String artifactVersion = mavenContentHelper.getArtifactVersion( artifactDir, selector );
        final String classifier = MavenContentHelper.getClassifier( selector );
        final String extension = MavenContentHelper.getArtifactExtension( selector );
        final String artifactId = StringUtils.isEmpty( selector.getArtifactId( ) ) ? selector.getProjectId( ) : selector.getArtifactId( );
        final String fileName = MavenContentHelper.getArtifactFileName( artifactId, artifactVersion, classifier, extension );
        final StorageAsset path = getAsset( selector.getNamespace( ), selector.getProjectId( ),
            selector.getVersion( ), fileName );
        try
        {
            return getArtifactFromPath( path );
        }
        catch ( LayoutException e )
        {
            throw new IllegalArgumentException( "The selector is not valid " + e.getMessage( ), e );
        }
    }

    @Override
    public Artifact getArtifact( String path ) throws LayoutException, ContentAccessException
    {
        StorageAsset asset = getAssetByPath( path );
        return getArtifactFromPath( asset );
    }

    /**
     * Returns all the subdirectories of the given namespace directory as project.
     */
    @Override
    public List<? extends Project> getProjects( Namespace namespace )
    {
        return namespace.getAsset( ).list( ).stream( )
            .filter( StorageAsset::isContainer )
            .map( a -> {
                try
                {
                    return getProjectFromPath( a );
                }
                catch ( LayoutException e )
                {
                    log.error( "Not a valid project path " + a.getPath( ), e );
                    return null;
                }
            } )
            .filter( Objects::nonNull )
            .collect( Collectors.toList( ) );
    }

    @Override
    public List<? extends Project> getProjects( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        return getProjects( getNamespace( selector ) );
    }

    /**
     * Returns a version object for each directory that is a direct child of the project directory.
     *
     * @param project the project for which the versions should be returned
     * @return the list of versions or a empty list, if not version was found
     */
    @Override
    public List<? extends Version> getVersions( final Project project )
    {
        StorageAsset asset = getAsset( project.getNamespace( ).getId( ), project.getId( ) );
        return asset.list( ).stream( ).filter( StorageAsset::isContainer )
            .map( a -> ArchivaVersion.withAsset( a )
                .withProject( project )
                .withVersion( a.getName( ) ).build( ) )
            .collect( Collectors.toList( ) );
    }

    /**
     * Returns the versions that can be found for the given selector.
     *
     * @param selector the item selector. At least namespace and projectId must be set.
     * @return the list of version objects or a empty list, if the selector does not match a version
     * @throws ContentAccessException   if the access to the underlying backend failed
     * @throws IllegalArgumentException if the selector has no projectId specified
     */
    @Override
    public List<? extends Version> getVersions( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if ( !selector.hasProjectId( ) )
        {
            log.error( "Bad item selector for version list: {}", selector );
            throw new IllegalArgumentException( "Project id not set, while retrieving versions." );
        }
        final Project project = getProject( selector );
        if ( selector.hasVersion( ) )
        {
            final StorageAsset asset = getAsset( selector.getNamespace( ), selector.getProjectId( ), selector.getVersion( ) );
            return asset.list( ).stream( ).map( a -> getArtifactInfoFromPath( selector.getVersion( ), a ) )
                .filter( ai -> StringUtils.isNotEmpty( ai.version ) )
                .map( v -> {
                    try
                    {
                        return getVersionFromArtifactPath( v.asset );
                    }
                    catch ( LayoutException e )
                    {
                        log.error( "Could not get version from asset " + v.asset.getPath( ) );
                        return null;
                    }
                } )
                .filter( Objects::nonNull )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
        else
        {
            return getVersions( project );
        }
    }

    public List<String> getArtifactVersions( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if ( !selector.hasProjectId( ) )
        {
            log.error( "Bad item selector for version list: {}", selector );
            throw new IllegalArgumentException( "Project id not set, while retrieving versions." );
        }
        final Project project = getProject( selector );
        if ( selector.hasVersion( ) )
        {
            final StorageAsset asset = getAsset( selector.getNamespace( ), selector.getProjectId( ), selector.getVersion( ) );
            return asset.list( ).stream( ).map( a -> getArtifactInfoFromPath( selector.getVersion( ), a ) )
                .filter( ai -> StringUtils.isNotEmpty( ai.version ) )
                .map( v -> v.version )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
        else
        {
            return project.getAsset( ).list( ).stream( ).map( a -> {
                try
                {
                    return getVersionFromPath( a );
                }
                catch ( LayoutException e )
                {
                    log.error( "Could not get version from path " + a.getPath( ) );
                    return null;
                }
            } ).filter( Objects::nonNull )
                .flatMap( v -> v.getAsset( ).list( ).stream( ).map( a -> getArtifactInfoFromPath( v.getId( ), a ) ) )
                .filter( ai -> StringUtils.isNotEmpty( ai.version ) )
                .map( v -> v.version )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
    }


    /**
     * See {@link #newArtifactStream(ItemSelector)}. This method collects the stream into a list.
     *
     * @param selector the selector for the artifacts
     * @return the list of artifacts
     * @throws ContentAccessException if the access to the underlying filesystem failed
     */
    @Override
    public List<? extends Artifact> getArtifacts( ItemSelector selector ) throws ContentAccessException
    {
        try ( Stream<? extends Artifact> stream = newArtifactStream( selector ) )
        {
            return stream.collect( Collectors.toList( ) );
        }
    }


    /*
     * File filter to select certain artifacts using the selector data.
     */
    private Predicate<StorageAsset> getArtifactFileFilterFromSelector( final ItemSelector selector )
    {
        Predicate<StorageAsset> p = StorageAsset::isLeaf;
        StringBuilder fileNamePattern = new StringBuilder( "^" );
        if ( selector.hasArtifactId( ) )
        {
            fileNamePattern.append( Pattern.quote( selector.getArtifactId( ) ) ).append( "-" );
        }
        else
        {
            fileNamePattern.append( "[A-Za-z0-9_\\-.]+-" );
        }
        if ( selector.hasArtifactVersion( ) )
        {
            if ( selector.getArtifactVersion( ).contains( "*" ) )
            {
                String[] tokens = StringUtils.splitByWholeSeparator( selector.getArtifactVersion( ), "*" );
                for ( String currentToken : tokens )
                {
                    if ( !currentToken.equals( "" ) )
                    {
                        fileNamePattern.append( Pattern.quote( currentToken ) );
                    }
                    fileNamePattern.append( "[A-Za-z0-9_\\-.]*" );
                }
            }
            else
            {
                fileNamePattern.append( Pattern.quote( selector.getArtifactVersion( ) ) );
            }
        }
        else
        {
            fileNamePattern.append( "[A-Za-z0-9_\\-.]+" );
        }
        String classifier = selector.hasClassifier( ) ? selector.getClassifier( ) :
            ( selector.hasType( ) ? MavenContentHelper.getClassifierFromType( selector.getType( ) ) : null );
        if ( classifier != null )
        {
            if ( "*".equals( classifier ) )
            {
                fileNamePattern.append( "(-[A-Za-z0-9]+)?\\." );
            }
            else
            {
                fileNamePattern.append( "-" ).append( Pattern.quote( classifier ) ).append( "\\." );
            }
        }
        else
        {
            fileNamePattern.append( "\\." );
        }
        String extension = selector.hasExtension( ) ? selector.getExtension( ) :
            ( selector.hasType( ) ? MavenContentHelper.getArtifactExtension( selector ) : null );
        if ( extension != null )
        {
            if ( selector.includeRelatedArtifacts( ) )
            {
                fileNamePattern.append( Pattern.quote( extension ) ).append( "(\\.[A-Za-z0-9]+)?" );
            }
            else
            {
                fileNamePattern.append( Pattern.quote( extension ) );
            }
        }
        else
        {
            fileNamePattern.append( "[A-Za-z0-9.]+" );
        }
        final Pattern pattern = Pattern.compile( fileNamePattern.toString( ) );
        return p.and( a -> pattern.matcher( a.getName( ) ).matches( ) );
    }


    /**
     * Returns the artifacts. The number of artifacts returned depend on the selector.
     * If the selector sets the flag {@link ItemSelector#includeRelatedArtifacts()} to <code>true</code>,
     * additional to the matching artifacts, related artifacts like hash values or signatures are included in the artifact
     * stream.
     * If the selector sets the flag {@link ItemSelector#recurse()} to <code>true</code>, artifacts of the given
     * namespace and from all sub namespaces that start with the given namespace are returned.
     * <ul>
     *     <li>If only a namespace is given, all artifacts with the given namespace or starting with the given
     *     namespace (see {@link ItemSelector#recurse()} are returned.</li>
     *     <li>If a namespace and a project id, or artifact id is given, the artifacts of all versions of the given
     *     namespace and project are returned.</li>
     *     <li>If a namespace and a project id or artifact id and a version is given, the artifacts of the given
     *     version are returned</li>
     *     <li>If no artifact version or artifact id is given, it will return all "artifacts" found in the directory.
     *     To select only artifacts that match the layout you should add the artifact id and artifact version
     *     (can contain a '*' pattern).</li>
     * </ul>
     * <p>
     * The '*' pattern can be used in classifiers and artifact versions and match zero or more characters.
     * <p>
     * There is no determinate order of the elements in the stream.
     * <p>
     * Returned streams are auto closable and should be used in a try-with-resources statement.
     *
     * @param selector the item selector
     * @throws ContentAccessException if the access to the underlying filesystem failed
     */
    @Override
    public Stream<? extends Artifact> newArtifactStream( ItemSelector selector ) throws ContentAccessException
    {
        String projectId = selector.hasProjectId( ) ? selector.getProjectId( ) : ( selector.hasArtifactId( ) ? selector.getArtifactId( )
            : null );
        final Predicate<StorageAsset> filter = getArtifactFileFilterFromSelector( selector );
        if ( projectId != null && selector.hasVersion( ) )
        {
            return getAsset( selector.getNamespace( ), projectId, selector.getVersion( ) )
                .list( ).stream( ).filter( filter )
                .map( this::getOptionalArtifactFromPath )
                .filter( Optional::isPresent ).map( Optional::get );
        }
        else if ( projectId != null )
        {
            final StorageAsset projDir = getAsset( selector.getNamespace( ), projectId );
            return projDir.list( ).stream( )
                .map( a -> a.isContainer( ) ? a.list( ) : Collections.singletonList( a ) )
                .flatMap( List::stream )
                .filter( filter )
                .map( this::getOptionalArtifactFromPath )
                .filter( Optional::isPresent ).map( Optional::get );
        }
        else
        {
            StorageAsset namespaceDir = getAsset( selector.getNamespace( ) );
            if ( selector.recurse( ) )
            {
                return StorageUtil.newAssetStream( namespaceDir, true )
                    .filter( filter )
                    .map( this::getOptionalArtifactFromPath )
                    .filter( Optional::isPresent ).map( Optional::get );
            }
            else
            {
                // We descend into 2 subdirectories (project and version)
                return namespaceDir.list( ).stream( )
                    .map( a -> a.isContainer( ) ? a.list( ) : Collections.singletonList( a ) )
                    .flatMap( List::stream )
                    .map( a -> a.isContainer( ) ? a.list( ) : Collections.singletonList( a ) )
                    .flatMap( List::stream )
                    .filter( filter )
                    .map( this::getOptionalArtifactFromPath )
                    .filter( Optional::isPresent ).map( Optional::get );
            }
        }
    }

    /**
     * Same as {@link #newArtifactStream(ContentItem)} but returns the collected stream as list.
     *
     * @param item the item the parent item
     * @return the list of artifacts or a empty list of no artifacts where found
     */
    @Override
    public List<? extends Artifact> getArtifacts( ContentItem item )
    {
        try ( Stream<? extends Artifact> stream = newArtifactStream( item ) )
        {
            return stream.collect( Collectors.toList( ) );
        }
    }

    /**
     * Returns all artifacts
     *
     * @param item the namespace to search for artifacts
     * @return the stream of artifacts
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    public Stream<? extends Artifact> newArtifactStream( Namespace item ) throws ContentAccessException
    {
        return newArtifactStream( ArchivaItemSelector.builder( ).withNamespace( item.getId( ) ).build( ) );
    }

    public Stream<? extends Artifact> newArtifactStream( Project item ) throws ContentAccessException
    {
        return newArtifactStream( ArchivaItemSelector.builder( ).withNamespace( item.getNamespace( ).getId( ) )
            .withProjectId( item.getId( ) ).build( ) );
    }

    public Stream<? extends Artifact> newArtifactStream( Version item ) throws ContentAccessException
    {
        return newArtifactStream( ArchivaItemSelector.builder( ).withNamespace( item.getProject( ).getNamespace( ).getId( ) )
            .withProjectId( item.getProject( ).getId( ) )
            .withVersion( item.getId( ) ).build( ) );
    }

    /**
     * Returns all related artifacts that match the given artifact. That means all artifacts that have
     * the same filename plus an additional extension, e.g. ${fileName}.sha2
     *
     * @param item the artifact
     * @return the stream of artifacts
     * @throws ContentAccessException if access to the underlying storage failed
     */
    public Stream<? extends Artifact> newArtifactStream( Artifact item ) throws ContentAccessException
    {
        final Version v = item.getVersion( );
        final String fileName = item.getFileName( );
        final Predicate<StorageAsset> filter = ( StorageAsset a ) ->
            a.getName( ).startsWith( fileName + "." );
        return v.getAsset( ).list( ).stream( ).filter( filter )
            .map( a -> {
                try
                {
                    return getArtifactFromPath( a );
                }
                catch ( LayoutException e )
                {
                    log.error( "Not a valid artifact path " + a.getPath( ), e );
                    return null;
                }
            } ).filter( Objects::nonNull );
    }

    /**
     * Returns the stream of artifacts that are children of the given item.
     *
     * @param item the item from where the artifacts should be returned
     * @return the stream of artifacts
     * @throws ContentAccessException if access to the underlying storage failed
     */
    @Override
    public Stream<? extends Artifact> newArtifactStream( ContentItem item ) throws ContentAccessException
    {
        if ( item instanceof Namespace )
        {
            return newArtifactStream( ( (Namespace) item ) );
        }
        else if ( item instanceof Project )
        {
            return newArtifactStream( (Project) item );
        }
        else if ( item instanceof Version )
        {
            return newArtifactStream( (Version) item );
        }
        else if ( item instanceof Artifact )
        {
            return newArtifactStream( (Artifact) item );
        }
        else
        {
            log.warn( "newArtifactStream for unsupported item requested: {}", item.getClass( ).getName( ) );
            return Stream.empty( );
        }
    }

    private void appendPatternRegex( StringBuilder builder, String name )
    {
        String[] patternArray = name.split( "[*]" );
        for ( int i = 0; i < patternArray.length - 1; i++ )
        {
            builder.append( Pattern.quote( patternArray[i] ) )
                .append( "[A-Za-z0-9_\\-]*" );
        }
        builder.append( Pattern.quote( patternArray[patternArray.length - 1] ) );
    }

    Predicate<StorageAsset> getItemFileFilterFromSelector( ItemSelector selector )
    {
        if ( !selector.hasNamespace( ) && !selector.hasProjectId( ) )
        {
            throw new IllegalArgumentException( "Selector must have at least namespace and projectid" );
        }
        StringBuilder pathMatcher = new StringBuilder( "^" );
        if ( selector.hasNamespace( ) )
        {
            String path = "/" + String.join( "/", selector.getNamespace( ).split( "\\." ) );
            if ( path.contains( "*" ) )
            {
                appendPatternRegex( pathMatcher, path );
            }
            else
            {
                pathMatcher.append( Pattern.quote( path ) );
            }

        }
        if ( selector.hasProjectId( ) )
        {
            pathMatcher.append( "/" );
            if ( selector.getProjectId( ).contains( "*" ) )
            {
                appendPatternRegex( pathMatcher, selector.getProjectId( ) );
            }
            else
            {
                pathMatcher.append( Pattern.quote( selector.getProjectId( ) ) );
            }
        }
        if ( selector.hasVersion( ) )
        {
            pathMatcher.append( "/" );
            if ( selector.getVersion( ).contains( "*" ) )
            {
                appendPatternRegex( pathMatcher, selector.getVersion( ) );
            }
            else
            {
                pathMatcher.append( Pattern.quote( selector.getVersion( ) ) );
            }
        }
        pathMatcher.append( ".*" );
        final Pattern pathPattern = Pattern.compile( pathMatcher.toString( ) );
        final Predicate<StorageAsset> pathPredicate = ( StorageAsset asset ) -> pathPattern.matcher( asset.getPath( ) ).matches( );
        if ( selector.hasArtifactId( ) || selector.hasArtifactVersion( ) || selector.hasClassifier( )
            || selector.hasType( ) || selector.hasExtension( ) )
        {
            return getArtifactFileFilterFromSelector( selector ).and( pathPredicate );
        }
        else
        {
            return pathPredicate;
        }
    }

    /**
     * Returns a concatenation of the asset and its children as stream, if they exist.
     * It descends <code>level+1</code> levels down.
     *
     * @param a the asset to start from
     * @param level the number of child levels to descend. 0 means only the children of the given asset, 1 means the children of childrens of the given asset, ...
     * @return the stream of storage assets
     */
    private Stream<StorageAsset> getChildrenDF( StorageAsset a, int level )
    {
        if ( a.isContainer( ) )
        {
            if (level>0) {
                return Stream.concat( a.list().stream( ).flatMap( ch -> getChildrenDF( ch, level - 1 ) ), Stream.of( a ) );
            } else
            {
                return Stream.concat( a.list( ).stream( ), Stream.of( a ) );
            }
        }
        else
        {
            return Stream.of( a );
        }
    }

    @Override
    public Stream<? extends ContentItem> newItemStream( ItemSelector selector, boolean parallel ) throws ContentAccessException, IllegalArgumentException
    {
        final Predicate<StorageAsset> filter = getItemFileFilterFromSelector( selector );
        StorageAsset startDir;
        if (selector.getNamespace().contains("*")) {
            startDir = getAsset("");
        } else if ( selector.hasProjectId( ) && selector.getProjectId().contains("*") )
        {
            startDir = getAsset( selector.getNamespace( ) );
        } else if ( selector.hasProjectId() && selector.hasVersion() && selector.getVersion().contains("*")) {
            startDir = getAsset( selector.getNamespace( ), selector.getProjectId( ) );
        }
        else if ( selector.hasProjectId( ) && selector.hasVersion( ) )
        {
            startDir = getAsset( selector.getNamespace( ), selector.getProjectId( ), selector.getVersion() );
        }
        else if ( selector.hasProjectId( ) )
        {
            startDir = getAsset( selector.getNamespace( ), selector.getProjectId( ) );
        }
        else
        {
            startDir = getAsset( selector.getNamespace( ) );
            if ( !selector.recurse( ) )
            {
                // We descend into 2 subdirectories (project and version)
                return startDir.list( ).stream( )
                    .flatMap( a -> getChildrenDF( a, 1 ) )
                    .map( this::getItemFromPath );
            }
        }
        return StorageUtil.newAssetStream( startDir, parallel )
            .filter( filter )
            .map( this::getItemFromPath );

    }

    /**
     * Checks, if the asset/file queried by the given selector exists.
     */
    @Override
    public boolean hasContent( ItemSelector selector )
    {
        return getItem( selector ).getAsset( ).exists( );
    }

    @Override
    public ContentItem getParent( ContentItem item )
    {
        return getItemFromPath( item.getAsset( ).getParent( ) );
    }

    @Override
    public List<? extends ContentItem> getChildren( ContentItem item )
    {
        if (item.getAsset().isLeaf()) {
            return Collections.emptyList( );
        } else {
            return item.getAsset( ).list( ).stream( ).map( this::getItemFromPath ).collect( Collectors.toList( ) );
        }
    }

    @Override
    public <T extends ContentItem> T applyCharacteristic( Class<T> clazz, ContentItem item ) throws LayoutException
    {
            if (item.getAsset().isLeaf()) {
                if (clazz.isAssignableFrom( Artifact.class )) {
                    Artifact artifact = getArtifactFromPath( item.getAsset( ) );
                    item.setCharacteristic( Artifact.class, artifact );
                    return (T) artifact;
                } else {
                    throw new LayoutException( "Could not adapt file to clazz " + clazz );
                }
            } else {
                if (clazz.isAssignableFrom( Version.class )) {
                    Version version = getVersionFromPath( item.getAsset( ) );
                    item.setCharacteristic( Version.class, version );
                    return (T) version;
                } else if (clazz.isAssignableFrom( Project.class )) {
                    Project project = getProjectFromPath( item.getAsset( ) );
                    item.setCharacteristic( Project.class, project );
                    return (T) project;
                } else if (clazz.isAssignableFrom( Namespace.class )) {
                    Namespace ns = getNamespaceFromPath( item.getAsset( ) );
                    item.setCharacteristic( Namespace.class, ns );
                    return (T) ns;
                } else {
                    throw new LayoutException( "Cannot adapt directory to clazz " + clazz );
                }
            }
    }

    @Override
    public <T extends ManagedRepositoryContentLayout> T getLayout( Class<T> clazz ) throws LayoutException
    {
        if (clazz.isAssignableFrom( this.getClass() )) {
            return (T) this;
        } else {
            throw new LayoutException( "Cannot convert to layout " + clazz );
        }
    }

    @Override
    public <T extends ManagedRepositoryContentLayout> boolean supportsLayout( Class<T> clazz )
    {
        return clazz.isAssignableFrom( this.getClass( ) );
    }

    @Override
    public List<Class<? extends ManagedRepositoryContentLayout>> getSupportedLayouts( )
    {
        return LAYOUTS;
    }

    /**
     * Moves the file to the artifact destination
     */
    @Override
    public void addArtifact( Path sourceFile, Artifact destination ) throws IllegalArgumentException, ContentAccessException
    {
        try
        {
            StorageAsset asset = destination.getAsset( );
            if ( !asset.exists( ) )
            {
                asset.create( );
            }
            asset.replaceDataFromFile( sourceFile );
        }
        catch ( IOException e )
        {
            log.error( "Could not push data to asset source={} destination={}. {}", sourceFile, destination.getAsset( ).getFilePath( ), e.getMessage( ) );
            throw new ContentAccessException( e.getMessage( ), e );
        }
    }

    @Override
    public ContentItem toItem( String path ) throws LayoutException
    {

        StorageAsset asset = getRepository( ).getAsset( path );
        ContentItem item = getItemFromPath( asset );
        if (item instanceof DataItem) {
            Artifact artifact = adaptItem( Artifact.class, item );
            if (asset.getParent()==null) {
                throw new LayoutException( "Path too short for maven artifact "+path );
            }
            String version = asset.getParent( ).getName( );
            if (asset.getParent().getParent()==null) {
                throw new LayoutException( "Path too short for maven artifact " + path );
            }
            String project = item.getAsset( ).getParent( ).getParent( ).getName( );
            DataItem dataItem = (DataItem) item;
            if (StringUtils.isEmpty( dataItem.getExtension())) {
                throw new LayoutException( "Missing type on maven artifact" );
            }
            if (!project.equals(artifact.getId())) {
                throw new LayoutException( "The maven artifact id "+artifact.getId() +" does not match the project id: " + project);
            }
            boolean versionIsGenericSnapshot = VersionUtil.isGenericSnapshot( version );
            boolean artifactVersionIsSnapshot = VersionUtil.isSnapshot( artifact.getArtifactVersion() );
            if ( versionIsGenericSnapshot && !artifactVersionIsSnapshot ) {
                throw new LayoutException( "The maven artifact has no snapshot version in snapshot directory " + dataItem );
            }
            if ( !versionIsGenericSnapshot && artifactVersionIsSnapshot) {
                throw new LayoutException( "The maven artifact version " + artifact.getArtifactVersion() + " is a snapshot version but inside a non snapshot directory " + version );
            }
            if ( !versionIsGenericSnapshot && !version.equals( artifact.getArtifactVersion() ) )
            {
                throw new LayoutException( "The maven artifact version " + artifact.getArtifactVersion() + " does not match the version directory " + version );
            }
        }
        return item;
    }

    @Override
    public ContentItem toItem( StorageAsset assetPath ) throws LayoutException
    {
        return toItem( assetPath.getPath( ) );
    }

    /// ************* End of new generation interface ******************

    @Override
    public String toPath( ContentItem item ) {
        return item.getAsset( ).getPath( );
    }

    @Override
    public DataItem getMetadataItem( Version version ) {
        StorageAsset metaPath = version.getAsset( ).resolve( MAVEN_METADATA );
        return getDataItemFromPath( metaPath );
    }

    @Override
    public DataItem getMetadataItem( Project project )
    {
        StorageAsset metaPath = project.getAsset( ).resolve( MAVEN_METADATA );
        return getDataItemFromPath( metaPath );
    }


    @Override
    public String getId( )
    {
        return repository.getId( );
    }

    @Override
    public ManagedRepository getRepository( )
    {
        return repository;
    }

    @Override
    public void setRepository( final ManagedRepository repo )
    {
        this.repository = repo;
        if ( repo != null )
        {
            if ( repository instanceof EditableManagedRepository )
            {
                ( (EditableManagedRepository) repository ).setContent( this );
            }
        }
    }

    private Path getRepoDir( )
    {
        return repository.getRoot().getFilePath( );
    }

    private RepositoryStorage getStorage( )
    {
        return repository.getRoot().getStorage( );
    }

    public void setFiletypes( FileTypes filetypes )
    {
        this.filetypes = filetypes;
    }

    public void setMavenContentHelper( MavenContentHelper contentHelper )
    {
        this.mavenContentHelper = contentHelper;
    }


    public MavenMetadataReader getMetadataReader( )
    {
        return metadataReader;
    }

    public void setMetadataReader( MavenMetadataReader metadataReader )
    {
        this.metadataReader = metadataReader;
    }
}
