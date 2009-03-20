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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.SnapshotVersion;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelWriter;
import org.apache.maven.archiva.repository.project.writers.ProjectModel400Writer;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Upload an artifact using Jakarta file upload in webwork. If set by the user a pom will also be generated. Metadata
 * will also be updated if one exists, otherwise it would be created.
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="uploadAction" instantiation-strategy="per-lookup"
 */
public class UploadAction
    extends PlexusActionSupport
    implements Validateable, Preparable, Auditable
{
    /**
      * @plexus.requirement
      */
     private RepositoryContentConsumers consumers;
     
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
     * The temporary file representing the artifact to be deployed.
     */
    private File artifactFile;

    /**
     * The temporary file representing the pom to be deployed alongside the artifact.
     */
    private File pomFile;

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
    
    private ChecksumAlgorithm[] algorithms = new ChecksumAlgorithm[] { ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 };

    private ProjectModelWriter pomWriter = new ProjectModel400Writer();
    
    public void setArtifact( File file )
    {
        this.artifactFile = file;
    }

    public void setArtifactContentType( String contentType )
    {
        StringUtils.trim( contentType );
    }

    public void setArtifactFileName( String filename )
    {
        StringUtils.trim( filename );
    }

    public void setPom( File file )
    {
        this.pomFile = file;
    }

    public void setPomContentType( String contentType )
    {
        StringUtils.trim( contentType );
    }

    public void setPomFileName( String filename )
    {
        StringUtils.trim( filename );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = StringUtils.trim( groupId );
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = StringUtils.trim( artifactId );
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = StringUtils.trim( version );
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = StringUtils.trim( packaging );
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = StringUtils.trim( classifier );
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
        managedRepoIdList = getManagableRepos();
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
        packaging = "";
        classifier = "";
        artifactFile = null;
        pomFile = null;
        repositoryId = "";
        generatePom = false;
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

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = -1;
            String timestamp = null;
            
            File metadataFile = getMetadata( targetPath.getAbsolutePath() );
            ArchivaRepositoryMetadata metadata = getMetadata( metadataFile );

            if (VersionUtil.isSnapshot(version))
            {
                TimeZone timezone = TimeZone.getTimeZone( "UTC" );
                DateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
                fmt.setTimeZone( timezone );
                timestamp = fmt.format( lastUpdatedTimestamp );
                if ( metadata.getSnapshotVersion() != null )
                {
                    newBuildNumber = metadata.getSnapshotVersion().getBuildNumber() + 1;
                }
                else
                {
                	metadata.setSnapshotVersion( new SnapshotVersion() );
                	newBuildNumber = 1;
                }
            }

            if ( !targetPath.exists() )
            {
                targetPath.mkdirs();
            }

            String filename = artifactPath.substring( lastIndex + 1 );
            if ( VersionUtil.isSnapshot( version ) )
            {
                filename = filename.replaceAll( "SNAPSHOT", timestamp + "-" + newBuildNumber );
            }

            try
            {
                copyFile( artifactFile, targetPath, filename );
                consumers.executeConsumers( repoConfig, repository.toFile( artifactReference ) );
            }
            catch ( IOException ie )
            {
                addActionError( "Error encountered while uploading file: " + ie.getMessage() );
                return ERROR;
            }

            String pomFilename = filename;
            if( classifier != null && !"".equals( classifier ) )
            {
                pomFilename = StringUtils.remove( pomFilename, "-" + classifier );
            }
            pomFilename = FilenameUtils.removeExtension( pomFilename ) + ".pom";
                
            if ( generatePom )
            {
                try
                {
                    File generatedPomFile = createPom( targetPath, pomFilename );
                    consumers.executeConsumers( repoConfig, generatedPomFile );
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
            
            if ( pomFile != null && pomFile.length() > 0 ) 
            {
                try
                {                    
                    copyFile( pomFile, targetPath, pomFilename );
                    consumers.executeConsumers( repoConfig, new File( targetPath, pomFilename ) );
                }
                catch ( IOException ie )
                {
                    addActionError( "Error encountered while uploading pom file: " + ie.getMessage() );
                    return ERROR;
                }
                
            }

            updateMetadata( metadata, metadataFile, lastUpdatedTimestamp, timestamp, newBuildNumber );

            String msg = "Artifact \'" + groupId + ":" + artifactId + ":" + version +
                "\' was successfully deployed to repository \'" + repositoryId + "\'";
                        
            triggerAuditEvent( repositoryId, groupId + ":" + artifactId + ":" + version, AuditEvent.UPLOAD_FILE );
            
            addActionMessage( msg );

            reset();
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

    private void copyFile( File sourceFile, File targetPath, String targetFilename )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( new File( targetPath, targetFilename ) );

        try
        {
            FileInputStream input = new FileInputStream( sourceFile );
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

    private File createPom( File targetPath, String filename )
        throws IOException, ProjectModelException
    {
        ArchivaProjectModel projectModel = new ArchivaProjectModel();
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );
        projectModel.setPackaging( packaging );
        
        File pomFile = new File( targetPath, filename);        
        pomWriter.write( projectModel, pomFile );

        return pomFile;
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
     * Update artifact level metadata. If it does not exist, create the metadata.
     * 
     * @param metadata
     */
    private void updateMetadata( ArchivaRepositoryMetadata metadata, File metadataFile, Date lastUpdatedTimestamp,
                                 String timestamp, int buildNumber )
        throws RepositoryMetadataException
    {
        List<String> availableVersions = new ArrayList<String>();
        String latestVersion = version;

        if ( metadataFile.exists() )
        {
            availableVersions = metadata.getAvailableVersions();

            Collections.sort( availableVersions, VersionComparator.getInstance() );

            if ( !availableVersions.contains( version ) )
            {
                availableVersions.add( version );
            }

            latestVersion = availableVersions.get( availableVersions.size() - 1 );
        }
        else
        {
            availableVersions.add( version );

            metadata.setGroupId( groupId );
            metadata.setArtifactId( artifactId );
        }

        if ( metadata.getGroupId() == null )
        {
        	metadata.setGroupId( groupId );
        }
        if ( metadata.getArtifactId() == null )
        {
        	metadata.setArtifactId( artifactId );
        }

        metadata.setLatestVersion( latestVersion );
        metadata.setLastUpdatedTimestamp( lastUpdatedTimestamp );
        metadata.setAvailableVersions( availableVersions );

        if ( !VersionUtil.isSnapshot( version ) )
        {
            metadata.setReleasedVersion( latestVersion );
        }
        else
        {
            metadata.getSnapshotVersion().setBuildNumber( buildNumber );

            metadata.getSnapshotVersion().setTimestamp( timestamp );
        }

        RepositoryMetadataWriter.write( metadata, metadataFile );
        ChecksummedFile checksum = new ChecksummedFile( metadataFile );
        checksum.fixChecksums( algorithms );
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

            if ( artifactFile == null || artifactFile.length() == 0 )
            {
                addActionError( "Please add a file to upload." );
            }
            
            if ( !VersionUtil.isVersion( version ) )
            {
                addActionError( "Invalid version." );
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
}
