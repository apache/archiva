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
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArchivaIndexingTaskExecutor;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
@Service( "repositoriesService#rest" )
public class DefaultRepositoriesService
    extends AbstractRestService
    implements RepositoriesService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Inject
    @Named( value = "taskExecutor#indexing" )
    private ArchivaIndexingTaskExecutor archivaIndexingTaskExecutor;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private MavenIndexerUtils mavenIndexerUtils;

    @Inject
    private SecuritySystem securitySystem;

    @Inject
    private RepositoryContentFactory repositoryFactory;

    @Inject
    private ArchivaAdministration archivaAdministration;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private ArchivaTaskScheduler scheduler;

    @Inject
    private DownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    @Inject
    @Named( value = "repositorySessionFactory" )
    protected RepositorySessionFactory repositorySessionFactory;

    @Inject
    protected List<RepositoryListener> listeners = new ArrayList<RepositoryListener>();

    @Inject
    private RepositoryScanner repoScanner;

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled", repositoryId );
            return Boolean.FALSE;
        }
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setScanAll( fullScan );
        try
        {
            repositoryTaskScheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to schedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
        return true;
    }

    public Boolean alreadyScanning( String repositoryId )
    {
        return repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId );
    }

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
            return Boolean.TRUE;
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

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
            throw new ArchivaRestServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean copyArtifact( ArtifactTransferRequest artifactTransferRequest )
        throws ArchivaRestServiceException
    {
        // check parameters
        String userName = getAuditInformation().getUser().getUsername();
        if ( StringUtils.isBlank( userName ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: userName not found" );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getRepositoryId() ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: sourceRepositoryId cannot be null" );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getTargetRepositoryId() ) )
        {
            throw new ArchivaRestServiceException( "copyArtifact call: targetRepositoryId cannot be null" );
        }

        ManagedRepository source = null;
        try
        {
            source = managedRepositoryAdmin.getManagedRepository( artifactTransferRequest.getRepositoryId() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }

        if ( source == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getRepositoryId() );
        }

        ManagedRepository target = null;
        try
        {
            target = managedRepositoryAdmin.getManagedRepository( artifactTransferRequest.getTargetRepositoryId() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }

        if ( target == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getTargetRepositoryId() );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getGroupId() ) )
        {
            throw new ArchivaRestServiceException( "groupId is mandatory" );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getArtifactId() ) )
        {
            throw new ArchivaRestServiceException( "artifactId is mandatory" );
        }

        if ( StringUtils.isBlank( artifactTransferRequest.getVersion() ) )
        {
            throw new ArchivaRestServiceException( "version is mandatory" );
        }

        if ( VersionUtil.isSnapshot( artifactTransferRequest.getVersion() ) )
        {
            throw new ArchivaRestServiceException( "copy of SNAPSHOT not supported" );
        }

        // end check parameters

        User user = null;
        try
        {
            user = securitySystem.getUserManager().findUser( userName );
        }
        catch ( UserNotFoundException e )
        {
            throw new ArchivaRestServiceException( "user " + userName + " not found" );
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
                    "not authorized to access repo:" + artifactTransferRequest.getRepositoryId() );
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "error reading permission: " + e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
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
                    "not authorized to write to repo:" + artifactTransferRequest.getTargetRepositoryId() );
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "error reading permission: " + e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
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
                log.error( "cannot find artifact " + artifactTransferRequest.toString() );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString() );
            }

            File artifactFile = new File( source.getLocation(), artifactSourcePath );

            if ( !artifactFile.exists() )
            {
                log.error( "cannot find artifact " + artifactTransferRequest.toString() );
                throw new ArchivaRestServiceException( "cannot find artifact " + artifactTransferRequest.toString() );
            }

            ManagedRepositoryContent targetRepository =
                repositoryFactory.getManagedRepositoryContent( artifactTransferRequest.getTargetRepositoryId() );

            String artifactPath = targetRepository.toPath( artifactReference );

            int lastIndex = artifactPath.lastIndexOf( '/' );

            String path = artifactPath.substring( 0, lastIndex );
            File targetPath = new File( target.getLocation(), path );

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = 1;
            String timestamp = null;

            File versionMetadataFile = new File( targetPath, MetadataTools.MAVEN_METADATA );
            ArchivaRepositoryMetadata versionMetadata = getMetadata( versionMetadataFile );

            if ( !targetPath.exists() )
            {
                targetPath.mkdirs();
            }

            String filename = artifactPath.substring( lastIndex + 1 );

            // FIXME some dupe with uploadaction

            boolean fixChecksums =
                !( archivaAdministration.getKnownContentConsumers().contains( "create-missing-checksums" ) );

            File targetFile = new File( targetPath, filename );
            if ( targetFile.exists() && target.isBlockRedeployments() )
            {
                throw new ArchivaRestServiceException(
                    "artifact already exists in target repo: " + artifactTransferRequest.getTargetRepositoryId()
                        + " and redeployment blocked" );
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

            File pomFile = new File(
                new File( source.getLocation(), artifactSourcePath.substring( 0, artifactPath.lastIndexOf( '/' ) ) ),
                pomFilename );

            if ( pomFile != null && pomFile.length() > 0 )
            {
                copyFile( pomFile, targetPath, pomFilename, fixChecksums );
                queueRepositoryTask( target.getId(), new File( targetPath, pomFilename ) );


            }

            // explicitly update only if metadata-updater consumer is not enabled!
            if ( !archivaAdministration.getKnownContentConsumers().contains( "metadata-updater" ) )
            {
                updateProjectMetadata( targetPath.getAbsolutePath(), lastUpdatedTimestamp, timestamp, newBuildNumber,
                                       fixChecksums, artifactTransferRequest );


            }

            String msg =
                "Artifact \'" + artifactTransferRequest.getGroupId() + ":" + artifactTransferRequest.getArtifactId()
                    + ":" + artifactTransferRequest.getVersion() + "\' was successfully deployed to repository \'"
                    + artifactTransferRequest.getTargetRepositoryId() + "\'";

        }
        catch ( RepositoryException e )
        {
            log.error( "RepositoryException: " + e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "RepositoryAdminException: " + e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
        catch ( IOException e )
        {
            log.error( "IOException: " + e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
        return true;
    }

    //FIXME some duplicate with UploadAction 

    private void queueRepositoryTask( String repositoryId, File localFile )
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
            log.error( "Unable to queue repository task to execute consumers on resource file ['" + localFile.getName()
                           + "']." );
        }
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

    private File getMetadata( String targetPath )
    {
        String artifactPath = targetPath.substring( 0, targetPath.lastIndexOf( File.separatorChar ) );

        return new File( artifactPath, MetadataTools.MAVEN_METADATA );
    }

    private void copyFile( File sourceFile, File targetPath, String targetFilename, boolean fixChecksums )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( new File( targetPath, targetFilename ) );
        FileInputStream input = new FileInputStream( sourceFile );

        try
        {
            IOUtils.copy( input, out );
        }
        finally
        {
            out.close();
            input.close();
        }

        if ( fixChecksums )
        {
            fixChecksums( new File( targetPath, targetFilename ) );
        }
    }

    private void fixChecksums( File file )
    {
        ChecksummedFile checksum = new ChecksummedFile( file );
        checksum.fixChecksums( algorithms );
    }

    private void updateProjectMetadata( String targetPath, Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                        boolean fixChecksums, ArtifactTransferRequest artifactTransferRequest )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<String>();
        String latestVersion = artifactTransferRequest.getVersion();

        File projectDir = new File( targetPath ).getParentFile();
        File projectMetadataFile = new File( projectDir, MetadataTools.MAVEN_METADATA );

        ArchivaRepositoryMetadata projectMetadata = getMetadata( projectMetadataFile );

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

        RepositoryMetadataWriter.write( projectMetadata, projectMetadataFile );

        if ( fixChecksums )
        {
            fixChecksums( projectMetadataFile );
        }
    }

    public Boolean deleteArtifact( Artifact artifact, String repositoryId )
        throws ArchivaRestServiceException
    {

        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "repositoryId cannot be null", 400 );
        }

        if ( !isAuthorizedToDeleteArtifacts( repositoryId ) )
        {
            throw new ArchivaRestServiceException( "not authorized to delete artifacts", 403 );
        }

        if ( artifact == null )
        {
            throw new ArchivaRestServiceException( "artifact cannot be null", 400 );
        }

        if ( StringUtils.isEmpty( artifact.getGroupId() ) )
        {
            throw new ArchivaRestServiceException( "artifact.groupId cannot be null", 400 );
        }

        if ( StringUtils.isEmpty( artifact.getArtifactId() ) )
        {
            throw new ArchivaRestServiceException( "artifact.artifactId cannot be null", 400 );
        }

        // TODO more control on artifact fields

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

            if ( StringUtils.isNotBlank( artifact.getClassifier() ) )
            {
                if ( StringUtils.isBlank( artifact.getPackaging() ) )
                {
                    throw new ArchivaRestServiceException( "You must configure a type/packaging when using classifier",
                                                           400 );
                }
                ArtifactReference artifactReference = new ArtifactReference();
                artifactReference.setArtifactId( artifact.getArtifactId() );
                artifactReference.setGroupId( artifact.getGroupId() );
                artifactReference.setVersion( artifact.getVersion() );
                artifactReference.setClassifier( artifact.getClassifier() );
                artifactReference.setType( artifact.getPackaging() );
                repository.deleteArtifact( artifactReference );

                // TODO cleanup facet which contains classifier information
                return Boolean.TRUE;
            }

            String path = repository.toMetadataPath( ref );
            int index = path.lastIndexOf( '/' );
            path = path.substring( 0, index );
            File targetPath = new File( repoConfig.getLocation(), path );

            if ( !targetPath.exists() )
            {
                throw new ContentNotFoundException(
                    artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() );
            }

            // TODO: this should be in the storage mechanism so that it is all tied together
            // delete from file system
            repository.deleteVersion( ref );

            File metadataFile = getMetadata( targetPath.getAbsolutePath() );
            ArchivaRepositoryMetadata metadata = getMetadata( metadataFile );

            updateMetadata( metadata, metadataFile, lastUpdatedTimestamp, artifact );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( repositoryId, artifact.getGroupId(), artifact.getArtifactId(),
                                                 artifact.getVersion() );

            for ( ArtifactMetadata artifactMetadata : artifacts )
            {
                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                if ( artifact.getVersion().equals( artifact.getVersion() ) )
                {
                    metadataRepository.removeArtifact( artifactMetadata.getRepositoryId(),
                                                       artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                                       artifact.getVersion(), artifactMetadata.getId() );

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
            repositorySession.save();
        }

        catch ( ContentNotFoundException e )
        {
            throw new ArchivaRestServiceException( "Artifact does not exist: " + e.getMessage(), 400 );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new ArchivaRestServiceException( "Target repository cannot be found: " + e.getMessage(), 400 );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500 );
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500 );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( "Repository exception: " + e.getMessage(), 500 );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( "RepositoryAdmin exception: " + e.getMessage(), 500 );
        }
        finally

        {
            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    public Boolean isAuthorizedToDeleteArtifacts( String repoId )
        throws ArchivaRestServiceException
    {
        String userName =
            getAuditInformation().getUser() == null ? "guest" : getAuditInformation().getUser().getUsername();

        try
        {
            boolean res = userRepositories.isAuthorizedToDeleteArtifacts( userName, repoId );
            return res;
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
    }

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
            throw new ArchivaRestServiceException( "RepositoryScannerException exception: " + e.getMessage(), 500 );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( "RepositoryScannerException exception: " + e.getMessage(), 500 );
        }
    }

    /**
     * Update artifact level metadata. Creates one if metadata does not exist after artifact deletion.
     *
     * @param metadata
     */
    private void updateMetadata( ArchivaRepositoryMetadata metadata, File metadataFile, Date lastUpdatedTimestamp,
                                 Artifact artifact )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<String>();
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

        RepositoryMetadataWriter.write( metadata, metadataFile );
        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
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


