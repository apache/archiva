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

import org.codehaus.plexus.xwork.action.PlexusActionSupport;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.ArchivaUser;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;

import com.opensymphony.xwork.Validateable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Upload an artifact.
 * 
 * @author Wendy Smoak
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="uploadAction"
 */
public class UploadAction
    extends PlexusActionSupport
    implements Validateable
{
    private String groupId;

    private String artifactId;

    private String version;

    private String packaging;

    private String classifier;

    private File file;

    private String contentType;

    private String filename;

    private String repositoryId;

    /**
     * @plexus.requirement role-hint="xwork"
     */
    private ArchivaUser archivaUser;

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

    public void setUpload( File file )
    {
        this.file = file;
    }

    public void setUploadContentType( String contentType )
    {
        this.contentType = contentType;
    }

    public void setUploadFileName( String filename )
    {
        this.filename = filename;
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

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String upload()
    {
        // TODO populate repository id field
        // TODO form validation

        getLogger().debug( "upload" );
        return INPUT;
    }

    public String doUpload()
    {
        try
        {
            ManagedRepositoryConfiguration repoConfig =
                configuration.getConfiguration().findManagedRepositoryById( repositoryId );

            ArtifactReference artifactReference = new ArtifactReference();
            artifactReference.setArtifactId( artifactId );
            artifactReference.setGroupId( groupId );
            artifactReference.setVersion( version );
            artifactReference.setClassifier( classifier );
            artifactReference.setType( packaging );

            ManagedRepositoryContent repository = repositoryFactory.getManagedRepositoryContent( repositoryId );

            String artifactPath = repository.toPath( artifactReference );

            int lastIndex = artifactPath.lastIndexOf( '/' );

            File targetPath = new File( repoConfig.getLocation(), artifactPath.substring( 0, lastIndex ) );

            if ( !targetPath.exists() )
            {
                targetPath.mkdirs();
            }

            copyFile( targetPath, artifactPath.substring( lastIndex + 1 ) );

            // 1. check if user has permission to deploy to the repository
            // - get writable user repositories (need to add new method
            // for this in DefaultUserRepositories)

            // 2. if user has write permission:
            // - get repository path (consider the layout -- default or legacy)
            // - if the artifact is not a pom, create pom file (use ProjectModel400Writer in archiva-repository-layer)
            // - create directories in the repository (groupId, artifactId, version)
            // - re-write uploaded jar file
            // - write generated pom
            // - update metadata

            // TODO delete temporary file (upload)
            // TODO improve action error messages below

            return SUCCESS;
        }
        catch ( IOException ie )
        {
            addActionError( "Error encountered while uploading file: " + ie.getMessage() );
            return ERROR;
        }
        catch ( RepositoryNotFoundException re )
        {
            addActionError( "Target repository cannot be found: " + re.getMessage() );
            return ERROR;
        }
        catch ( RepositoryException rep )
        {
            addActionError( "Repository exception: " + rep.getMessage() );
            return ERROR;
        }
    }

    private String getPrincipal()
    {
        return archivaUser.getActivePrincipal();
    }

    private void copyFile( File targetPath, String artifactFilename )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( new File( targetPath, artifactFilename ) );

        try
        {
            FileInputStream input = new FileInputStream( file );
            int i = 0;
            while ( ( i = input.read() ) != -1 )
            {
                out.write( i );
            }
            out.flush();
        }
        finally
        {
            out.close();
        }
    }

    private void generatePom()
    {
        // TODO: use ProjectModel400Writer
    }

    public void validate()
    {
        try
        {
            // is this enough check for the repository permission?
            if ( !userRepositories.isAuthorizedToUploadArtifacts( getPrincipal(), repositoryId ) )
            {
                addActionError( "User is not authorized to upload in repository " + repositoryId );
            }
        }
        catch ( PrincipalNotFoundException pe )
        {
            addActionError( pe.getMessage() );
        }
        catch ( ArchivaSecurityException ae )
        {
            addActionError( ae.getMessage() );
        }
    }
}
