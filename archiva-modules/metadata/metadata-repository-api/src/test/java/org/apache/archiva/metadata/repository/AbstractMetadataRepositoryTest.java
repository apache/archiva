package org.apache.archiva.metadata.repository;

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
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.metadata.QueryParameter;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.generic.GenericMetadataFacetFactory;
import org.apache.archiva.metadata.model.*;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml"} )
public abstract class AbstractMetadataRepositoryTest
    extends TestCase
{
    protected static final String OTHER_REPO_ID = "other-repo";


    protected static final String TEST_REPO_ID = "test";

    protected static final String TEST_PROJECT = "myproject";

    protected static final String TEST_NAMESPACE = "mytest";

    protected static final String TEST_PROJECT_VERSION = "1.0";

    private static final String TEST_PROJECT_VERSION_2_0 = "2.0";

    protected static final String TEST_URL = "http://archiva.apache.org";

    private static final Organization TEST_ORGANIZATION = new Organization( "Apache", "http://apache.org" );

    private static final String TEST_FACET_ID = "test-facet-id";

    private static final String TEST_NAME = "test/name";

    private static final String TEST_VALUE = "test-value";

    private static final String UNKNOWN = "unknown";

    private static final String TEST_SHA256 = "e43857b4e75e04a09d167564ca9a4636e5d233035483ba4ecf1243e34325d565";

    private static final String TEST_MD5 = "bd4a9b642562547754086de2dab26b7d";

    private static final String TEST_SHA1 = "2e5daf0201ddeb068a62d5e08da18657ab2c6be9";

    private static final String TEST_METADATA_KEY = "testkey";

    private static final String TEST_METADATA_VALUE = "testmetadata";

    protected Logger log = LoggerFactory.getLogger( getClass( ) );

    protected int assertMaxTries =10;
    protected int assertRetrySleepMs=500;

    /*
     * Used by tryAssert to allow to throw exceptions in the lambda expression.
     */
    @FunctionalInterface
    protected interface AssertFunction
    {
        void accept( ) throws Exception;
    }

    protected void tryAssert( AssertFunction func ) throws Exception
    {
        tryAssert( func, assertMaxTries, assertRetrySleepMs );
    }


    protected abstract RepositorySessionFactory getSessionFactory( );

    protected abstract MetadataRepository getRepository( );

    /*
     * Runs the assert method until the assert is successful or the number of retries
     * is reached. This is needed because the JCR Oak index update is asynchronous, so updates
     * may not be visible immediately after the modification.
     */
    private void tryAssert( AssertFunction func, int retries, int sleepMillis ) throws Exception
    {
        Throwable t = null;
        int retry = retries;
        while ( retry-- > 0 )
        {
            try
            {
                func.accept( );
                return;
            }
            catch ( Exception | AssertionError e )
            {
                t = e;
                Thread.currentThread( ).sleep( sleepMillis );
                log.warn( "Retrying assert {}: {}", retry, e.getMessage( ) );
            }
        }
        log.warn( "Retries: {}, Exception: {}", retry, t.getMessage( ) );
        if ( retry <= 0 && t != null )
        {
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
            else if ( t instanceof Exception )
            {
                throw (Exception) t;
            }
            else if ( t instanceof Error )
            {
                throw (Error) t;
            }
        }
    }


    public static List<MetadataFacetFactory> createTestMetadataFacetFactories( )
    {
        List<MetadataFacetFactory> factories = new ArrayList<>( );
        factories.add( new MetadataFacetFactory<TestMetadataFacet>( )
        {
            @Override
            public TestMetadataFacet createMetadataFacet( )
            {
                return new TestMetadataFacet( TEST_METADATA_VALUE );
            }

            @Override
            public TestMetadataFacet createMetadataFacet( String repositoryId, String name )
            {
                return new TestMetadataFacet( TEST_FACET_ID, TEST_METADATA_VALUE, name );
            }

            @Override
            public Class<TestMetadataFacet> getFacetClass( )
            {
                return TestMetadataFacet.class;
            }

            @Override
            public String getFacetId( )
            {
                return TEST_FACET_ID;
            }
        } );

        // for the getArtifactsByProjectVersionFacet tests
        factories.add( new GenericMetadataFacetFactory( ) );

        return factories;
    }


    @Test
    public void testRootNamespaceWithNoMetadataRepository( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {
                Collection<String> namespaces = getRepository( ).getRootNamespaces( session, TEST_REPO_ID );
                assertThat( namespaces ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testGetNamespaceOnly( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {
                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isEmpty( );
            } );
            getRepository( ).updateNamespace( session, TEST_REPO_ID, TEST_NAMESPACE );

            tryAssert( ( ) -> {
                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isNotEmpty( ).contains(
                    TEST_NAMESPACE ).hasSize( 1 );
            } );
            getRepository( ).removeNamespace( session, TEST_REPO_ID, TEST_NAMESPACE );

            tryAssert( ( ) -> {
                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testGetProjectOnly( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            tryAssert( ( ) -> {
                assertNull( getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isEmpty( );
            } );

            ProjectMetadata project = new ProjectMetadata( );
            project.setId( TEST_PROJECT );
            project.setNamespace( TEST_NAMESPACE );

            getRepository( ).updateProject( session, TEST_REPO_ID, project );

            tryAssert( ( ) -> {
                ProjectMetadata proj = getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
                assertEquals( TEST_PROJECT, proj.getId( ) );
                assertEquals( TEST_NAMESPACE, proj.getNamespace( ) );
            } );

            // test that namespace is also constructed


            tryAssert( ( ) -> {
                Collection<String> namespaces = getRepository( ).getRootNamespaces( session, TEST_REPO_ID );

                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).contains( TEST_NAMESPACE ).hasSize( 1 );
            } );
        }
    }

    @Test
    public void testGetProjectVersionOnly( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            tryAssert( ( ) -> {
                assertNull( getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) );
                assertNull( getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isEmpty( );
            } );

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );

            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            metadata = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            assertEquals( TEST_PROJECT_VERSION, metadata.getId( ) );

            tryAssert( ( ) -> {

                // test that namespace and project is also constructed
                Collection<String> namespaces = getRepository( ).getRootNamespaces( session, TEST_REPO_ID );

                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_NAMESPACE );

            } );

            tryAssert( ( ) -> {

                ProjectMetadata projectMetadata = getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
                assertNotNull( projectMetadata );
                assertEquals( TEST_PROJECT, projectMetadata.getId( ) );
                assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace( ) );
            } );
        }
    }

    @Test
    public void testGetArtifactOnly( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            tryAssert( ( ) -> {
                assertThat( new ArrayList<>(
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                        TEST_PROJECT, TEST_PROJECT_VERSION ) ) ).isNotNull( ).isEmpty( );
                assertThat(
                    getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ).isNull( );
                assertThat( getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) ).isNull( );

                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isEmpty( );

            } );

            ArtifactMetadata metadata = createArtifact( );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                //assertEquals( Collections.singletonList( metadata ), new ArrayList<ArtifactMetadata>( artifacts ) );
                assertThat( artifacts ).containsExactly( metadata );
                // test that namespace, project and project version is also constructed

                assertThat( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ) ).isNotNull( ).isNotEmpty( ).contains(
                    TEST_NAMESPACE ).hasSize( 1 );

                ProjectMetadata projectMetadata = getRepository( ).getProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
                assertEquals( TEST_PROJECT, projectMetadata.getId( ) );
                assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace( ) );

                ProjectVersionMetadata projectVersionMetadata =
                    getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertEquals( TEST_PROJECT_VERSION, projectVersionMetadata.getId( ) );
            } );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataWithNoOtherArchives( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            MailingList mailingList = new MailingList( );
            mailingList.setName( "Foo List" );
            mailingList.setOtherArchives( Collections.<String>emptyList( ) );
            metadata.setMailingLists( Arrays.asList( mailingList ) );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            metadata = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            assertEquals( TEST_PROJECT_VERSION, metadata.getId( ) );

            List<MailingList> mailingLists = metadata.getMailingLists( );

            assertThat( mailingLists ).isNotNull( ).isNotEmpty( ).hasSize( 1 );

            mailingList = metadata.getMailingLists( ).get( 0 );
            assertEquals( "Foo List", mailingList.getName( ) );

            List<String> others = mailingList.getOtherArchives( );
            assertThat( others ).isNotNull( ).isEmpty( );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataWithAllElements( )
        throws Exception
    {

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );

            metadata.setName( "project name" );
            metadata.setDescription( "project description" );
            metadata.setUrl( "the url" );

            MailingList mailingList = new MailingList( );
            mailingList.setName( "Foo List" );
            mailingList.setUnsubscribeAddress( "UnsubscribeAddress" );
            mailingList.setSubscribeAddress( "SubscribeAddress" );
            mailingList.setPostAddress( "PostAddress" );
            mailingList.setMainArchiveUrl( "MainArchiveUrl" );
            mailingList.setOtherArchives( Arrays.asList( "other archive" ) );
            metadata.setMailingLists( Arrays.asList( mailingList ) );

            Scm scm = new Scm( );
            scm.setConnection( "connection" );
            scm.setDeveloperConnection( "dev conn" );
            scm.setUrl( "url" );
            metadata.setScm( scm );

            CiManagement ci = new CiManagement( );
            ci.setSystem( "system" );
            ci.setUrl( "ci url" );
            metadata.setCiManagement( ci );

            IssueManagement tracker = new IssueManagement( );
            tracker.setSystem( "system" );
            tracker.setUrl( "issue tracker url" );
            metadata.setIssueManagement( tracker );

            metadata.setOrganization( TEST_ORGANIZATION );

            License l = new License( );
            l.setName( "license name" );
            l.setUrl( "license url" );
            metadata.addLicense( l );

            Dependency d = new Dependency( );
            d.setArtifactId( "artifactId" );
            d.setClassifier( "classifier" );
            d.setNamespace( "groupId" );
            d.setScope( "scope" );
            d.setSystemPath( "system path" );
            d.setType( "type" );
            d.setVersion( "version" );
            d.setOptional( true );
            metadata.addDependency( d );

            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            metadata = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            assertEquals( TEST_PROJECT_VERSION, metadata.getId( ) );
            assertEquals( TEST_PROJECT_VERSION, metadata.getVersion( ) );
            assertEquals( "project name", metadata.getName( ) );
            assertEquals( "project description", metadata.getDescription( ) );
            assertEquals( "the url", metadata.getUrl( ) );

            assertEquals( "connection", metadata.getScm( ).getConnection( ) );
            assertEquals( "dev conn", metadata.getScm( ).getDeveloperConnection( ) );
            assertEquals( "url", metadata.getScm( ).getUrl( ) );

            assertEquals( "system", metadata.getCiManagement( ).getSystem( ) );
            assertEquals( "ci url", metadata.getCiManagement( ).getUrl( ) );

            assertEquals( "system", metadata.getIssueManagement( ).getSystem( ) );
            assertEquals( "issue tracker url", metadata.getIssueManagement( ).getUrl( ) );

            assertEquals( TEST_ORGANIZATION.getName( ), metadata.getOrganization( ).getName( ) );
            assertEquals( TEST_ORGANIZATION.getUrl( ), metadata.getOrganization( ).getUrl( ) );

            assertEquals( 1, metadata.getMailingLists( ).size( ) );
            MailingList retrievedMailingList = metadata.getMailingLists( ).get( 0 );
            assertEquals( mailingList.getName( ), retrievedMailingList.getName( ) );
            assertEquals( mailingList.getMainArchiveUrl( ), retrievedMailingList.getMainArchiveUrl( ) );
            assertEquals( mailingList.getPostAddress( ), retrievedMailingList.getPostAddress( ) );
            assertEquals( mailingList.getSubscribeAddress( ), retrievedMailingList.getSubscribeAddress( ) );
            assertEquals( mailingList.getUnsubscribeAddress( ), retrievedMailingList.getUnsubscribeAddress( ) );
            assertThat( retrievedMailingList.getOtherArchives( ) ) //
                .isNotNull( ) //
                .isNotEmpty( ) //
                .hasSize( 1 ) //
                .contains( "other archive" );

            assertEquals( 1, metadata.getLicenses( ).size( ) );
            l = metadata.getLicenses( ).get( 0 );
            assertEquals( "license name", l.getName( ) );
            assertEquals( "license url", l.getUrl( ) );

            assertEquals( 1, metadata.getDependencies( ).size( ) );
            d = metadata.getDependencies( ).get( 0 );
            assertEquals( "artifactId", d.getArtifactId( ) );
            assertEquals( "classifier", d.getClassifier( ) );
            assertEquals( "groupId", d.getNamespace( ) );
            assertEquals( "scope", d.getScope( ) );
            assertEquals( "system path", d.getSystemPath( ) );
            assertEquals( "type", d.getType( ) );
            assertEquals( "version", d.getVersion( ) );
            assertTrue( d.isOptional( ) );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataIncomplete( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            metadata.setIncomplete( true );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            tryAssert( ( ) -> {
                ProjectVersionMetadata metadata1 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertEquals( true, metadata1.isIncomplete( ) );
                assertNull( metadata1.getCiManagement( ) );
                assertNull( metadata1.getScm( ) );
                assertNull( metadata1.getIssueManagement( ) );
                assertNull( metadata1.getOrganization( ) );
                assertTrue( metadata1.getDescription( )==null || "".equals(metadata1.getDescription()) );
                assertTrue( metadata1.getName( )==null || "".equals(metadata1.getName()) );
                assertEquals( TEST_PROJECT_VERSION, metadata1.getId( ) );
                assertEquals( TEST_PROJECT_VERSION, metadata1.getVersion( ) );
                assertTrue( metadata1.getMailingLists( ).isEmpty( ) );
                assertTrue( metadata1.getLicenses( ).isEmpty( ) );
                assertTrue( metadata1.getDependencies( ).isEmpty( ) );
            } );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataWithExistingFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            MetadataFacet facet = new TestMetadataFacet( "baz" );
            metadata.addFacet( facet );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            tryAssert( ( ) -> {

                ProjectVersionMetadata metadata1 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertEquals( Collections.singleton( TEST_FACET_ID ), metadata1.getFacetIds( ) );
            } );

            metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            tryAssert( ( ) -> {

                ProjectVersionMetadata metadata2 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertEquals( Collections.singleton( TEST_FACET_ID ), metadata2.getFacetIds( ) );
                TestMetadataFacet testFacet = (TestMetadataFacet) metadata2.getFacet( TEST_FACET_ID );
                assertEquals( "baz", testFacet.getValue( ) );
            } );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataWithNoExistingFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );


            tryAssert( ( ) -> {
                ProjectVersionMetadata metadata1 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( metadata1.getFacetIds( ) ).isNotNull( ).isEmpty( );
            } );

            ProjectVersionMetadata metadata1 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            tryAssert( ( ) -> {
                ProjectVersionMetadata metadata2 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertThat( metadata2.getFacetIds( ) ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testUpdateProjectVersionMetadataWithExistingFacetsFacetPropertyWasRemoved( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );

            Map<String, String> additionalProps = new HashMap<>( );
            additionalProps.put( "deleteKey", "deleteValue" );

            MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz", additionalProps );
            metadata.addFacet( facet );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );


            tryAssert( ( ) -> {

                ProjectVersionMetadata metad = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( metad.getFacetIds( ) ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_FACET_ID );


            } );

            ProjectVersionMetadata metad = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            TestMetadataFacet testFacet = (TestMetadataFacet) metad.getFacet( TEST_FACET_ID );
            Map<String, String> facetProperties = testFacet.toProperties( );

            assertEquals( "deleteValue", facetProperties.get( "deleteKey" ) );

            facetProperties.remove( "deleteKey" );

            TestMetadataFacet newTestFacet = new TestMetadataFacet( TEST_FACET_ID, testFacet.getValue( ), facetProperties );
            metadata.addFacet( newTestFacet );

            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );


            tryAssert( ( ) -> {
                ProjectVersionMetadata metad2 = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( metad2.getFacetIds( ) ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_FACET_ID );
                TestMetadataFacet testFacet2 = (TestMetadataFacet) metad2.getFacet( TEST_FACET_ID );
                assertFalse( testFacet2.toProperties( ).containsKey( "deleteKey" ) );
            } );
        }
    }

    @Test
    public void testGetArtifactsDoesntReturnProjectVersionMetadataFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata( );
            versionMetadata.setId( TEST_PROJECT_VERSION );

            MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz" );
            versionMetadata.addFacet( facet );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, versionMetadata );
            ArtifactMetadata artifactMetadata = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifactMetadata );
            session.save( );

            tryAssert( ( ) -> {


                Collection<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

                artifacts = getRepository( ).getArtifacts( session, TEST_REPO_ID );
                assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

                artifacts = getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA1 );
                assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

                artifacts = getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_MD5 );
                assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

                artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, null );
                assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );
            } );
        }
    }

    @Test
    public void testUpdateArtifactMetadataWithExistingFacetsFacetPropertyWasRemoved( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ArtifactMetadata metadata = createArtifact( );

            Map<String, String> additionalProps = new HashMap<>( );
            additionalProps.put( "deleteKey", "deleteValue" );

            MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz", additionalProps );
            metadata.addFacet( facet );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( artifacts ).isNotNull( ).isNotEmpty( ).hasSize( 1 );
            } );

            Collection<ArtifactMetadata> artifacts =
                getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            metadata = artifacts.iterator( ).next( );

            Collection<String> ids = metadata.getFacetIds( );
            assertThat( ids ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_FACET_ID );

            TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
            Map<String, String> facetProperties = testFacet.toProperties( );

            assertEquals( "deleteValue", facetProperties.get( "deleteKey" ) );

            facetProperties.remove( "deleteKey" );

            TestMetadataFacet newTestFacet = new TestMetadataFacet( TEST_FACET_ID, testFacet.getValue( ), facetProperties );
            metadata.addFacet( newTestFacet );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );
            session.save( );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts1 = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( artifacts1 ).isNotNull( ).isNotEmpty( ).hasSize( 1 );
            } );
            Collection<ArtifactMetadata> artifacts1 = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            metadata = artifacts.iterator( ).next( );

            ids = metadata.getFacetIds( );
            assertThat( ids ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_FACET_ID );

            testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );

            Map<String, String> props = testFacet.toProperties( );
            assertThat( props ).isNotNull( ).doesNotContainKey( "deleteKey" );
        }
    }

    @Test
    public void testUpdateArtifactMetadataWithExistingFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ArtifactMetadata metadata = createArtifact( );
            MetadataFacet facet = new TestMetadataFacet( "baz" );
            metadata.addFacet( facet );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

            metadata = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION ).iterator( ).next( );
            assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds( ) );

            metadata = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

            metadata = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION ).iterator( ).next( );
            assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds( ) );
            TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
            assertEquals( "baz", testFacet.getValue( ) );
        }
    }

    @Test
    public void testUpdateArtifactMetadataWithNoExistingFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata metadata = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

            tryAssert( ( ) -> {

                ArtifactMetadata metadata2 = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                    TEST_PROJECT, TEST_PROJECT_VERSION ).iterator( ).next( );
                assertEquals( Collections.<String>emptyList( ), new ArrayList<String>( metadata2.getFacetIds( ) ) );

            } );

            metadata = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION ).iterator( ).next( );
            metadata = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );


            tryAssert( ( ) -> {
                ArtifactMetadata metadata3 = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                    TEST_PROJECT, TEST_PROJECT_VERSION ).iterator( ).next( );
                assertEquals( Collections.<String>emptyList( ), new ArrayList<String>( metadata3.getFacetIds( ) ) );
            } );
        }
    }

    @Test
    public void testGetMetadataFacet( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

            TestMetadataFacet test =
                (TestMetadataFacet) getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

            assertEquals( new TestMetadataFacet( TEST_VALUE ), test );

        }
    }

    @Test
    public void testGetMetadataFacetByClass( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

            TestMetadataFacet test =
                (TestMetadataFacet) getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TestMetadataFacet.class, TEST_NAME );

            assertEquals( new TestMetadataFacet( TEST_VALUE ), test );

        }
    }

    @Test
    public void testGetMetadataFacetWhenEmpty( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> assertNull( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) ) );
        }
    }

    @Test
    public void testGetMetadataFacetWhenUnknownName( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

            tryAssert( ( ) -> assertNull( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, UNKNOWN ) ) );
        }
    }

    @Test
    public void testGetMetadataFacetWhenDefaultValue( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( null ) );

            tryAssert( ( ) -> {
                MetadataFacet metadataFacet = getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

                assertEquals( new TestMetadataFacet( TEST_METADATA_VALUE ), metadataFacet );
            } );

        }
    }

    @Test
    public void testGetMetadataFacetWhenUnknownFacetId( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            assertNull( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, UNKNOWN, TEST_NAME ) );

        }
    }

    @Test
    public void testGetMetadataFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

            assertEquals( Collections.singletonList( TEST_NAME ),
                getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID ) );

        }
    }

    @Test
    public void testGetMetadataFacetsStream( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );
        }

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {
                Stream<TestMetadataFacet> str = getRepository( ).getMetadataFacetStream( session, TEST_REPO_ID, TestMetadataFacet.class );
                assertNotNull( str );
                List<TestMetadataFacet> result = str.collect( Collectors.toList( ) );
                assertEquals( 1, result.size( ) );
                assertNotNull( result.get( 0 ) );
                assertEquals( TEST_NAME, result.get( 0 ).getName( ) );
            } );

        }
    }

    @Test
    public void testGetMetadataFacetsStreamWithLimit( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            for (int i = 0; i<500; i++)
            {
                getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_FACET_ID, TEST_VALUE, TEST_NAME+"/"+String.format("%03d",i) ) );
            }
        }

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            session.refreshAndDiscard();
            tryAssert( ( ) -> {
                Stream<TestMetadataFacet> str = getRepository( ).getMetadataFacetStream( session, TEST_REPO_ID, TestMetadataFacet.class, new QueryParameter(0, 100));
                assertNotNull( str );
                List<TestMetadataFacet> result = str.collect( Collectors.toList( ) );
                assertEquals( 100, result.size( ) );
                assertNotNull( result.get( 0 ) );
                for (int i=0; i<10; i++) {
                    assertEquals(TEST_NAME + "/" + String.format("%03d",i), result.get(i).getName());
                }
            }, 5, 500 );

        }
    }

    @Test
    public void testGetMetadataFacetsStreamWithOffset( )
            throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            for (int i = 0; i<100; i++)
            {
                getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_FACET_ID, TEST_VALUE, TEST_NAME+"/"+String.format("%03d", i) ) );
            }
        }

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            session.refreshAndDiscard();
            tryAssert( ( ) -> {
                Stream<TestMetadataFacet> str = getRepository( ).getMetadataFacetStream( session, TEST_REPO_ID, TestMetadataFacet.class, new QueryParameter(5, 10));
                assertNotNull( str );
                List<TestMetadataFacet> result = str.collect( Collectors.toList( ) );
                assertEquals( 10, result.size( ) );
                assertNotNull( result.get( 0 ) );
                for (int i=0; i<10; i++) {
                    assertEquals(TEST_NAME + "/" + String.format("%03d",i+5), result.get(i).getName());
                }
            }, 5, 500 );

        }
    }

    @Test
    public void testGetArtifactsByDateRangeStreamLowerAndUpperBound( )
            throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            final ArtifactMetadata artifact1 = createArtifact( );
            artifact1.setWhenGathered(ZonedDateTime.now().minusDays(1));
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );

            ZonedDateTime gatheredNow = ZonedDateTime.now();
            final ArtifactMetadata artifact2 = createArtifact( );
            String artifact2Id = artifact2.getId() + "-2";
            artifact2.setId(artifact2Id);
            artifact2.setVersion(TEST_PROJECT_VERSION+"-2");
            artifact2.setWhenGathered(gatheredNow);
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );
            final ArtifactMetadata artifact3 = createArtifact();
            String artifact3Id = artifact3.getId() + "-3";
            artifact3.setId(artifact3Id);
            artifact3.setVersion(TEST_PROJECT_VERSION+"-3");
            artifact3.setWhenGathered(gatheredNow.minusSeconds(5));

            final ArtifactMetadata artifact4 = createArtifact();
            artifact4.setId(artifact4.getId()+"-4");
            artifact4.setVersion(TEST_PROJECT_VERSION+"-4");
            artifact4.setWhenGathered(gatheredNow.plusDays(1));

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact3 );
            session.save( );

            ZonedDateTime lower = artifact2.getWhenGathered().minusSeconds(10);
            ZonedDateTime upper = artifact2.getWhenGathered().plusSeconds(10);

            tryAssert( ( ) -> {
                Stream<ArtifactMetadata> stream = getRepository().getArtifactByDateRangeStream(session, TEST_REPO_ID, lower, upper, new QueryParameter());
                assertNotNull(stream);

                List<ArtifactMetadata> artifacts = stream.collect(Collectors.toList());
                assertEquals(2, artifacts.size());
                assertEquals(artifact3Id, artifacts.get(0).getId());
                assertEquals(artifact2Id, artifacts.get(1).getId());

            } );
        }
    }

    @Test
    public void testGetMetadataFacetsWhenEmpty( )
        throws Exception
    {

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {

                List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
                assertTrue( facets.isEmpty( ) );
            } );
        }
    }

    @Test
    public void testRemoveFacets( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

            List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
            assertFalse( facets.isEmpty( ) );

            getRepository( ).removeMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );

            facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
            assertTrue( facets.isEmpty( ) );

        }
    }

    @Test
    public void testRemoveFacetsWhenEmpty( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
            assertTrue( facets.isEmpty( ) );

            getRepository( ).removeMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );

            tryAssert( ( ) -> {
                List<String> facets1 = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
                assertTrue( facets1.isEmpty( ) );
            } );
        }
    }

    @Test
    public void testRemoveFacetsWhenUnknown( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
// testing no exception
            getRepository( ).removeMetadataFacets( session, TEST_REPO_ID, UNKNOWN );

        }
    }

    @Test
    public void testRemoveFacetWhenUnknown( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
// testing no exception
            getRepository( ).removeMetadataFacet( session, TEST_REPO_ID, UNKNOWN, TEST_NAME );

        }
    }

    @Test
    public void testRemoveFacet( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            TestMetadataFacet metadataFacet = new TestMetadataFacet( TEST_VALUE );
            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, metadataFacet );

            assertEquals( metadataFacet, getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
            List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
            assertFalse( facets.isEmpty( ) );

            getRepository( ).removeMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

            assertNull( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
            facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
            assertTrue( facets.isEmpty( ) );

        }
    }

    @Test
    public void testRemoveFacetWhenEmpty( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {

                List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
                assertThat( facets ).isNotNull( ).isEmpty( );
                assertThat( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) ).isNull( );

            } );
            getRepository( ).removeMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

            tryAssert( ( ) -> {

                List<String> facets2 = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, TEST_FACET_ID );
                assertThat( facets2 ).isNotNull( ).isEmpty( );
                assertThat( getRepository( ).getMetadataFacet( session, TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) ).isNull( );

            } );
        }
    }

    @Test
    public void hasMetadataFacetStart( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            assertFalse( getRepository( ).hasMetadataFacet( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) ) );


        }
    }

    @Test
    public void hasMetadataFacet( )
        throws Exception
    {

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            assertFalse( getRepository( ).hasMetadataFacet( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) ) );

            Calendar cal = Calendar.getInstance( );

            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new KindOfRepositoryStatistics( "first", cal.getTime( ) ) );

            assertTrue( getRepository( ).hasMetadataFacet( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) ) );

            cal.add( Calendar.MINUTE, 2 );

            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new KindOfRepositoryStatistics( "second", cal.getTime( ) ) );

            cal.add( Calendar.MINUTE, 2 );

            getRepository( ).addMetadataFacet( session, TEST_REPO_ID, new KindOfRepositoryStatistics( "third", cal.getTime( ) ) );

            List<String> facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) );

            assertThat( facets ).isNotNull( ).isNotEmpty( ).hasSize( 3 );

            assertTrue( getRepository( ).hasMetadataFacet( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) ) );

            getRepository( ).removeMetadataFacets( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) );

            assertFalse( getRepository( ).hasMetadataFacet( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) ) );

            facets = getRepository( ).getMetadataFacets( session, TEST_REPO_ID, KindOfRepositoryStatistics.class.getName( ) );

            assertThat( facets ).isNotNull( ).isEmpty( );


        }
    }


    @Test
    public void testGetArtifacts( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact1 = createArtifact( );
            ArtifactMetadata artifact2 = createArtifact( "pom" );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                ArrayList<ArtifactMetadata> actual = new ArrayList<>( artifacts );
                Collections.sort( actual, ( o1, o2 ) -> o1.getId( ).compareTo( o2.getId( ) ) );
                assertEquals( Arrays.asList( artifact1, artifact2 ), actual );
            } );

        }
    }

    @Test
    public void testGetArtifactStream( )
        throws Exception
    {
        ArtifactMetadata artifact1 = createArtifact( );
        ArtifactMetadata artifact2 = createArtifact( "pom" );
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );
        }
        try ( RepositorySession session = getSessionFactory( ).createSession( ) ) {
            tryAssert( ( ) -> {
                Stream<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifactStream( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
                assertNotNull( artifacts );
                List<ArtifactMetadata> actual = artifacts
                    .sorted( ( o1, o2 ) -> o1.getId( ).compareTo( o2.getId( ) ) ).collect( Collectors.toList( ) );
                assertEquals( 2, actual.size( ) );
                assertEquals( Arrays.asList( artifact1, artifact2 ), actual );
            } );

        }
    }

    @Test
    public void testGetArtifactVersions( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact1 = createArtifact( );
            String version1 = "1.0-20091212.012345-1";
            artifact1.setId( artifact1.getProject( ) + "-" + version1 + ".jar" );
            artifact1.setVersion( version1 );
            ArtifactMetadata artifact2 = createArtifact( );
            String version2 = "1.0-20091212.123456-2";
            artifact2.setId( artifact2.getProject( ) + "-" + version2 + ".jar" );
            artifact2.setVersion( version2 );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

            tryAssert( ( ) -> {
                Collection<String> versions =
                    getRepository( ).getArtifactVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( versions ).isNotNull( ).isNotEmpty( ).contains( version1, version2 );
            } );

        }
    }

    @Test
    public void testGetArtifactVersionsMultipleArtifactsSingleVersion( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact1 = createArtifact( );
            artifact1.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
            ArtifactMetadata artifact2 = createArtifact( );
            artifact2.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "-sources.jar" );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

            tryAssert( ( ) -> {
                Collection<String> versions =
                    getRepository( ).getArtifactVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( versions ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).containsExactly( TEST_PROJECT_VERSION );
            } );


        }
    }

    @Test
    public void testGetArtifactsByDateRangeOpen( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, null );

                assertEquals( Collections.singletonList( artifact ), artifacts );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeSparseNamespace( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            String namespace = "org.apache.archiva";
            ArtifactMetadata artifact = createArtifact( );
            artifact.setNamespace( namespace );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, null );

                tryAssert( ( ) -> assertEquals( Collections.singletonList( artifact ), artifacts ) );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeLowerBound( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            ZonedDateTime date = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).minusSeconds(10);

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, date, null );

                assertEquals( Collections.singletonList( artifact ), artifacts );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeLowerBoundOutOfRange( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            ZonedDateTime date = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).plusSeconds(10);

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, date, null );

                assertThat( artifacts ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeLowerAndUpperBound( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            ZonedDateTime lower = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).minusSeconds(10);
            ZonedDateTime upper = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).plusSeconds(10);

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, lower, upper );

                assertEquals( Collections.singletonList( artifact ), artifacts );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeUpperBound( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            ZonedDateTime upper = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).plusSeconds(10);

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, upper );

                assertEquals( Collections.singletonList( artifact ), artifacts );
            } );
        }
    }

    @Test
    public void testGetArtifactsByDateRangeUpperBoundOutOfRange( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            ZonedDateTime upper = ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())).minusSeconds(10);

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts = getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, upper );

                assertThat( artifacts ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testGetArtifactsByRepoId( )
        throws Exception
    {
        ArtifactMetadata artifact;
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );
        }
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            tryAssert( ( ) -> {
                    session.refreshAndDiscard( );
                    List<ArtifactMetadata> artifacts = getRepository( ).getArtifacts( session, TEST_REPO_ID );
                    assertEquals( Collections.singletonList( artifact ), artifacts );
                }
            );
        }
    }

    @Test
    public void testGetArtifactsByRepoIdMultipleCopies( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            ArtifactMetadata secondArtifact = createArtifact( );
            secondArtifact.setRepositoryId( OTHER_REPO_ID );
            getRepository( ).updateArtifact( session, OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
            session.save( );

            // test it restricts to the appropriate repository
            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifact1 = getRepository( ).getArtifacts( session, TEST_REPO_ID );
                assertEquals( Collections.singletonList( artifact ), artifact1 );
                assertEquals( Collections.singletonList( secondArtifact ), getRepository( ).getArtifacts( session, OTHER_REPO_ID ) );
            });

        }
    }


    @Test
    public void testGetArtifactsByDateRangeMultipleCopies( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            ArtifactMetadata secondArtifact = createArtifact( );
            secondArtifact.setRepositoryId( OTHER_REPO_ID );
            getRepository( ).updateArtifact( session, OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
            session.save( );

            tryAssert( ( ) -> {
                // test it restricts to the appropriate repository
                assertEquals( Collections.singletonList( artifact ),
                    getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, null ) );
            } );
            tryAssert( ( ) -> {
                assertEquals( Collections.singletonList( secondArtifact ),
                    getRepository( ).getArtifactsByDateRange( session, OTHER_REPO_ID, null, null ) );
            } );
        }
    }

    @Test
    public void testGetArtifactsByChecksumMultipleCopies( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            ArtifactMetadata secondArtifact = createArtifact( );
            secondArtifact.setRepositoryId( OTHER_REPO_ID );
            getRepository( ).updateArtifact( session, OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
            session.save( );

            tryAssert( ( ) -> {
                // test it restricts to the appropriate repository
                assertEquals( Collections.singletonList( artifact ),
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA1 ) ) );
            } );
            tryAssert( ( ) -> {
                assertEquals( Collections.singletonList( secondArtifact ), new ArrayList<>(
                    getRepository( ).getArtifactsByChecksum( session, OTHER_REPO_ID, TEST_SHA1 ) ) );
            } );
            tryAssert( ( ) -> {
                assertEquals( Collections.singletonList( artifact ),
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_MD5 ) ) );
            } );
            tryAssert( ( ) -> {
                assertEquals( Collections.singletonList( secondArtifact ),
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, OTHER_REPO_ID, TEST_MD5 ) ) );

            } );
        }
    }

    @Test
    public void testGetNamespacesWithSparseDepth( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            getRepository( ).updateNamespace( session, TEST_REPO_ID, "org.apache.maven.shared" );

            tryAssert( ( ) -> {

                Collection<String> namespaces = getRepository( ).getRootNamespaces( session, TEST_REPO_ID );

                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( "org" );

                namespaces = getRepository( ).getChildNamespaces( session, TEST_REPO_ID, "org" );
                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( "apache" );

                namespaces = getRepository( ).getChildNamespaces( session, TEST_REPO_ID, "org.apache" );
                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( "maven" );

                namespaces = getRepository( ).getChildNamespaces( session, TEST_REPO_ID, "org.apache.maven" );
                assertThat( namespaces ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( "shared" );

            } );
        }
    }

    @Test
    public void testGetNamespacesWithProjectsPresent( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            String namespace = "org.apache.maven.shared";
            getRepository( ).updateNamespace( session, TEST_REPO_ID, namespace );

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, namespace, TEST_PROJECT, metadata );

            Collection<String> namespaces = getRepository( ).getChildNamespaces( session, TEST_REPO_ID, namespace );

            assertThat( namespaces ).isNotNull( ).isEmpty( );


        }
    }

    @Test
    public void testGetProjectsWithOtherNamespacesPresent( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ProjectMetadata projectMetadata = new ProjectMetadata( );
            projectMetadata.setId( TEST_PROJECT );
            projectMetadata.setNamespace( "org.apache.maven" );
            getRepository( ).updateProject( session, TEST_REPO_ID, projectMetadata );

            getRepository( ).updateNamespace( session, TEST_REPO_ID, "org.apache.maven.shared" );

            Collection<String> projects = getRepository( ).getProjects( session, TEST_REPO_ID, "org.apache.maven" );

            assertThat( projects ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( TEST_PROJECT );

        }
    }

    @Test
    public void testGetProjectVersionsWithOtherNamespacesPresent( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
// an unusual case but technically possible where a project namespace matches another project's name

            ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata( );
            versionMetadata.setId( TEST_PROJECT_VERSION );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, "org.apache.maven", TEST_PROJECT, versionMetadata );

            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, "org.apache.maven." + TEST_PROJECT,
                "other-project", versionMetadata );

            Collection<String> versions =
                getRepository( ).getProjectVersions( session, TEST_REPO_ID, "org.apache.maven." + TEST_PROJECT, "other-project" );
            assertThat( versions ).isNotNull( ).isNotEmpty( ).contains( TEST_PROJECT_VERSION );

            versions = getRepository( ).getProjectVersions( session, TEST_REPO_ID, "org.apache.maven", TEST_PROJECT );

            assertThat( versions ).isNotNull( ).isNotEmpty( ).contains( TEST_PROJECT_VERSION );

        }
    }

    @Test
    public void testGetArtifactsByChecksumSingleResultSha256( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            artifact.setChecksum( ChecksumAlgorithm.SHA256,  TEST_SHA256);
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            tryAssert( () ->
                assertEquals( Collections.singletonList( artifact ),
                new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA256 ) ))
            );

        }
    }

    @Test
    public void testGetArtifactsByChecksumSingleResultMd5( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            assertEquals( Collections.singletonList( artifact ),
                new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_MD5 ) ) );

        }
    }

    @Test
    public void testGetArtifactsByChecksumSingleResultSha1( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            assertEquals( Collections.singletonList( artifact ),
                new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA1 ) ) );

        }
    }

    @Test
    public void testGetArtifactsByChecksumDeepNamespace( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            String namespace = "multi.level.ns";
            artifact.setNamespace( namespace );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
            session.save( );

            tryAssert( ( ) ->
            {
                assertEquals( Collections.singletonList( artifact ),
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA1 ) ) );
                assertEquals( Collections.singletonList( artifact ),
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_MD5 ) ) );
            });

        }
    }

    @Test
    public void testGetArtifactsByChecksumMultipleResult( )
        throws Exception
    {

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact1 = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );

            String newProjectId = "another-project";
            ArtifactMetadata artifact2 = createArtifact( );
            artifact2.setProject( newProjectId );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, newProjectId, TEST_PROJECT_VERSION, artifact2 );
            session.save( );

            tryAssert( ( ) -> {
                List<ArtifactMetadata> artifacts =
                    new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_SHA1 ) );
                Collections.sort( artifacts, new ArtifactMetadataComparator( ) );
                assertEquals( Arrays.asList( artifact2, artifact1 ), artifacts );
            } );

            tryAssert( ( ) -> {
                ArrayList<ArtifactMetadata> artifacts = new ArrayList<>( getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, TEST_MD5 ) );
                Collections.sort( artifacts, new ArtifactMetadataComparator( ) );
                assertEquals( Arrays.asList( artifact2, artifact1 ), artifacts );
            } );
        }
    }

    @Test
    public void testGetArtifactsByChecksumNoResult( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByChecksum =
                    getRepository( ).getArtifactsByChecksum( session, TEST_REPO_ID, "not checksum" );

                assertThat( artifactsByChecksum ).isNotNull( ).isEmpty( );
            } );

        }
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadata( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session, 10 );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByMetadata =
                    getRepository( ).getArtifactsByProjectVersionFacet( session, TEST_METADATA_KEY, TEST_METADATA_VALUE, TEST_REPO_ID );

                assertThat( artifactsByMetadata ).hasSize( 1 );
                ArtifactMetadata artifactMetadata = artifactsByMetadata.iterator( ).next( );
                assertThat( artifactMetadata.getId( ) ).isEqualTo( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
                assertThat( artifactMetadata.getSha1( ) ).isEqualTo( TEST_SHA1 );
                assertThat( artifactMetadata.getRepositoryId( ) ).isEqualTo( TEST_REPO_ID );
            } );
        }
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadataNoRepository( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            tryAssert( ( ) -> {

                Collection<ArtifactMetadata> artifactsByMetadata =
                    getRepository( ).getArtifactsByProjectVersionFacet( session, TEST_METADATA_KEY, TEST_METADATA_VALUE, null );
                assertThat( artifactsByMetadata ).hasSize( 1 );
                assertThat( artifactsByMetadata.iterator( ).next( ).getRepositoryId( ) ).isNotNull( ).isNotEmpty( );
            } );
        }
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadataAllRepositories( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            tryAssert( ( ) -> {

                Collection<ArtifactMetadata> artifactsByMetadata =
                    getRepository( ).getArtifactsByProjectVersionFacet( session, TEST_METADATA_KEY, TEST_METADATA_VALUE, null );
                assertThat( artifactsByMetadata ).hasSize( 1 );
            } );
        }
    }

    @Test
    public void testGetArtifactsByMetadataAllRepositories( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithMavenArtifactFacet( session );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByMetadata =
                    getRepository( ).getArtifactsByAttribute( session, "foo", TEST_METADATA_VALUE, null );
                assertThat( artifactsByMetadata ).hasSize( 1 );
                ArtifactMetadata artifactMetadata = artifactsByMetadata.iterator( ).next( );
                assertThat( artifactMetadata.getId( ) ).isEqualTo( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
                assertThat( artifactMetadata.getSha1( ) ).isEqualTo( TEST_SHA1 );
                assertThat( artifactMetadata.getRepositoryId( ) ).isEqualTo( TEST_REPO_ID );
                MetadataFacet facet = artifactMetadata.getFacet( TEST_FACET_ID );
                assertThat( facet ).isNotNull( );
                assertThat( facet.toProperties( ).get("foo").equals(TEST_METADATA_VALUE) );
            } );
        }
    }

    @Test
    public void testGetArtifactsByPropertySingleResult( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithData( session );
            // only works on JCR implementation
            // Collection<ArtifactMetadata> artifactsByProperty = getRepository().getArtifactsByProjectVersionAttribute( "org.name", TEST_ORGANIZATION.getName(), TEST_REPO_ID );
            tryAssert( ( ) -> {

                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).getArtifactsByProjectVersionAttribute( session, "url", TEST_URL, TEST_REPO_ID );
                assertThat( artifactsByProperty ).hasSize( 1 );
                ArtifactMetadata artifactMetadata = artifactsByProperty.iterator( ).next( );
                assertThat( artifactMetadata.getId( ) ).isEqualTo( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
                assertThat( artifactMetadata.getSha1( ) ).isEqualTo( TEST_SHA1 );
                assertThat( artifactMetadata.getRepositoryId( ) ).isEqualTo( TEST_REPO_ID );
            } );

        }
    }

    @Test
    public void testDeleteRepository( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            getRepository( ).updateNamespace( session, TEST_REPO_ID, TEST_NAMESPACE );

            ProjectMetadata project1 = new ProjectMetadata( );
            project1.setNamespace( TEST_NAMESPACE );
            project1.setId( "project1" );
            getRepository( ).updateProject( session, TEST_REPO_ID, project1 );
            ProjectMetadata project2 = new ProjectMetadata( );
            project2.setNamespace( TEST_NAMESPACE );
            project2.setId( "project2" );
            getRepository( ).updateProject( session, TEST_REPO_ID, project2 );

            ArtifactMetadata artifact1 = createArtifact( );
            artifact1.setProject( "project1" );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, "project1", TEST_PROJECT_VERSION, artifact1 );
            ArtifactMetadata artifact2 = createArtifact( );
            artifact2.setProject( "project2" );
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, "project2", TEST_PROJECT_VERSION, artifact2 );
            session.save( );

            List<ArtifactMetadata> expected = Arrays.asList( artifact1, artifact2 );
            Collections.sort( expected, new ArtifactMetadataComparator( ) );


            tryAssert( ( ) -> {
                List<ArtifactMetadata> actual =
                    new ArrayList<>( getRepository( ).getArtifactsByDateRange( session, TEST_REPO_ID, null, null ) );
                Collections.sort( actual, new ArtifactMetadataComparator( ) );
                assertEquals( expected, actual );
            } );

            getRepository( ).removeRepository( session, TEST_REPO_ID );

            tryAssert( ( ) -> {
                assertTrue( getRepository( ).getArtifacts( session, TEST_REPO_ID ).isEmpty( ) );
                assertTrue( getRepository( ).getRootNamespaces( session, TEST_REPO_ID ).isEmpty( ) );
            } );
        }
    }


    @Test
    public void testDeleteArtifact( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ArtifactMetadata artifact = createArtifact( );
            artifact.addFacet( new TestMetadataFacet( "value" ) );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            assertThat( getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION ) ).containsExactly( artifact );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION_2_0, artifact );

            Collection<String> versions = getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

            log.info( "versions {}", versions );

            assertThat( versions ).isNotNull( ).isNotEmpty( ).hasSize( 2 ).contains( "1.0", "2.0" );

            getRepository( ).removeArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact.getId( ) );

            versions = getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

            log.info( "versions {}", versions );

            assertThat( versions ).isNotNull( ).isNotEmpty( ).hasSize( 1 ).contains( "2.0" );

            assertThat( getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION ) ).isNotNull( ).isEmpty( );

            assertThat( getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE,
                TEST_PROJECT, TEST_PROJECT_VERSION_2_0 ) ).isNotEmpty( ).hasSize( 1 );
        }
    }

    @Test
    public void deleteArtifact( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            artifact.addFacet( new TestMetadataFacet( "value" ) );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts =
                    getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertEquals( Collections.singletonList( artifact ), new ArrayList<>( artifacts ) );

            } );

            getRepository( ).removeArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact.getId( ) );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifacts = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

                assertThat( artifacts ).isNotNull( ).isEmpty( );
            } );

        }
    }

    @Test
    public void deleteVersion( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            artifact.addFacet( new TestMetadataFacet( "value" ) );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            tryAssert( ( ) -> {

                Collection<String> versions = getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

                assertThat( versions ).isNotNull( ).isNotEmpty( ).hasSize( 1 );
            } );

            getRepository( ).removeProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

            tryAssert( ( ) -> {

                Collection<String> versions1 = getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

                assertThat( versions1 ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void deleteProject( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            ArtifactMetadata artifact = createArtifact( );
            artifact.addFacet( new TestMetadataFacet( "value" ) );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

            assertEquals( 1, getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ).size( ) );

            getRepository( ).removeProject( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

            Collection<String> versions = getRepository( ).getProjectVersions( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

            assertThat( versions ).isNotNull( ).isEmpty( );

        }
    }


    @Test
    public void deleteSnapshotVersion( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ArtifactMetadata artifactOne = createArtifact( );
            artifactOne.setVersion( "2.0-20120618.214127-1" );
            artifactOne.setProjectVersion( "2.0-SNAPSHOT" );
            artifactOne.addFacet( new TestMetadataFacet( "value" ) );
            artifactOne.setId( TEST_PROJECT + "-" + "2.0-20120618.214127-1" + "." + "jar" );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT", artifactOne );

            ArtifactMetadata artifactTwo = createArtifact( );
            artifactTwo.setVersion( "2.0-20120618.214135-2" );
            artifactTwo.setProjectVersion( "2.0-SNAPSHOT" );
            artifactTwo.addFacet( new TestMetadataFacet( "value" ) );
            artifactTwo.setId( TEST_PROJECT + "-" + "2.0-20120618.214135-2" + "." + "jar" );

            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT", artifactTwo );

            Collection<ArtifactMetadata> artifactMetadatas =
                getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

            assertThat( artifactMetadatas ).isNotNull( ).isNotEmpty( ).hasSize( 2 );

            log.info( "artifactMetadatas: {}", artifactMetadatas );

            getRepository( ).removeTimestampedArtifact( session, artifactOne, "2.0-SNAPSHOT" );

            artifactMetadatas = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

            assertThat( artifactMetadatas ).isNotNull( ).isNotEmpty( ).hasSize( 1 );

            getRepository( ).removeTimestampedArtifact( session, artifactTwo, "2.0-SNAPSHOT" );

            artifactMetadatas = getRepository( ).getArtifacts( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

            assertThat( artifactMetadatas ).isNotNull( ).isEmpty( );
        }
    }


    @Test
    public void testgetProjectReferences( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            ProjectVersionMetadata metadata = new ProjectVersionMetadata( );
            metadata.setId( TEST_PROJECT_VERSION );

            metadata.setName( "project name" );
            metadata.setDescription( "project description" );
            metadata.setUrl( "the url" );

            Dependency d = new Dependency( );
            d.setArtifactId( "artifactId" );
            d.setClassifier( "classifier" );
            d.setNamespace( "groupId" );
            d.setScope( "scope" );
            d.setSystemPath( "system path" );
            d.setType( "type" );
            d.setVersion( "version" );
            d.setOptional( true );
            metadata.addDependency( d );

            d = new Dependency( );
            d.setArtifactId( "artifactId1" );
            d.setClassifier( "classifier" );
            d.setNamespace( "groupId" );
            d.setScope( "scope" );
            d.setSystemPath( "system path" );
            d.setType( "type" );
            d.setVersion( "version1" );
            d.setOptional( true );
            metadata.addDependency( d );

            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

            session.save( );

            metadata = getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            final Dependency dd = d;


            tryAssert( ( ) -> {

                Collection<ProjectVersionReference> references =
                    getRepository( ).getProjectReferences( session, TEST_REPO_ID, dd.getNamespace( ), dd.getArtifactId( ), dd.getVersion( ) );
                    log.info( "references: {}", references );
                assertThat( references ).isNotNull( ).hasSize( 1 ).contains(
                    new ProjectVersionReference( ProjectVersionReference.ReferenceType.DEPENDENCY, TEST_PROJECT, TEST_NAMESPACE,
                        TEST_PROJECT_VERSION ));
            }
            );

        }
    }

    @Test
    public void testSearchArtifactsByKey( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithData( session );
        }

        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            session.refreshAndDiscard( );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, "url", TEST_URL, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );
        }
    }

    @Test
    public void testSearchArtifactsByKeyExact( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithData( session );
        }
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            session.refreshAndDiscard( );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, "url", TEST_URL, true );
                assertThat( artifactsByProperty ).describedAs( "Artifact search by url=%s must give a result.", TEST_URL ).isNotNull( ).isNotEmpty( );
                artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, "org.name", "pache", true );
                assertThat( artifactsByProperty ).describedAs( "Artifact search by text org.name='pache' must be empty" ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testSearchArtifactsByFacetKey( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, TEST_METADATA_KEY, TEST_METADATA_VALUE, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );

        }
    }

    @Test
    public void testSearchArtifactsByFacetKeyAllRepos( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {

            createArtifactWithGenericMetadataFacet( session );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, null, TEST_METADATA_KEY, TEST_METADATA_VALUE, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );
        }
    }

    @Test
    public void testSearchArtifactsFullText( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            // only works in JCR
            // Collection<ArtifactMetadata> artifactsByProperty = getRepository().searchArtifacts( TEST_URL, TEST_REPO_ID, false );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty =
                    getRepository( ).searchArtifacts( session, TEST_REPO_ID, TEST_METADATA_VALUE, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );

        }
    }

    @Test
    public void testSearchArtifactsFullTextExact( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            // only works in JCR
            // Collection<ArtifactMetadata> artifactsByProperty = getRepository().searchArtifacts( TEST_URL, TEST_REPO_ID, true );

            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty =
                    getRepository( ).searchArtifacts( session, TEST_REPO_ID, TEST_METADATA_VALUE, true );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );
            tryAssert( ( ) -> {

                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, TEST_METADATA_VALUE.substring( 2 ), true );
                assertThat( artifactsByProperty ).isNotNull( ).isEmpty( );
            } );
        }
    }

    @Test
    public void testSearchArtifactsFullTextByFacet( )
        throws Exception
    {
        try ( RepositorySession session = getSessionFactory( ).createSession( ) )
        {
            createArtifactWithGenericMetadataFacet( session );
            tryAssert( ( ) -> {
                Collection<ArtifactMetadata> artifactsByProperty = getRepository( ).searchArtifacts( session, TEST_REPO_ID, TEST_METADATA_VALUE, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            } );

        }
    }

    private static ProjectMetadata createProject( )
    {
        return createProject( TEST_NAMESPACE );
    }

    private static ProjectMetadata createProject( String ns )
    {
        ProjectMetadata project = new ProjectMetadata( );
        project.setId( TEST_PROJECT );
        project.setNamespace( ns );
        return project;
    }

    private void createArtifactWithGenericMetadataFacet( RepositorySession session )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        createArtifactWithGenericMetadataFacet( session, 1 );
    }

    private void createArtifactWithGenericMetadataFacet( RepositorySession session, int artifacts )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        MetadataFacet metadataFacet = new GenericMetadataFacet( );
        Map<String, String> properties = new HashMap<>( );
        properties.put( TEST_METADATA_KEY, TEST_METADATA_VALUE );
        metadataFacet.fromProperties( properties );
        createArtifactWithFacet( session, artifacts, null, metadataFacet );
    }

    private void createArtifactWithMavenArtifactFacet( RepositorySession session )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        createArtifactWithMavenArtifactFacet( session, 1 );
    }

    private void createArtifactWithMavenArtifactFacet( RepositorySession session, int artifacts )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        TestMetadataFacet facet = new TestMetadataFacet( TEST_METADATA_VALUE );
        createArtifactWithFacet( session, artifacts, facet, null );
    }

    private void createArtifactWithFacet( RepositorySession session, int artifacts, MetadataFacet artifactFacet,
                                          MetadataFacet projectVersionMetadataFacet )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        for ( int i = 0; i < artifacts; i++ )
        {
            ArtifactMetadata artifact = createArtifact( );
            if ( artifactFacet != null )
            {
                artifact.addFacet( artifactFacet );
            }
            getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        }
        if ( projectVersionMetadataFacet != null )
        {
            ProjectVersionMetadata metadata =
                getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            metadata.addFacet( projectVersionMetadataFacet );
            metadata.setOrganization( TEST_ORGANIZATION );
            metadata.setUrl( TEST_URL );
            getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
        }
        session.save( );
    }

    protected void createArtifactWithData( RepositorySession session )
        throws MetadataRepositoryException, MetadataResolutionException, MetadataSessionException
    {
        ArtifactMetadata artifact = createArtifact( );
        getRepository( ).updateArtifact( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        ProjectVersionMetadata metadata =
            getRepository( ).getProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        metadata.setOrganization( TEST_ORGANIZATION );
        metadata.setUrl( TEST_URL );

        getRepository( ).updateProjectVersion( session, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
        session.save( );
    }

    private static ArtifactMetadata createArtifact( )
    {
        return createArtifact( "jar" );
    }

    private static ArtifactMetadata createArtifact( String type )
    {
        ArtifactMetadata artifact = new ArtifactMetadata( );
        artifact.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "." + type );
        artifact.setWhenGathered( ZonedDateTime.now() );
        artifact.setNamespace( TEST_NAMESPACE );
        artifact.setProject( TEST_PROJECT );
        artifact.setRepositoryId( TEST_REPO_ID );
        artifact.setFileLastModified( System.currentTimeMillis( ) );
        artifact.setVersion( TEST_PROJECT_VERSION );
        artifact.setProjectVersion( TEST_PROJECT_VERSION );
        artifact.setMd5( TEST_MD5 );
        artifact.setSha1( TEST_SHA1 );
        return artifact;
    }

    private static class ArtifactMetadataComparator
        implements Comparator<ArtifactMetadata>
    {
        @Override
        public final int compare( ArtifactMetadata a, ArtifactMetadata b )
        {
            return a.getProject( ).compareTo( b.getProject( ) );
        }
    }

    private static class KindOfRepositoryStatistics
        implements MetadataFacet
    {
        private String value;

        private Date date;

        static final String SCAN_TIMESTAMP_FORMAT = "yyyy/MM/dd/HHmmss.SSS";

        private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

        private KindOfRepositoryStatistics( String value, Date date )
        {
            this.value = value;
            this.date = date;
        }

        @Override
        public String getName( )
        {
            return createNameFormat( ).format( date );
        }

        private static SimpleDateFormat createNameFormat( )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( SCAN_TIMESTAMP_FORMAT );
            fmt.setTimeZone( UTC_TIME_ZONE );
            return fmt;
        }

        @Override
        public String getFacetId( )
        {
            return KindOfRepositoryStatistics.class.getName( );
        }

        @Override
        public Map<String, String> toProperties( )
        {
            return Collections.emptyMap( );
        }

        @Override
        public void fromProperties( Map<String, String> properties )
        {
            // no op
        }
    }

    private static class TestMetadataFacet
        implements MetadataFacet
    {
        private String testFacetId;

        private Map<String, String> additionalProps;

        private String value;

        private String name = TEST_NAME;

        private TestMetadataFacet( String value )
        {
            this.value = value;
            testFacetId = TEST_FACET_ID;
        }



        private TestMetadataFacet( String facetId, String value )
        {
            this.value = value;
            testFacetId = facetId;
        }

        private TestMetadataFacet( String facetId, String value, String name)
        {
            this.value = value;
            testFacetId = facetId;
            this.name = name;
        }

        private TestMetadataFacet( String facetId, String value, String name, Map<String, String> additionalProps )
        {
            this( facetId, value, name );
            this.additionalProps = additionalProps;

        }

        private TestMetadataFacet( String facetId, String value, Map<String, String> additionalProps )
        {
            this( facetId, value );
            this.additionalProps = additionalProps;
        }

        @Override
        public String getFacetId( )
        {
            return testFacetId;
        }

        @Override
        public String getName( )
        {
            return name;
        }

        @Override
        public Map<String, String> toProperties( )
        {
            if ( value != null )
            {
                if ( additionalProps == null )
                {
                    return Collections.singletonMap( "foo", value );
                }
                else
                {
                    Map<String, String> props = new HashMap<>( );
                    props.put( "foo", value );

                    for ( String key : additionalProps.keySet( ) )
                    {
                        props.put( key, additionalProps.get( key ) );
                    }
                    return props;
                }
            }
            else
            {
                return Collections.emptyMap( );
            }
        }

        @Override
        public void fromProperties( Map<String, String> properties )
        {
            String value = properties.get( "foo" );
            if ( value != null )
            {
                this.value = value;
            }

            properties.remove( "foo" );

            if ( additionalProps == null )
            {
                additionalProps = new HashMap<>( );
            }

            for ( String key : properties.keySet( ) )
            {
                additionalProps.put( key, properties.get( key ) );
            }
        }

        public String getValue( )
        {
            return value;
        }

        @Override
        public String toString( )
        {
            return "TestMetadataFacet{ name='"+ name+ "' value='" + value + '\'' + '}';
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass( ) != o.getClass( ) )
            {
                return false;
            }

            TestMetadataFacet that = (TestMetadataFacet) o;

            if ( value != null ? !value.equals( that.value ) : that.value != null )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode( )
        {
            return value != null ? value.hashCode( ) : 0;
        }
    }
}
