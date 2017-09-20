package org.apache.archiva.repository.metadata;

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
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.model.*;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * MetadataTools
 *
 *
 */
@Service( "metadataTools#default" )
public class MetadataTools
    implements RegistryListener
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    public static final String MAVEN_METADATA = "maven-metadata.xml";

    public static final String MAVEN_ARCHETYPE_CATALOG ="archetype-catalog.xml";

    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

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

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    private List<String> artifactPatterns;

    private Map<String, Set<String>> proxies;

    private static final char NUMS[] = new char[]{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    private SimpleDateFormat lastUpdatedFormat;

    public MetadataTools()
    {
        lastUpdatedFormat = new SimpleDateFormat( "yyyyMMddHHmmss" );
        lastUpdatedFormat.setTimeZone( DateUtils.UTC_TIME_ZONE );
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
                                               VersionedReference reference )
        throws LayoutException, IOException, ContentNotFoundException
    {
        Set<String> foundVersions = managedRepository.getVersions( reference );

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
    public VersionedReference toVersionedReference( String path )
        throws RepositoryMetadataException
    {
        if ( !path.endsWith( "/" + MAVEN_METADATA ) )
        {
            throw new RepositoryMetadataException( "Cannot convert to versioned reference, not a metadata file. " );
        }

        VersionedReference reference = new VersionedReference();

        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        int versionOffset = pathParts.length - 2;
        int artifactIdOffset = versionOffset - 1;
        int groupIdEnd = artifactIdOffset - 1;

        reference.setVersion( pathParts[versionOffset] );

        if ( !hasNumberAnywhere( reference.getVersion() ) )
        {
            // Scary check, but without it, all paths are version references;
            throw new RepositoryMetadataException(
                "Not a versioned reference, as version id on path has no number in it." );
        }

        reference.setArtifactId( pathParts[artifactIdOffset] );

        StringBuilder gid = new StringBuilder();
        for ( int i = 0; i <= groupIdEnd; i++ )
        {
            if ( i > 0 )
            {
                gid.append( "." );
            }
            gid.append( pathParts[i] );
        }

        reference.setGroupId( gid.toString() );

        return reference;
    }

    private boolean hasNumberAnywhere( String version )
    {
        return StringUtils.indexOfAny( version, NUMS ) != ( -1 );
    }

    public ProjectReference toProjectReference( String path )
        throws RepositoryMetadataException
    {
        if ( !path.endsWith( "/" + MAVEN_METADATA ) )
        {
            throw new RepositoryMetadataException( "Cannot convert to versioned reference, not a metadata file. " );
        }

        ProjectReference reference = new ProjectReference();

        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        // Assume last part of the path is the version.

        int artifactIdOffset = pathParts.length - 2;
        int groupIdEnd = artifactIdOffset - 1;

        reference.setArtifactId( pathParts[artifactIdOffset] );

        StringBuilder gid = new StringBuilder();
        for ( int i = 0; i <= groupIdEnd; i++ )
        {
            if ( i > 0 )
            {
                gid.append( "." );
            }
            gid.append( pathParts[i] );
        }

        reference.setGroupId( gid.toString() );

        return reference;
    }

    public String toPath( ProjectReference reference )
    {
        StringBuilder path = new StringBuilder();

        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public String toPath( VersionedReference reference )
    {
        StringBuilder path = new StringBuilder();

        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        if ( reference.getVersion() != null )
        {
            // add the version only if it is present
            path.append( VersionUtil.getBaseVersion( reference.getVersion() ) ).append( PATH_SEPARATOR );
        }
        path.append( MAVEN_METADATA );

        return path.toString();
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
        this.artifactPatterns = new ArrayList<>();
        this.proxies = new HashMap<>();
        initConfigVariables();

        configuration.addChangeListener( this );
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ManagedRepositoryContent managedRepository,
                                                        ProjectReference reference, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, toPath( reference ) );
        Path metadataFile = Paths.get( managedRepository.getRepoRoot(), metadataPath );

        if ( !Files.exists(metadataFile) || !Files.isRegularFile( metadataFile ))
        {
            // Nothing to do. return null.
            return null;
        }

        try
        {
            return MavenMetadataReader.read( metadataFile );
        }
        catch ( XMLException e )
        {
            // TODO: [monitor] consider a monitor for this event.
            // TODO: consider a read-redo on monitor return code?
            log.warn( "Unable to read metadata: {}", metadataFile.toAbsolutePath(), e );
            return null;
        }
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ManagedRepositoryContent managedRepository,
                                                        String logicalResource, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, logicalResource );
        Path metadataFile = Paths.get( managedRepository.getRepoRoot(), metadataPath );

        if ( !Files.exists(metadataFile) || !Files.isRegularFile( metadataFile))
        {
            // Nothing to do. return null.
            return null;
        }

        try
        {
            return MavenMetadataReader.read( metadataFile );
        }
        catch ( XMLException e )
        {
            // TODO: [monitor] consider a monitor for this event.
            // TODO: consider a read-redo on monitor return code?
            log.warn( "Unable to read metadata: {}", metadataFile.toAbsolutePath(), e );
            return null;
        }
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ManagedRepositoryContent managedRepository,
                                                        VersionedReference reference, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, toPath( reference ) );
        Path metadataFile = Paths.get( managedRepository.getRepoRoot(), metadataPath );

        if ( !Files.exists(metadataFile) || !Files.isRegularFile(metadataFile))
        {
            // Nothing to do. return null.
            return null;
        }

        try
        {
            return MavenMetadataReader.read( metadataFile );
        }
        catch ( XMLException e )
        {
            // TODO: [monitor] consider a monitor for this event.
            // TODO: consider a read-redo on monitor return code?
            log.warn( "Unable to read metadata: {}", metadataFile.toAbsolutePath(), e );
            return null;
        }
    }

    public void updateMetadata( ManagedRepositoryContent managedRepository, String logicalResource )
        throws RepositoryMetadataException
    {
        final Path metadataFile = Paths.get( managedRepository.getRepoRoot(), logicalResource );
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

        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
    }

    /**
     * Skims the parent directory of a metadata in vain hope of finding
     * subdirectories that contain poms.
     *
     * @param metadataParentDirectory
     * @return origional set plus newly found versions
     */
    private Set<String> findPossibleVersions( Set<String> versions, Path metadataParentDirectory )
    {

        Set<String> result = new HashSet<String>( versions );

        try (Stream<Path> stream = Files.list( metadataParentDirectory )) {
            stream.filter( Files::isDirectory ).filter(
                p ->
                {
                    try(Stream<Path> substream = Files.list(p))
                    {
                        return substream.anyMatch( f -> Files.isRegularFile( f ) && f.toString().endsWith( ".pom" ));
                    }
                    catch ( IOException e )
                    {
                        return false;
                    }
                }
            ).forEach(
                p -> result.add(p.getFileName().toString())
            );
        } catch (IOException e) {
            //
        }
        return result;
    }

    private List<ArchivaRepositoryMetadata> getMetadatasForManagedRepository(
        ManagedRepositoryContent managedRepository, String logicalResource )
    {
        List<ArchivaRepositoryMetadata> metadatas = new ArrayList<>();
        Path file = Paths.get( managedRepository.getRepoRoot(), logicalResource );
        if ( Files.exists(file) )
        {
            try
            {
                ArchivaRepositoryMetadata existingMetadata = MavenMetadataReader.read( file );
                if ( existingMetadata != null )
                {
                    metadatas.add( existingMetadata );
                }
            }
            catch ( XMLException e )
            {
                log.debug( "Could not read metadata at {}. Metadata will be removed.", file.toAbsolutePath() );
                FileUtils.deleteQuietly( file );
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
    public void updateMetadata( ManagedRepositoryContent managedRepository, ProjectReference reference )
        throws LayoutException, RepositoryMetadataException, IOException, ContentNotFoundException
    {
        Path metadataFile = Paths.get( managedRepository.getRepoRoot(), toPath( reference ) );

        long lastUpdated = getExistingLastUpdated( metadataFile );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getGroupId() );
        metadata.setArtifactId( reference.getArtifactId() );

        // Gather up all versions found in the managed repository.
        Set<String> allVersions = managedRepository.getVersions( reference );

        // Gather up all plugins found in the managed repository.
        // TODO: do we know this information instead?
//        Set<Plugin> allPlugins = managedRepository.getPlugins( reference );
        Set<Plugin> allPlugins;
        if ( Files.exists(metadataFile))
        {
            try
            {
                allPlugins = new LinkedHashSet<Plugin>( MavenMetadataReader.read( metadataFile ).getPlugins() );
            }
            catch ( XMLException e )
            {
                throw new RepositoryMetadataException( e.getMessage(), e );
            }
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
        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
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
        Calendar cal = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
        cal.setTimeInMillis( lastUpdated );

        return cal.getTime();
    }

    private long toLastUpdatedLong( String timestampString )
    {
        try
        {
            Date date = lastUpdatedFormat.parse( timestampString );
            Calendar cal = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
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

    private long getExistingLastUpdated( Path metadataFile )
    {
        if ( !Files.exists(metadataFile) )
        {
            // Doesn't exist.
            return 0;
        }

        try
        {
            ArchivaRepositoryMetadata metadata = MavenMetadataReader.read( metadataFile );

            return getLastUpdated( metadata );
        }
        catch ( XMLException e )
        {
            // Error.
            return 0;
        }
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
    public void updateMetadata( ManagedRepositoryContent managedRepository, VersionedReference reference )
        throws LayoutException, RepositoryMetadataException, IOException, ContentNotFoundException
    {
        Path metadataFile = Paths.get( managedRepository.getRepoRoot(), toPath( reference ) );

        long lastUpdated = getExistingLastUpdated( metadataFile );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getGroupId() );
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
                    "No snapshot versions found on reference [" + VersionedReference.toKey( reference ) + "]." );
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
        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
    }

    private void initConfigVariables()
    {
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

    /**
     * Get the first Artifact found in the provided VersionedReference location.
     *
     * @param managedRepository the repository to search within.
     * @param reference         the reference to the versioned reference to search within
     * @return the ArtifactReference to the first artifact located within the versioned reference. or null if
     *         no artifact was found within the versioned reference.
     * @throws IOException     if the versioned reference is invalid (example: doesn't exist, or isn't a directory)
     * @throws LayoutException
     */
    public ArtifactReference getFirstArtifact( ManagedRepositoryContent managedRepository,
                                               VersionedReference reference )
        throws LayoutException, IOException
    {
        String path = toPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        Path repoDir = Paths.get( managedRepository.getRepoRoot(), path );

        if ( !Files.exists(repoDir))
        {
            throw new IOException( "Unable to gather the list of snapshot versions on a non-existant directory: "
                                       + repoDir.toAbsolutePath() );
        }

        if ( !Files.isDirectory( repoDir ))
        {
            throw new IOException(
                "Unable to gather the list of snapshot versions on a non-directory: " + repoDir.toAbsolutePath() );
        }

        try(Stream<Path> stream = Files.list(repoDir)) {
            String result = stream.filter(  Files::isRegularFile ).map( path1 ->
                PathUtil.getRelative( managedRepository.getRepoRoot(), path1 )
            ).filter( filetypes::matchesArtifactPattern ).findFirst().orElse( null );
            if (result!=null) {
                return managedRepository.toArtifactReference( result );
            }
        }
        // No artifact was found.
        return null;
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
}
