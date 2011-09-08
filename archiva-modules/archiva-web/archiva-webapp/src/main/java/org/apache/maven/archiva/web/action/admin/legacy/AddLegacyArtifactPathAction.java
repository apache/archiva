package org.apache.maven.archiva.web.action.admin.legacy;

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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.admin.ArchivaAdministration;
import org.apache.archiva.admin.repository.admin.LegacyArtifactPath;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Add a LegacyArtifactPath to archiva configuration
 *
 * @since 1.1
 */
@Controller( "addLegacyArtifactPathAction" )
@Scope( "prototype" )
public class AddLegacyArtifactPathAction
    extends AbstractActionSupport
    implements Preparable, Validateable
{

    @Inject
    private ArchivaAdministration archivaAdministration;

    @Inject
    @Named( value = "managedRepositoryContent#legacy" )
    private ManagedRepositoryContent repositoryContent;


    private LegacyArtifactPath legacyArtifactPath;

    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    private String type;


    public void prepare()
    {
        this.legacyArtifactPath = new LegacyArtifactPath();
    }

    public String input()
    {
        return INPUT;
    }

    public String commit()
    {
        this.legacyArtifactPath.setArtifact(
            this.groupId + ":" + this.artifactId + ":" + this.version + ":" + this.classifier + ":" + this.type );

        // Check the proposed Artifact macthes the path
        ArtifactReference artifact = new ArtifactReference();

        artifact.setGroupId( this.groupId );
        artifact.setArtifactId( this.artifactId );
        artifact.setClassifier( this.classifier );
        artifact.setVersion( this.version );
        artifact.setType( this.type );

        String path = repositoryContent.toPath( artifact );
        if ( !path.equals( this.legacyArtifactPath.getPath() ) )
        {
            addActionError( "artifact reference does not match the initial path : " + path );
            return ERROR;
        }

        try
        {
            getArchivaAdministration().addLegacyArtifactPath( legacyArtifactPath );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error occured " + e.getMessage() );
            return INPUT;
        }
        return SUCCESS;
    }

    public LegacyArtifactPath getLegacyArtifactPath()
    {
        return legacyArtifactPath;
    }

    public void setLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
    {
        this.legacyArtifactPath = legacyArtifactPath;
    }

    public void validate()
    {
        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( legacyArtifactPath.getPath() ) )
        {
            legacyArtifactPath.setPath( legacyArtifactPath.getPath().trim() );
        }

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

        if ( StringUtils.isNotEmpty( classifier ) )
        {
            classifier = classifier.trim();
        }

        if ( StringUtils.isNotEmpty( type ) )
        {
            type = type.trim();
        }
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

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
