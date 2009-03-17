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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.updater.DatabaseConsumers;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactVersionsConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.UserRepositories;
import org.codehaus.plexus.redback.rbac.RbacManagerException;

import org.apache.struts2.ServletActionContext;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;

/**
 * Delete an artifact. Metadata will be updated if one exists, otherwise it would be created.
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="deleteArtifactAction" instantiation-strategy="per-lookup"
 */
public class DeleteArtifactAction
    extends PlexusActionSupport
    implements Validateable, Preparable, Auditable
{
    /**
     * @plexus.requirement
     */
    private ArchivaXworkUser archivaXworkUser;

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

    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArtifactDAO artifactDAO;

    /**
     * @plexus.requirement 
     */
    private DatabaseConsumers databaseConsumers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.audit.AuditListener"
     */
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[] { ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

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
        managedRepos = new ArrayList<String>( configuration.getConfiguration().getManagedRepositoriesAsMap().keySet() );
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
        try
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

            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            String path = repository.toMetadataPath( ref );
            int index = path.lastIndexOf( '/' );
            File targetPath = new File( repoConfig.getLocation(), path.substring( 0, index ) );

            if ( !targetPath.exists() )
            {
                throw new ContentNotFoundException( groupId + ":" + artifactId + ":" + version );
            }

            // delete from file system
            repository.deleteVersion( ref );

            File metadataFile = getMetadata( targetPath.getAbsolutePath() );
            ArchivaRepositoryMetadata metadata = getMetadata( metadataFile );

            updateMetadata( metadata, metadataFile, lastUpdatedTimestamp );

            ArtifactVersionsConstraint constraint =
                new ArtifactVersionsConstraint( repositoryId, groupId, artifactId, false );
            List<ArchivaArtifact> artifacts = null;

            try
            {
                artifacts = artifactDAO.queryArtifacts( constraint );

                if ( artifacts != null )
                {
                    for ( ArchivaArtifact artifact : artifacts )
                    {
                        if ( artifact.getVersion().equals( version ) )
                        {
                            databaseConsumers.executeCleanupConsumer( artifact );
                        }
                    }
                }
            }
            catch ( ArchivaDatabaseException e )
            {
                addActionError( "Error occurred while cleaning up database: " + e.getMessage() );
                return ERROR;
            }

            String msg =
                "Artifact \'" + groupId + ":" + artifactId + ":" + version +
                    "\' was successfully deleted from repository \'" + repositoryId + "\'";

            triggerAuditEvent( getPrincipal(), repositoryId, groupId + ":" + artifactId + ":" + version,
                               AuditEvent.REMOVE_FILE );

            addActionMessage( msg );

            reset();
            return SUCCESS;
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
    }

    @SuppressWarnings("unchecked")
    private String getPrincipal()
    {
        return archivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() );
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
            if ( metadata.getReleasedVersion().equals( version ) )
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
        catch ( RbacManagerException e )
        {
            addActionError( e.getMessage() );
        }
    }

    public void addAuditListener( AuditListener listener )
    {
        this.auditListeners.add( listener );
    }

    public void clearAuditListeners()
    {
        this.auditListeners.clear();
    }

    public void removeAuditListener( AuditListener listener )
    {
        this.auditListeners.remove( listener );
    }

    private void triggerAuditEvent( String user, String repositoryId, String resource, String action )
    {
        AuditEvent event = new AuditEvent( repositoryId, user, resource, action );
        event.setRemoteIP( ServletActionContext.getRequest().getRemoteAddr() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }
}
