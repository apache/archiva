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
import org.apache.maven.archiva.configuration.Configuration; 
// import org.apache.maven.archiva.configuration.ArchivaConfiguration;
// import org.apache.maven.archiva.configuration.RepositoryConfiguration;
// import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
// import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
// import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import java.io.File;

/**
 * Upload an artifact.
 * 
 * @author Wendy Smoak
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="uploadAction"
 */
public class UploadAction
    extends PlexusActionSupport
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
     * @plexus.requirement role-hint="default"
     */
    // private ArchivaConfiguration configuration;
    /**
     * @plexus.requirement role-hint="default"
     */
    // private BidirectionalRepositoryLayoutFactory layoutFactory;
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
        getLogger().debug( "upload" );
        return SUCCESS;
    }

    public String doUpload()
    // throws LayoutException
    {
        // TODO: adapt to changes in RepositoryConfiguration from the MRM-462 branch
        // RepositoryConfiguration rc = configuration.getConfiguration().findRepositoryById( repositoryId );
        // String layout = rc.getLayout();
        // String url = rc.getUrl();
        // ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, packaging );
        // BidirectionalRepositoryLayout repositoryLayout = layoutFactory.getLayout( layout );

        // output from getLogger().debug(...) not appearing in logs, so...
        // System.out.println( "doUpload, file: " + file.getAbsolutePath() );
        // System.out.println( "doUpload, path: " + repositoryLayout.toPath( artifact ) );

        return SUCCESS;
    }
    
}
