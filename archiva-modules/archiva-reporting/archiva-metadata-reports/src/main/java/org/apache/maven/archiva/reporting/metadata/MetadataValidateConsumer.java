package org.apache.maven.archiva.reporting.metadata;

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


/**
 * MetadataValidateConsumer 
 *
 * @version $Id$
 * 
 * TODO: whoops, how do we consumer metadata?  
 */
public class MetadataValidateConsumer
{

//    /**
//     * Process the metadata encountered in the repository and report all errors found, if any.
//     *
//     * @param metadata   the metadata to be processed.
//     * @param repository the repository where the metadata was encountered
//     * @param reporter   the ReportingDatabase to receive processing results
//     */
//    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository )
//    {
//        if ( metadata.storedInGroupDirectory() )
//        {
//            try
//            {
//                checkPluginMetadata( metadata, repository );
//            }
//            catch ( IOException e )
//            {
//                addWarning( metadata, null, "Error getting plugin artifact directories versions: " + e );
//            }
//        }
//        else
//        {
//            Versioning versioning = metadata.getMetadata().getVersioning();
//            boolean found = false;
//            if ( versioning != null )
//            {
//                String lastUpdated = versioning.getLastUpdated();
//                if ( lastUpdated != null && lastUpdated.length() != 0 )
//                {
//                    found = true;
//                }
//            }
//            if ( !found )
//            {
//                addFailure( metadata, "missing-last-updated", "Missing lastUpdated element inside the metadata." );
//            }
//
//            if ( metadata.storedInArtifactVersionDirectory() )
//            {
//                checkSnapshotMetadata( metadata, repository );
//            }
//            else
//            {
//                checkMetadataVersions( metadata, repository );
//
//                try
//                {
//                    checkRepositoryVersions( metadata, repository );
//                }
//                catch ( IOException e )
//                {
//                    String reason = "Error getting plugin artifact directories versions: " + e;
//                    addWarning( metadata, null, reason );
//                }
//            }
//        }
//    }
//
//    private void addWarning( RepositoryMetadata metadata, String problem, String reason )
//    {
//        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
//        database.addWarning( metadata, ROLE_HINT, problem, reason );
//    }
//
//    /**
//     * Method for processing a GroupRepositoryMetadata
//     *
//     * @param metadata   the metadata to be processed.
//     * @param repository the repository where the metadata was encountered
//     * @param reporter   the ReportingDatabase to receive processing results
//     */
//    private void checkPluginMetadata( RepositoryMetadata metadata, ArtifactRepository repository )
//        throws IOException
//    {
//        File metadataDir = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( metadata ) )
//            .getParentFile();
//        List pluginDirs = getArtifactIdFiles( metadataDir );
//
//        Map prefixes = new HashMap();
//        for ( Iterator plugins = metadata.getMetadata().getPlugins().iterator(); plugins.hasNext(); )
//        {
//            Plugin plugin = (Plugin) plugins.next();
//
//            String artifactId = plugin.getArtifactId();
//            if ( artifactId == null || artifactId.length() == 0 )
//            {
//                addFailure( metadata, "missing-artifact-id:" + plugin.getPrefix(),
//                            "Missing or empty artifactId in group metadata for plugin " + plugin.getPrefix() );
//            }
//
//            String prefix = plugin.getPrefix();
//            if ( prefix == null || prefix.length() == 0 )
//            {
//                addFailure( metadata, "missing-plugin-prefix:" + artifactId,
//                            "Missing or empty plugin prefix for artifactId " + artifactId + "." );
//            }
//            else
//            {
//                if ( prefixes.containsKey( prefix ) )
//                {
//                    addFailure( metadata, "duplicate-plugin-prefix:" + prefix, "Duplicate plugin prefix found: "
//                        + prefix + "." );
//                }
//                else
//                {
//                    prefixes.put( prefix, plugin );
//                }
//            }
//
//            if ( artifactId != null && artifactId.length() > 0 )
//            {
//                File pluginDir = new File( metadataDir, artifactId );
//                if ( !pluginDirs.contains( pluginDir ) )
//                {
//                    addFailure( metadata, "missing-plugin-from-repository:" + artifactId, "Metadata plugin "
//                        + artifactId + " not found in the repository" );
//                }
//                else
//                {
//                    pluginDirs.remove( pluginDir );
//                }
//            }
//        }
//
//        if ( pluginDirs.size() > 0 )
//        {
//            for ( Iterator plugins = pluginDirs.iterator(); plugins.hasNext(); )
//            {
//                File plugin = (File) plugins.next();
//                addFailure( metadata, "missing-plugin-from-metadata:" + plugin.getName(), "Plugin " + plugin.getName()
//                    + " is present in the repository but " + "missing in the metadata." );
//            }
//        }
//    }
//
//    /**
//     * Method for processing a SnapshotArtifactRepository
//     *
//     * @param metadata   the metadata to be processed.
//     * @param repository the repository where the metadata was encountered
//     * @param reporter   the ReportingDatabase to receive processing results
//     */
//    private void checkSnapshotMetadata( RepositoryMetadata metadata, ArtifactRepository repository )
//    {
//        RepositoryQueryLayer repositoryQueryLayer = repositoryQueryLayerFactory.createRepositoryQueryLayer( repository );
//
//        Versioning versioning = metadata.getMetadata().getVersioning();
//        if ( versioning != null )
//        {
//            Snapshot snapshot = versioning.getSnapshot();
//
//            String version = StringUtils.replace( metadata.getBaseVersion(), Artifact.SNAPSHOT_VERSION, snapshot
//                .getTimestamp()
//                + "-" + snapshot.getBuildNumber() );
//            Artifact artifact = artifactFactory.createProjectArtifact( metadata.getGroupId(), metadata.getArtifactId(),
//                                                                       version );
//            artifact.isSnapshot(); // trigger baseVersion correction
//
//            if ( !repositoryQueryLayer.containsArtifact( artifact ) )
//            {
//                addFailure( metadata, "missing-snapshot-artifact-from-repository:" + version, "Snapshot artifact "
//                    + version + " does not exist." );
//            }
//        }
//    }
//
//    /**
//     * Method for validating the versions declared inside an ArtifactRepositoryMetadata
//     *
//     * @param metadata   the metadata to be processed.
//     * @param repository the repository where the metadata was encountered
//     * @param reporter   the ReportingDatabase to receive processing results
//     */
//    private void checkMetadataVersions( RepositoryMetadata metadata, ArtifactRepository repository )
//    {
//        RepositoryQueryLayer repositoryQueryLayer = repositoryQueryLayerFactory.createRepositoryQueryLayer( repository );
//
//        Versioning versioning = metadata.getMetadata().getVersioning();
//        if ( versioning != null )
//        {
//            for ( Iterator versions = versioning.getVersions().iterator(); versions.hasNext(); )
//            {
//                String version = (String) versions.next();
//
//                Artifact artifact = artifactFactory.createProjectArtifact( metadata.getGroupId(), metadata
//                    .getArtifactId(), version );
//
//                if ( !repositoryQueryLayer.containsArtifact( artifact ) )
//                {
//                    addFailure( metadata, "missing-artifact-from-repository:" + version, "Artifact version " + version
//                        + " is present in metadata but " + "missing in the repository." );
//                }
//            }
//        }
//    }
//
//    /**
//     * Searches the artifact repository directory for all versions and verifies that all of them are listed in the
//     * ArtifactRepositoryMetadata
//     *
//     * @param metadata   the metadata to be processed.
//     * @param repository the repository where the metadata was encountered
//     * @param reporter   the ReportingDatabase to receive processing results
//     * @throws java.io.IOException if there is a problem reading from the file system
//     */
//    private void checkRepositoryVersions( RepositoryMetadata metadata, ArtifactRepository repository )
//        throws IOException
//    {
//        Versioning versioning = metadata.getMetadata().getVersioning();
//        List metadataVersions = versioning != null ? versioning.getVersions() : Collections.EMPTY_LIST;
//        File versionsDir = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( metadata ) )
//            .getParentFile();
//
//        // TODO: I don't know how this condition can happen, but it was seen on the main repository.
//        // Avoid hard failure
//        if ( versionsDir.exists() )
//        {
//            List versions = FileUtils.getFileNames( versionsDir, "*/*.pom", null, false );
//            for ( Iterator i = versions.iterator(); i.hasNext(); )
//            {
//                File path = new File( (String) i.next() );
//                String version = path.getParentFile().getName();
//                if ( !metadataVersions.contains( version ) )
//                {
//                    addFailure( metadata, "missing-artifact-from-metadata:" + version, "Artifact version " + version
//                        + " found in the repository but " + "missing in the metadata." );
//                }
//            }
//        }
//        else
//        {
//            addFailure( metadata, null, "Metadata's directory did not exist: " + versionsDir );
//        }
//    }
//
//    /**
//     * Used to gather artifactIds from a groupId directory.
//     *
//     * @param groupIdDir the directory of the group
//     * @return the list of artifact ID File objects for each directory
//     * @throws IOException if there was a failure to read the directories
//     */
//    private List getArtifactIdFiles( File groupIdDir )
//        throws IOException
//    {
//        List artifactIdFiles = new ArrayList();
//
//        File[] files = groupIdDir.listFiles();
//        if ( files != null )
//        {
//            for ( Iterator i = Arrays.asList( files ).iterator(); i.hasNext(); )
//            {
//                File artifactDir = (File) i.next();
//
//                if ( artifactDir.isDirectory() )
//                {
//                    List versions = FileUtils.getFileNames( artifactDir, "*/*.pom", null, false );
//                    if ( versions.size() > 0 )
//                    {
//                        artifactIdFiles.add( artifactDir );
//                    }
//                }
//            }
//        }
//
//        return artifactIdFiles;
//    }
//
//    private void addFailure( RepositoryMetadata metadata, String problem, String reason )
//    {
//        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
//        database.addFailure( metadata, ROLE_HINT, problem, reason );
//    }
    
}
