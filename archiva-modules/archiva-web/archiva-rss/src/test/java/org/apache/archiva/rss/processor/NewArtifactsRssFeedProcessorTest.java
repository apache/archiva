package org.apache.archiva.rss.processor;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class NewArtifactsRssFeedProcessorTest
    extends PlexusTestCase
{
    private RssFeedProcessor newArtifactsProcessor;
    
    private String rssDirectory;

    public void setUp()
        throws Exception
    {
        super.setUp();

        newArtifactsProcessor = (RssFeedProcessor) lookup( RssFeedProcessor.class, "new-artifacts" );
        rssDirectory = getBasedir() + "/target/test-classes/rss-feeds/";
        
        RssFeedGenerator generator = ( ( NewArtifactsRssFeedProcessor ) newArtifactsProcessor ).getGenerator();
        generator.setRssDirectory( rssDirectory );
        ( (NewArtifactsRssFeedProcessor) newArtifactsProcessor ).setGenerator( generator );
    }

    public void testProcess()
        throws Exception
    {
        List<ArchivaArtifact> newArtifacts = new ArrayList<ArchivaArtifact>();
        
        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.0", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.1", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "2.0", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.1", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.2", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.3-SNAPSHOT", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-three", "2.0-SNAPSHOT", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-four", "1.1-beta-2", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        newArtifacts.add( artifact );
        
        newArtifactsProcessor.process( newArtifacts );

        File outputFile = new File( rssDirectory, "new_artifacts_test-repo.xml" );        
        assertTrue( outputFile.exists() );
        
        outputFile = new File( rssDirectory, "new_versions_org.apache.archiva:artifact-one.xml" );        
        assertTrue( outputFile.exists() );
        
        outputFile = new File( rssDirectory, "new_versions_org.apache.archiva:artifact-two.xml" );        
        assertTrue( outputFile.exists() );
        
        outputFile = new File( rssDirectory, "new_versions_org.apache.archiva:artifact-three.xml" );        
        assertTrue( outputFile.exists() );
        
        outputFile = new File( rssDirectory, "new_versions_org.apache.archiva:artifact-four.xml" );        
        assertTrue( outputFile.exists() );
    }
}
