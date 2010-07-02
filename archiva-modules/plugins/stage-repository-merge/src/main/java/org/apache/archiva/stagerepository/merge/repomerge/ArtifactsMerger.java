package org.apache.archiva.stagerepository.merge.repomerge;

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

import org.apache.archiva.stagerepository.merge.repodetails.SourceAritfacts;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @plexus.component role="org.apache.archiva.stagerepository.merge.repomerge.ArtifactsMerger"
 */
public class ArtifactsMerger
{

    /**
     * @plexus.requirement
     */
    private MetadataResolver metadataResolver;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArchivaConfiguration configuration;

    private SourceAritfacts sourceArtifacts;

    private String targetRepo;

    private String sourceRepo;

    private ArrayList<ArchivaArtifact> sourceArtifactsList;

    private Configuration config;

    private ManagedRepositoryConfiguration targetRepoConfig;

    private ManagedRepositoryConfiguration sourceRepoConfig;

    private ManagedRepositoryContent targetRepository;

    private ManagedRepositoryContent sourceRepository;

    private final static String PATH_SEPERATOR = "/";

    public void setMetadataResolver( MetadataResolver metadataResolver )
    {
        this.metadataResolver = metadataResolver;
    }

    public void setSourceArtifacts( SourceAritfacts sourceArtifacts )
    {
        this.sourceArtifacts = sourceArtifacts;
        setSourceArtifactsList();
    }

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public ArtifactsMerger( String targetRepo, String sourceRepo )
    {
        this.targetRepo = targetRepo;
        this.sourceRepo = sourceRepo;

    }

    private void setSourceArtifactsList()
    {
        sourceArtifacts.setRepoId( sourceRepo );
        sourceArtifactsList = (ArrayList) sourceArtifacts.getSourceArtifactList();
    }

    private boolean isArtifactAvailableIntagerRepo( ArchivaArtifact artifact )
    {

        boolean isAvailable = false;

        Collection<ArtifactMetadata> list =
            metadataResolver.getArtifacts( targetRepo, artifact.getGroupId(), artifact.getArtifactId(),
                                           artifact.getVersion() );

        if ( list.isEmpty() )
        {
            isAvailable = false;
        }
        else
        {
            isAvailable = true;
        }
        return isAvailable;
    }

    public void doMerge()
        throws Exception
    {
        try
        {
            config = configuration.getConfiguration();

            targetRepoConfig = config.findManagedRepositoryById( targetRepo );

            targetRepository = repositoryFactory.getManagedRepositoryContent( targetRepo );

            sourceRepoConfig = config.findManagedRepositoryById( sourceRepo );

            sourceRepository = repositoryFactory.getManagedRepositoryContent( sourceRepo );

            // iterates through available arfifacts list
            for ( ArchivaArtifact sourceArtifact : sourceArtifactsList )
            {

                if ( isArtifactAvailableIntagerRepo( sourceArtifact ) )
                {
                    // TODO here we need to implement replacing the exixting one
                }
                else
                {
                    // when the artifact does not exist in the repo folder structure creation is done
                    createFolderStructure( sourceArtifact );
                }
            }
        }
        catch ( RepositoryNotFoundException re )
        {
            throw re;
        }
        catch ( RepositoryException rep )
        {
            throw rep;
        }
    }

    private void createFolderStructure( ArchivaArtifact artifact )
        throws IOException, RepositoryMetadataException
    {

        Date lastUpdatedTimestamp = Calendar.getInstance().getTime();

        TimeZone timezone = TimeZone.getTimeZone( "UTC" );

        DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );

        fmt.setTimeZone( timezone );

        String timestamp = fmt.format( lastUpdatedTimestamp );

        String targetRepoPath = targetRepoConfig.getLocation();

        String sourceRepoPath = sourceRepoConfig.getLocation();

        String artifactPath = sourceRepository.toPath( artifact );

        File sourceArtifactFile = new File( sourceRepoPath, artifactPath );

        File targetArtifactFile = new File( targetRepoPath, artifactPath );

        int lastIndex = artifactPath.lastIndexOf( '/' );

        // create a file object to the artifact version directory eg
        // :/boot/gsoc/apps/apache-archiva-1.4-SNAPSHOT/data/repositories/internal/ant/ant/1.5.1
        File targetFile = new File( targetRepoPath, artifactPath.substring( 0, lastIndex ) );

        if ( !targetFile.exists() )
        {
            // create the folder structure when it does not exist
            targetFile.mkdirs();
        }

        // artifact copying
        copyFile( sourceArtifactFile, targetArtifactFile );
        // pom file copying
        String index = artifactPath.substring( lastIndex + 1 );
        int last = index.lastIndexOf( '.' );
        File sourcePomFile =
            new File( sourceRepoPath, artifactPath.substring( 0, lastIndex ) + "/"
                + artifactPath.substring( lastIndex + 1 ).substring( 0, last ) + ".pom" );
        File targetPomFile =
            new File( targetRepoPath, artifactPath.substring( 0, lastIndex ) + "/"
                + artifactPath.substring( lastIndex + 1 ).substring( 0, last ) + ".pom" );

        if ( !targetPomFile.exists() )
        {
            copyFile( sourcePomFile, targetPomFile );
        }

