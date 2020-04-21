package org.apache.archiva.rest.services;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.maven.model.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.storage.fs.FsStorageUtil;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.RepositoryMetadataWriter;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.archiva.repository.scanner.RepositoryScannerInstance;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.model.StringList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.maven.ArchivaIndexingTaskExecutor;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service("repositoriesService#rest")
public class DefaultRepositoriesService
    extends AbstractRestService
    implements RepositoriesService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "taskExecutor#indexing")
    private ArchivaIndexingTaskExecutor archivaIndexingTaskExecutor;

    @Inject
    private RepositoryRegistry repositoryRegistry;

    @Inject
    private SecuritySystem securitySystem;

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private ArchivaTaskScheduler<RepositoryTask> scheduler;

    @Inject
    private DownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    @Inject
    @Named(value = "repositorySessionFactory")
    protected RepositorySessionFactory repositorySessionFactory;

    @Inject
    @Autowired(required = false)
    protected List<RepositoryListener> listeners = new ArrayList<RepositoryListener>();

    @Inject
    private RepositoryScanner repoScanner;

    /**
     * Cache used for namespaces
     */
    @Inject
    @Named(value = "cache#namespaces")
    private Cache<String, Collection<String>> namespacesCache;

    private List<ChecksumAlgorithm> algorithms = Arrays.asList(ChecksumAlgorithm.SHA256, ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 );

    @Override
    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        return doScanRepository( repositoryId, fullScan );
    }

    @Override
    public Boolean alreadyScanning( String repositoryId )
    {
        // check queue first to make sure it doesn't get dequeued between calls
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            return true;
        }
        for ( RepositoryScannerInstance scan : repoScanner.getInProgressScans() )
        {
            if ( scan.getRepository().getId().equals( repositoryId ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean removeScanningTaskFromQueue( String repositoryId )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        try
        {
            return repositoryTaskScheduler.unQueueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to unschedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
    }

    private ManagedRepositoryContent getManagedRepositoryContent(String id) throws RepositoryException
    {
        org.apache.archiva.repository.ManagedRepository repo = repositoryRegistry.getManagedRepository( id );
        if (repo==null) {
            throw new RepositoryException( "Repository not found "+id );
        }
        return repo.getContent();
    }

    @Override
    public Boolean scanRepositoryNow( String repositoryId, boolean fullScan )
        throws ArchivaRestServiceException
    {

        try
        {

            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );


            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, repository.getIndexingContext() );

            task.setExecuteOnEntireRepo( true );
            task.setOnlyUpdate( !fullScan );

            archivaIndexingTaskExecutor.executeTask( task );

            scheduler.queueTask( new RepositoryTask( repositoryId, fullScan ) );

            return Boolean.TRUE;
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean scheduleDownloadRemoteIndex( String repositoryId, boolean now, boolean fullDownload )
        throws ArchivaRestServiceException
    {
        try
        {
            downloadRemoteIndexScheduler.scheduleDownloadRemote( repositoryId, now, fullDownload );
        }
        catch ( DownloadRemoteIndexException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean copyArtifact( ArtifactTransferRequest artifactTransferRequest )
        throws ArchivaRestServiceException
    {
        // check parameters
        String userName = getAuditInformation().getUser().getUsername();
        if ( StringUtils.isBlank( userName ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: userName not found", null );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getRepositoryId() ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: sourceRepositoryId cannot be null", null );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getTargetRepositoryId() ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: targetRepositoryId cannot be null", null );
        }

        ManagedRepository source = null;
        source = repositoryRegistry.getManagedRepository( artifactTransferRequest.getRepositoryId() );

        if ( source == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getRepositoryId(), null );
        }

        ManagedRepository target = null;
        target = repositoryRegistry.getManagedRepository( artifactTransferRequest.getTargetRepositoryId() );

        if ( target == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getTargetRepositoryId(), null );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getGroupId() ) )
        {
            throw new ArchivaRestServiceException( "groupId is mandatory", null );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getArtifactId() ) )
        {
            throw new ArchivaRestServiceException( "artifactId is mandatory", null );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getVersion() ) )
        {
            throw new ArchivaRestServiceException( "version is mandatory", null );
        }

        if ( VersionUtil.isSnapshot( artifactTransferRequest.getVersion() ) )
        {
            throw new ArchivaRestServiceException( "copy of SNAPSHOT not supported", null );
        }

        // end check parameters

        User user = null;
        try
        {
            user = securitySystem.getUserManager().findUser( userName );
        }
        catch ( UserNotFoundException e )
        {
            throw new ArchivaRestServiceException( "user " + userName + " not found", e );
        }
        catch ( UserManagerException e )
        {
            throw new ArchivaRestServiceException( "ArchivaRestServiceException:" + e.getMessage(), e );
        }

        // check karma on source : read
        AuthenticationResult authn = new AuthenticationResult( true, userName, null );
        SecuritySession securitySession = new DefaultSecuritySession( authn, user );
        try
        {
            boolean authz =
                securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS,
                                             artifactTransferRequest.getRepositoryId() );
            if ( !authz )
            {
                throw new ArchivaRestServiceException(
                    "not authorized to access repo:" + artifactTransferRequest.getRepositoryId(), null );
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "error reading permission: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

        // check karma on target: write
        try
        {
            boolean authz =
                securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD,
                                             artifactTransferRequest.getTargetRepositoryId() );
            if ( !authz )
            {
                throw new ArchivaRestServiceException(
                    "not authorized to write to repo:" + artifactTransferRequest.getTargetRepositoryId(), null );
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "error reading permission: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

        // sounds good we can continue !

        ArtifactReference artifactReference = new ArtifactReference();
        artifactReference.setArtifactId( artifactTransferRequest.getArtifactId() );
        artifactReference.setGroupId( artifactTransferRequest.getGroupId() );
        artifactReference.setVersion( artifactTransferRequest.getVersion() );
        artifactReference.setClassifier( artifactTransferRequest.getClassifier() );
        String packaging = StringUtils.trim( artifactTransferRequest.getPackaging() );
        artifactReference.setType( StringUtils.isEmpty( packaging ) ? "jar" : packaging );

        try
        {

            ManagedRepositoryContent sourceRepository =
                getManagedRepositoryContent( artifactTransferRequest.getRepositoryId() );

            String artifactSourcePath = sourceRepository.toPath( artifactReference );

            if ( StringUtils.isEmpty( artifactSourcePath ) )
            {
                log.error( "cannot find artifact {}", artifactTransferRequest );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString(),
                                                       null );
            }

            StorageAsset artifactFile = source.getAsset( artifactSourcePath );

            if ( !artifactFile.exists() )
            {
                log.error( "cannot find artifact {}", artifactTransferRequest );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString(),
                                                       null );
            }

            ManagedRepositoryContent targetRepository =
                getManagedRepositoryContent( artifactTransferRequest.getTargetRepositoryId() );

            String artifactPath = targetRepository.toPath( artifactReference );

            int lastIndex = artifactPath.lastIndexOf( '/' );

            String path = artifactPath.substring( 0, lastIndex );
            StorageAsset targetDir = target.getAsset( path );

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = 1;
            String timestamp = null;

            StorageAsset versionMetadataFile = target.getAsset(path + "/" + MetadataTools.MAVEN_METADATA );
            /* unused */ getMetadata( targetRepository.getRepository().getType(), versionMetadataFile );

            if ( !targetDir.exists() )
            {
                targetDir = target.addAsset(targetDir.getPath(), true);
                targetDir.create();
            }

            String filename = artifactPath.substring( lastIndex + 1 );

            boolean fixChecksums =
                !( archivaAdministration.getKnownContentConsumers().contains( "create-missing-checksums" ) );

            StorageAsset targetFile = target.getAsset(targetDir.getPath() + "/" + filename );
            if ( targetFile.exists() && target.blocksRedeployments())
            {
                throw new ArchivaRestServiceException(
                    "artifact already exists in target repo: " + artifactTransferRequest.getTargetRepositoryId()
                        + " and redeployment blocked", null
                );
            }
            else
            {
                copyFile(artifactFile, targetFile, fixChecksums );
                queueRepositoryTask( target.getId(), targetFile );
            }

            // copy source pom to target repo
            String pomFilename = filename;
            if ( StringUtils.isNotBlank( artifactTransferRequest.getClassifier() ) )
            {
                pomFilename = StringUtils.remove( pomFilename, "-" + artifactTransferRequest.getClassifier() );
            }
            pomFilename = FilenameUtils.removeExtension( pomFilename ) + ".pom";

            StorageAsset pomFile = source.getAsset(
                artifactSourcePath.substring( 0, artifactPath.lastIndexOf( '/' ) )+"/"+ pomFilename );

            if ( pomFile != null && pomFile.exists() )
            {
                StorageAsset targetPomFile = target.getAsset( targetDir.getPath() + "/" + pomFilename );
                copyFile(pomFile, targetPomFile, fixChecksums );
                queueRepositoryTask( target.getId(), targetPomFile );


            }

            // explicitly update only if metadata-updater consumer is not enabled!
            if ( !archivaAdministration.getKnownContentConsumers().contains( "metadata-updater" ) )
            {
                updateProjectMetadata( target.getType(), target, targetDir, lastUpdatedTimestamp, timestamp, newBuildNumber,
                                       fixChecksums, artifactTransferRequest );


            }

            String msg =
                "Artifact \'" + artifactTransferRequest.getGroupId() + ":" + artifactTransferRequest.getArtifactId()
                    + ":" + artifactTransferRequest.getVersion() + "\' was successfully deployed to repository \'"
                    + artifactTransferRequest.getTargetRepositoryId() + "\'";
            log.debug("copyArtifact {}", msg);

        }
        catch ( RepositoryException e )
        {
            log.error( "RepositoryException: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "RepositoryAdminException: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            log.error( "IOException: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        return true;
    }

    private void queueRepositoryTask( String repositoryId, StorageAsset localFile )
    {

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setResourceFile( localFile );
        task.setUpdateRelatedArtifacts( true );
        //task.setScanAll( true );

        try
        {
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Unable to queue repository task to execute consumers on resource file ['{}"
                           + "'].", localFile.getName());
        }
    }

    private ArchivaRepositoryMetadata getMetadata( RepositoryType repositoryType, StorageAsset metadataFile )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if ( metadataFile.exists() )
        {
            metadata = repositoryRegistry.getMetadataReader( repositoryType ).read( metadataFile );
        }
        return metadata;
    }

    private StorageAsset getMetadata( RepositoryStorage storage, String targetPath )
    {
        return storage.getAsset( targetPath + "/" + MetadataTools.MAVEN_METADATA );

    }

    /*
     * Copies the asset to the new target.
     */
    private void copyFile(StorageAsset sourceFile, StorageAsset targetPath, boolean fixChecksums)
        throws IOException
    {

        FsStorageUtil.copyAsset( sourceFile, targetPath, true );
        if ( fixChecksums )
        {
            fixChecksums( targetPath );
        }
    }

    private void fixChecksums( StorageAsset file )
    {
        Path destinationFile = file.getFilePath();
        if (destinationFile!=null)
        {
            ChecksummedFile checksum = new ChecksummedFile( destinationFile );
            checksum.fixChecksums( algorithms );
        }
    }

    private void updateProjectMetadata( RepositoryType repositoryType, RepositoryStorage storage, StorageAsset targetPath, Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                        boolean fixChecksums, ArtifactTransferRequest artifactTransferRequest )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<>();
        String latestVersion = artifactTransferRequest.getVersion();

        StorageAsset projectDir = targetPath.getParent();
        StorageAsset projectMetadataFile = storage.getAsset( projectDir.getPath()+"/"+MetadataTools.MAVEN_METADATA );

        ArchivaRepositoryMetadata projectMetadata = getMetadata( repositoryType, projectMetadataFile );

        if ( projectMetadataFile.exists() )
        {
            availableVersions = projectMetadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( artifactTransferRequest.getVersion() ) )
            {
                availableVersions.add( artifactTransferRequest.getVersion() );
            }

            latestVersion = availableVersions.get( availableVersions.size() - 1 );
        }
        else
        {
            availableVersions.add( artifactTransferRequest.getVersion() );

            projectMetadata.setGroupId( artifactTransferRequest.getGroupId() );
            projectMetadata.setArtifactId( artifactTransferRequest.getArtifactId() );
        }

        if ( projectMetadata.getGroupId() == null )
        {
            projectMetadata.setGroupId( artifactTransferRequest.getGroupId() );
        }

        if ( projectMetadata.getArtifactId() == null )
        {
            projectMetadata.setArtifactId( artifactTransferRequest.getArtifactId() );
        }

        projectMetadata.setLatestVersion( latestVersion );
        projectMetadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        projectMetadata.setAvailableVersions( availableVersions );

        if ( !VersionUtil.isSnapshot( artifactTransferRequest.getVersion() ) )
        {
            projectMetadata.setReleasedVersion( latestVersion );
        }

        try(OutputStreamWriter writer = new OutputStreamWriter(projectMetadataFile.getWriteStream(true))) {
            RepositoryMetadataWriter.write(projectMetadata, writer);
        } catch (IOException e) {
            throw new RepositoryMetadataException(e);
        }

        if ( fixChecksums )
        {
            fixChecksums( projectMetadataFile );
        }
    }

    @Override
    public Boolean removeProjectVersion( String repositoryId, String namespace, String projectId, String version )
        throws ArchivaRestServiceException
    {
        // if not a generic we can use the standard way to delete artifact
        if ( !VersionUtil.isGenericSnapshot( version ) )
        {
            Artifact artifact = new Artifact( namespace, projectId, version );
            artifact.setRepositoryId( repositoryId );
            artifact.setContext( repositoryId );
            return deleteArtifact( artifact );
        }

        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "repositoryId cannot be null", 400, null );
        }

        if ( !isAuthorizedToDeleteArtifacts( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "not authorized to delete artifacts", 403, null );
        }

        if ( StringUtils.isEmpty( namespace ) )
        {
            throw new ArchivaRestServiceException( "groupId cannot be null", 400, null );
        }

        if ( StringUtils.isEmpty( projectId ) )
        {
            throw new ArchivaRestServiceException( "artifactId cannot be null", 400, null );
        }

        if ( StringUtils.isEmpty( version ) )
        {
            throw new ArchivaRestServiceException( "version cannot be null", 400, null );
        }

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            ManagedRepositoryContent repository = getManagedRepositoryContent( repositoryId );

            VersionedReference ref = new VersionedReference();
            ref.setArtifactId( projectId );
            ref.setGroupId( namespace );
            ref.setVersion( version );

            repository.deleteVersion( ref );


            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setGroupId( namespace );
            artifactReference.setArtifactId( projectId );
            artifactReference.setVersion( version );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            List<ArtifactReference> related = repository.getRelatedArtifacts( repository.toVersion(artifactReference) );
            log.debug( "related: {}", related );
            for ( ArtifactReference artifactRef : related )
            {
                repository.deleteArtifact( artifactRef );
            }

            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts(repositorySession , repositoryId, namespace, projectId, version );

            for ( ArtifactMetadata artifactMetadata : artifacts )
            {
                metadataRepository.removeTimestampedArtifact(repositorySession , artifactMetadata, version );
            }

            metadataRepository.removeProjectVersion(repositorySession , repositoryId, namespace, projectId, version );
        }
        catch ( MetadataRepositoryException | MetadataResolutionException | RepositoryException | LayoutException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            try {
                repositorySession.save();
            } catch (MetadataSessionException e) {
                log.error("Session save failed {}", e.getMessage());
            }

            repositorySession.close();
        }


        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteArtifact( Artifact artifact )
        throws ArchivaRestServiceException
    {

        String repositoryId = artifact.getContext();
        // some rest call can use context or repositoryId
        // so try both!!
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            repositoryId = artifact.getRepositoryId();
        }
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "repositoryId cannot be null", 400, null );
        }

        if ( !isAuthorizedToDeleteArtifacts( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "not authorized to delete artifacts", 403, null );
        }

        if ( artifact == null )
        {
            throw new ArchivaRestServiceException( "artifact cannot be null", 400, null );
        }

        if ( StringUtils.isEmpty( artifact.getGroupId() ) )
        {
            throw new ArchivaRestServiceException( "artifact.groupId cannot be null", 400, null );
        }

        if ( StringUtils.isEmpty( artifact.getArtifactId() ) )
        {
            throw new ArchivaRestServiceException( "artifact.artifactId cannot be null", 400, null );
        }

        // TODO more control on artifact fields

        boolean snapshotVersion =
            VersionUtil.isSnapshot( artifact.getVersion() ) | VersionUtil.isGenericSnapshot( artifact.getVersion() );

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();

            TimeZone timezone = TimeZone.getTimeZone( "UTC" );
            DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
            fmt.setTimeZone( timezone );
            ManagedRepository repo = repositoryRegistry.getManagedRepository( repositoryId );

            VersionedReference ref = new VersionedReference();
            ref.setArtifactId( artifact.getArtifactId() );
            ref.setGroupId( artifact.getGroupId() );
            ref.setVersion( artifact.getVersion() );

            ManagedRepositoryContent repository = getManagedRepositoryContent( repositoryId );

            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setArtifactId( artifact.getArtifactId() );
            artifactReference.setGroupId( artifact.getGroupId() );
            artifactReference.setVersion( artifact.getVersion() );
            artifactReference.setClassifier( artifact.getClassifier() );
            artifactReference.setType( artifact.getType() );

            ArchivaItemSelector selector = ArchivaItemSelector.builder( )
                .withNamespace( artifact.getGroupId( ) )
                .withProjectId( artifact.getArtifactId( ) )
                .withVersion( artifact.getVersion( ) )
                .withClassifier( artifact.getClassifier( ) )
                .withArtifactId( artifact.getArtifactId( ) )
                .withType( artifact.getType( ) )
                .includeRelatedArtifacts()
                .build( );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            String path = repository.toMetadataPath( ref );

            if ( StringUtils.isNotBlank( artifact.getClassifier() ) )
            {
                if ( StringUtils.isBlank( artifact.getPackaging() ) )
                {
                    throw new ArchivaRestServiceException( "You must configure a type/packaging when using classifier",
                                                           400, null );
                }
                List<? extends org.apache.archiva.repository.content.Artifact> artifactItems = repository.getArtifacts( selector );
                for ( org.apache.archiva.repository.content.Artifact aRef : artifactItems ) {
                    try
                    {
                        repository.deleteItem( aRef );
                    }
                    catch ( ItemNotFoundException e )
                    {
                        log.error( "Could not delete item, seems to be deleted by other thread. {}, {} ", aRef, e.getMessage( ) );
                    }
                }

            }
            else
            {

                int index = path.lastIndexOf( '/' );
                path = path.substring( 0, index );
                StorageAsset targetPath = repo.getAsset( path );

                if ( !targetPath.exists() )
                {
                    //throw new ContentNotFoundException(
                    //    artifact.getNamespace() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() );
                    log.warn( "targetPath {} not found skip file deletion", targetPath );
                    return false;
                }

                // TODO: this should be in the storage mechanism so that it is all tied together
                // delete from file system
                if ( !snapshotVersion )
                {
                    repository.deleteVersion( ref );
                }
                else
                {
                    // We are deleting all version related artifacts for a snapshot version
                    VersionedReference versionRef = repository.toVersion( artifactReference );
                    List<ArtifactReference> related = repository.getRelatedArtifacts( versionRef );
                    log.debug( "related: {}", related );
                    for ( ArtifactReference artifactRef : related )
                    {
                        try
                        {
                            repository.deleteArtifact( artifactRef );
                        } catch (ContentNotFoundException e) {
                            log.warn( "Artifact that should be deleted, was not found: {}", artifactRef );
                        }
                    }
                    StorageAsset metadataFile = getMetadata( repo, targetPath.getPath() );
                    ArchivaRepositoryMetadata metadata = getMetadata( repository.getRepository().getType(), metadataFile );

                    updateMetadata( metadata, metadataFile, lastUpdatedTimestamp, artifact );
                }
            }
            Collection<ArtifactMetadata> artifacts = Collections.emptyList();

            if ( snapshotVersion )
            {
                String baseVersion = VersionUtil.getBaseVersion( artifact.getVersion() );
                artifacts =
                    metadataRepository.getArtifacts(repositorySession , repositoryId, artifact.getGroupId(),
                        artifact.getArtifactId(), baseVersion );
            }
            else
            {
                artifacts =
                    metadataRepository.getArtifacts(repositorySession , repositoryId, artifact.getGroupId(),
                        artifact.getArtifactId(), artifact.getVersion() );
            }

            log.debug( "artifacts: {}", artifacts );

            if ( artifacts.isEmpty() )
            {
                if ( !snapshotVersion )
                {
                    // verify metata repository doesn't contains anymore the version
                    Collection<String> projectVersions =
                        metadataRepository.getProjectVersions(repositorySession , repositoryId,
                            artifact.getGroupId(), artifact.getArtifactId() );

                    if ( projectVersions.contains( artifact.getVersion() ) )
                    {
                        log.warn( "artifact not found when deleted but version still here ! so force cleanup" );
                        metadataRepository.removeProjectVersion(repositorySession , repositoryId,
                            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
                    }

                }
            }

            for ( ArtifactMetadata artifactMetadata : artifacts )
            {

                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                if ( artifactMetadata.getVersion().equals( artifact.getVersion() ) )
                {
                    if ( StringUtils.isNotBlank( artifact.getClassifier() ) )
                    {
                        if ( StringUtils.isBlank( artifact.getPackaging() ) )
                        {
                            throw new ArchivaRestServiceException(
                                "You must configure a type/packaging when using classifier", 400, null );
                        }
                        // cleanup facet which contains classifier information
                        MavenArtifactFacet mavenArtifactFacet =
                            (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );

                        if ( StringUtils.equals( artifact.getClassifier(), mavenArtifactFacet.getClassifier() ) )
                        {
                            artifactMetadata.removeFacet( MavenArtifactFacet.FACET_ID );
                            String groupId = artifact.getGroupId(), artifactId = artifact.getArtifactId(), version =
                                artifact.getVersion();
                            MavenArtifactFacet mavenArtifactFacetToCompare = new MavenArtifactFacet();
                            mavenArtifactFacetToCompare.setClassifier( artifact.getClassifier() );
                            metadataRepository.removeFacetFromArtifact(repositorySession , repositoryId, groupId, artifactId,
                                version, mavenArtifactFacetToCompare );
                            repositorySession.save();
                        }

                    }
                    else
                    {
                        if ( snapshotVersion )
                        {
                            metadataRepository.removeTimestampedArtifact(repositorySession ,
                                artifactMetadata, VersionUtil.getBaseVersion( artifact.getVersion() ) );
                        }
                        else
                        {
                            metadataRepository.removeArtifact(repositorySession ,
                                artifactMetadata.getRepositoryId(),
                                artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                artifact.getVersion(), artifactMetadata.getId() );
                        }
                    }
                    // TODO: move into the metadata repository proper - need to differentiate attachment of
                    //       repository metadata to an artifact
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.deleteArtifact( metadataRepository, repository.getId(),
                                                 artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                                 artifactMetadata.getVersion(), artifactMetadata.getId() );
                    }

                    triggerAuditEvent( repositoryId, path, AuditEvent.REMOVE_FILE );
                }
            }
        }
        catch ( ContentNotFoundException e )
        {
            throw new ArchivaRestServiceException( "Artifact does not exist: " + e.getMessage(), 400, e );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new ArchivaRestServiceException( "Target repository cannot be found: " + e.getMessage(), 400, e );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch (MetadataResolutionException | MetadataSessionException | MetadataRepositoryException | LayoutException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            try {
                repositorySession.save();
            } catch (MetadataSessionException e) {
                log.error("Could not save sesion {}", e.getMessage());
            }

            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteGroupId( String groupId, String repositoryId )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "repositoryId cannot be null", 400, null );
        }

        if ( !isAuthorizedToDeleteArtifacts( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "not authorized to delete artifacts", 403, null );
        }

        if ( StringUtils.isEmpty( groupId ) )
        {
            throw new ArchivaRestServiceException( "groupId cannot be null", 400, null );
        }

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            ManagedRepositoryContent repository = getManagedRepositoryContent( repositoryId );

            repository.deleteGroupId( groupId );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.removeNamespace(repositorySession , repositoryId, groupId );

            // just invalidate cache entry
            String cacheKey = repositoryId + "-" + groupId;
            namespacesCache.remove( cacheKey );
            namespacesCache.remove( repositoryId );

            repositorySession.save();
        }
        catch (MetadataRepositoryException | MetadataSessionException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            repositorySession.close();
        }
        return true;
    }

    @Override
    public Boolean deleteProject( String groupId, String projectId, String repositoryId )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "repositoryId cannot be null", 400, null );
        }

        if ( !isAuthorizedToDeleteArtifacts( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "not authorized to delete artifacts", 403, null );
        }

        if ( StringUtils.isEmpty( groupId ) )
        {
            throw new ArchivaRestServiceException( "groupId cannot be null", 400, null );
        }

        if ( StringUtils.isEmpty( projectId ) )
        {
            throw new ArchivaRestServiceException( "artifactId cannot be null", 400, null );
        }

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            ManagedRepositoryContent repository = getManagedRepositoryContent( repositoryId );

            repository.deleteProject( groupId, projectId );
        }
        catch ( ContentNotFoundException e )
        {
            log.warn( "skip ContentNotFoundException: {}", e.getMessage() );
        }
        catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }

        try
        {

            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.removeProject(repositorySession , repositoryId, groupId, projectId );

            repositorySession.save();
        }
        catch (MetadataRepositoryException | MetadataSessionException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            repositorySession.close();
        }
        return true;

    }

    @Override
    public Boolean isAuthorizedToDeleteArtifacts( String repoId )
        throws ArchivaRestServiceException
    {
        String userName =
            getAuditInformation().getUser() == null ? "guest" : getAuditInformation().getUser().getUsername();

        try
        {
            return userRepositories.isAuthorizedToDeleteArtifacts( userName, repoId );
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }

    @Override
    public RepositoryScanStatistics scanRepositoryDirectoriesNow( String repositoryId )
        throws ArchivaRestServiceException
    {
        long sinceWhen = RepositoryScanner.FRESH_SCAN;
        try
        {
            return repoScanner.scan( repositoryRegistry.getManagedRepository( repositoryId ), sinceWhen );
        }
        catch ( RepositoryScannerException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "RepositoryScannerException exception: " + e.getMessage(), 500, e );
        }
    }

    /**
     * Update artifact level metadata. Creates one if metadata does not exist after artifact deletion.
     *
     * @param metadata
     */
    private void updateMetadata( ArchivaRepositoryMetadata metadata, StorageAsset metadataFile, Date lastUpdatedTimestamp,
                                 Artifact artifact )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<>();
        String latestVersion = "";

        if ( metadataFile.exists() )
        {
            if ( metadata.getAvailableVersions() != null )
            {
                availableVersions = metadata.getAvailableVersions();

                if ( availableVersions.size() > 0 )
                {
                    Collections.sort( availableVersions, VersionComparator.getInstance() );

                    if ( availableVersions.contains( artifact.getVersion() ) )
                    {
                        availableVersions.remove( availableVersions.indexOf( artifact.getVersion() ) );
                    }
                    if ( availableVersions.size() > 0 )
                    {
                        latestVersion = availableVersions.get( availableVersions.size() - 1 );
                    }
                }
            }
        }

        if ( metadata.getGroupId() == null )
        {
            metadata.setGroupId( artifact.getGroupId() );
        }
        if ( metadata.getArtifactId() == null )
        {
            metadata.setArtifactId( artifact.getArtifactId() );
        }

        if ( !VersionUtil.isSnapshot( artifact.getVersion() ) )
        {
            if ( metadata.getReleasedVersion() != null && metadata.getReleasedVersion().equals(
                artifact.getVersion() ) )
            {
                metadata.setReleasedVersion( latestVersion );
            }
        }

        metadata.setLatestVersion( latestVersion );
        metadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        metadata.setAvailableVersions( availableVersions );

        try (OutputStreamWriter writer = new OutputStreamWriter(metadataFile.getWriteStream(true))) {
            RepositoryMetadataWriter.write(metadata, writer);
        } catch (IOException e) {
            throw new RepositoryMetadataException(e);
        }
        ChecksummedFile checksum = new ChecksummedFile( metadataFile.getFilePath() );
        checksum.fixChecksums( algorithms );
    }

    @Override
    public StringList getRunningRemoteDownloadIds()
    {
        return new StringList( downloadRemoteIndexScheduler.getRunningRemoteDownloadIds() );
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    public List<RepositoryListener> getListeners()
    {
        return listeners;
    }

    public void setListeners( List<RepositoryListener> listeners )
    {
        this.listeners = listeners;
    }

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}


