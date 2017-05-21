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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 *
 */
@Service ("repositoryMerger#maven2")
public class Maven2RepositoryMerger
    implements RepositoryMerger
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    /**
     *
     */
    private ArchivaConfiguration configuration;

    /**
     *
     */
    private RepositoryPathTranslator pathTranslator;

    private static final String METADATA_FILENAME = "maven-metadata.xml";

    @Inject
    public Maven2RepositoryMerger(
        @Named (value = "archivaConfiguration#default") ArchivaConfiguration archivaConfiguration,
        @Named (value = "repositoryPathTranslator#maven2") RepositoryPathTranslator repositoryPathTranslator )
    {
        this.configuration = archivaConfiguration;
        this.pathTranslator = repositoryPathTranslator;
    }

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }

    @Override
    public void merge( MetadataRepository metadataRepository, String sourceRepoId, String targetRepoId )
        throws RepositoryMergerException
    {

        try
        {
            List<ArtifactMetadata> artifactsInSourceRepo = metadataRepository.getArtifacts( sourceRepoId );
            for ( ArtifactMetadata artifactMetadata : artifactsInSourceRepo )
            {
                artifactMetadata.setRepositoryId( targetRepoId );
                createFolderStructure( sourceRepoId, targetRepoId, artifactMetadata );
            }
        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
    }

    // TODO when UI needs a subset to merge
    @Override
    public void merge( MetadataRepository metadataRepository, String sourceRepoId, String targetRepoId,
                       Filter<ArtifactMetadata> filter )
        throws RepositoryMergerException
    {
        try
        {
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( sourceRepoId );
            for ( ArtifactMetadata metadata : sourceArtifacts )
            {
                if ( filter.accept( metadata ) )
                {
                    createFolderStructure( sourceRepoId, targetRepoId, metadata );
                }
            }
        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
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

        String artifactPath = pathTranslator.toPath( artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                                     artifactMetadata.getProjectVersion(), artifactMetadata.getId() );

        File sourceArtifactFile = new File( sourceRepoPath, artifactPath );

        File targetArtifactFile = new File( targetRepoPath, artifactPath );

        log.debug( "artifactPath {}", artifactPath );

        int lastIndex = artifactPath.lastIndexOf( RepositoryPathTranslator.PATH_SEPARATOR );

        File targetFile = new File( targetRepoPath, artifactPath.substring( 0, lastIndex ) );

        if ( !targetFile.exists() )
        {
            // create the folder structure when it does not exist
            targetFile.mkdirs();
        }
        // artifact copying
        copyFile( sourceArtifactFile, targetArtifactFile );

        // pom file copying
        // TODO need to use path translator to get the pom file path
//        String fileName = artifactMetadata.getProject() + "-" + artifactMetadata.getVersion() + ".pom";
//
//        File sourcePomFile =
//            pathTranslator.toFile( new File( sourceRepoPath ), artifactMetadata.getId(), artifactMetadata.getProject(),
//                                   artifactMetadata.getVersion(), fileName );
//
//        String relativePathToPomFile = sourcePomFile.getAbsolutePath().split( sourceRepoPath )[1];
//        File targetPomFile = new File( targetRepoPath, relativePathToPomFile );

        //pom file copying  (file path is taken with out using path translator)

        String index = artifactPath.substring( lastIndex + 1 );
        int last = index.lastIndexOf( '.' );
        File sourcePomFile = new File( sourceRepoPath,
                                       artifactPath.substring( 0, lastIndex ) + "/" + artifactPath.substring(
                                           lastIndex + 1 ).substring( 0, last ) + ".pom" );
        File targetPomFile = new File( targetRepoPath,
                                       artifactPath.substring( 0, lastIndex ) + "/" + artifactPath.substring(
                                           lastIndex + 1 ).substring( 0, last ) + ".pom" );

        if ( !targetPomFile.exists() && sourcePomFile.exists() )
        {
            copyFile( sourcePomFile, targetPomFile );
        }

        // explicitly update only if metadata-updater consumer is not enabled!
        if ( !config.getRepositoryScanning().getKnownContentConsumers().contains( "metadata-updater" ) )
        {

            // updating version metadata files
            File versionMetaDataFileInSourceRepo =
                pathTranslator.toFile( new File( sourceRepoPath ), artifactMetadata.getNamespace(),
                                       artifactMetadata.getProject(), artifactMetadata.getVersion(),
                                       METADATA_FILENAME );

            if ( versionMetaDataFileInSourceRepo.exists() )
            {//Pattern quote for windows path
                String relativePathToVersionMetadataFile =
                    versionMetaDataFileInSourceRepo.getAbsolutePath().split( Pattern.quote( sourceRepoPath ) )[1];
                File versionMetaDataFileInTargetRepo = new File( targetRepoPath, relativePathToVersionMetadataFile );

                if ( !versionMetaDataFileInTargetRepo.exists() )
                {
                    copyFile( versionMetaDataFileInSourceRepo, versionMetaDataFileInTargetRepo );
                }
                else
                {
                    updateVersionMetadata( versionMetaDataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp );

                }
            }

            // updating project meta data file
            String projectDirectoryInSourceRepo = new File( versionMetaDataFileInSourceRepo.getParent() ).getParent();
            File projectMetadataFileInSourceRepo = new File( projectDirectoryInSourceRepo, METADATA_FILENAME );

            if ( projectMetadataFileInSourceRepo.exists() )
            {
                String relativePathToProjectMetadataFile =
                    projectMetadataFileInSourceRepo.getAbsolutePath().split( Pattern.quote( sourceRepoPath ) )[1];
                File projectMetadataFileInTargetRepo = new File( targetRepoPath, relativePathToProjectMetadataFile );

                if ( !projectMetadataFileInTargetRepo.exists() )
                {

                    copyFile( projectMetadataFileInSourceRepo, projectMetadataFileInTargetRepo );
                }
                else
                {
                    updateProjectMetadata( projectMetadataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp,
                                           timestamp );
                }
            }
        }

    }

    private void copyFile( File sourceFile, File targetFile )
        throws IOException
    {

        FileUtils.copyFile( sourceFile, targetFile );

    }

    private void updateProjectMetadata( File projectMetaDataFileIntargetRepo, ArtifactMetadata artifactMetadata,
                                        Date lastUpdatedTimestamp, String timestamp )
        throws RepositoryMetadataException
    {
        ArrayList<String> availableVersions = new ArrayList<>();
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
            try
            {
                metadata = MavenMetadataReader.read( metadataFile );
            }
            catch ( XMLException e )
            {
                throw new RepositoryMetadataException( e.getMessage(), e );
            }
        }
        return metadata;
    }

    @Override
    public List<ArtifactMetadata> getConflictingArtifacts( MetadataRepository metadataRepository, String sourceRepo,
                                                           String targetRepo )
        throws RepositoryMergerException
    {
        try
        {
            List<ArtifactMetadata> targetArtifacts = metadataRepository.getArtifacts( targetRepo );
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( sourceRepo );
            List<ArtifactMetadata> conflictsArtifacts = new ArrayList<>();

            for ( ArtifactMetadata targetArtifact : targetArtifacts )
            {
                for ( ArtifactMetadata sourceArtifact : sourceArtifacts )
                {
                    if ( isEquals( targetArtifact, sourceArtifact ) )
                    {
                        if ( !conflictsArtifacts.contains( sourceArtifact ) )
                        {
                            conflictsArtifacts.add( sourceArtifact );
                        }
                    }
                }
            }

            sourceArtifacts.removeAll( conflictsArtifacts );

            return conflictsArtifacts;
        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
    }

    private boolean isEquals( ArtifactMetadata sourceArtifact, ArtifactMetadata targetArtifact )
    {
        boolean isSame = false;

        if ( ( sourceArtifact.getNamespace().equals( targetArtifact.getNamespace() ) )
            && ( sourceArtifact.getProject().equals( targetArtifact.getProject() ) ) && ( sourceArtifact.getId().equals(
            targetArtifact.getId() ) ) && ( sourceArtifact.getProjectVersion().equals(
            targetArtifact.getProjectVersion() ) ) )

        {
            isSame = true;

        }

        return isSame;
    }
}
