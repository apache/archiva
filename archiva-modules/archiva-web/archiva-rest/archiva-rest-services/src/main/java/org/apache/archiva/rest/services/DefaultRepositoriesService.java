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
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArchivaIndexingTaskExecutor;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
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

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled" );
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

    public Boolean removeScanningTaskFromQueue( @PathParam( "repositoryId" ) String repositoryId )
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

            IndexingContext context =
                ArtifactIndexingTask.createContext( repository, plexusSisuBridge.lookup( NexusIndexer.class ),
                                                    new ArrayList<IndexCreator>(
                                                        mavenIndexerUtils.getAllIndexCreators() ) );
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, context );

            task.setExecuteOnEntireRepo( true );
            task.setOnlyUpdate( false );

            archivaIndexingTaskExecutor.executeTask( task );
            return Boolean.TRUE;
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
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

        if ( StringUtils.isBlank( artifactTransferRequest.getSourceRepositoryId() ) )
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
            source = managedRepositoryAdmin.getManagedRepository( artifactTransferRequest.getSourceRepositoryId() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }

        if ( source == null )
        {
            throw new ArchivaRestServiceException(
                "cannot find repository with id " + artifactTransferRequest.getSourceRepositoryId() );
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
                                             artifactTransferRequest.getSourceRepositoryId() );
            if ( !authz )
            {
                throw new ArchivaRestServiceException(
                    "not authorized to access repo:" + artifactTransferRequest.getSourceRepositoryId() );
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
                repositoryFactory.getManagedRepositoryContent( artifactTransferRequest.getSourceRepositoryId() );

            String artifactSourcePath = sourceRepository.toPath( artifactReference );

            File artifactFile = new File( source.getLocation(), artifactSourcePath );

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
            metadata = RepositoryMetadataReader.read( metadataFile );
        }
        return metadata;
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
}


