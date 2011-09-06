package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
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
 * Delete an artifact. Metadata will be updated if one exists, otherwise it would be created.
 */
@Controller( "deleteArtifactAction" )
@Scope( "prototype" )
public class DeleteArtifactAction
    extends AbstractActionSupport
    implements Validateable, Preparable, Auditable
{
    /**
     * The groupId of the artifact to be deleted.
     */
    private String groupId;

    /**
     * The artifactId of the artifact to be deleted.
     */
    private String artifactId;

    /**
     * The version of the artifact to be deleted.
     */
    private String version;

    /**
     * The repository where the artifact is to be deleted.
     */
    private String repositoryId;

    /**
     * List of managed repositories to delete from.
     */
    private List<String> managedRepos;

    @Inject
    private UserRepositories userRepositories;

    @Inject
    private ArchivaConfiguration configuration;

    @Inject
    private RepositoryContentFactory repositoryFactory;

    @Inject
    private List<RepositoryListener> listeners;

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[]{ ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    @PostConstruct
    public void initialize()
    {
        super.initialize();
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List<String> getManagedRepos()
    {
        return managedRepos;
    }

    public void setManagedRepos( List<String> managedRepos )
    {
        this.managedRepos = managedRepos;
    }

    public void prepare()
    {
        managedRepos = getManagableRepos();
    }

    public String input()
    {
        return INPUT;
    }

    private void reset()
    {
        // reset the fields so the form is clear when 
        // the action returns to the jsp page
        groupId = "";
        artifactId = "";
        version = "";
        repositoryId = "";
    }

    public String doDelete()
    {
        Date lastUpdatedTimestamp = Calendar.getInstance().getTime();

        TimeZone timezone = TimeZone.getTimeZone( "UTC" );
        DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        fmt.setTimeZone( timezone );
        ManagedRepositoryConfiguration repoConfig =
            configuration.getConfiguration().findManagedRepositoryById( repositoryId );

        VersionedReference ref = new VersionedReference();
        ref.setArtifactId( artifactId );
        ref.setGroupId( groupId );
        ref.setVersion( version );

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            String path = repository.toMetadataPath( ref );
            int index = path.lastIndexOf( '/' );
            path = path.substring( 0, index );
            File targetPath = new File( repoConfig.getLocation(), path );

            if ( !targetPath.exists() )
            {
                throw new ContentNotFoundException( groupId + ":" + artifactId + ":" + version );
            }

            // TODO: this should be in the storage mechanism so that it is all tied together
            // delete from file system
            repository.deleteVersion( ref );

            File metadataFile = getMetadata( targetPath.getAbsolutePath() );
            ArchivaRepositoryMetadata metadata = getMetadata( metadataFile );

            updateMetadata( metadata, metadataFile, lastUpdatedTimestamp );

            MetadataRepository metadataRepository = repositorySession.getRepository();
            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( repositoryId, groupId, artifactId, version );

            for ( ArtifactMetadata artifact : artifacts )
            {
                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                if ( artifact.getVersion().equals( version ) )
                {
                    metadataRepository.removeArtifact( artifact.getRepositoryId(), artifact.getNamespace(),
                                                       artifact.getProject(), artifact.getVersion(), artifact.getId() );

                    // TODO: move into the metadata repository proper - need to differentiate attachment of
                    //       repository metadata to an artifact
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.deleteArtifact( metadataRepository, repository.getId(), artifact.getNamespace(),
                                                 artifact.getProject(), artifact.getVersion(), artifact.getId() );
                    }

                    triggerAuditEvent( repositoryId, path, AuditEvent.REMOVE_FILE );
                }
            }
            repositorySession.save();
        }
        catch ( ContentNotFoundException e )
        {
            addActionError( "Artifact does not exist: " + e.getMessage() );
            return ERROR;
        }
        catch ( RepositoryNotFoundException e )
        {
            addActionError( "Target repository cannot be found: " + e.getMessage() );
            return ERROR;
        }
        catch ( RepositoryException e )
        {
            addActionError( "Repository exception: " + e.getMessage() );
            return ERROR;
        }
        catch ( MetadataResolutionException e )
        {
            addActionError( "Repository exception: " + e.getMessage() );
            return ERROR;
        }
        catch ( MetadataRepositoryException e )
        {
            addActionError( "Repository exception: " + e.getMessage() );
            return ERROR;
        }
        finally
        {
            repositorySession.close();
        }

        String msg = "Artifact \'" + groupId + ":" + artifactId + ":" + version
            + "\' was successfully deleted from repository \'" + repositoryId + "\'";

        addActionMessage( msg );

        reset();
        return SUCCESS;
    }

    private File getMetadata( String targetPath )
    {
        String artifactPath = targetPath.substring( 0, targetPath.lastIndexOf( File.separatorChar ) );

        return new File( artifactPath, MetadataTools.MAVEN_METADATA );
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

    /**
     * Update artifact level metadata. Creates one if metadata does not exist after artifact deletion.
     *
     * @param metadata
     */
    private void updateMetadata( ArchivaRepositoryMetadata metadata, File metadataFile, Date lastUpdatedTimestamp )
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

                    if ( availableVersions.contains( version ) )
                    {
                        availableVersions.remove( availableVersions.indexOf( version ) );
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
            metadata.setGroupId( groupId );
        }
        if ( metadata.getArtifactId() == null )
        {
            metadata.setArtifactId( artifactId );
        }

        if ( !VersionUtil.isSnapshot( version ) )
        {
            if ( metadata.getReleasedVersion() != null && metadata.getReleasedVersion().equals( version ) )
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

    public void validate()
    {
        try
        {
            if ( !userRepositories.isAuthorizedToDeleteArtifacts( getPrincipal(), repositoryId ) )
            {
                addActionError( "User is not authorized to delete artifacts in repository '" + repositoryId + "'." );
            }

            if ( ( version.length() > 0 ) && ( !VersionUtil.isVersion( version ) ) )
            {
                addActionError( "Invalid version." );
            }
        }
        catch ( AccessDeniedException e )
        {
            addActionError( e.getMessage() );
        }
        catch ( ArchivaSecurityException e )
        {
            addActionError( e.getMessage() );
        }

        // trims all request parameter values, since the trailing/leading white-spaces are ignored during validation.
        trimAllRequestParameterValues();
    }

    private List<String> getManagableRepos()
    {
        try
        {
            return userRepositories.getManagableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
            // TODO: pass this onto the screen.
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( groupId ) )
        {
            groupId = groupId.trim();
        }

        if ( StringUtils.isNotEmpty( artifactId ) )
        {
            artifactId = artifactId.trim();
        }

        if ( StringUtils.isNotEmpty( version ) )
        {
            version = version.trim();
        }

        if ( StringUtils.isNotEmpty( repositoryId ) )
        {
            repositoryId = repositoryId.trim();
        }
    }

    public List<RepositoryListener> getListeners()
    {
        return listeners;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }
}
