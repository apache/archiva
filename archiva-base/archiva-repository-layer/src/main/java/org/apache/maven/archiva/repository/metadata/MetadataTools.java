package org.apache.maven.archiva.repository.metadata;

/*
 * Copyright 2001-2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.SnapshotVersion;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * MetadataTools
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.metadata.MetadataTools"
 * @todo use the maven-repository-metadata classes instead for merging
 */
public class MetadataTools
    implements RegistryListener, Initializable
{
    private static Logger log = LoggerFactory.getLogger( MetadataTools.class );

    public static final String MAVEN_METADATA = "maven-metadata.xml";

    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    private List<String> artifactPatterns;

    private Map<String, Set<String>> proxies;

    private static final char NUMS[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isProxyConnector( propertyName ) )
        {
            initConfigVariables();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do */
    }

    /**
     * Gather the Available Versions (on disk) for a specific Project Reference, based on filesystem
     * information.
     *
     * @return the Set of available versions, based on the project reference.
     * @throws LayoutException
     */
    public Set<String> gatherAvailableVersions( ArchivaRepository managedRepository, ProjectReference reference )
        throws LayoutException, IOException
    {
        String path = toPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( managedRepository.getUrl().getPath(), path );

        if ( !repoDir.exists() )
        {
            throw new IOException( "Unable to calculate Available Local Versions on a non-existant directory: " +
                repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new IOException(
                "Unable to calculate Available Local Versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<String>();
        VersionedReference versionRef = new VersionedReference();
        versionRef.setGroupId( reference.getGroupId() );
        versionRef.setArtifactId( reference.getArtifactId() );

        File repoFiles[] = repoDir.listFiles();
        for ( int i = 0; i < repoFiles.length; i++ )
        {
            if ( !repoFiles[i].isDirectory() )
            {
                // Skip it. not a directory.
                continue;
            }

            // Test if dir has an artifact, which proves to us that it is a valid version directory.
            String version = repoFiles[i].getName();
            versionRef.setVersion( version );

            if ( hasArtifact( managedRepository, versionRef ) )
            {
                // Found an artifact, must be a valid version.
                foundVersions.add( version );
            }
        }

        return foundVersions;
    }

    private boolean hasArtifact( ArchivaRepository managedRepository, VersionedReference reference )
        throws LayoutException
    {
        try
        {
            return ( getFirstArtifact( managedRepository, reference ) != null );
        }
        catch ( IOException e )
        {
            return false;
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
    public ArtifactReference getFirstArtifact( ArchivaRepository managedRepository, VersionedReference reference )
        throws LayoutException, IOException
    {
        BidirectionalRepositoryLayout layout = layoutFactory.getLayout( managedRepository.getLayoutType() );
        String path = toPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( managedRepository.getUrl().getPath(), path );

        if ( !repoDir.exists() )
        {
            throw new IOException( "Unable to gather the list of snapshot versions on a non-existant directory: " +
                repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new IOException(
                "Unable to gather the list of snapshot versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        File repoFiles[] = repoDir.listFiles();
        for ( int i = 0; i < repoFiles.length; i++ )
        {
            if ( repoFiles[i].isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( managedRepository.getUrl().getPath(), repoFiles[i] );

            if ( matchesArtifactPattern( relativePath ) )
            {
                ArtifactReference artifact = layout.toArtifactReference( relativePath );

                return artifact;
            }
        }

        // No artifact was found.
        return null;
    }

    /**
     * Gather the set of snapshot versions found in a particular versioned reference.
     *
     * @return the Set of snapshot artifact versions found.
     * @throws LayoutException
     */
    public Set<String> gatherSnapshotVersions( ArchivaRepository managedRepository, VersionedReference reference )
        throws LayoutException, IOException
    {
        BidirectionalRepositoryLayout layout = layoutFactory.getLayout( managedRepository.getLayoutType() );
        String path = toPath( reference );

        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            path = path.substring( 0, idx );
        }

        File repoDir = new File( managedRepository.getUrl().getPath(), path );

        if ( !repoDir.exists() )
        {
            throw new IOException( "Unable to gather the list of snapshot versions on a non-existant directory: " +
                repoDir.getAbsolutePath() );
        }

        if ( !repoDir.isDirectory() )
        {
            throw new IOException(
                "Unable to gather the list of snapshot versions on a non-directory: " + repoDir.getAbsolutePath() );
        }

        Set<String> foundVersions = new HashSet<String>();

        // First gather up the versions found as artifacts in the managed repository.
        File repoFiles[] = repoDir.listFiles();
        for ( int i = 0; i < repoFiles.length; i++ )
        {
            if ( repoFiles[i].isDirectory() )
            {
                // Skip it. it's a directory.
                continue;
            }

            String relativePath = PathUtil.getRelative( managedRepository.getUrl().getPath(), repoFiles[i] );

            if ( matchesArtifactPattern( relativePath ) )
            {
                ArtifactReference artifact = layout.toArtifactReference( relativePath );

                if ( VersionUtil.isSnapshot( artifact.getVersion() ) )
                {
                    foundVersions.add( artifact.getVersion() );
                }
            }
        }

        // Next gather up the referenced 'latest' versions found in any proxied repositories
        // maven-metadata-${proxyId}.xml files that may be present.

        // Does this repository have a set of remote proxied repositories?
        Set proxiedRepoIds = this.proxies.get( managedRepository.getId() );

        if ( proxiedRepoIds != null )
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

        StringBuffer gid = new StringBuffer();
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

        StringBuffer gid = new StringBuffer();
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
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public String toPath( VersionedReference reference )
    {
        StringBuffer path = new StringBuffer();

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

    /**
     * Adjusts a path for a metadata.xml file to its repository specific path.
     *
     * @param repository the repository to base new path off of.
     * @param path       the path to the metadata.xml file to adjust the name of.
     * @return the newly adjusted path reference to the repository specific metadata path.
     */
    public String getRepositorySpecificName( ArchivaRepository repository, String path )
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
        StringBuffer ret = new StringBuffer();

        int idx = path.lastIndexOf( "/" );
        if ( idx > 0 )
        {
            ret.append( path.substring( 0, idx + 1 ) );
        }

        // TODO: need to filter out 'bad' characters from the proxy id.
        ret.append( "maven-metadata-" ).append( proxyId ).append( ".xml" );

        return ret.toString();
    }

    public void initialize()
        throws InitializationException
    {
        this.artifactPatterns = new ArrayList<String>();
        this.proxies = new HashMap();
        initConfigVariables();

        configuration.addChangeListener( this );
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ArchivaRepository managedRepository, ProjectReference reference,
                                                        String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, toPath( reference ) );
        File metadataFile = new File( managedRepository.getUrl().getPath(), metadataPath );

        try
        {
            return RepositoryMetadataReader.read( metadataFile );
        }
        catch ( RepositoryMetadataException e )
        {
            // TODO: [monitor] consider a monitor for this event.
            // TODO: consider a read-redo on monitor return code?
            log.warn( "Unable to read metadata: " + metadataFile.getAbsolutePath(), e );
            return null;
        }
    }

    public ArchivaRepositoryMetadata readProxyMetadata( ArchivaRepository managedRepository,
                                                        VersionedReference reference, String proxyId )
    {
        String metadataPath = getRepositorySpecificName( proxyId, toPath( reference ) );
        File metadataFile = new File( managedRepository.getUrl().getPath(), metadataPath );

        try
        {
            return RepositoryMetadataReader.read( metadataFile );
        }
        catch ( RepositoryMetadataException e )
        {
            // TODO: [monitor] consider a monitor for this event.
            // TODO: consider a read-redo on monitor return code?
            log.warn( "Unable to read metadata: " + metadataFile.getAbsolutePath(), e );
            return null;
        }
    }

    /**
     * Update the metadata to represent the all versions of
     * the provided groupId:artifactId project reference,
     * based off of information present in the repository,
     * the maven-metadata.xml files, and the proxy/repository specific
     * metadata file contents.
     *
     * @param managedRepository the managed repository where the metadata is kept.
     * @param reference         the versioned referencfe to update.
     * @throws LayoutException
     * @throws RepositoryMetadataException
     * @throws IOException
     */
    public void updateMetadata( ArchivaRepository managedRepository, ProjectReference reference )
        throws LayoutException, RepositoryMetadataException, IOException
    {
        Comparator<String> comparator = VersionComparator.getInstance();

        File metadataFile = new File( managedRepository.getUrl().getPath(), toPath( reference ) );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getGroupId() );
        metadata.setArtifactId( reference.getArtifactId() );

        // Gather up the versions found in the managed repository.
        Set<String> availableVersions = gatherAvailableVersions( managedRepository, reference );

        // Does this repository have a set of remote proxied repositories?
        Set proxiedRepoIds = this.proxies.get( managedRepository.getId() );

        String latestVersion = null;
        String releaseVersion = null;
        if ( proxiedRepoIds != null )
        {
            // Add in the proxied repo version ids too.
            Iterator<String> it = proxiedRepoIds.iterator();
            while ( it.hasNext() )
            {
                String proxyId = it.next();

                ArchivaRepositoryMetadata proxyMetadata = readProxyMetadata( managedRepository, reference, proxyId );
                if ( proxyMetadata != null )
                {
                    availableVersions.addAll( proxyMetadata.getAvailableVersions() );

                    if ( latestVersion == null ||
                        comparator.compare( proxyMetadata.getLatestVersion(), latestVersion ) > 0 )
                    {
                        latestVersion = proxyMetadata.getLatestVersion();
                    }

                    if ( releaseVersion == null ||
                        comparator.compare( proxyMetadata.getReleasedVersion(), releaseVersion ) > 0 )
                    {
                        releaseVersion = proxyMetadata.getReleasedVersion();
                    }
                }
            }
        }

        if ( availableVersions.size() == 0 )
        {
            throw new IOException( "No versions found for reference." );
        }

        // Sort the versions
        List<String> sortedVersions = new ArrayList<String>();
        sortedVersions.addAll( availableVersions );
        Collections.sort( sortedVersions, new VersionComparator() );

        // Add the versions to the metadata model.
        metadata.setAvailableVersions( sortedVersions );

        metadata.setLatestVersion( latestVersion );
        metadata.setReleasedVersion( releaseVersion );

        // Save the metadata model to disk.
        RepositoryMetadataWriter.write( metadata, metadataFile );
    }

    /**
     * Update the metadata based on the following rules.
     * <p/>
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
     */
    public void updateMetadata( ArchivaRepository managedRepository, VersionedReference reference )
        throws LayoutException, RepositoryMetadataException, IOException
    {
        BidirectionalRepositoryLayout layout = layoutFactory.getLayout( managedRepository.getLayoutType() );
        File metadataFile = new File( managedRepository.getUrl().getPath(), toPath( reference ) );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        metadata.setGroupId( reference.getGroupId() );
        metadata.setArtifactId( reference.getArtifactId() );

        if ( VersionUtil.isSnapshot( reference.getVersion() ) )
        {
            // Do SNAPSHOT handling.
            metadata.setVersion( VersionUtil.getBaseVersion( reference.getVersion() ) );

            // Gather up all of the versions found in the reference dir, and any
            // proxied maven-metadata.xml files.
            Set snapshotVersions = gatherSnapshotVersions( managedRepository, reference );

            if ( snapshotVersions.isEmpty() )
            {
                throw new IOException( "Not snapshot versions found to reference." );
            }

            // sort the list to determine to aide in determining the Latest version.
            List<String> sortedVersions = new ArrayList<String>();
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
                        metadata.setLastUpdated( tsDate + tsTime );
                        metadata.getSnapshotVersion().setTimestamp( m.group( 2 ) );
                    }
                }
            }
            else if ( VersionUtil.isGenericSnapshot( latestVersion ) )
            {
                // The latestVersion ends with the generic version string.
                // Example: 1.0-alpha-5-SNAPSHOT

                metadata.setSnapshotVersion( new SnapshotVersion() );

                /* TODO: Should this be the last updated timestamp of the file, or in the case of an 
                 * archive, the most recent timestamp in the archive?
                 */
                ArtifactReference artifact = getFirstArtifact( managedRepository, reference );

                if ( artifact == null )
                {
                    throw new IOException( "Not snapshot artifact found to reference in " + reference );
                }

                File artifactFile = new File( managedRepository.getUrl().getPath(), layout.toPath( artifact ) );

                if ( artifactFile.exists() )
                {
                    Date lastModified = new Date( artifactFile.lastModified() );
                    metadata.setLastUpdatedTimestamp( lastModified );
                }
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

        // Save the metadata model to disk.
        RepositoryMetadataWriter.write( metadata, metadataFile );
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

            List proxyConfigs = configuration.getConfiguration().getProxyConnectors();
            Iterator it = proxyConfigs.iterator();
            while ( it.hasNext() )
            {
                ProxyConnectorConfiguration proxyConfig = (ProxyConnectorConfiguration) it.next();
                String key = proxyConfig.getSourceRepoId();

                Set remoteRepoIds = this.proxies.get( key );

                if ( remoteRepoIds == null )
                {
                    remoteRepoIds = new HashSet<String>();
                }

                remoteRepoIds.add( proxyConfig.getTargetRepoId() );

                this.proxies.put( key, remoteRepoIds );
            }
        }
    }
}
