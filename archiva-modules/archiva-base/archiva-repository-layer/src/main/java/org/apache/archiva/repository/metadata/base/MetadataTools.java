package org.apache.archiva.repository.metadata.base;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.repository.content.BaseRepositoryContentLayout;
import org.apache.archiva.repository.content.ContentNotFoundException;
import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// import org.apache.archiva.maven2.metadata.MavenMetadataReader;

/**
 * MetadataTools
 *
 *
 */
@Service( "metadataTools#default" )
public class MetadataTools
    implements RegistryListener, ConfigurationListener
{
    private static final Logger log = LoggerFactory.getLogger( MetadataTools.class );

    public static final String MAVEN_METADATA = "maven-metadata.xml";

    public static final String MAVEN_ARCHETYPE_CATALOG ="archetype-catalog.xml";

    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    @Inject
    private RepositoryRegistry repositoryRegistry;

    /**
     *
     */
    @Inject
    @Named( value = "archivaConfiguration#default" )
    private ArchivaConfiguration configuration;

    /**
     *
     */
    @Inject
    @Named( value = "fileTypes" )
    private FileTypes filetypes;

    private List<ChecksumAlgorithm> algorithms = Arrays.asList(ChecksumAlgorithm.SHA256, ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 );

    private List<String> artifactPatterns;

    private Map<String, Set<String>> proxies;

    private static final char NUMS[] = new char[]{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    private SimpleDateFormat lastUpdatedFormat;

    public MetadataTools()
    {
        lastUpdatedFormat = new SimpleDateFormat( "yyyyMMddHHmmss" );
        lastUpdatedFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isProxyConnector( propertyName ) )
        {
            initConfigVariables();
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do */
    }

    /**
     * Gather the set of snapshot versions found in a particular versioned reference.
     *
     * @return the Set of snapshot artifact versions found.
     * @throws LayoutException
     * @throws ContentNotFoundException
     */
    public Set<String> gatherSnapshotVersions( ManagedRepositoryContent managedRepository,
                                               ItemSelector reference )
        throws LayoutException, IOException, ContentNotFoundException
    {
        Set<String> foundVersions = null;
        try
        {
            ArchivaItemSelector selector = ArchivaItemSelector.builder( )
                .withNamespace( reference.getNamespace() )
                .withProjectId( reference.getArtifactId( ) )
                .withArtifactId( reference.getArtifactId( ) )
                .withVersion( reference.getVersion( ) )
                .build( );
            try(Stream<? extends Artifact> stream = managedRepository.getLayout( BaseRepositoryContentLayout.class ).newArtifactStream( selector )) {
                foundVersions = stream.map( a -> a.getArtifactVersion( ) )
                    .filter( StringUtils::isNotEmpty )
                    .collect( Collectors.toSet( ) );
            }
        }
        catch ( ContentAccessException e )
        {
            log.error( "Error while accessing content {}", e.getMessage( ) );
            throw new IOException( "Could not access repository content: " + e.getMessage( ) );
        }

        // Next gather up the referenced 'latest' versions found in any proxied repositories
        // maven-metadata-${proxyId}.xml files that may be present.

        // Does this repository have a set of remote proxied repositories?
        Set<String> proxiedRepoIds = this.proxies.get( managedRepository.getId() );

        if ( CollectionUtils.isNotEmpty( proxiedRepoIds ) )
        {
            String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
            baseVersion = baseVersion.substring( 0, baseVersion.indexOf( VersionUtil.SNAPSHOT ) - 1 );

            // Add in the proxied repo version ids too.
            Iterator<String> it = proxiedRepoIds.iterator();
            while ( it.hasNext() )
            {
                String proxyId = it.next();

                ArchivaRepositoryMetadata proxyMetadata = readProxyMetadata( managedRepository, reference, proxyId );
                if ( proxyMetadata == null )
                {
                    // There is no proxy metadata, skip it.
                    continue;
                }

                // Is there some snapshot info?
                SnapshotVersion snapshot = proxyMetadata.getSnapshotVersion();
                if ( snapshot != null )
                {
                    String timestamp = snapshot.getTimestamp();
                    int buildNumber = snapshot.getBuildNumber();

                    // Only interested in the timestamp + buildnumber.
                    if ( StringUtils.isNotBlank( timestamp ) && ( buildNumber > 0 ) )
                    {
                        foundVersions.add( baseVersion + "-" + timestamp + "-" + buildNumber );
                    }
                }
            }
        }

        return foundVersions;
    }


    /**
     * Take a path to a maven-metadata.xml, and attempt to translate it to a VersionedReference.
     *
     * @param path
     * @return
     */
    public ItemSelector toVersionedSelector( String path )
        throws RepositoryMetadataException
    {
        if ( !path.endsWith( "/" + MAVEN_METADATA ) )
        {
            throw new RepositoryMetadataException( "Cannot convert to versioned reference, not a metadata file. " );
        }

        ArchivaItemSelector.Builder builder = ArchivaItemSelector.builder( );

        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        int versionOffset = pathParts.length - 2;
        int artifactIdOffset = versionOffset - 1;
        int groupIdEnd = artifactIdOffset - 1;

        builder.withVersion(  pathParts[versionOffset] );

        if ( !hasNumberAnywhere( pathParts[versionOffset] ) )
        {
            // Scary check, but without it, all paths are version references;
            throw new RepositoryMetadataException(
                "Not a versioned reference, as version id on path has no number in it." );
        }

        builder.withArtifactId( pathParts[artifactIdOffset] );
        builder.withProjectId( pathParts[artifactIdOffset] );

        StringBuilder gid = new StringBuilder();
        for ( int i = 0; i <= groupIdEnd; i++ )
        {
            if ( i > 0 )
            {
                gid.append( "." );
            }
            gid.append( pathParts[i] );
        }

        builder.withNamespace( gid.toString( ) );

        return builder.build();
    }

    private boolean hasNumberAnywhere( String version )
    {
        return StringUtils.indexOfAny( version, NUMS ) != ( -1 );
    }

    public ItemSelector toProjectSelector( String path )
        throws RepositoryMetadataException
    {
        if ( !path.endsWith( "/" + MAVEN_METADATA ) )
        {
            throw new RepositoryMetadataException( "Cannot convert to versioned reference, not a metadata file. " );
        }

        ArchivaItemSelector.Builder builder = ArchivaItemSelector.builder( );

        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        // Assume last part of the path is the version.

        int artifactIdOffset = pathParts.length - 2;
        int groupIdEnd = artifactIdOffset - 1;

        builder.withArtifactId( pathParts[artifactIdOffset] );
        builder.withProjectId( pathParts[artifactIdOffset] );

        StringBuilder gid = new StringBuilder();
        for ( int i = 0; i <= groupIdEnd; i++ )
        {
            if ( i > 0 )
            {
                gid.append( "." );
            }
            gid.append( pathParts[i] );
        }

        builder.withNamespace( gid.toString( ) );
        return builder.build();
    }

    public String toPath( ContentItem reference )
    {

        return reference.getAsset().resolve( MAVEN_METADATA ).getPath();

    }

    public String toPath( ItemSelector reference )
    {
        StringBuilder path = new StringBuilder();

        path.append( formatAsDirectory( reference.getNamespace() ) ).append( PATH_SEPARATOR );
        path.append( reference.getProjectId() ).append( PATH_SEPARATOR );
        if (reference.hasVersion()) {
            path.append( reference.getVersion( ) ).append( PATH_SEPARATOR );
        }
        path.append( MAVEN_METADATA );
        return path.toString( );
    }


    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    /**
     * Adjusts a path for a metadata.xml file to its repository specific path.
     *
     * @param repository the repository to base new path off of.
     * @param path       the path to the metadata.xml file to adjust the name of.
     * @return the newly adjusted path reference to the repository specific metadata path.
     */
    public String getRepositorySpecificName( RemoteRepositoryContent repository, String path )
    {
        return getRepositorySpecificName( repository.getId(), path );
    }

    /**
     * Adjusts a path for a metadata.xml file to its repository specific path.
     *
     * @param proxyId the repository id to base new path off of.
     * @param path    the path to the metadata.xml file to adjust the name of.
     * @return the newly adjusted path reference to the repository specific metadata path.
     */
    public String getRepositorySpecificName( String proxyId, String path )
    {
        StringBuilder ret = new StringBuilder();

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            ret.append( path.substring( 0, idx + 1 ) );
        }

        // TODO: need to filter out 'bad' characters from the proxy id.
        ret.append( "maven-metadata-" ).append( proxyId ).append( ".xml" );

        return ret.toString();
    }

    @PostConstruct
    public void initialize()
    {
        assert(configuration != null);
        this.artifactPatterns = new ArrayList<>();
        this.proxies = new HashMap<>();
        initConfigVariables();

        configuration.addChangeListener( this );
        configuration.addListener( this );
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ManagedRepositoryContent managedRepository,
                                                        ItemSelector reference, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, toPath( reference ) );
        StorageAsset metadataFile = managedRepository.getRepository().getAsset( metadataPath );

        return readMetadataFile( managedRepository, metadataFile );
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ManagedRepositoryContent managedRepository,
                                                        String logicalResource, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, logicalResource );
        StorageAsset metadataFile = managedRepository.getRepository().getAsset( metadataPath );
        return readMetadataFile( managedRepository, metadataFile );
    }

    public void updateMetadata( ManagedRepositoryContent managedRepository, String logicalResource )
        throws RepositoryMetadataException
    {
        final StorageAsset metadataFile = managedRepository.getRepository().getAsset( logicalResource );
        ArchivaRepositoryMetadata metadata = null;

        //Gather and merge all metadata available
        List<ArchivaRepositoryMetadata> metadatas =
            getMetadatasForManagedRepository( managedRepository, logicalResource );
        for ( ArchivaRepositoryMetadata proxiedMetadata : metadatas )
        {
            if ( metadata == null )
            {
                metadata = proxiedMetadata;
                continue;
            }
            metadata = RepositoryMetadataMerge.merge( metadata, proxiedMetadata );
        }

        if ( metadata == null )
        {
            log.debug( "No metadata to update for {}", logicalResource );
            return;
        }

        Set<String> availableVersions = new HashSet<String>();
        List<String> metadataAvailableVersions = metadata.getAvailableVersions();
        if ( metadataAvailableVersions != null )
        {
            availableVersions.addAll( metadataAvailableVersions );
        }
        availableVersions = findPossibleVersions( availableVersions, metadataFile.getParent() );

        if ( availableVersions.size() > 0 )
        {
            updateMetadataVersions( availableVersions, metadata );
        }

        RepositoryMetadataWriter.write( metadata, metadataFile );

        ChecksummedFile checksum = new ChecksummedFile( metadataFile.getFilePath() );
        checksum.fixChecksums( algorithms );
    }

    /**
     * Skims the parent directory of a metadata in vain hope of finding
     * subdirectories that contain poms.
     *
     * @param metadataParentDirectory
     * @return origional set plus newly found versions
     */
    private Set<String> findPossibleVersions( Set<String> versions, StorageAsset metadataParentDirectory )
    {

        Set<String> result = new HashSet<String>( versions );

        metadataParentDirectory.list().stream().filter(asset ->
                asset.isContainer()).filter(asset -> {
                    return asset.list().stream().anyMatch(f -> !f.isContainer() && f.getName().endsWith(".pom"));
                }
                ).forEach( p -> result.add(p.getName()));

        return result;
    }

    private List<ArchivaRepositoryMetadata> getMetadatasForManagedRepository(
        ManagedRepositoryContent managedRepository, String logicalResource )
    {
        List<ArchivaRepositoryMetadata> metadatas = new ArrayList<>();
        StorageAsset file = managedRepository.getRepository().getAsset( logicalResource );

        if ( file.exists() )
        {
            ArchivaRepositoryMetadata existingMetadata = readMetadataFile( managedRepository, file );
            if ( existingMetadata != null )
            {
                metadatas.add( existingMetadata );
            }
        }

        Set<String> proxyIds = proxies.get( managedRepository.getId() );
        if ( proxyIds != null )
        {
            for ( String proxyId : proxyIds )
            {
                ArchivaRepositoryMetadata proxyMetadata =
                    readProxyMetadata( managedRepository, logicalResource, proxyId );
                if ( proxyMetadata != null )
                {
                    metadatas.add( proxyMetadata );
                }
            }
        }

        return metadatas;
    }


    /**
     * Update the metadata to represent the all versions/plugins of
     * the provided groupId:artifactId project or group reference,
     * based off of information present in the repository,
     * the maven-metadata.xml files, and the proxy/repository specific
     * metadata file contents.
     * <p>
     * We must treat this as a group or a project metadata file as there is no way to know in advance
     *
     * @param managedRepository the managed repository where the metadata is kept.
     * @param reference         the reference to update.
     * @throws LayoutException
     * @throws RepositoryMetadataException
     * @throws IOException
     * @throws ContentNotFoundException
     * @deprecated
     */
    public void updateProjectMetadata( ManagedRepositoryContent managedRepository, ItemSelector reference )
        throws LayoutException, RepositoryMetadataException, IOException, ContentNotFoundException
    {

        StorageAsset metadataFile = managedRepository.getRepository().getAsset( toPath( reference ) );
        ArchivaRepositoryMetadata existingMetadata = readMetadataFile( managedRepository, metadataFile );
        BaseRepositoryContentLayout layout = managedRepository.getLayout( BaseRepositoryContentLayout.class );

        long lastUpdated = getExistingLastUpdated( existingMetadata );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getNamespace() );
        metadata.setArtifactId( reference.getProjectId() );

        // Gather up all versions found in the managed repository.
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( reference.getNamespace() )
            .withProjectId( reference.getProjectId( ) )
            .build();
        Set<String> allVersions = null;
        try
        {
            Project project = layout.getProject( selector );
            allVersions = layout.getVersions( project ).stream()
            .map( v -> v.getId() ).collect( Collectors.toSet());
        }
        catch ( ContentAccessException e )
        {
            log.error( "Error while accessing repository: {}", e.getMessage( ), e );
            throw new RepositoryMetadataException( "Error while accessing repository " + e.getMessage( ), e );
        }

        // Gather up all plugins found in the managed repository.
        // TODO: do we know this information instead?
