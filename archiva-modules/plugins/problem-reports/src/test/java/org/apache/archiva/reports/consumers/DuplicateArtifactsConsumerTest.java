package org.apache.archiva.reports.consumers;

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

import junit.framework.TestCase;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.Mockito.*;

@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class DuplicateArtifactsConsumerTest
    extends TestCase
{
    @Inject
    @Named( value = "knownRepositoryContentConsumer#duplicate-artifacts" )
    private DuplicateArtifactsConsumer consumer;

    private ManagedRepository config;

    private MetadataRepository metadataRepository;

    private static final String TEST_REPO = "test-repo";

    private static final String TEST_CHECKSUM = "edf5938e646956f445c6ecb719d44579cdeed974";

    private static final String TEST_PROJECT = "test-artifact";

    private static final String TEST_NAMESPACE = "com.example.test";

    private static final String TEST_FILE =
        "com/example/test/test-artifact/1.0-SNAPSHOT/test-artifact-1.0-20100308.230825-1.jar";

    private static final String TEST_VERSION = "1.0-20100308.230825-1";

    private static final ArtifactMetadata TEST_METADATA = createMetadata( TEST_VERSION );

    @Inject
    @Named( value = "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    ApplicationContext applicationContext;


    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        assertNotNull( consumer );

        config = new ManagedRepository();
        config.setId( TEST_REPO );
        config.setLocation( new File( "target/test-repository" ).getAbsolutePath() );

        metadataRepository = mock( MetadataRepository.class );

        RepositorySession session = mock( RepositorySession.class );
        when( session.getRepository() ).thenReturn( metadataRepository );

        RepositorySessionFactory factory = applicationContext.getBean( RepositorySessionFactory.class );
        //(RepositorySessionFactory) lookup( RepositorySessionFactory.class );
        when( factory.createSession() ).thenReturn( session );

        when( pathTranslator.getArtifactForPath( TEST_REPO, TEST_FILE ) ).thenReturn( TEST_METADATA );
    }

    @Test
    public void testConsumerArtifactNotDuplicated()
        throws Exception
    {
        when( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ) ).thenReturn(
            Arrays.asList( TEST_METADATA ) );

        consumer.beginScan( config, new Date() );
        consumer.processFile( TEST_FILE );
        consumer.completeScan();

        verify( metadataRepository, never() ).addMetadataFacet( eq( TEST_REPO ), Matchers.<MetadataFacet>anyObject() );
    }

    // TODO: Doesn't currently work
//    public void testConsumerArtifactNotDuplicatedForOtherSnapshots()
//        throws ConsumerException
//    {
//        when( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ) ).thenReturn( Arrays.asList(
//            TEST_METADATA, createMetadata( "1.0-20100309.002023-2" ) ) );
//
//        consumer.beginScan( config, new Date() );
//        consumer.processFile( TEST_FILE );
//        consumer.completeScan();
//
//        verify( metadataRepository, never() ).addMetadataFacet( eq( TEST_REPO ), Matchers.<MetadataFacet>anyObject() );
//    }

    @Test
    public void testConsumerArtifactDuplicated()
        throws Exception
    {
        when( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ) ).thenReturn(
            Arrays.asList( TEST_METADATA, createMetadata( "1.0" ) ) );

        consumer.beginScan( config, new Date() );
        consumer.processFile( TEST_FILE );
        consumer.completeScan();

        ArgumentCaptor<RepositoryProblemFacet> argument = ArgumentCaptor.forClass( RepositoryProblemFacet.class );
        verify( metadataRepository ).addMetadataFacet( eq( TEST_REPO ), argument.capture() );
        RepositoryProblemFacet problem = argument.getValue();
        assertProblem( problem );
    }

    @Test
    public void testConsumerArtifactDuplicatedButSelfNotInMetadataRepository()
        throws Exception
    {
        when( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ) ).thenReturn(
            Arrays.asList( createMetadata( "1.0" ) ) );

        consumer.beginScan( config, new Date() );
        consumer.processFile( TEST_FILE );
        consumer.completeScan();

        ArgumentCaptor<RepositoryProblemFacet> argument = ArgumentCaptor.forClass( RepositoryProblemFacet.class );
        verify( metadataRepository ).addMetadataFacet( eq( TEST_REPO ), argument.capture() );
        RepositoryProblemFacet problem = argument.getValue();
        assertProblem( problem );
    }

    @Test
    public void testConsumerArtifactFileNotExist()
        throws Exception
    {
        consumer.beginScan( config, new Date() );
        try
        {
            consumer.processFile( "com/example/test/test-artifact/2.0/test-artifact-2.0.jar" );
            fail( "Should have failed to find file" );
        }
        catch ( ConsumerException e )
        {
            assertTrue( e.getCause() instanceof FileNotFoundException );
        }
        finally
        {
            consumer.completeScan();
        }

        verify( metadataRepository, never() ).addMetadataFacet( eq( TEST_REPO ), Matchers.<MetadataFacet>anyObject() );
    }

    @Test
    public void testConsumerArtifactNotAnArtifactPathNoResults()
        throws Exception
    {
        consumer.beginScan( config, new Date() );
        // No exception unnecessarily for something we can't report on
        consumer.processFile( "com/example/invalid-artifact.txt" );
        consumer.completeScan();

        verify( metadataRepository, never() ).addMetadataFacet( eq( TEST_REPO ), Matchers.<MetadataFacet>anyObject() );
    }

    @Test
    public void testConsumerArtifactNotAnArtifactPathResults()
        throws Exception
    {
        when( metadataRepository.getArtifactsByChecksum( eq( TEST_REPO ), anyString() ) ).thenReturn(
            Arrays.asList( TEST_METADATA, createMetadata( "1.0" ) ) );

        // override, this feels a little overspecified though
        when( pathTranslator.getArtifactForPath( TEST_REPO, "com/example/invalid-artifact.txt" ) ).thenThrow(
            new IllegalArgumentException() );

        consumer.beginScan( config, new Date() );
        // No exception unnecessarily for something we can't report on
        consumer.processFile( "com/example/invalid-artifact.txt" );
        consumer.completeScan();

        verify( metadataRepository, never() ).addMetadataFacet( eq( TEST_REPO ), Matchers.<MetadataFacet>anyObject() );
    }

    private static void assertProblem( RepositoryProblemFacet problem )
    {
        assertEquals( TEST_REPO, problem.getRepositoryId() );
        assertEquals( TEST_NAMESPACE, problem.getNamespace() );
        assertEquals( TEST_PROJECT, problem.getProject() );
        assertEquals( TEST_VERSION, problem.getVersion() );
        assertEquals( TEST_PROJECT + "-" + TEST_VERSION + ".jar", problem.getId() );
        assertNotNull( problem.getMessage() );
        assertEquals( "duplicate-artifact", problem.getProblem() );
    }

    private static ArtifactMetadata createMetadata( String version )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( TEST_PROJECT + "-" + version + ".jar" );
        artifact.setNamespace( TEST_NAMESPACE );
        artifact.setProject( TEST_PROJECT );
        artifact.setProjectVersion( version );
        artifact.setVersion( version );
        artifact.setRepositoryId( TEST_REPO );
        return artifact;
    }
}
