package org.apache.archiva.stagerepository.merge;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.filter.IncludesFilter;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @plexus.component role="org.apache.archiva.stagerepository.merge.RepositoryMerger" role-hint="maven2"
 */
public class Maven2RepositoryMerger
    implements RepositoryMerger
{

    /**
     * @plexus.requirement role-hint="default"
     */
    private MetadataRepository metadataRepository;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement role-hint="maven2"
     */
    private RepositoryPathTranslator pathTranslator;

    private static final String METADATA_FILENAME = "maven-metadata.xml";

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public void merge( String sourceRepoId, String targetRepoId )
        throws Exception
    {

        List<ArtifactMetadata> artifactsInSourceRepo = metadataRepository.getArtifacts( sourceRepoId );
        for ( ArtifactMetadata artifactMetadata : artifactsInSourceRepo )
        {
            createFolderStructure( sourceRepoId, targetRepoId, artifactMetadata );
        }
    }

    // TODO when UI needs a subset to merge
    public void merge( String sourceRepoId, String targetRepoId, Filter<ArtifactMetadata> filter )
    {

    }

    private void createFolderStructure( String sourceRepoId, String targetRepoId, ArtifactMetadata artifactMetadata )
        throws IOException, RepositoryException
    {
        Configuration config = configuration.getConfiguration();

        ManagedRepositoryConfiguration targetRepoConfig = config.findManagedRepositoryById( targetRepoId );

        ManagedRepositoryConfiguration sourceRepoConfig = config.findManagedRepositoryById( sourceRepoId );

        Date lastUpdatedTimestamp = Calendar.getInstance().getTime();

        TimeZone timezone = TimeZone.getTimeZone( "UTC" );

        DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );

        fmt.setTimeZone( timezone );

        String timestamp = fmt.format( lastUpdatedTimestamp );

        String targetRepoPath = targetRepoConfig.getLocation();

        String sourceRepoPath = sourceRepoConfig.getLocation();

        String artifactPath =
            pathTranslator.toPath( artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                   artifactMetadata.getProjectVersion(), artifactMetadata.getId() );

        File sourceArtifactFile = new File( sourceRepoPath, artifactPath );

        File targetArtifactFile = new File( targetRepoPath, artifactPath );

        int lastIndex = artifactPath.lastIndexOf( '/' );

        File targetFile = new File( targetRepoPath, artifactPath.substring( 0, lastIndex ) );

        if ( !targetFile.exists() )
        {
            // create the folder structure when it does not exist
            targetFile.mkdirs();
        }
        // artifact copying
        copyFile( sourceArtifactFile, targetArtifactFile );

        // pom file copying
        String fileName = artifactMetadata.getProject() + "-" + artifactMetadata.getVersion() + ".pom";

        File sourcePomFile =
            pathTranslator.toFile( new File( sourceRepoPath ), artifactMetadata.getId(), artifactMetadata.getProject(),
                                   artifactMetadata.getVersion(), fileName );
        String relativePathToPomFile = sourcePomFile.getAbsolutePath().split( sourceRepoPath )[1];
        File targetPomFile = new File( targetRepoPath, relativePathToPomFile );

        if ( !targetPomFile.exists() )
        {
            copyFile( sourcePomFile, targetPomFile );
        }

        // explicitly update only if metadata-updater consumer is not enabled!
        if ( !config.getRepositoryScanning().getKnownContentConsumers().contains( "metadata-updater" ) )
        {

            // updating version metadata files
            File versionMetaDataFileInSourceRepo =
                pathTranslator.toFile( new File( sourceRepoPath ), artifactMetadata.getNamespace(),
                                       artifactMetadata.getProject(), artifactMetadata.getVersion(), METADATA_FILENAME );
            String relativePathToVersionMetadataFile =
                versionMetaDataFileInSourceRepo.getAbsolutePath().split( sourceRepoPath )[1];
            File versionMetaDataFileInTargetRepo = new File( targetRepoPath, relativePathToVersionMetadataFile );

            if ( !versionMetaDataFileInTargetRepo.exists() )
            {
                copyFile( versionMetaDataFileInSourceRepo, versionMetaDataFileInTargetRepo );
            }
            else
            {
                updateVersionMetadata( versionMetaDataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp );

            }

            // updating project meta data file
            String projectDirectoryInSourceRepo = new File( versionMetaDataFileInSourceRepo.getParent() ).getParent();
            File projectMetadataFileInSourceRepo = new File( projectDirectoryInSourceRepo, METADATA_FILENAME );

            String relativePathToProjectMetadataFile =
                projectMetadataFileInSourceRepo.getAbsolutePath().split( sourceRepoPath )[1];
            File projectMetadataFileInTargetRepo = new File( targetRepoPath, relativePathToProjectMetadataFile );

            if ( !projectMetadataFileInTargetRepo.exists() )
            {

                copyFile( versionMetaDataFileInSourceRepo, projectMetadataFileInSourceRepo );
            }
            else
            {
                updateProjectMetadata( projectMetadataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp,
                                       timestamp );
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

    private void updateProjectMetadata( File projectMetaDataFileIntargetRepo, ArtifactMetadata artifactMetadata,
                                        Date lastUpdatedTimestamp, String timestamp )
        throws RepositoryMetadataException
    {
        ArrayList<String> availableVersions = new ArrayList<String>();
        String latestVersion = artifactMetadata.getProjectVersion();

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetaDataFileIntargetRepo );

        if ( projectMetaDataFileIntargetRepo.exists() )
        {
            availableVersions = (ArrayList<String>) projectMetadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( artifactMetadata.getVersion() ) )
            {
                availableVersions.add( artifactMetadata.getVersion() );
            }

            latestVersion = availableVersions.get( availableVersions.size() - 1 );
        }
        else
        {
            availableVersions.add( artifactMetadata.getProjectVersion() );
            projectMetadata.setGroupId( artifactMetadata.getNamespace() );
            projectMetadata.setArtifactId( artifactMetadata.getProject() );
        }

        if ( projectMetadata.getGroupId() == null )
        {
            projectMetadata.setGroupId( artifactMetadata.getNamespace() );
        }

        if ( projectMetadata.getArtifactId() == null )
        {
            projectMetadata.setArtifactId( artifactMetadata.getProject() );
        }

        projectMetadata.setLatestVersion( latestVersion );
        projectMetadata.setAvailableVersions( availableVersions );
        projectMetadata.setLastUpdated( timestamp );
        projectMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );

        if ( !VersionUtil.isSnapshot( artifactMetadata.getVersion() ) )
        {
            projectMetadata.setReleasedVersion( latestVersion );
        }

        RepositoryMetadataWriter.write( projectMetadata, projectMetaDataFileIntargetRepo );

    }

    private void updateVersionMetadata( File versionMetaDataFileInTargetRepo, ArtifactMetadata artifactMetadata,
                                        Date lastUpdatedTimestamp )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata versionMetadata = getMetadata( versionMetaDataFileInTargetRepo );
        if ( !versionMetaDataFileInTargetRepo.exists() )
        {
            versionMetadata.setGroupId( artifactMetadata.getNamespace() );
            versionMetadata.setArtifactId( artifactMetadata.getProject() );
            versionMetadata.setVersion( artifactMetadata.getProjectVersion() );
        }

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
