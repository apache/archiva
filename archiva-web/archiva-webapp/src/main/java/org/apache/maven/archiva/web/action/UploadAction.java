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
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelWriter;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.ArchivaUser;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;

import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Upload an artifact using Jakarta file upload in webwork. If set by the user
 * a pom will also be generated. Metadata will also be updated if one exists, 
 * otherwise it would be created.
 * 
 * @author <a href="mailto:wsmoak@apache.org">Wendy Smoak</a>
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="uploadAction"
 */
public class UploadAction
    extends PlexusActionSupport
    implements Validateable, Preparable
{
    /**
     * The groupId of the artifact to be deployed.
     */
    private String groupId;

    /**
     * The artifactId of the artifact to be deployed.
     */
    private String artifactId;

    /**
     * The version of the artifact to be deployed.
     */
    private String version;

    /**
     * The packaging of the artifact to be deployed.
     */
    private String packaging;

    /**
     * The classifier of the artifact to be deployed.
     */
    private String classifier;

    /**
     * The artifact to be deployed.
     */
    private File file;

    /**
     * The content type of the artifact to be deployed.
     */
    private String contentType;

    /**
     * The temporary filename of the artifact to be deployed.
     */
    private String filename;

    /**
     * The repository where the artifact is to be deployed.
     */
    private String repositoryId;

    /**
     * Flag whether to generate a pom for the artifact or not.
     */
    private boolean generatePom;
    
    /**
     * List of managed repositories to deploy to.
     */
    private List<String> managedRepoIdList;

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

    /**
     * @plexus.requirement role-hint="model400"
     */
    private ProjectModelWriter pomWriter;

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

    public boolean isGeneratePom()
    {
        return generatePom;
    }

    public void setGeneratePom( boolean generatePom )
    {
        this.generatePom = generatePom;
    }

    public List<String> getManagedRepoIdList()
    {
        return managedRepoIdList;
    }

    public void setManagedRepoIdList( List<String> managedRepoIdList )
    {
        this.managedRepoIdList = managedRepoIdList;
    }

    public void prepare()
    {
        managedRepoIdList =
            new ArrayList<String>( configuration.getConfiguration().getManagedRepositoriesAsMap().keySet() );
    }

    public String upload()
    {
        // TODO form validation
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

            try
            {
                copyFile( targetPath, artifactPath.substring( lastIndex + 1 ) );
            }
            catch ( IOException ie )
            {
                addActionError( "Error encountered while uploading file: " + ie.getMessage() );
                return ERROR;
            }

            if ( generatePom )
            {
                try
                {
                    createPom( targetPath, artifactPath.substring( lastIndex + 1 ) );
                }
                catch ( IOException ie )
                {
                    addActionError( "Error encountered while writing pom file: " + ie.getMessage() );
                    return ERROR;
                }
                catch ( ProjectModelException pe )
                {
                    addActionError( "Error encountered while generating pom file: " + pe.getMessage() );
                    return ERROR;
                }
            }

            updateMetadata( getMetadata( targetPath.getAbsolutePath() ) );
           
            return SUCCESS;
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

    private void createPom( File targetPath, String filename )
        throws IOException, ProjectModelException
    {
        ArchivaProjectModel projectModel = new ArchivaProjectModel();
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );
        projectModel.setPackaging( packaging );

        File pomFile = new File( targetPath, filename.replaceAll( packaging, "pom" ) );

        pomWriter.write( projectModel, pomFile );
    }

    private File getMetadata( String targetPath )
    {
        String artifactPath = targetPath.substring( 0, targetPath.lastIndexOf( '/' ) );

        return new File( artifactPath, MetadataTools.MAVEN_METADATA );
    }

    /**
     * Update artifact level metadata. If it does not exist, create the metadata.
     * 
     * @param targetPath
     */
    private void updateMetadata( File metadataFile )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<String>();
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();

        if ( metadataFile.exists() )
        {
            metadata = RepositoryMetadataReader.read( metadataFile );
            availableVersions = metadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( version ) )
            {
                availableVersions.add( version );
            }
            
            String latestVersion = availableVersions.get( availableVersions.size() - 1 );
            metadata.setLatestVersion( latestVersion );
            metadata.setAvailableVersions( availableVersions );
            metadata.setLastUpdatedTimestamp( Calendar.getInstance().getTime() );
            
            if( !VersionUtil.isSnapshot( version ) )
            {
                metadata.setReleasedVersion( latestVersion );
            }  
            // TODO:
            // what about the metadata checksums? re-calculate or 
            //      just leave it to the consumers to fix it?
        }
        else
        {
            availableVersions.add( version );

            metadata.setGroupId( groupId );
            metadata.setArtifactId( artifactId );
            metadata.setLatestVersion( version );
            metadata.setLastUpdatedTimestamp( Calendar.getInstance().getTime() );
            metadata.setAvailableVersions( availableVersions );
            
            if( !VersionUtil.isSnapshot( version ) )
            {
                metadata.setReleasedVersion( version );
            }
        }         
        
        RepositoryMetadataWriter.write( metadata, metadataFile );
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

            // TODO fix validation
            /*
            if ( file == null || file.length() == 0 )
            {
                addActionError( "Please add a file to upload." );
            }

            if ( !VersionUtil.isVersion( version ) )
            {
                addActionError( "Invalid version." );
            }
            */
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
