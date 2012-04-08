package org.apache.archiva.web.action;

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
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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
     * @since 1.4-M2
     *        The classifier of the artifact to be deleted (optionnal)
     */
    private String classifier;

    /**
     * @since 1.4-M2
     *        The type of the artifact to be deleted (optionnal) (default jar)
     */
    private String type;

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
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RepositoriesService repositoriesService;

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

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
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
        classifier = "";
        type = "";
    }

    public String doDelete()
    {
        // services need a ThreadLocal variable to test karma
        RedbackAuthenticationThreadLocal.set( getRedbackRequestInformation() );
        try
        {
            Artifact artifact = new Artifact();
            artifact.setGroupId( groupId );
            artifact.setArtifactId( artifactId );
            artifact.setVersion( version );
            artifact.setClassifier( classifier );
            artifact.setPackaging( type );

            repositoriesService.deleteArtifact( artifact, repositoryId );
        }
        catch ( ArchivaRestServiceException e )
        {
            addActionError( "ArchivaRestServiceException exception: " + e.getMessage() );
            return ERROR;
        }
        finally
        {
            RedbackAuthenticationThreadLocal.set( null );
        }

        StringBuilder msg = new StringBuilder( "Artifact \'" ).append( groupId ).append( ":" ).append( artifactId );

        if ( StringUtils.isNotEmpty( classifier ) )
        {

            msg.append( ":" ).append( classifier );


        }
        msg.append( ":" ).append( version ).append( "' was successfully deleted from repository '" ).append(
            repositoryId ).append( "'" );
        addActionMessage( msg.toString() );
        reset();
        return SUCCESS;
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

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    public RepositoriesService getRepositoriesService()
    {
        return repositoriesService;
    }

    public void setRepositoriesService( RepositoriesService repositoriesService )
    {
        this.repositoriesService = repositoriesService;
    }
}