//        Set<Plugin> allPlugins = managedRepository.getPlugins( reference );
        Set<Plugin> allPlugins;
        if ( existingMetadata!=null)
        {
            allPlugins = new LinkedHashSet<Plugin>( existingMetadata.getPlugins() );
        }
        else
        {
            allPlugins = new LinkedHashSet<Plugin>();
        }

        // Does this repository have a set of remote proxied repositories?
        Set<String> proxiedRepoIds = this.proxies.get( managedRepository.getId() );

        if ( CollectionUtils.isNotEmpty( proxiedRepoIds ) )
        {
            // Add in the proxied repo version ids too.
            Iterator<String> it = proxiedRepoIds.iterator();
            while ( it.hasNext() )
            {
                String proxyId = it.next();

                ArchivaRepositoryMetadata proxyMetadata = readProxyMetadata( managedRepository, reference, proxyId );
                if ( proxyMetadata != null )
                {
                    allVersions.addAll( proxyMetadata.getAvailableVersions() );
                    allPlugins.addAll( proxyMetadata.getPlugins() );
                    long proxyLastUpdated = getLastUpdated( proxyMetadata );

                    lastUpdated = Math.max( lastUpdated, proxyLastUpdated );
                }
            }
        }

        if ( !allVersions.isEmpty() )
        {
            updateMetadataVersions( allVersions, metadata );
        }
        else
        {
            // Add the plugins to the metadata model.
            metadata.setPlugins( new ArrayList<>( allPlugins ) );

            // artifact ID was actually the last part of the group
            metadata.setGroupId( metadata.getGroupId() + "." + metadata.getArtifactId() );
            metadata.setArtifactId( null );
        }

        if ( lastUpdated > 0 )
        {
            metadata.setLastUpdatedTimestamp( toLastUpdatedDate( lastUpdated ) );
        }

        // Save the metadata model to disk.
        RepositoryMetadataWriter.write( metadata, metadataFile );
        ChecksummedFile checksum = new ChecksummedFile( metadataFile.getFilePath() );
        checksum.fixChecksums( algorithms );
    }

    public MetadataReader getMetadataReader( ManagedRepositoryContent managedRepository )
    {
        if (managedRepository!=null)
        {
            return repositoryRegistry.getMetadataReader( managedRepository.getRepository( ).getType( ) );
        } else {
            return repositoryRegistry.getMetadataReader( RepositoryType.MAVEN );
        }
    }

    private void updateMetadataVersions( Collection<String> allVersions, ArchivaRepositoryMetadata metadata )
    {
        // Sort the versions
        List<String> sortedVersions = new ArrayList<>( allVersions );
        Collections.sort( sortedVersions, VersionComparator.getInstance() );

        // Split the versions into released and snapshots.
        List<String> releasedVersions = new ArrayList<>();
        List<String> snapshotVersions = new ArrayList<>();

        for ( String version : sortedVersions )
        {
            if ( VersionUtil.isSnapshot( version ) )
            {
                snapshotVersions.add( version );
            }
            else
            {
                releasedVersions.add( version );
            }
        }

        Collections.sort( releasedVersions, VersionComparator.getInstance() );
        Collections.sort( snapshotVersions, VersionComparator.getInstance() );

        String latestVersion = sortedVersions.get( sortedVersions.size() - 1 );
        String releaseVersion = null;

        if ( CollectionUtils.isNotEmpty( releasedVersions ) )
        {
            releaseVersion = releasedVersions.get( releasedVersions.size() - 1 );
        }

        // Add the versions to the metadata model.
        metadata.setAvailableVersions( sortedVersions );

        metadata.setLatestVersion( latestVersion );
        metadata.setReleasedVersion( releaseVersion );
    }

    private Date toLastUpdatedDate( long lastUpdated )
    {
        Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("UTC") );
        cal.setTimeInMillis( lastUpdated );

        return cal.getTime();
    }

    private long toLastUpdatedLong( String timestampString )
    {
        try
        {
            Date date = lastUpdatedFormat.parse( timestampString );
            Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("UTC"));
            cal.setTime( date );

            return cal.getTimeInMillis();
        }
        catch ( ParseException e )
        {
            return 0;
        }
    }

    private long getLastUpdated( ArchivaRepositoryMetadata metadata )
    {
        if ( metadata == null )
        {
            // Doesn't exist.
            return 0;
        }

        try
        {
            String lastUpdated = metadata.getLastUpdated();
            if ( StringUtils.isBlank( lastUpdated ) )
            {
                // Not set.
                return 0;
            }

            Date lastUpdatedDate = lastUpdatedFormat.parse( lastUpdated );
            return lastUpdatedDate.getTime();
        }
        catch ( ParseException e )
        {
            // Bad format on the last updated string.
            return 0;
        }
    }

    ArchivaRepositoryMetadata readMetadataFile( ManagedRepositoryContent repository, StorageAsset asset) {
        MetadataReader reader = getMetadataReader( repository );
        try
        {
            if (asset.exists() && !asset.isContainer())
            {
                return reader.read( asset );
            } else {
                log.error( "Trying to read metadata from container: {}", asset.getPath( ) );
                return null;
            }
        }
        catch ( RepositoryMetadataException e )
        {
            log.error( "Could not read metadata file {}", asset, e );
            return null;
        }
    }

    private long getExistingLastUpdated( ArchivaRepositoryMetadata metadata )
    {
        if ( metadata==null )
        {
            // Doesn't exist.
            return 0;
        }

        return getLastUpdated( metadata );
    }

    /**
     * Update the metadata based on the following rules.
     * <p>
     * 1) If this is a SNAPSHOT reference, then utilize the proxy/repository specific
     * metadata files to represent the current / latest SNAPSHOT available.
     * 2) If this is a RELEASE reference, and the metadata file does not exist, then
     * create the metadata file with contents required of the VersionedReference
     *
     * @param managedRepository the managed repository where the metadata is kept.
     * @param reference         the versioned reference to update
     * @throws LayoutException
     * @throws RepositoryMetadataException
     * @throws IOException
     * @throws ContentNotFoundException
     * @deprecated
     */
    public void updateVersionMetadata( ManagedRepositoryContent managedRepository, ItemSelector reference )
        throws LayoutException, RepositoryMetadataException, IOException, ContentNotFoundException
    {
        StorageAsset metadataFile = managedRepository.getRepository().getAsset( toPath( reference ) );
        ArchivaRepositoryMetadata existingMetadata = readMetadataFile(managedRepository, metadataFile );

        long lastUpdated = getExistingLastUpdated( existingMetadata );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getNamespace() );
        metadata.setArtifactId( reference.getArtifactId() );

        if ( VersionUtil.isSnapshot( reference.getVersion() ) )
        {
            // Do SNAPSHOT handling.
            metadata.setVersion( VersionUtil.getBaseVersion( reference.getVersion() ) );

            // Gather up all of the versions found in the reference dir, and any
            // proxied maven-metadata.xml files.
            Set<String> snapshotVersions = gatherSnapshotVersions( managedRepository, reference );

            if ( snapshotVersions.isEmpty() )
            {
                throw new ContentNotFoundException(
                    "No snapshot versions found on reference [" + reference  + "]." );
            }

            // sort the list to determine to aide in determining the Latest version.
            List<String> sortedVersions = new ArrayList<>();
            sortedVersions.addAll( snapshotVersions );
            Collections.sort( sortedVersions, new VersionComparator() );

            String latestVersion = sortedVersions.get( sortedVersions.size() - 1 );

            if ( VersionUtil.isUniqueSnapshot( latestVersion ) )
            {
                // The latestVersion will contain the full version string "1.0-alpha-5-20070821.213044-8"
                // This needs to be broken down into ${base}-${timestamp}-${build_number}

                Matcher m = VersionUtil.UNIQUE_SNAPSHOT_PATTERN.matcher( latestVersion );
                if ( m.matches() )
                {
                    metadata.setSnapshotVersion( new SnapshotVersion() );
                    int buildNumber = NumberUtils.toInt( m.group( 3 ), -1 );
                    metadata.getSnapshotVersion().setBuildNumber( buildNumber );

                    Matcher mtimestamp = VersionUtil.TIMESTAMP_PATTERN.matcher( m.group( 2 ) );
                    if ( mtimestamp.matches() )
                    {
                        String tsDate = mtimestamp.group( 1 );
                        String tsTime = mtimestamp.group( 2 );

                        long snapshotLastUpdated = toLastUpdatedLong( tsDate + tsTime );

                        lastUpdated = Math.max( lastUpdated, snapshotLastUpdated );

                        metadata.getSnapshotVersion().setTimestamp( m.group( 2 ) );
                    }
                }
            }
            else if ( VersionUtil.isGenericSnapshot( latestVersion ) )
            {
                // The latestVersion ends with the generic version string.
                // Example: 1.0-alpha-5-SNAPSHOT

                metadata.setSnapshotVersion( new SnapshotVersion() );

                /* Disabled due to decision in [MRM-535].
                 * Do not set metadata.lastUpdated to file.lastModified.
                 *
                 * Should this be the last updated timestamp of the file, or in the case of an
                 * archive, the most recent timestamp in the archive?
                 *
                ArtifactReference artifact = getFirstArtifact( managedRepository, reference );

                if ( artifact == null )
                {
                    throw new IOException( "Not snapshot artifact found to reference in " + reference );
                }

                File artifactFile = managedRepository.toFile( artifact );

                if ( artifactFile.exists() )
                {
                    Date lastModified = new Date( artifactFile.lastModified() );
                    metadata.setLastUpdatedTimestamp( lastModified );
                }
                */
            }
            else
            {
                throw new RepositoryMetadataException(
                    "Unable to process snapshot version <" + latestVersion + "> reference <" + reference + ">" );
            }
        }
        else
        {
            // Do RELEASE handling.
            metadata.setVersion( reference.getVersion() );
        }

        // Set last updated
        if ( lastUpdated > 0 )
        {
            metadata.setLastUpdatedTimestamp( toLastUpdatedDate( lastUpdated ) );
        }

        // Save the metadata model to disk.
        RepositoryMetadataWriter.write( metadata, metadataFile );
        ChecksummedFile checksum = new ChecksummedFile( metadataFile.getFilePath() );
        checksum.fixChecksums( algorithms );
    }

    private void initConfigVariables()
    {
        assert(this.artifactPatterns!=null);
        assert(proxies!=null);
        synchronized ( this.artifactPatterns )
        {
            this.artifactPatterns.clear();

            this.artifactPatterns.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
        }

        synchronized ( proxies )
        {
            this.proxies.clear();

            List<ProxyConnectorConfiguration> proxyConfigs = configuration.getConfiguration().getProxyConnectors();
            for ( ProxyConnectorConfiguration proxyConfig : proxyConfigs )
            {
                String key = proxyConfig.getSourceRepoId();

                Set<String> remoteRepoIds = this.proxies.get( key );

                if ( remoteRepoIds == null )
                {
                    remoteRepoIds = new HashSet<String>();
                }

                remoteRepoIds.add( proxyConfig.getTargetRepoId() );

                this.proxies.put( key, remoteRepoIds );
            }
        }
    }

    public ArchivaConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public FileTypes getFiletypes()
    {
        return filetypes;
    }

    public void setFiletypes( FileTypes filetypes )
    {
        this.filetypes = filetypes;
    }

    @Override
    public void configurationEvent( ConfigurationEvent event )
    {
        log.debug( "Configuration event {}", event );
    }


}
