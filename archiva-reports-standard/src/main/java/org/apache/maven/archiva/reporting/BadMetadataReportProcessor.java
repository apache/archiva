package org.apache.maven.archiva.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class will report on bad metadata files.  These include invalid version declarations and incomplete version
 * information inside the metadata file.  Plugin metadata will be checked for validity of the latest plugin artifacts.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.MetadataReportProcessor" role-hint="bad-metadata"
 */
public class BadMetadataReportProcessor
    implements MetadataReportProcessor
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryQueryLayerFactory repositoryQueryLayerFactory;

    /**
     * Process the metadata encountered in the repository and report all errors found, if any.
     *
     * @param metadata   the metadata to be processed.
     * @param repository the repository where the metadata was encountered
     * @param reporter   the ArtifactReporter to receive processing results
     * @throws ReportProcessorException if an error was occurred while processing the metadata
     */
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository, ArtifactReporter reporter )
        throws ReportProcessorException
    {
        boolean hasFailures = false;

        if ( metadata.storedInGroupDirectory() )
        {
            try
            {
                hasFailures = checkPluginMetadata( metadata, repository, reporter );
            }
            catch ( IOException e )
            {
                throw new ReportProcessorException( "Error getting plugin artifact directories versions", e );
            }
        }
        else
        {
            String lastUpdated = metadata.getMetadata().getVersioning().getLastUpdated();
            if ( lastUpdated == null || lastUpdated.length() == 0 )
            {
                reporter.addFailure( metadata, "Missing lastUpdated element inside the metadata." );
                hasFailures = true;
            }

            if ( metadata.storedInArtifactVersionDirectory() )
            {
                hasFailures |= checkSnapshotMetadata( metadata, repository, reporter );
            }
            else
            {
                if ( !checkMetadataVersions( metadata, repository, reporter ) )
                {
                    hasFailures = true;
                }

                try
                {
                    if ( checkRepositoryVersions( metadata, repository, reporter ) )
                    {
                        hasFailures = true;
                    }
                }
                catch ( IOException e )
                {
                    throw new ReportProcessorException( "Error getting versions", e );
                }
            }
        }

        if ( !hasFailures )
        {
            reporter.addSuccess( metadata );
        }
    }

    /**
     * Method for processing a GroupRepositoryMetadata
     *
     * @param metadata   the metadata to be processed.
     * @param repository the repository where the metadata was encountered
     * @param reporter   the ArtifactReporter to receive processing results
     */
    private boolean checkPluginMetadata( RepositoryMetadata metadata, ArtifactRepository repository,
                                         ArtifactReporter reporter )
        throws IOException
    {
        boolean hasFailures = false;

        File metadataDir =
            new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( metadata ) ).getParentFile();
        List pluginDirs = getArtifactIdFiles( metadataDir );

        Map prefixes = new HashMap();
        for ( Iterator plugins = metadata.getMetadata().getPlugins().iterator(); plugins.hasNext(); )
        {
            Plugin plugin = (Plugin) plugins.next();

            String artifactId = plugin.getArtifactId();
            if ( artifactId == null || artifactId.length() == 0 )
            {
                reporter.addFailure( metadata, "Missing or empty artifactId in group metadata." );
                hasFailures = true;
            }

            String prefix = plugin.getPrefix();
            if ( prefix == null || prefix.length() == 0 )
            {
                reporter.addFailure( metadata, "Missing or empty plugin prefix for artifactId " + artifactId + "." );
                hasFailures = true;
            }
            else
            {
                if ( prefixes.containsKey( prefix ) )
                {
                    reporter.addFailure( metadata, "Duplicate plugin prefix found: " + prefix + "." );
                    hasFailures = true;
                }
                else
                {
                    prefixes.put( prefix, plugin );
                }
            }

            if ( artifactId != null && artifactId.length() > 0 )
            {
                File pluginDir = new File( metadataDir, artifactId );
                if ( !pluginDirs.contains( pluginDir ) )
                {
                    reporter.addFailure( metadata, "Metadata plugin " + artifactId + " not found in the repository" );
                    hasFailures = true;
                }
                else
                {
                    pluginDirs.remove( pluginDir );
                }
            }
        }

        if ( pluginDirs.size() > 0 )
        {
            for ( Iterator plugins = pluginDirs.iterator(); plugins.hasNext(); )
            {
                File plugin = (File) plugins.next();
                reporter.addFailure( metadata, "Plugin " + plugin.getName() + " is present in the repository but " +
                    "missing in the metadata." );
            }
            hasFailures = true;
        }

        return hasFailures;
    }

    /**
     * Method for processing a SnapshotArtifactRepository
     *
     * @param metadata   the metadata to be processed.
     * @param repository the repository where the metadata was encountered
     * @param reporter   the ArtifactReporter to receive processing results
     */
    private boolean checkSnapshotMetadata( RepositoryMetadata metadata, ArtifactRepository repository,
                                           ArtifactReporter reporter )
    {
        RepositoryQueryLayer repositoryQueryLayer =
            repositoryQueryLayerFactory.createRepositoryQueryLayer( repository );

        boolean hasFailures = false;

        Snapshot snapshot = metadata.getMetadata().getVersioning().getSnapshot();
        String timestamp = snapshot.getTimestamp();
        String buildNumber = String.valueOf( snapshot.getBuildNumber() );

        Artifact artifact = createArtifact( metadata );
        if ( !repositoryQueryLayer.containsArtifact( artifact, snapshot ) )
        {
            reporter.addFailure( metadata, "Snapshot artifact " + timestamp + "-" + buildNumber + " does not exist." );
            hasFailures = true;
        }

        return hasFailures;
    }

    /**
     * Method for validating the versions declared inside an ArtifactRepositoryMetadata
     *
     * @param metadata   the metadata to be processed.
     * @param repository the repository where the metadata was encountered
     * @param reporter   the ArtifactReporter to receive processing results
     */
    private boolean checkMetadataVersions( RepositoryMetadata metadata, ArtifactRepository repository,
                                           ArtifactReporter reporter )
    {
        RepositoryQueryLayer repositoryQueryLayer =
            repositoryQueryLayerFactory.createRepositoryQueryLayer( repository );

        boolean hasFailures = false;
        Versioning versioning = metadata.getMetadata().getVersioning();
        for ( Iterator versions = versioning.getVersions().iterator(); versions.hasNext(); )
        {
            String version = (String) versions.next();

            Artifact artifact = createArtifact( metadata, version );

            if ( !repositoryQueryLayer.containsArtifact( artifact ) )
            {
                reporter.addFailure( metadata, "Artifact version " + version + " is present in metadata but " +
                    "missing in the repository." );
                hasFailures = true;
            }
        }
        return hasFailures;
    }

    /**
     * Searches the artifact repository directory for all versions and verifies that all of them are listed in the
     * ArtifactRepositoryMetadata
     *
     * @param metadata   the metadata to be processed.
     * @param repository the repository where the metadata was encountered
     * @param reporter   the ArtifactReporter to receive processing results
     */
    private boolean checkRepositoryVersions( RepositoryMetadata metadata, ArtifactRepository repository,
                                             ArtifactReporter reporter )
        throws IOException
    {
        boolean hasFailures = false;
        Versioning versioning = metadata.getMetadata().getVersioning();
        File versionsDir =
            new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( metadata ) ).getParentFile();
        List versions = FileUtils.getFileNames( versionsDir, "*/*.pom", null, false );
        for ( Iterator i = versions.iterator(); i.hasNext(); )
        {
            File path = new File( (String) i.next() );
            String version = path.getParentFile().getName();
            if ( !versioning.getVersions().contains( version ) )
            {
                reporter.addFailure( metadata, "Artifact version " + version + " found in the repository but " +
                    "missing in the metadata." );
                hasFailures = true;
            }
        }
        return hasFailures;
    }

    /**
     * Used to create an artifact object from a metadata base version
     */
    private Artifact createArtifact( RepositoryMetadata metadata )
    {
        return artifactFactory.createBuildArtifact( metadata.getGroupId(), metadata.getArtifactId(),
                                                    metadata.getBaseVersion(), "pom" );
    }

    /**
     * Used to create an artifact object with a specified version
     */
    private Artifact createArtifact( RepositoryMetadata metadata, String version )
    {
        return artifactFactory.createBuildArtifact( metadata.getGroupId(), metadata.getArtifactId(), version, "pom" );
    }

    /**
     * Used to gather artifactIds from a groupId directory
     */
    private List getArtifactIdFiles( File groupIdDir )
        throws IOException
    {
        List artifactIdFiles = new ArrayList();

        List fileArray = new ArrayList( Arrays.asList( groupIdDir.listFiles() ) );
        for ( Iterator files = fileArray.iterator(); files.hasNext(); )
        {
            File artifactDir = (File) files.next();

            if ( artifactDir.isDirectory() )
            {
                List versions = FileUtils.getFileNames( artifactDir, "*/*.pom", null, false );
                if ( versions.size() > 0 )
                {
                    artifactIdFiles.add( artifactDir );
                }
            }
        }

        return artifactIdFiles;
    }
}
