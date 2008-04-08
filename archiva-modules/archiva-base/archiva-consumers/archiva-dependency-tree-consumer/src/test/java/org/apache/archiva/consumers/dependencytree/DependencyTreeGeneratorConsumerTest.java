package org.apache.archiva.consumers.dependencytree;

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
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.profiles.DefaultProfileManager;
import org.codehaus.plexus.spring.PlexusContainerAdapter;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class DependencyTreeGeneratorConsumerTest
    extends PlexusInSpringTestCase
{
    private DependencyTreeGeneratorConsumer consumer;

    private ManagedRepositoryConfiguration repository;

    private File repositoryLocation;

    private File generatedRepositoryLocation;

    public void setUp()
        throws Exception
    {
        super.setUp();

        consumer =
            (DependencyTreeGeneratorConsumer) lookup( KnownRepositoryContentConsumer.class, "dependency-tree-generator" );

        repositoryLocation = getTestFile( "target/test-" + getName() + "/test-repo" );
        FileUtils.deleteDirectory( repositoryLocation );
        FileUtils.copyDirectory( getTestFile( "target/test-classes/test-repo" ), repositoryLocation );

        generatedRepositoryLocation = getTestFile( "target/test-" + getName() + "/generated-test-repo" );
        FileUtils.deleteDirectory( generatedRepositoryLocation );

        consumer.setGeneratedRepositoryLocation( generatedRepositoryLocation );

        repository = new ManagedRepositoryConfiguration();
        repository.setId( "dependency-tree" );
        repository.setLocation( repositoryLocation.getAbsolutePath() );
    }

    public void testGenerateBasicTree()
        throws IOException, ConsumerException
    {
        consumer.beginScan( repository );

        String path = "org/apache/maven/maven-core/2.0/maven-core-2.0.pom";
        consumer.processFile( path );

        File generatedFile = new File( generatedRepositoryLocation, path + ".xml" );
        assertEquals( IOUtils.toString( getClass().getResourceAsStream( "/test-data/maven-core-2.0-tree.xml" ) ),
                      FileUtils.readFileToString( generatedFile ) );

        consumer.completeScan();
    }

    public void testInvalidCoordinate()
        throws IOException, ConsumerException
    {
        consumer.beginScan( repository );

        String path = "openejb/jaxb-xjc/2.0EA3/jaxb-xjc-2.0EA3.pom";
        try
        {
            consumer.processFile( path );

            fail( "Should not have successfully processed the file" );
        }
        catch ( ConsumerException e )
        {
            File generatedFile = new File( generatedRepositoryLocation, path + ".xml" );
            assertFalse( generatedFile.exists() );
        }

        consumer.completeScan();
    }

    public void testProfiles()
        throws IOException, ConsumerException
    {
        PlexusContainerAdapter container = new PlexusContainerAdapter();
        container.setApplicationContext( getApplicationContext() );
        
        DefaultProfileManager m = new DefaultProfileManager( container );
        
        consumer.beginScan( repository );

        String path = "org/apache/maven/surefire/surefire-testng/2.0/surefire-testng-2.0.pom";
        consumer.processFile( path );

        File generatedFile = new File( generatedRepositoryLocation, path + ".xml" );
        assertEquals( IOUtils.toString( getClass().getResourceAsStream( "/test-data/surefire-testng-2.0-tree.xml" ) ),
                      FileUtils.readFileToString( generatedFile ) );

        consumer.completeScan();
    }
}
