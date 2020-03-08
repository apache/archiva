package org.apache.archiva.repository.maven.merge;

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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.filter.Filter;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.RepositoryMetadataWriter;
import org.apache.archiva.repository.storage.fs.FilesystemAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.stagerepository.merge.RepositoryMerger;
import org.apache.archiva.stagerepository.merge.RepositoryMergerException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
@Service ("repositoryMerger#maven2")
public class Maven2RepositoryMerger
    implements RepositoryMerger
{

    @Inject
    @Named("metadataReader#maven")
    private MavenMetadataReader metadataReader;

    private static final Logger log = LoggerFactory.getLogger( Maven2RepositoryMerger.class );

    private static final Comparator<ArtifactMetadata> META_COMPARATOR = Comparator.comparing(ArtifactMetadata::getNamespace)
            .thenComparing(ArtifactMetadata::getProject)
            .thenComparing(ArtifactMetadata::getId)
            .thenComparing(ArtifactMetadata::getVersion);

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
    private RepositorySessionFactory repositorySessionFactory;

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
    public boolean supportsRepository( RepositoryType type )
    {
        return RepositoryType.MAVEN.equals( type );
    }

    @Override
    public void merge( MetadataRepository metadataRepository, String sourceRepoId, String targetRepoId )
        throws RepositoryMergerException
    {

        try(RepositorySession session = repositorySessionFactory.createSession())
        {
            List<ArtifactMetadata> artifactsInSourceRepo = metadataRepository.getArtifacts(session , sourceRepoId );
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
        try(RepositorySession session = repositorySessionFactory.createSession())
        {
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts(session , sourceRepoId );
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

        Path sourceArtifactFile = Paths.get( sourceRepoPath, artifactPath );

        Path targetArtifactFile = Paths.get( targetRepoPath, artifactPath );

        log.debug( "artifactPath {}", artifactPath );

        int lastIndex = artifactPath.lastIndexOf( RepositoryPathTranslator.PATH_SEPARATOR );

        Path targetFile = Paths.get( targetRepoPath, artifactPath.substring( 0, lastIndex ) );

        if ( !Files.exists(targetFile) )
        {
            // create the folder structure when it does not exist
            Files.createDirectories(targetFile);
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
        Path sourcePomFile = Paths.get( sourceRepoPath,
                                       artifactPath.substring( 0, lastIndex ) + "/" + artifactPath.substring(
                                           lastIndex + 1 ).substring( 0, last ) + ".pom" );
        Path targetPomFile = Paths.get( targetRepoPath,
                                       artifactPath.substring( 0, lastIndex ) + "/" + artifactPath.substring(
                                           lastIndex + 1 ).substring( 0, last ) + ".pom" );

        if ( !Files.exists(targetPomFile) && Files.exists(sourcePomFile) )
        {
            copyFile( sourcePomFile, targetPomFile );
        }

        // explicitly update only if metadata-updater consumer is not enabled!
        if ( !config.getRepositoryScanning().getKnownContentConsumers().contains( "metadata-updater" ) )
        {

            // updating version metadata files
            FilesystemStorage fsStorage = new FilesystemStorage(Paths.get(sourceRepoPath), new DefaultFileLockManager());

            StorageAsset versionMetaDataFileInSourceRepo =
                pathTranslator.toFile( new FilesystemAsset(fsStorage, "", Paths.get(sourceRepoPath)), artifactMetadata.getNamespace(),
                                       artifactMetadata.getProject(), artifactMetadata.getVersion(),
                                       METADATA_FILENAME );

            if ( versionMetaDataFileInSourceRepo.exists() )
            {//Pattern quote for windows path
                String relativePathToVersionMetadataFile =
                        getRelativeAssetPath(versionMetaDataFileInSourceRepo);
                Path versionMetaDataFileInTargetRepo = Paths.get( targetRepoPath, relativePathToVersionMetadataFile );

                if ( !Files.exists(versionMetaDataFileInTargetRepo) )
                {
                    copyFile( versionMetaDataFileInSourceRepo.getFilePath(), versionMetaDataFileInTargetRepo );
                }
                else
                {
                    updateVersionMetadata( versionMetaDataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp );

                }
            }

            // updating project meta data file
            StorageAsset projectDirectoryInSourceRepo = versionMetaDataFileInSourceRepo.getParent().getParent();
            StorageAsset projectMetadataFileInSourceRepo = projectDirectoryInSourceRepo.resolve(METADATA_FILENAME );

            if ( projectMetadataFileInSourceRepo.exists() )
            {
                String relativePathToProjectMetadataFile =
                        getRelativeAssetPath(projectMetadataFileInSourceRepo);
                Path projectMetadataFileInTargetRepo = Paths.get( targetRepoPath, relativePathToProjectMetadataFile );

                if ( !Files.exists(projectMetadataFileInTargetRepo) )
                {

                    copyFile( projectMetadataFileInSourceRepo.getFilePath(), projectMetadataFileInTargetRepo );
                }
                else
                {
                    updateProjectMetadata( projectMetadataFileInTargetRepo, artifactMetadata, lastUpdatedTimestamp,
                                           timestamp );
                }
            }
        }

    }

    private String getRelativeAssetPath(final StorageAsset asset) {
        String relPath = asset.getPath();
        while(relPath.startsWith("/")) {
            relPath = relPath.substring(1);
        }
        return relPath;
    }

    private void copyFile( Path sourceFile, Path targetFile )
        throws IOException
    {

        FileUtils.copyFile( sourceFile.toFile(), targetFile.toFile() );

    }

    private void updateProjectMetadata( Path projectMetaDataFileIntargetRepo, ArtifactMetadata artifactMetadata,
                                        Date lastUpdatedTimestamp, String timestamp )
        throws RepositoryMetadataException
    {
        ArrayList<String> availableVersions = new ArrayList<>();
        String latestVersion = artifactMetadata.getProjectVersion();

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetaDataFileIntargetRepo );

        if ( Files.exists(projectMetaDataFileIntargetRepo) )
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

        try(BufferedWriter writer = Files.newBufferedWriter(projectMetaDataFileIntargetRepo)) {
            RepositoryMetadataWriter.write( projectMetadata, writer );
        } catch (IOException e) {
            throw new RepositoryMetadataException(e);
        }

    }

    private void updateVersionMetadata( Path versionMetaDataFileInTargetRepo, ArtifactMetadata artifactMetadata,
                                        Date lastUpdatedTimestamp )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata versionMetadata = getMetadata( versionMetaDataFileInTargetRepo );
        if ( !Files.exists(versionMetaDataFileInTargetRepo) )
        {
            versionMetadata.setGroupId( artifactMetadata.getNamespace() );
            versionMetadata.setArtifactId( artifactMetadata.getProject() );
            versionMetadata.setVersion( artifactMetadata.getProjectVersion() );
        }

        versionMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        try(BufferedWriter writer = Files.newBufferedWriter(versionMetaDataFileInTargetRepo) ) {
            RepositoryMetadataWriter.write( versionMetadata, writer);
        } catch (IOException e) {
            throw new RepositoryMetadataException(e);
        }
    }

    private ArchivaRepositoryMetadata getMetadata( Path metadataFile )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if ( Files.exists(metadataFile) )
        {
            metadata = metadataReader.read( metadataFile );
        }
        return metadata;
    }

    @Override
    public List<ArtifactMetadata> getConflictingArtifacts( MetadataRepository metadataRepository, String sourceRepo,
                                                           String targetRepo )
        throws RepositoryMergerException
    {
        try(RepositorySession session = repositorySessionFactory.createSession())
        {
            TreeSet<ArtifactMetadata> targetArtifacts = new TreeSet<>(META_COMPARATOR);
            targetArtifacts.addAll(metadataRepository.getArtifacts(session , targetRepo ));
            TreeSet<ArtifactMetadata> sourceArtifacts = new TreeSet<>(META_COMPARATOR);
            sourceArtifacts.addAll(metadataRepository.getArtifacts(session , sourceRepo ));
            sourceArtifacts.retainAll(targetArtifacts);

            return new ArrayList<>(sourceArtifacts);
        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryMergerException( e.getMessage(), e );
        }
    }

    public RepositorySessionFactory getRepositorySessionFactory( )
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }
}