        // explicitly update only if metadata-updater consumer is not enabled!
        if ( !config.getRepositoryScanning().getKnownContentConsumers().contains( "metadata-updater" ) )
        {

            // maven version meta data file copying

            File versionMetadataFileInSourceArtifact =
                new File( sourceRepoPath, artifactPath.substring( 0, lastIndex ) + "/" + MetadataTools.MAVEN_METADATA );

            File versionMetadataFileInTargetArtifact = null;

            // check metadata xml is available in source repo. if there is a metadata xml we are going to merge is as
            // well
            if ( versionMetadataFileInSourceArtifact.exists() )
            {
                versionMetadataFileInTargetArtifact =
                    new File( targetRepoPath, artifactPath.substring( 0, lastIndex ) + "/"
                        + MetadataTools.MAVEN_METADATA );

                // check metadata xml is available in target repo. if it is not available copy it from the source
                // artifact
                if ( !versionMetadataFileInTargetArtifact.exists() )
                {
                    copyFile( versionMetadataFileInSourceArtifact, versionMetadataFileInTargetArtifact );
                }
                else
                {
                    // if version metadata file exists then update it.
                    updateVersionMetadata( versionMetadataFileInTargetArtifact, artifact, lastUpdatedTimestamp,
                                           timestamp );
                }
            }

            // project level maven meta data xml copying
            String projectDirectoryInSourceRepo =
                new File( versionMetadataFileInSourceArtifact.getParent() ).getParent();
            File projectMetadataFileInSourceArtifact =
                new File( projectDirectoryInSourceRepo, MetadataTools.MAVEN_METADATA );

            // check metadata xml is available in source repo. if there is a metadata xml we are going to merge is as
            // well
            if ( projectMetadataFileInSourceArtifact.exists() )
            {

                String projectDirectoryInTargetRepo =
                    new File( versionMetadataFileInTargetArtifact.getParent() ).getParent();
                File projectMetadataFileInTargetArtifact =
                    new File( projectDirectoryInTargetRepo, MetadataTools.MAVEN_METADATA );
                // check metadata xml is available in target repo.if it is not available copy it from the source
                // artifact
                if ( !projectMetadataFileInTargetArtifact.exists() )
                {
                    copyFile( projectMetadataFileInSourceArtifact, projectMetadataFileInTargetArtifact );
                }
                else
                {
                    // // if project metadata file exists then update it.
                    updateProjectMetadata( projectMetadataFileInTargetArtifact, artifact, lastUpdatedTimestamp,
                                           timestamp );
                }
            }
        }
    }

    private void copyFile( File sourceFile, File targetFile )
        throws IOException
    {

        FileOutputStream out = new FileOutputStream( targetFile );
        FileInputStream input = new FileInputStream( sourceFile );

        try
        {
            int i;
            while ( ( i = input.read() ) != -1 )
            {
                out.write( i );
            }
            out.flush();
        }
        finally
        {
            out.close();
            input.close();
        }
    }

    /**
     * Update artifact level metadata.
     */
    private void updateProjectMetadata( File projectMetaDataFileIntargetRepo, ArchivaArtifact artifact,
                                        Date lastUpdatedTimestamp, String timestamp )
        throws RepositoryMetadataException
    {
        ArrayList<String> availableVersions = new ArrayList<String>();
        String latestVersion = "";

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetaDataFileIntargetRepo );

        if ( projectMetaDataFileIntargetRepo.exists() )
        {
            availableVersions = (ArrayList<String>) projectMetadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( artifact.getVersion() ) )
            {
                availableVersions.add( artifact.getVersion() );
            }

            latestVersion = availableVersions.get( availableVersions.size() - 1 );
        }
        else
        {
            availableVersions.add( artifact.getVersion() );
            projectMetadata.setGroupId( artifact.getGroupId() );
            projectMetadata.setArtifactId( artifact.getArtifactId() );
        }

        if ( projectMetadata.getGroupId() == null )
        {
            projectMetadata.setGroupId( artifact.getGroupId() );
        }

        if ( projectMetadata.getArtifactId() == null )
        {
            projectMetadata.setArtifactId( artifact.getArtifactId() );
        }

        projectMetadata.setLatestVersion( latestVersion );
        projectMetadata.setAvailableVersions( availableVersions );
        projectMetadata.setLastUpdated( timestamp );
        projectMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );

        if ( !VersionUtil.isSnapshot( artifact.getVersion() ) )
        {
            projectMetadata.setReleasedVersion( latestVersion );
        }

        RepositoryMetadataWriter.write( projectMetadata, projectMetaDataFileIntargetRepo );

    }

    private void updateVersionMetadata( File versionMetaDataFileInTargetRepo, ArchivaArtifact artifact,
                                        Date lastUpdatedTimestamp, String timestamp )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata versionMetadata = getMetadata( versionMetaDataFileInTargetRepo );
        if ( !versionMetaDataFileInTargetRepo.exists() )
        {
            versionMetadata.setGroupId( artifact.getGroupId() );
            versionMetadata.setArtifactId( artifact.getArtifactId() );
            versionMetadata.setVersion( artifact.getVersion() );
        }

        // versionMetadata.getSnapshotVersion().setTimestamp(timestamp);
        versionMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        RepositoryMetadataWriter.write( versionMetadata, versionMetaDataFileInTargetRepo );
    }

    private ArchivaRepositoryMetadata getMetadata( File metadataFile )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if ( metadataFile.exists() )
        {
            metadata = RepositoryMetadataReader.read( metadataFile );
        }
        return metadata;
    }
}
