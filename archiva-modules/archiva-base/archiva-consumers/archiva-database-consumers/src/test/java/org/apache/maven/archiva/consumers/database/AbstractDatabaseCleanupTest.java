package org.apache.maven.archiva.consumers.database;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public abstract class AbstractDatabaseCleanupTest
    extends PlexusInSpringTestCase
{
    ArchivaConfiguration archivaConfig;
    
    RepositoryContentFactory repositoryFactory;

    public static final String TEST_GROUP_ID = "org.apache.maven.archiva";

    public static final String TEST_ARTIFACT_ID = "cleanup-artifact-test";

    public static final String TEST_VERSION = "1.0";

    public static final String TEST_REPO_ID = "test-repo";

    public void setUp()
        throws Exception
    {
        super.setUp();

        // archiva configuration (need to update the repository url)
        File userFile = getTestFile( "target/test/repository-manager.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        userFile.getParentFile().mkdirs();
        FileUtils.copyFileToDirectory( getTestFile( "src/test/conf/repository-manager.xml" ),
                                       userFile.getParentFile() );

        archivaConfig = (ArchivaConfiguration) lookup( ArchivaConfiguration.class, "database-cleanup" );

        Configuration configuration = archivaConfig.getConfiguration();
        ManagedRepositoryConfiguration repo = configuration.findManagedRepositoryById( TEST_REPO_ID );
        repo.setLocation( new File( getBasedir(), "src/test/resources/test-repo" ).toString() );

        archivaConfig.save( configuration );
        
        repositoryFactory = (RepositoryContentFactory) lookup( RepositoryContentFactory.class );
    }

    protected ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        ArchivaArtifactModel model = new ArchivaArtifactModel();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setType( type );
        model.setRepositoryId( TEST_REPO_ID );

        return new ArchivaArtifact( model );
    }

    protected ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel projectModel = new ArchivaProjectModel();
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );

        return projectModel;
    }
}
