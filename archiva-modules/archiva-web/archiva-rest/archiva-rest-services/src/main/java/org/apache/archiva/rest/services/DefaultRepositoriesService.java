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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.model.maven2.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.archiva.repository.scanner.RepositoryScannerInstance;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.model.StringList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArchivaIndexingTaskExecutor;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.context.IndexingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private SecuritySystem securitySystem;

    @Inject
    private RepositoryContentFactory repositoryFactory;

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private ArchivaTaskScheduler scheduler;

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

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

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

    @Override
    public Boolean scanRepositoryNow( String repositoryId, boolean fullScan )
        throws ArchivaRestServiceException
    {

        try
        {
            ManagedRepository repository = managedRepositoryAdmin.getManagedRepository( repositoryId );

            IndexingContext context = managedRepositoryAdmin.createIndexContext( repository );

            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, context );

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
        try
        {
            source = managedRepositoryAdmin.getManagedRepository( artifactTransferRequest.getRepositoryId() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

        if ( source == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getRepositoryId(), null );
        }

        ManagedRepository target = null;
        try
        {
            target = managedRepositoryAdmin.getManagedRepository( artifactTransferRequest.getTargetRepositoryId() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

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
                repositoryFactory.getManagedRepositoryContent( artifactTransferRequest.getRepositoryId() );

            String artifactSourcePath = sourceRepository.toPath( artifactReference );

            if ( StringUtils.isEmpty( artifactSourcePath ) )
            {
                log.error( "cannot find artifact {}", artifactTransferRequest );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString(),
                                                       null );
            }

            Path artifactFile = Paths.get( source.getLocation(), artifactSourcePath );

            if ( !Files.exists(artifactFile) )
            {
                log.error( "cannot find artifact {}", artifactTransferRequest );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString(),
                                                       null );
            }

            ManagedRepositoryContent targetRepository =
                repositoryFactory.getManagedRepositoryContent( artifactTransferRequest.getTargetRepositoryId() );

            String artifactPath = targetRepository.toPath( artifactReference );

            int lastIndex = artifactPath.lastIndexOf( '/' );

            String path = artifactPath.substring( 0, lastIndex );
            Path targetPath = Paths.get( target.getLocation(), path );

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = 1;
            String timestamp = null;

            Path versionMetadataFile = targetPath.resolve( MetadataTools.MAVEN_METADATA );
            /* unused */ getMetadata( versionMetadataFile );

            if ( !Files.exists(targetPath) )
            {
                Files.createDirectories( targetPath );
            }

            String filename = artifactPath.substring( lastIndex + 1 );

            boolean fixChecksums =
                !( archivaAdministration.getKnownContentConsumers().contains( "create-missing-checksums" ) );

            Path targetFile = targetPath.resolve( filename );
            if ( Files.exists(targetFile) && target.isBlockRedeployments() )
            {
                throw new ArchivaRestServiceException(
                    "artifact already exists in target repo: " + artifactTransferRequest.getTargetRepositoryId()
                        + " and redeployment blocked", null
                );
            }
            else
            {
                copyFile( artifactFile, targetPath, filename, fixChecksums );
                queueRepositoryTask( target.getId(), targetFile );
            }

            // copy source pom to target repo
            String pomFilename = filename;
            if ( StringUtils.isNotBlank( artifactTransferRequest.getClassifier() ) )
            {
                pomFilename = StringUtils.remove( pomFilename, "-" + artifactTransferRequest.getClassifier() );
            }
            pomFilename = FilenameUtils.removeExtension( pomFilename ) + ".pom";

            Path pomFile = Paths.get(source.getLocation(),
                artifactSourcePath.substring( 0, artifactPath.lastIndexOf( '/' ) ) ,
                pomFilename );

            if ( pomFile != null && Files.size( pomFile ) > 0 )
            {
                copyFile( pomFile, targetPath, pomFilename, fixChecksums );
                queueRepositoryTask( target.getId(), targetPath.resolve( pomFilename ) );


            }

            // explicitly update only if metadata-updater consumer is not enabled!
            if ( !archivaAdministration.getKnownContentConsumers().contains( "metadata-updater" ) )
            {
                updateProjectMetadata( targetPath.toAbsolutePath().toString(), lastUpdatedTimestamp, timestamp, newBuildNumber,
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

    private void queueRepositoryTask( String repositoryId, Path localFile )
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
                           + "'].", localFile.getFileName());
        }
    }

    private ArchivaRepositoryMetadata getMetadata( Path metadataFile )
        throws RepositoryMetadataException
    {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if ( Files.exists(metadataFile) )
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

    private Path getMetadata( String targetPath )
    {
        String artifactPath = targetPath.substring( 0, targetPath.lastIndexOf( FileSystems.getDefault().getSeparator() ));

        return Paths.get( artifactPath, MetadataTools.MAVEN_METADATA );
    }

    private void copyFile( Path sourceFile, Path targetPath, String targetFilename, boolean fixChecksums )
        throws IOException
    {
        Files.copy( sourceFile, targetPath.resolve( targetFilename ), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES );

        if ( fixChecksums )
        {
            fixChecksums( targetPath.resolve( targetFilename ) );
        }
    }

    private void fixChecksums( Path file )
    {
        ChecksummedFile checksum = new ChecksummedFile( file );
        checksum.fixChecksums( algorithms );
    }

    private void updateProjectMetadata( String targetPath, Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                        boolean fixChecksums, ArtifactTransferRequest artifactTransferRequest )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<>();
        String latestVersion = artifactTransferRequest.getVersion();

        Path projectDir = Paths.get( targetPath ).getParent();
        Path projectMetadataFile = projectDir.resolve( MetadataTools.MAVEN_METADATA );

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetadataFile );

        if ( Files.exists(projectMetadataFile) )
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

        RepositoryMetadataWriter.write( projectMetadata, projectMetadataFile);

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

        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            VersionedReference ref = new VersionedReference();
            ref.setArtifactId( projectId );
            ref.setGroupId( namespace );
            ref.setVersion( version );

            repository.deleteVersion( ref );

            /*
            ProjectReference projectReference = new ProjectReference();
            projectReference.setGroupId( namespace );
            projectReference.setArtifactId( projectId );

            repository.getVersions(  )
            */

            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setGroupId( namespace );
            artifactReference.setArtifactId( projectId );
            artifactReference.setVersion( version );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            Set<ArtifactReference> related = repository.getRelatedArtifacts( artifactReference );
            log.debug( "related: {}", related );
            for ( ArtifactReference artifactRef : related )
            {
                repository.deleteArtifact( artifactRef );
            }

            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( repositoryId, namespace, projectId, version );

            for ( ArtifactMetadata artifactMetadata : artifacts )
            {
                metadataRepository.removeArtifact( artifactMetadata, version );
            }

            metadataRepository.removeProjectVersion( repositoryId, namespace, projectId, version );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            repositorySession.save();

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

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();

            TimeZone timezone = TimeZone.getTimeZone( "UTC" );
            DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
            fmt.setTimeZone( timezone );
            ManagedRepository repoConfig = managedRepositoryAdmin.getManagedRepository( repositoryId );

            VersionedReference ref = new VersionedReference();
            ref.setArtifactId( artifact.getArtifactId() );
            ref.setGroupId( artifact.getGroupId() );
            ref.setVersion( artifact.getVersion() );

            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setArtifactId( artifact.getArtifactId() );
            artifactReference.setGroupId( artifact.getGroupId() );
            artifactReference.setVersion( artifact.getVersion() );
            artifactReference.setClassifier( artifact.getClassifier() );
            artifactReference.setType( artifact.getPackaging() );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            String path = repository.toMetadataPath( ref );

            if ( StringUtils.isNotBlank( artifact.getClassifier() ) )
            {
                if ( StringUtils.isBlank( artifact.getPackaging() ) )
                {
                    throw new ArchivaRestServiceException( "You must configure a type/packaging when using classifier",
                                                           400, null );
                }

                repository.deleteArtifact( artifactReference );

            }
            else
            {

                int index = path.lastIndexOf( '/' );
                path = path.substring( 0, index );
                Path targetPath = Paths.get( repoConfig.getLocation(), path );

                if ( !Files.exists(targetPath) )
                {
                    //throw new ContentNotFoundException(
                    //    artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() );
                    log.warn( "targetPath {} not found skip file deletion", targetPath );
                }

                // TODO: this should be in the storage mechanism so that it is all tied together
                // delete from file system
                if ( !snapshotVersion )
                {
                    repository.deleteVersion( ref );
                }
                else
                {
                    Set<ArtifactReference> related = repository.getRelatedArtifacts( artifactReference );
                    log.debug( "related: {}", related );
                    for ( ArtifactReference artifactRef : related )
                    {
                        repository.deleteArtifact( artifactRef );
                    }
                }
                Path metadataFile = getMetadata( targetPath.toAbsolutePath().toString() );
                ArchivaRepositoryMetadata metadata = getMetadata( metadataFile );

                updateMetadata( metadata, metadataFile, lastUpdatedTimestamp, artifact );
            }
            Collection<ArtifactMetadata> artifacts = Collections.emptyList();

            if ( snapshotVersion )
            {
                String baseVersion = VersionUtil.getBaseVersion( artifact.getVersion() );
                artifacts =
                    metadataRepository.getArtifacts( repositoryId, artifact.getGroupId(), artifact.getArtifactId(),
                                                     baseVersion );
            }
            else
            {
                artifacts =
                    metadataRepository.getArtifacts( repositoryId, artifact.getGroupId(), artifact.getArtifactId(),
                                                     artifact.getVersion() );
            }

            log.debug( "artifacts: {}", artifacts );

            if ( artifacts.isEmpty() )
            {
                if ( !snapshotVersion )
                {
                    // verify metata repository doesn't contains anymore the version
                    Collection<String> projectVersions =
                        metadataRepository.getProjectVersions( repositoryId, artifact.getGroupId(),
                                                               artifact.getArtifactId() );

                    if ( projectVersions.contains( artifact.getVersion() ) )
                    {
                        log.warn( "artifact not found when deleted but version still here ! so force cleanup" );
                        metadataRepository.removeProjectVersion( repositoryId, artifact.getGroupId(),
                                                                 artifact.getArtifactId(), artifact.getVersion() );
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
                            metadataRepository.removeArtifact( repositoryId, groupId, artifactId, version,
                                                               mavenArtifactFacetToCompare );
                            metadataRepository.save();
                        }

                    }
                    else
                    {
                        if ( snapshotVersion )
                        {
                            metadataRepository.removeArtifact( artifactMetadata,
                                                               VersionUtil.getBaseVersion( artifact.getVersion() ) );
                        }
                        else
                        {
                            metadataRepository.removeArtifact( artifactMetadata.getRepositoryId(),
                                                               artifactMetadata.getNamespace(),
                                                               artifactMetadata.getProject(), artifact.getVersion(),
                                                               artifactMetadata.getId() );
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
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500, e );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( "RepositoryAdmin exception: " + e.getMessage(), 500, e );
        }
        finally
        {

            repositorySession.save();

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

        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            repository.deleteGroupId( groupId );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.removeNamespace( repositoryId, groupId );

            // just invalidate cache entry
            String cacheKey = repositoryId + "-" + groupId;
            namespacesCache.remove( cacheKey );
            namespacesCache.remove( repositoryId );

            metadataRepository.save();
        }
        catch ( MetadataRepositoryException e )
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

        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

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

            metadataRepository.removeProject( repositoryId, groupId, projectId );

            metadataRepository.save();
        }
        catch ( MetadataRepositoryException e )
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
            return repoScanner.scan( getManagedRepositoryAdmin().getManagedRepository( repositoryId ), sinceWhen );
        }
        catch ( RepositoryScannerException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "RepositoryScannerException exception: " + e.getMessage(), 500, e );
        }
        catch ( RepositoryAdminException e )
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
    private void updateMetadata( ArchivaRepositoryMetadata metadata, Path metadataFile, Date lastUpdatedTimestamp,
                                 Artifact artifact )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<>();
        String latestVersion = "";

        if ( Files.exists(metadataFile) )
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

        RepositoryMetadataWriter.write( metadata, metadataFile);
        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
    }

    @Override
    public StringList getRunningRemoteDownloadIds()
    {
        return new StringList( downloadRemoteIndexScheduler.getRunningRemoteDownloadIds() );
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    public RepositoryContentFactory getRepositoryFactory()
    {
        return repositoryFactory;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
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


