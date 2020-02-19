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

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.DefaultArtifactMappingProvider;
import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentAccessException;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.EditableManagedRepository;
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
import org.apache.archiva.repository.content.base.ArchivaNamespace;
import org.apache.archiva.repository.content.base.ArchivaProject;
import org.apache.archiva.repository.content.base.ArchivaVersion;
import org.apache.archiva.repository.content.base.builder.ArtifactOptBuilder;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ManagedDefaultRepositoryContent
 */
public class ManagedDefaultRepositoryContent
    extends AbstractDefaultRepositoryContent
    implements ManagedRepositoryContent
{

    public  static final String METADATA_FILENAME = "maven-metadata.xml";
    private FileTypes filetypes;

    public void setFileTypes(FileTypes fileTypes) {
        this.filetypes = fileTypes;
    }

    private ManagedRepository repository;

    FileLockManager lockManager;

    @Inject
    @Named("repositoryPathTranslator#maven2")
    private RepositoryPathTranslator pathTranslator;

    @Inject
    @Named( "metadataReader#maven" )
    MavenMetadataReader metadataReader;

    /**
     * We are caching content items in a weak reference map. To avoid always recreating the
     * the hierarchical structure.
     * TODO: Better use a object cache? E.g. our spring cache implementation?
     */
    private ReferenceMap<String, Namespace> namespaceMap = new ReferenceMap<>( );
    private ReferenceMap<StorageAsset, Project> projectMap = new ReferenceMap<>( );
    private ReferenceMap<StorageAsset, Version> versionMap = new ReferenceMap<>( );
    private ReferenceMap<StorageAsset, Artifact> artifactMap = new ReferenceMap<>( );

    public ManagedDefaultRepositoryContent(ManagedRepository repository, FileTypes fileTypes, FileLockManager lockManager) {
        super(Collections.singletonList( new DefaultArtifactMappingProvider() ));
        setFileTypes( fileTypes );
        this.lockManager = lockManager;
        setRepository( repository );
    }

    public ManagedDefaultRepositoryContent( ManagedRepository repository, List<? extends ArtifactMappingProvider> artifactMappingProviders, FileTypes fileTypes, FileLockManager lockManager )
    {
        super(artifactMappingProviders==null ? Collections.singletonList( new DefaultArtifactMappingProvider() ) : artifactMappingProviders);
        setFileTypes( fileTypes );
        this.lockManager = lockManager;
        setRepository( repository );

    }

    /**
     * Returns a version reference from the coordinates
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the versioned reference object
     */
    @Override
    public VersionedReference toVersion( String groupId, String artifactId, String version ) {
        return new VersionedReference().groupId( groupId ).artifactId( artifactId ).version( version );
    }

    @Override
    public VersionedReference toGenericVersion( ArtifactReference artifactReference )
    {
        return toVersion( artifactReference.getGroupId( ), artifactReference.getArtifactId( ), VersionUtil.getBaseVersion( artifactReference.getVersion( ) ));
    }

    /**
     * Return the version the artifact is part of
     * @param artifactReference
     * @return
     */
    public VersionedReference toVersion( ArtifactReference artifactReference) {
        return toVersion( artifactReference.getGroupId( ), artifactReference.getArtifactId( ), artifactReference.getVersion( ) );
    }

    @Override
    public ArtifactReference toArtifact( String groupId, String artifactId, String version, String type, String classifier) {
        return new ArtifactReference( ).groupId( groupId ).artifactId( artifactId ).version( version ).type( type ).classifier( classifier );
    }

    @Override
    public void deleteItem( ContentItem item ) throws ItemNotFoundException, ContentAccessException
    {
        final Path baseDirectory = getRepoDir( );
        final Path itemPath = item.getAsset( ).getFilePath( );
        if ( !Files.exists( itemPath ) )
        {
            throw new ItemNotFoundException( "The item " + item.toString() + "does not exist in the repository " + getId( ) );
        }
        if ( !itemPath.toAbsolutePath().startsWith( baseDirectory.toAbsolutePath() ) )
        {
            log.error( "The namespace {} to delete from repository {} is not a subdirectory of the repository base.", item, getId( ) );
            log.error( "Namespace directory: {}", itemPath );
            log.error( "Repository directory: {}", baseDirectory );
            throw new ContentAccessException( "Inconsistent directories found. Could not delete namespace." );
        }
        try
        {
            if (Files.isDirectory( itemPath ))
            {
                FileUtils.deleteDirectory( itemPath );
            } else {
                Files.deleteIfExists( itemPath );
            }
        }
        catch ( IOException e )
        {
            log.error( "Could not delete namespace directory {}: {}", itemPath, e.getMessage( ), e );
            throw new ContentAccessException( "Error occured while deleting namespace " + item + ": " + e.getMessage( ), e );
        }
    }

    private StorageAsset getAsset(String namespace) {
        String namespacePath = formatAsDirectory( namespace.trim() );
        if (StringUtils.isEmpty( namespacePath )) {
            namespacePath = "";
        }
        return getAsset(namespacePath);
    }

    private StorageAsset getAsset(String namespace, String project) {
        return getAsset( namespace ).resolve( project );
    }

    private StorageAsset getAsset(String namespace, String project, String version) {
        return getAsset( namespace, project ).resolve( version );
    }

    private StorageAsset getAsset(String namespace, String project, String version, String fileName) {
        return getAsset( namespace, project, version ).resolve( fileName );
    }


    @Override
    public Namespace getNamespace( final ItemSelector namespaceSelector ) throws ContentAccessException, IllegalArgumentException
    {
        return namespaceMap.computeIfAbsent( namespaceSelector.getNamespace(),
            namespace -> {
                StorageAsset nsPath = getAsset( namespace );
                return ArchivaNamespace.withRepository( this ).withAsset( nsPath ).
                    withNamespace( namespace ).build( );
            });
    }


    @Override
    public Project getProject( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if (!selector.hasProjectId()) {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        final StorageAsset path = getAsset( selector.getNamespace( ), selector.getProjectId( ) );
        return projectMap.computeIfAbsent( path, projectPath -> {
            final Namespace ns = getNamespace( selector );
            return ArchivaProject.withAsset( projectPath ).withNamespace( ns ).withId( selector.getProjectId( ) ).build( );
        }
        );
    }


    @Override
    public Version getVersion( final ItemSelector selector ) throws ContentAccessException, IllegalArgumentException
    {
        if (!selector.hasProjectId()) {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        if (!selector.hasVersion() ) {
            throw new IllegalArgumentException( "Version must be set" );
        }
        final StorageAsset path = getAsset(selector.getNamespace(), selector.getProjectId(), selector.getVersion());
        return versionMap.computeIfAbsent( path, versionPath -> {
            final Project project = getProject( selector );
            return ArchivaVersion.withAsset( path )
                .withProject( project )
                .withVersion( selector.getVersion( ) ).build();
        } );
    }


    /*
    TBD
     */
    private String getArtifactFileName(ItemSelector selector, String artifactVersion,
                                       String classifier, String extension) {
        StringBuilder fileName = new StringBuilder( selector.getArtifactId() ).append( "-" );
        fileName.append( artifactVersion );
        if ( !StringUtils.isEmpty( classifier ) )
        {
            fileName.append( "-" ).append( classifier );
        }
        fileName.append( "." ).append( extension );
        return fileName.toString( );
    }

    private String getClassifier(ItemSelector selector) {
        if (selector.hasClassifier()) {
            return selector.getClassifier();
        } else if (selector.hasType()) {
            return getClassifierFromType( selector.getType( ) );
        } else {
            return "";
        }
    }

    private String getClassifierFromType(final String type) {
        if ("pom".equalsIgnoreCase(type) ||  "jar".equalsIgnoreCase( type )
            || "maven-plugin".equalsIgnoreCase( type )
            || "ejb".equalsIgnoreCase( type )
            || "ear".equalsIgnoreCase( type )
            || "war".equalsIgnoreCase( type )
            || "rar".equalsIgnoreCase( type )
        ) return "";
        if ("test-jar".equalsIgnoreCase( type )) {
            return "tests";
        } else if ("ejb-client".equalsIgnoreCase( type )) {
            return "client";
        } else if ("java-source".equalsIgnoreCase( type )) {
            return "sources";
        } else if ("javadoc".equalsIgnoreCase( type )) {
            return "javadoc";
        } else {
            return "";
        }
    }

    private String getArtifactExtension(ItemSelector selector) {
        if (selector.hasExtension()) {
            return selector.getExtension( );
        } else if (selector.hasType()) {
            String type = selector.getType( ).toLowerCase( );
            if ("test-jar".equals( type )) {
                return "jar";
            } else if ("ejb-client".equals( type )) {
                return "jar";
            } else if ("java-source".equals( type )) {
                return "jar";
            } else if ("javadoc".equals( type )) {
                return "jar";
            } else {
                return "jar";
            }
        } else {
            return "jar";
        }
    }

    /*
    TBD
     */
    private String getArtifactVersion(StorageAsset artifactDir, ItemSelector selector) {
        if (selector.hasArtifactVersion()) {
            return selector.getArtifactVersion();
        } else if (selector.hasVersion()) {
            if ( VersionUtil.isGenericSnapshot( selector.getVersion() ) ) {
                return getArtifactSnapshotVersion( artifactDir, selector.getVersion( ) );
            } else {
                return selector.getVersion( );
            }
        } else {
            throw new IllegalArgumentException( "No version set on the selector " );
        }
    }


    public String getArtifactSnapshotVersion(StorageAsset artifactDir, String snapshotVersion) {
        final StorageAsset metadataFile = artifactDir.resolve( METADATA_FILENAME );
        StringBuilder version = new StringBuilder( );
        try
        {
            ArchivaRepositoryMetadata metadata = metadataReader.read( metadataFile );

            // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
            SnapshotVersion metadataVersion = metadata.getSnapshotVersion( );
            if ( metadataVersion != null )
            {
                version.append( snapshotVersion, 0, snapshotVersion.length( ) - 8 ); // remove SNAPSHOT from end
                version.append( metadataVersion.getTimestamp( )).append("-").append( metadataVersion.getBuildNumber( ) );
            }
            return version.toString( );
        }
        catch ( RepositoryMetadataException e )
        {
            // unable to parse metadata - LOGGER it, and continue with the version as the original SNAPSHOT version
            log.warn( "Invalid metadata: {} - {}", metadataFile, e.getMessage( ) );
            return snapshotVersion;
        }
    }

    private Artifact createArtifact(final StorageAsset artifactPath, final ItemSelector selector,
        final String classifier, final String extension) {
        Version version = getVersion(selector);
        ArtifactOptBuilder builder = org.apache.archiva.repository.content.base.ArchivaArtifact.withAsset( artifactPath )
            .withVersion( version )
            .withId( selector.getArtifactId( ) )
            .withArtifactVersion( getArtifactVersion( artifactPath, selector ) );
        if (selector.hasClassifier()) {
            builder.withClassifier( selector.getClassifier( ) );
        }
        if (selector.hasType()) {
            builder.withType( selector.getType( ) );
        }
        return builder.build( );
    }

    @Override
    public Artifact getArtifact( final ItemSelector selector ) throws ContentAccessException
    {
        if (!selector.hasProjectId( )) {
            throw new IllegalArgumentException( "Project id must be set" );
        }
        if (!selector.hasVersion( )) {
            throw new IllegalArgumentException( "Version must be set" );
        }
        if (!selector.hasArtifactId( )) {
            throw new IllegalArgumentException( "Artifact Id must be set" );
        }
        final StorageAsset artifactDir = getAsset(selector.getNamespace(), selector.getProjectId(),
            selector.getVersion());
        final String artifactVersion = getArtifactVersion( artifactDir, selector );
        final String classifier = getClassifier( selector );
        final String extension = getArtifactExtension( selector );
        final String fileName = getArtifactFileName( selector, artifactVersion, classifier, extension );
        final StorageAsset path = getAsset( selector.getNamespace( ), selector.getProjectId( ),
            selector.getVersion( ), fileName );
        return artifactMap.computeIfAbsent( path, artifactPath -> createArtifact( path, selector, classifier, extension ) );
    }

    /*
        TBD
     */
    @Override
    public List<Artifact> getAllArtifacts( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public Stream<Artifact> getAllArtifactStream( ItemSelector selector ) throws ContentAccessException
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public List<Project> getProjects( Namespace namespace )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public List<Version> getVersions( Project project )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public List<Artifact> getArtifacts( ContentItem item )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public List<Artifact> getArtifactsStartingWith( Namespace namespace )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public Stream<Artifact> getArtifactStream( ContentItem item )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public Stream<Artifact> getArtifactStreamStartingWith( Namespace namespace )
    {
        return null;
    }

    /*
        TBD
     */
    @Override
    public boolean hasContent( ItemSelector selector )
    {
        return false;
    }

    /*
        TBD
     */
    @Override
    public void copyArtifact( Path sourceFile, ItemSelector destination ) throws IllegalArgumentException
    {

    }

    @Override
    public void deleteVersion( VersionedReference ref ) throws ContentNotFoundException, ContentAccessException
    {
        final String path = toPath( ref );
        final Path deleteTarget = getRepoDir().resolve(path);
        if ( !Files.exists(deleteTarget) )
        {
            log.warn( "Version path for repository {} does not exist: {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Version not found for repository "+getId()+": "+path );
        }
        if ( Files.isDirectory(deleteTarget) )
        {
            try
            {
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( deleteTarget );
            }
            catch ( IOException e )
            {
                log.error( "Could not delete file path {}: {}", deleteTarget, e.getMessage( ), e );
                throw new ContentAccessException( "Error while trying to delete path "+path+" from repository "+getId()+": "+e.getMessage( ), e );
            }
        } else {
            log.warn( "Version path for repository {} is not a directory {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Version path for repository "+getId()+" is not directory: " + path );
        }
    }

    @Override
    public void deleteProject( ProjectReference ref )
        throws ContentNotFoundException, ContentAccessException
    {
        final String path = toPath( ref );
        final Path deleteTarget = getRepoDir( ).resolve( path );
        if ( !Files.exists(deleteTarget) )
        {
            log.warn( "Project path for repository {} does not exist: {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Project not found for repository "+getId()+": "+path );
        }
        if ( Files.isDirectory(deleteTarget) )
        {
            try
            {
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( deleteTarget );
            }
            catch ( IOException e )
            {
                log.error( "Could not delete file path {}: {}", deleteTarget, e.getMessage( ), e );
                throw new ContentAccessException( "Error while trying to delete path "+path+" from repository "+getId()+": "+e.getMessage( ), e );
            }
        }
        else
        {
            log.warn( "Project path for repository {} is not a directory {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Project path for repository "+getId()+" is not directory: " + path );
        }

    }

    @Override
    public void deleteProject( String namespace, String projectId ) throws ContentNotFoundException, ContentAccessException
    {
        this.deleteProject( new ProjectReference().groupId( namespace ).artifactId( projectId ) );
    }

    @Override
    public void deleteArtifact( ArtifactReference ref ) throws ContentNotFoundException, ContentAccessException
    {
        final String path = toPath( ref );
        final Path repoDir = getRepoDir( );
        Path deleteTarget = repoDir.resolve( path );
        if ( Files.exists(deleteTarget) )
        {
            try
            {
                if (Files.isDirectory( deleteTarget ))
                {
                    org.apache.archiva.common.utils.FileUtils.deleteDirectory( deleteTarget );
                } else {
                    Files.delete( deleteTarget );
                }
            }
            catch ( IOException e )
            {
                log.error( "Could not delete file path {}: {}", deleteTarget, e.getMessage( ), e );
                throw new ContentAccessException( "Error while trying to delete path "+path+" from repository "+getId()+": "+e.getMessage( ), e );
            }
        } else {
            log.warn( "Artifact path for repository {} does not exist: {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Artifact not found for repository "+getId()+": "+path );
        }

    }

    @Override
    public void deleteGroupId( String groupId )
        throws ContentNotFoundException, ContentAccessException
    {
        final String path = toPath( groupId );
        final Path deleteTarget = getRepoDir( ).resolve( path );
        if (!Files.exists(deleteTarget)) {
            log.warn( "Namespace path for repository {} does not exist: {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Namespace not found for repository "+getId()+": "+path );
        }
        if ( Files.isDirectory(deleteTarget) )
        {
            try
            {
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( deleteTarget );
            }
            catch ( IOException e )
            {
                log.error( "Could not delete file path {}: {}", deleteTarget, e.getMessage( ), e );
                throw new ContentAccessException( "Error while trying to delete path "+path+" from repository "+getId()+": "+e.getMessage( ), e );
            }
        } else {
            log.warn( "Namespace path for repository {} is not a directory {}", getId(), deleteTarget );
            throw new ContentNotFoundException( "Namespace path for repository "+getId()+" is not directory: " + path );

        }
    }

    @Override
    public String getId()
    {
        return repository.getId();
    }

    @Override
    public List<ArtifactReference> getRelatedArtifacts( VersionedReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        StorageAsset artifactDir = toFile( reference );
        if ( !artifactDir.exists())
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-existant directory: " + artifactDir.getPath() );
        }

        if ( !artifactDir.isContainer() )
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-directory: " + artifactDir.getPath() );
        }

        // First gather up the versions found as artifacts in the managed repository.

        try (Stream<StorageAsset> stream = artifactDir.list().stream() ) {
            return stream.filter(asset -> !asset.isContainer()).map(path -> {
                try {
                    ArtifactReference artifact = toArtifactReference(path.getPath());
                    if( artifact.getGroupId().equals( reference.getGroupId() ) && artifact.getArtifactId().equals(
                            reference.getArtifactId() ) && artifact.getVersion().equals( reference.getVersion() )) {
                        return artifact;
                    } else {
                        return null;
                    }
                } catch (LayoutException e) {
                    log.debug( "Not processing file that is not an artifact: {}", e.getMessage() );
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause( );
            if (cause!=null) {
                if (cause instanceof LayoutException) {
                    throw (LayoutException)cause;
                } else
                {
                    throw new ContentAccessException( cause.getMessage( ), cause );
                }
            } else {
                throw new ContentAccessException( e.getMessage( ), e );
            }
        }
    }

    /*
     * Create the filter for various combinations of classifier and type
     */
    private Predicate<ArtifactReference> getChecker(ArtifactReference referenceObject, String extension) {
        // TODO: Check, if extension is the correct parameter here
        // We compare type with extension which works for artifacts like .jar.md5 but may
        // be not the best way.

        if (referenceObject.getClassifier()!=null && referenceObject.getType()!=null) {
            return ((ArtifactReference a) ->
                referenceObject.getGroupId().equals( a.getGroupId() )
                && referenceObject.getArtifactId().equals( a.getArtifactId() )
                && referenceObject.getVersion( ).equals( a.getVersion( ) )
                && ( (a.getType()==null)
                    || referenceObject.getType().equals( a.getType() )
                    || a.getType().startsWith(extension) )
                && referenceObject.getClassifier().equals( a.getClassifier() )
            );
        } else if (referenceObject.getClassifier()!=null && referenceObject.getType()==null){
            return ((ArtifactReference a) ->
                referenceObject.getGroupId().equals( a.getGroupId() )
                    && referenceObject.getArtifactId().equals( a.getArtifactId() )
                    && referenceObject.getVersion( ).equals( a.getVersion( ) )
                    && referenceObject.getClassifier().equals( a.getClassifier() )
            );
        } else if (referenceObject.getClassifier()==null && referenceObject.getType()!=null){
            return ((ArtifactReference a) ->
                referenceObject.getGroupId().equals( a.getGroupId() )
                    && referenceObject.getArtifactId().equals( a.getArtifactId() )
                    && referenceObject.getVersion( ).equals( a.getVersion( ) )
                    && ( (a.getType()==null)
                    || referenceObject.getType().equals( a.getType() )
                    || a.getType().startsWith(extension) )
            );
        } else {
            return ((ArtifactReference a) ->
                referenceObject.getGroupId().equals( a.getGroupId() )
                    && referenceObject.getArtifactId().equals( a.getArtifactId() )
                    && referenceObject.getVersion( ).equals( a.getVersion( ) )
            );
        }


    }

    @Override
    public List<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        if ( StringUtils.isEmpty( reference.getType() ) && StringUtils.isEmpty( reference.getClassifier() ) ) {
            return getRelatedArtifacts( toVersion( reference ) );
        }

        StorageAsset artifactFile = toFile( reference );
        StorageAsset repoDir = artifactFile.getParent();
        String ext;
        if (!artifactFile.isContainer()) {
            ext = StringUtils.substringAfterLast( artifactFile.getName(), ".");
        } else {
            ext = "";
        }

        if ( !repoDir.exists())
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-existant directory: " + repoDir.getPath() );
        }

        if ( !repoDir.isContainer() )
        {
            throw new ContentNotFoundException(
                "Unable to get related artifacts using a non-directory: " + repoDir.getPath() );
        }

        // First gather up the versions found as artifacts in the managed repository.

        try (Stream<StorageAsset> stream = repoDir.list().stream() ) {
            return stream.filter(
                asset -> !asset.isContainer())
                .map(path -> {
                try {
                    return toArtifactReference(path.getPath());
                } catch (LayoutException e) {
                    log.debug( "Not processing file that is not an artifact: {}", e.getMessage() );
                    return null;
                }
            }).filter(Objects::nonNull).filter(getChecker( reference, ext )).collect(Collectors.toList());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause( );
            if (cause!=null) {
                if (cause instanceof LayoutException) {
                    throw (LayoutException)cause;
                } else
                {
                    throw new ContentAccessException( cause.getMessage( ), cause );
                }
            } else {
                throw new ContentAccessException( e.getMessage( ), e );
            }
        }
    }

    @Override
    public List<StorageAsset> getRelatedAssets( ArtifactReference reference ) throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        return null;
    }

    @Override
    public String getRepoRoot()
    {
        return convertUriToPath( repository.getLocation() );
    }

    private String convertUriToPath( URI uri ) {
        if (uri.getScheme()==null) {
            return Paths.get(uri.getPath()).toString();
        } else if ("file".equals(uri.getScheme())) {
            return Paths.get(uri).toString();
        } else {
            return uri.toString();
        }
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
     * @throws LayoutException
     */
    @Override
    public Set<String> getVersions( ProjectReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        final String path = toPath( reference );
        final Path projDir = getRepoDir().resolve(toPath(reference));
        if ( !Files.exists(projDir) )
        {
            throw new ContentNotFoundException(
                "Unable to get Versions on a non-existant directory for repository "+getId()+": " + path );
        }

        if ( !Files.isDirectory(projDir) )
        {
            throw new ContentNotFoundException(
                "Unable to get Versions on a non-directory for repository "+getId()+": " + path );
        }

        final String groupId = reference.getGroupId();
        final String artifactId = reference.getArtifactId();
        try(Stream<Path> stream = Files.list(projDir)) {
            return stream.filter(Files::isDirectory).map(
                    p -> toVersion(groupId, artifactId, p.getFileName().toString())
            ).filter(this::hasArtifact).map(ref -> ref.getVersion())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Could not read directory {}: {}", projDir, e.getMessage(), e);
            throw new ContentAccessException( "Could not read path for repository "+getId()+": "+ path, e );
        } catch (RuntimeException e) {
            Throwable cause = e.getCause( );
            if (cause!=null)
            {
                if ( cause instanceof LayoutException )
                {
                    throw (LayoutException) cause;
                } else {
                    log.error("Could not read directory {}: {}", projDir, cause.getMessage(), cause);
                    throw new ContentAccessException( "Could not read path for repository "+getId()+": "+ path, cause );
                }
            } else {
                log.error("Could not read directory {}: {}", projDir, e.getMessage(), e);
                throw new ContentAccessException( "Could not read path for repository "+getId()+": "+ path, cause );
            }
        }
    }

    @Override
    public Set<String> getVersions( VersionedReference reference )
        throws ContentNotFoundException, ContentAccessException, LayoutException
    {
        try(Stream<ArtifactReference> stream = getArtifactStream( reference ))
        {
            return stream.filter( Objects::nonNull )
                .map( ar -> ar.getVersion( ) )
                .collect( Collectors.toSet( ) );
        } catch (IOException e) {
            final String path = toPath( reference );
            log.error("Could not read directory from repository {} - {}: ", getId(), path, e.getMessage(), e);
            throw new ContentAccessException( "Could not read path for repository "+getId()+": "+ path, e );
        }
    }

    @Override
    public boolean hasContent( ArtifactReference reference ) throws ContentAccessException
    {
        StorageAsset artifactFile = toFile( reference );
        return artifactFile.exists() && !artifactFile.isContainer();
    }

    @Override
    public boolean hasContent( ProjectReference reference ) throws ContentAccessException
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
    public boolean hasContent( VersionedReference reference ) throws ContentAccessException
    {
        try
        {
            return ( getFirstArtifact( reference ) != null );
        }
        catch ( LayoutException | ContentNotFoundException e )
        {
            return false;
        }
        catch ( IOException e )
        {
            String path = toPath( reference );
            log.error("Could not read directory from repository {} - {}: ", getId(), path, e.getMessage(), e);
            throw new ContentAccessException( "Could not read path from repository " + getId( ) + ": " + path, e );
        }
    }

    @Override
    public void setRepository( final ManagedRepository repo )
    {
        this.repository = repo;
        if (repo!=null) {
            if (repository instanceof EditableManagedRepository) {
                ((EditableManagedRepository) repository).setContent(this);
            }
        }
    }

    private Path getRepoDir() {
        return repository.getAsset( "" ).getFilePath( );
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
        String repoPath = convertUriToPath( repository.getLocation() );
        if ( ( path != null ) && path.startsWith( repoPath ) && repoPath.length() > 0 )
        {
            return super.toArtifactReference( path.substring( repoPath.length() + 1 ) );
        } else {
            repoPath = path;
            if (repoPath!=null) {
                while (repoPath.startsWith("/")) {
                    repoPath = repoPath.substring(1);
                }
            }
            return super.toArtifactReference( repoPath );
        }
    }


    // The variant with runtime exception for stream usage
    private ArtifactReference toArtifactRef(String path) {
        try {
            return toArtifactReference(path);
        } catch (LayoutException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public StorageAsset toFile( ArtifactReference reference )
    {
        return repository.getAsset(toPath(reference));
    }

    @Override
    public StorageAsset toFile( ArchivaArtifact reference )
    {
        return repository.getAsset( toPath( reference ) );
    }

    @Override
    public StorageAsset toFile( VersionedReference reference )
    {
        return repository.getAsset( toPath( reference ) );
    }

    /**
     * Get the first Artifact found in the provided VersionedReference location.
     *
     * @param reference the reference to the versioned reference to search within
     * @return the ArtifactReference to the first artifact located within the versioned reference. or null if
     *         no artifact was found within the versioned reference.
     * @throws java.io.IOException     if the versioned reference is invalid (example: doesn't exist, or isn't a directory)
     * @throws LayoutException
     */
    private ArtifactReference getFirstArtifact( VersionedReference reference )
        throws ContentNotFoundException, LayoutException, IOException
    {
        try(Stream<ArtifactReference> stream = getArtifactStream( reference ))
        {
            return stream.findFirst( ).orElse( null );
        } catch (RuntimeException e) {
            throw new ContentNotFoundException( e.getMessage( ), e.getCause( ) );
        }
    }

    private Stream<ArtifactReference> getArtifactStream(VersionedReference reference) throws ContentNotFoundException, LayoutException, IOException {
        final Path repoBase = getRepoDir( );
        String path = toMetadataPath( reference );
        Path versionDir = repoBase.resolve( path ).getParent();
        if ( !Files.exists(versionDir) )
        {
            throw new ContentNotFoundException( "Unable to gather the list of artifacts on a non-existant directory: "
                + versionDir.toAbsolutePath() );
        }

        if ( !Files.isDirectory(versionDir) )
        {
            throw new ContentNotFoundException(
                "Unable to gather the list of snapshot versions on a non-directory: " + versionDir.toAbsolutePath() );
        }
        return Files.list(versionDir).filter(Files::isRegularFile)
                .map(p -> repoBase.relativize(p).toString())
                .filter(p -> !filetypes.matchesDefaultExclusions(p))
                .filter(filetypes::matchesArtifactPattern)
                .map(this::toArtifactRef);
    }

    public List<ArtifactReference> getArtifacts(VersionedReference reference) throws ContentNotFoundException, LayoutException, ContentAccessException
    {
        try (Stream<ArtifactReference> stream = getArtifactStream( reference ))
        {
            return stream.collect( Collectors.toList( ) );
        } catch ( IOException e )
        {
            String path = toPath( reference );
            log.error("Could not read directory from repository {} - {}: ", getId(), path, e.getMessage(), e);
            throw new ContentAccessException( "Could not read path from repository " + getId( ) + ": " + path, e );

        }
    }

    private boolean hasArtifact( VersionedReference reference )

    {
        try(Stream<ArtifactReference> stream = getArtifactStream( reference ))
        {
            return stream.anyMatch( e -> true );
        } catch (ContentNotFoundException e) {
            return false;
        } catch ( LayoutException | IOException e) {
            // We throw the runtime exception for better stream handling
            throw new RuntimeException(e);
        }
    }

    public void setFiletypes( FileTypes filetypes )
    {
        this.filetypes = filetypes;
    }


}
