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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.generic.GenericMetadataFacetFactory;
import org.apache.archiva.metadata.model.*;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public abstract class AbstractMetadataRepositoryTest
    extends TestCase
{
    protected static final String OTHER_REPO_ID = "other-repo";

    protected MetadataRepository repository;

    protected static final String TEST_REPO_ID = "test";

    protected static final String TEST_PROJECT = "projectId";

    protected static final String TEST_NAMESPACE = "namespace";

    protected static final String TEST_PROJECT_VERSION = "1.0";

    private static final String TEST_PROJECT_VERSION_2_0 = "2.0";

    private static final String TEST_URL = "http://archiva.apache.org";

    private static final Organization TEST_ORGANIZATION = new Organization( "Apache", "http://apache.org" );

    private static final String TEST_FACET_ID = "test-facet-id";

    private static final String TEST_NAME = "test/name";

    private static final String TEST_VALUE = "test-value";

    private static final String UNKNOWN = "unknown";

    private static final String TEST_MD5 = "bd4a9b642562547754086de2dab26b7d";

    private static final String TEST_SHA1 = "2e5daf0201ddeb068a62d5e08da18657ab2c6be9";

    private static final String TEST_METADATA_KEY = "testkey";

    private static final String TEST_METADATA_VALUE = "testmetadata";

    protected Logger log = LoggerFactory.getLogger( getClass() );

    /*
     * Used by tryAssert to allow to throw exceptions in the lambda expression.
     */
    @FunctionalInterface
    private interface AssertFunction {
        void accept() throws Exception;
    }

    private void tryAssert(AssertFunction func) throws Exception {
        tryAssert(func, 5, 500);
    }

    /*
     * Runs the assert method until the assert is successful or the number of retries
      * is reached. Needed because the JCR Oak index update is asynchronous, so updates
      * may not be visible immediately after the modification.
     */
    private void tryAssert(AssertFunction func, int retries, int sleepMillis) throws Exception {
        Throwable t = null;
        int retry = retries;
        while(retry-->0) {
            try {
                func.accept();
                return;
            } catch (Exception|AssertionError e ) {
                t = e;
                Thread.currentThread().sleep(sleepMillis);
                log.warn("Retrying assert "+retry);
            }
        }
        if (retry<=0 && t!=null) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else if (t instanceof Exception) {
                throw (Exception)t;
            }
        }
    }


    public static Map<String, MetadataFacetFactory> createTestMetadataFacetFactories()
    {
        Map<String, MetadataFacetFactory> factories = new HashMap<>();
        factories.put( TEST_FACET_ID, new MetadataFacetFactory()
        {
            @Override
            public MetadataFacet createMetadataFacet()
            {
                return new TestMetadataFacet( TEST_METADATA_VALUE );
            }

            @Override
            public MetadataFacet createMetadataFacet( String repositoryId, String name )
            {
                return new TestMetadataFacet( TEST_METADATA_VALUE );
            }
        } );

        // add to ensure we don't accidentally create an empty facet ID.
        factories.put( "", new MetadataFacetFactory()
        {
            @Override
            public MetadataFacet createMetadataFacet()
            {
                return new TestMetadataFacet( "", TEST_VALUE );
            }

            @Override
            public MetadataFacet createMetadataFacet( String repositoryId, String name )
            {
                return new TestMetadataFacet( "", TEST_VALUE );
            }
        } );

        // for the getArtifactsByProjectVersionMetadata tests
        factories.put( GenericMetadataFacet.FACET_ID, new GenericMetadataFacetFactory() );

        return factories;
    }

    @Test
    public void testRootNamespaceWithNoMetadataRepository()
        throws Exception
    {
        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );
        assertThat( namespaces ).isNotNull().isEmpty();
    }

    @Test
    public void testGetNamespaceOnly()
        throws Exception
    {
        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isEmpty();

        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );

        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isNotEmpty().contains(
            TEST_NAMESPACE ).hasSize( 1 );

        repository.removeNamespace( TEST_REPO_ID, TEST_NAMESPACE );

        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isEmpty();

    }

    @Test
    public void testGetProjectOnly()
        throws Exception
    {
        assertNull( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isEmpty();

        ProjectMetadata project = new ProjectMetadata();
        project.setId( TEST_PROJECT );
        project.setNamespace( TEST_NAMESPACE );

        repository.updateProject( TEST_REPO_ID, project );

        project = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertEquals( TEST_PROJECT, project.getId() );
        assertEquals( TEST_NAMESPACE, project.getNamespace() );

        // test that namespace is also constructed

        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );

        assertThat( namespaces ).isNotNull().isNotEmpty().contains( TEST_NAMESPACE ).hasSize( 1 );
    }

    @Test
    public void testGetProjectVersionOnly()
        throws Exception
    {
        assertNull( repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) );
        assertNull( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isEmpty();

        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );

        // test that namespace and project is also constructed
        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );

        assertThat( namespaces ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_NAMESPACE );

        ProjectMetadata projectMetadata = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertNotNull( projectMetadata );
        assertEquals( TEST_PROJECT, projectMetadata.getId() );
        assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace() );
    }

    @Test
    public void testGetArtifactOnly()
        throws Exception
    {
        assertThat( new ArrayList<>(
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                     TEST_PROJECT_VERSION ) ) ).isNotNull().isEmpty();
        assertThat(
            repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ).isNull();
        assertThat( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) ).isNull();

        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isEmpty();

        ArtifactMetadata metadata = createArtifact();

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        Collection<ArtifactMetadata> artifacts =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        //assertEquals( Collections.singletonList( metadata ), new ArrayList<ArtifactMetadata>( artifacts ) );
        assertThat( artifacts ).containsExactly( metadata );
        // test that namespace, project and project version is also constructed

        assertThat( repository.getRootNamespaces( TEST_REPO_ID ) ).isNotNull().isNotEmpty().contains(
            TEST_NAMESPACE ).hasSize( 1 );

        ProjectMetadata projectMetadata = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertEquals( TEST_PROJECT, projectMetadata.getId() );
        assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace() );

        ProjectVersionMetadata projectVersionMetadata =
            repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, projectVersionMetadata.getId() );
    }

    @Test
    public void testUpdateProjectVersionMetadataWithNoOtherArchives()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setOtherArchives( Collections.<String>emptyList() );
        metadata.setMailingLists( Arrays.asList( mailingList ) );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );

        List<MailingList> mailingLists = metadata.getMailingLists();

        assertThat( mailingLists ).isNotNull().isNotEmpty().hasSize( 1 );

        mailingList = metadata.getMailingLists().get( 0 );
        assertEquals( "Foo List", mailingList.getName() );

        List<String> others = mailingList.getOtherArchives();
        assertThat( others ).isNotNull().isEmpty();
    }

    @Test
    public void testUpdateProjectVersionMetadataWithAllElements()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        metadata.setName( "project name" );
        metadata.setDescription( "project description" );
        metadata.setUrl( "the url" );

        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setUnsubscribeAddress( "UnsubscribeAddress" );
        mailingList.setSubscribeAddress( "SubscribeAddress" );
        mailingList.setPostAddress( "PostAddress" );
        mailingList.setMainArchiveUrl( "MainArchiveUrl" );
        mailingList.setOtherArchives( Arrays.asList( "other archive" ) );
        metadata.setMailingLists( Arrays.asList( mailingList ) );

        Scm scm = new Scm();
        scm.setConnection( "connection" );
        scm.setDeveloperConnection( "dev conn" );
        scm.setUrl( "url" );
        metadata.setScm( scm );

        CiManagement ci = new CiManagement();
        ci.setSystem( "system" );
        ci.setUrl( "ci url" );
        metadata.setCiManagement( ci );

        IssueManagement tracker = new IssueManagement();
        tracker.setSystem( "system" );
        tracker.setUrl( "issue tracker url" );
        metadata.setIssueManagement( tracker );

        metadata.setOrganization( TEST_ORGANIZATION );

        License l = new License();
        l.setName( "license name" );
        l.setUrl( "license url" );
        metadata.addLicense( l );

        Dependency d = new Dependency();
        d.setArtifactId( "artifactId" );
        d.setClassifier( "classifier" );
        d.setGroupId( "groupId" );
        d.setScope( "scope" );
        d.setSystemPath( "system path" );
        d.setType( "type" );
        d.setVersion( "version" );
        d.setOptional( true );
        metadata.addDependency( d );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );
        assertEquals( TEST_PROJECT_VERSION, metadata.getVersion() );
        assertEquals( "project name", metadata.getName() );
        assertEquals( "project description", metadata.getDescription() );
        assertEquals( "the url", metadata.getUrl() );

        assertEquals( "connection", metadata.getScm().getConnection() );
        assertEquals( "dev conn", metadata.getScm().getDeveloperConnection() );
        assertEquals( "url", metadata.getScm().getUrl() );

        assertEquals( "system", metadata.getCiManagement().getSystem() );
        assertEquals( "ci url", metadata.getCiManagement().getUrl() );

        assertEquals( "system", metadata.getIssueManagement().getSystem() );
        assertEquals( "issue tracker url", metadata.getIssueManagement().getUrl() );

        assertEquals( TEST_ORGANIZATION.getName(), metadata.getOrganization().getName() );
        assertEquals( TEST_ORGANIZATION.getUrl(), metadata.getOrganization().getUrl() );

        assertEquals( 1, metadata.getMailingLists().size() );
        MailingList retrievedMailingList = metadata.getMailingLists().get( 0 );
        assertEquals( mailingList.getName(), retrievedMailingList.getName() );
        assertEquals( mailingList.getMainArchiveUrl(), retrievedMailingList.getMainArchiveUrl() );
        assertEquals( mailingList.getPostAddress(), retrievedMailingList.getPostAddress() );
        assertEquals( mailingList.getSubscribeAddress(), retrievedMailingList.getSubscribeAddress() );
        assertEquals( mailingList.getUnsubscribeAddress(), retrievedMailingList.getUnsubscribeAddress() );
        assertThat( retrievedMailingList.getOtherArchives() ) //
            .isNotNull() //
            .isNotEmpty() //
            .hasSize( 1 ) //
            .contains( "other archive" );

        assertEquals( 1, metadata.getLicenses().size() );
        l = metadata.getLicenses().get( 0 );
        assertEquals( "license name", l.getName() );
        assertEquals( "license url", l.getUrl() );

        assertEquals( 1, metadata.getDependencies().size() );
        d = metadata.getDependencies().get( 0 );
        assertEquals( "artifactId", d.getArtifactId() );
        assertEquals( "classifier", d.getClassifier() );
        assertEquals( "groupId", d.getGroupId() );
        assertEquals( "scope", d.getScope() );
        assertEquals( "system path", d.getSystemPath() );
        assertEquals( "type", d.getType() );
        assertEquals( "version", d.getVersion() );
        assertTrue( d.isOptional() );
    }

    @Test
    public void testGetRepositories()
        throws Exception
    {
        // currently set up this way so the behaviour of both the test and the mock config return the same repository
        // set as the File implementation just uses the config rather than the content
        repository.updateNamespace( TEST_REPO_ID, "namespace" );
        repository.updateNamespace( OTHER_REPO_ID, "namespace" );

        Collection<String> repositories = repository.getRepositories();

        assertEquals( "repository.getRepositories() -> " + repositories, //
                      Sets.newLinkedHashSet( TEST_REPO_ID, OTHER_REPO_ID ), //
                      new LinkedHashSet<String>( repositories ) ); //
    }

    @Test
    public void testUpdateProjectVersionMetadataIncomplete()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        metadata.setIncomplete( true );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( true, metadata.isIncomplete() );
        assertNull( metadata.getCiManagement() );
        assertNull( metadata.getScm() );
        assertNull( metadata.getIssueManagement() );
        assertNull( metadata.getOrganization() );
        assertNull( metadata.getDescription() );
        assertNull( metadata.getName() );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );
        assertEquals( TEST_PROJECT_VERSION, metadata.getVersion() );
        assertTrue( metadata.getMailingLists().isEmpty() );
        assertTrue( metadata.getLicenses().isEmpty() );
        assertTrue( metadata.getDependencies().isEmpty() );
    }

    @Test
    public void testUpdateProjectVersionMetadataWithExistingFacets()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        MetadataFacet facet = new TestMetadataFacet( "baz" );
        metadata.addFacet( facet );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );

        metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );
        TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        assertEquals( "baz", testFacet.getValue() );
    }

    @Test
    public void testUpdateProjectVersionMetadataWithNoExistingFacets()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( metadata.getFacetIds() ).isNotNull().isEmpty();

        metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertThat( metadata.getFacetIds() ).isNotNull().isEmpty();

    }

    @Test
    public void testUpdateProjectVersionMetadataWithExistingFacetsFacetPropertyWasRemoved()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put( "deleteKey", "deleteValue" );

        MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz", additionalProps );
        metadata.addFacet( facet );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( metadata.getFacetIds() ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_FACET_ID );

        TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        Map<String, String> facetProperties = testFacet.toProperties();

        assertEquals( "deleteValue", facetProperties.get( "deleteKey" ) );

        facetProperties.remove( "deleteKey" );

        TestMetadataFacet newTestFacet = new TestMetadataFacet( TEST_FACET_ID, testFacet.getValue(), facetProperties );
        metadata.addFacet( newTestFacet );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( metadata.getFacetIds() ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_FACET_ID );
        testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        assertFalse( testFacet.toProperties().containsKey( "deleteKey" ) );
    }

    @Test
    public void testGetArtifactsDoesntReturnProjectVersionMetadataFacets()
        throws Exception
    {
        ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata();
        versionMetadata.setId( TEST_PROJECT_VERSION );

        MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz" );
        versionMetadata.addFacet( facet );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, versionMetadata );

        ArtifactMetadata artifactMetadata = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifactMetadata );
        repository.save();

        Collection<ArtifactMetadata> artifacts =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

        artifacts = repository.getArtifacts( TEST_REPO_ID );
        assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

        artifacts = repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 );
        assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

        artifacts = repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_MD5 );
        assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );

        artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, null, null );
        assertEquals( Collections.singletonList( artifactMetadata ), new ArrayList<>( artifacts ) );
    }

    @Test
    public void testUpdateArtifactMetadataWithExistingFacetsFacetPropertyWasRemoved()
        throws Exception
    {
        ArtifactMetadata metadata = createArtifact();

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put( "deleteKey", "deleteValue" );

        MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz", additionalProps );
        metadata.addFacet( facet );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        Collection<ArtifactMetadata> artifacts =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 1 );
        metadata = artifacts.iterator().next();

        Collection<String> ids = metadata.getFacetIds();
        assertThat( ids ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_FACET_ID );

        TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        Map<String, String> facetProperties = testFacet.toProperties();

        assertEquals( "deleteValue", facetProperties.get( "deleteKey" ) );

        facetProperties.remove( "deleteKey" );

        TestMetadataFacet newTestFacet = new TestMetadataFacet( TEST_FACET_ID, testFacet.getValue(), facetProperties );
        metadata.addFacet( newTestFacet );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        artifacts = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 1 );
        metadata = artifacts.iterator().next();

        ids = metadata.getFacetIds();
        assertThat( ids ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_FACET_ID );

        testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );

        Map<String, String> props = testFacet.toProperties();
        assertThat( props ).isNotNull().doesNotContainKey( "deleteKey" );
    }

    @Test
    public void testUpdateArtifactMetadataWithExistingFacets()
        throws Exception
    {
        ArtifactMetadata metadata = createArtifact();
        MetadataFacet facet = new TestMetadataFacet( "baz" );
        metadata.addFacet( facet );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        metadata = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                            TEST_PROJECT_VERSION ).iterator().next();
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );

        metadata = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        metadata = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                            TEST_PROJECT_VERSION ).iterator().next();
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );
        TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        assertEquals( "baz", testFacet.getValue() );
    }

    @Test
    public void testUpdateArtifactMetadataWithNoExistingFacets()
        throws Exception
    {
        ArtifactMetadata metadata = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        metadata = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                            TEST_PROJECT_VERSION ).iterator().next();
        assertEquals( Collections.<String>emptyList(), new ArrayList<String>( metadata.getFacetIds() ) );

        metadata = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        metadata = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                            TEST_PROJECT_VERSION ).iterator().next();
        assertEquals( Collections.<String>emptyList(), new ArrayList<String>( metadata.getFacetIds() ) );
    }

    @Test
    public void testGetMetadataFacet()
        throws Exception
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        TestMetadataFacet test =
            (TestMetadataFacet) repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

        assertEquals( new TestMetadataFacet( TEST_VALUE ), test );
    }

    @Test
    public void testGetMetadataFacetWhenEmpty()
        throws Exception
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    @Test
    public void testGetMetadataFacetWhenUnknownName()
        throws Exception
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, UNKNOWN ) );
    }

    @Test
    public void testGetMetadataFacetWhenDefaultValue()
        throws Exception
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( null ) );

        MetadataFacet metadataFacet = repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

        assertEquals( new TestMetadataFacet( TEST_METADATA_VALUE ), metadataFacet );
    }

    @Test
    public void testGetMetadataFacetWhenUnknownFacetId()
        throws Exception
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME ) );
    }

    @Test
    public void testGetMetadataFacets()
        throws Exception
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( Collections.singletonList( TEST_NAME ),
                      repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID ) );
    }

    @Test
    public void testGetMetadataFacetsWhenEmpty()
        throws Exception
    {

        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    @Test
    public void testRemoveFacets()
        throws Exception
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertFalse( facets.isEmpty() );

        repository.removeMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    @Test
    public void testRemoveFacetsWhenEmpty()
        throws Exception
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );

        repository.removeMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    @Test
    public void testRemoveFacetsWhenUnknown()
        throws Exception
    {
        // testing no exception
        repository.removeMetadataFacets( TEST_REPO_ID, UNKNOWN );
    }

    @Test
    public void testRemoveFacetWhenUnknown()
        throws Exception
    {
        // testing no exception
        repository.removeMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME );
    }

    @Test
    public void testRemoveFacet()
        throws Exception
    {
        TestMetadataFacet metadataFacet = new TestMetadataFacet( TEST_VALUE );
        repository.addMetadataFacet( TEST_REPO_ID, metadataFacet );

        assertEquals( metadataFacet, repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertFalse( facets.isEmpty() );

        repository.removeMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    @Test
    public void testRemoveFacetWhenEmpty()
        throws Exception
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertThat( facets ).isNotNull().isEmpty();
        assertThat( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) ).isNull();

        repository.removeMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertThat( facets ).isNotNull().isEmpty();
        assertThat( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) ).isNull();
    }

    @Test
    public void hasMetadataFacetStart()
        throws Exception
    {
        assertFalse( repository.hasMetadataFacet( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() ) );

    }

    @Test
    public void hasMetadataFacet()
        throws Exception
    {

        assertFalse( repository.hasMetadataFacet( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() ) );

        Calendar cal = Calendar.getInstance();

        repository.addMetadataFacet( TEST_REPO_ID, new KindOfRepositoryStatistics( "first", cal.getTime() ) );

        assertTrue( repository.hasMetadataFacet( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() ) );

        cal.add( Calendar.MINUTE, 2 );

        repository.addMetadataFacet( TEST_REPO_ID, new KindOfRepositoryStatistics( "second", cal.getTime() ) );

        cal.add( Calendar.MINUTE, 2 );

        repository.addMetadataFacet( TEST_REPO_ID, new KindOfRepositoryStatistics( "third", cal.getTime() ) );

        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() );

        assertThat( facets ).isNotNull().isNotEmpty().hasSize( 3 );

        assertTrue( repository.hasMetadataFacet( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() ) );

        repository.removeMetadataFacets( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() );

        assertFalse( repository.hasMetadataFacet( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() ) );

        facets = repository.getMetadataFacets( TEST_REPO_ID, KindOfRepositoryStatistics.class.getName() );

        assertThat( facets ).isNotNull().isEmpty();

    }


    @Test
    public void testGetArtifacts()
        throws Exception
    {
        ArtifactMetadata artifact1 = createArtifact();
        ArtifactMetadata artifact2 = createArtifact( "pom" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        Collection<ArtifactMetadata> artifacts =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        ArrayList<ArtifactMetadata> actual = new ArrayList<>( artifacts );
        Collections.sort( actual,( o1, o2 ) -> o1.getId().compareTo( o2.getId() ) );
        assertEquals( Arrays.asList( artifact1, artifact2 ), actual );
    }

    @Test
    public void testGetArtifactVersions()
        throws Exception
    {
        ArtifactMetadata artifact1 = createArtifact();
        String version1 = "1.0-20091212.012345-1";
        artifact1.setId( artifact1.getProject() + "-" + version1 + ".jar" );
        artifact1.setVersion( version1 );
        ArtifactMetadata artifact2 = createArtifact();
        String version2 = "1.0-20091212.123456-2";
        artifact2.setId( artifact2.getProject() + "-" + version2 + ".jar" );
        artifact2.setVersion( version2 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        Collection<String> versions =
            repository.getArtifactVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( versions ).isNotNull().isNotEmpty().contains( version1, version2 );
    }

    @Test
    public void testGetArtifactVersionsMultipleArtifactsSingleVersion()
        throws Exception
    {
        ArtifactMetadata artifact1 = createArtifact();
        artifact1.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "-sources.jar" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        Collection<String> versions =
            repository.getArtifactVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( versions ).isNotNull().isNotEmpty().hasSize( 1 ).containsExactly( TEST_PROJECT_VERSION );

    }

    @Test
    public void testGetArtifactsByDateRangeOpen()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, null, null );

        assertEquals( Collections.singletonList( artifact ), artifacts );
    }

    @Test
    public void testGetArtifactsByDateRangeSparseNamespace()
        throws Exception
    {
        String namespace = "org.apache.archiva";
        ArtifactMetadata artifact = createArtifact();
        artifact.setNamespace( namespace );
        repository.updateArtifact( TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, null, null );

        tryAssert(()->assertEquals( Collections.singletonList( artifact ), artifacts ));
    }

    @Test
    public void testGetArtifactsByDateRangeLowerBound()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        Date date = new Date( artifact.getWhenGathered().getTime() - 10000 );

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, date, null );

        assertEquals( Collections.singletonList( artifact ), artifacts );
    }

    @Test
    public void testGetArtifactsByDateRangeLowerBoundOutOfRange()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date date = new Date( artifact.getWhenGathered().getTime() + 10000 );

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, date, null );

        assertThat( artifacts ).isNotNull().isEmpty();
    }

    @Test
    public void testGetArtifactsByDateRangeLowerAndUpperBound()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        Date lower = new Date( artifact.getWhenGathered().getTime() - 10000 );
        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, lower, upper );

        assertEquals( Collections.singletonList( artifact ), artifacts );
    }

    @Test
    public void testGetArtifactsByDateRangeUpperBound()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, null, upper );

        assertEquals( Collections.singletonList( artifact ), artifacts );
    }

    @Test
    public void testGetArtifactsByDateRangeUpperBoundOutOfRange()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        Date upper = new Date( artifact.getWhenGathered().getTime() - 10000 );

        List<ArtifactMetadata> artifacts = repository.getArtifactsByDateRange( TEST_REPO_ID, null, upper );

        assertThat( artifacts ).isNotNull().isEmpty();
    }

    @Test
    public void testGetArtifactsByRepoId()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        tryAssert(()-> {
            List<ArtifactMetadata> artifacts = repository.getArtifacts(TEST_REPO_ID);
            assertEquals(Collections.singletonList(artifact), artifacts);
        });
    }

    @Test
    public void testGetArtifactsByRepoIdMultipleCopies()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        ArtifactMetadata secondArtifact = createArtifact();
        secondArtifact.setRepositoryId( OTHER_REPO_ID );
        repository.updateArtifact( OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
        repository.save();

        // test it restricts to the appropriate repository
        tryAssert(() -> assertEquals( Collections.singletonList( artifact ), repository.getArtifacts( TEST_REPO_ID ) ));
        tryAssert(() -> assertEquals( Collections.singletonList( secondArtifact ), repository.getArtifacts( OTHER_REPO_ID ) ));
    }


    @Test
    public void testGetArtifactsByDateRangeMultipleCopies()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        ArtifactMetadata secondArtifact = createArtifact();
        secondArtifact.setRepositoryId( OTHER_REPO_ID );
        repository.updateArtifact( OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
        repository.save();

        // test it restricts to the appropriate repository
        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, null, null ) );
        assertEquals( Collections.singletonList( secondArtifact ),
                      repository.getArtifactsByDateRange( OTHER_REPO_ID, null, null ) );
    }

    @Test
    public void testGetArtifactsByChecksumMultipleCopies()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        ArtifactMetadata secondArtifact = createArtifact();
        secondArtifact.setRepositoryId( OTHER_REPO_ID );
        repository.updateArtifact( OTHER_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, secondArtifact );
        repository.save();

        // test it restricts to the appropriate repository
        assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 ) ) );
        assertEquals( Collections.singletonList( secondArtifact ), new ArrayList<>(
            repository.getArtifactsByChecksum( OTHER_REPO_ID, TEST_SHA1 ) ) );
        assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_MD5 ) ) );
        assertEquals( Collections.singletonList( secondArtifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( OTHER_REPO_ID, TEST_MD5 ) ) );
    }

    @Test
    public void testGetNamespacesWithSparseDepth()
        throws Exception
    {
        repository.updateNamespace( TEST_REPO_ID, "org.apache.maven.shared" );

        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );

        assertThat( namespaces ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "org" );

        namespaces = repository.getNamespaces( TEST_REPO_ID, "org" );
        assertThat( namespaces ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "apache" );

        namespaces = repository.getNamespaces( TEST_REPO_ID, "org.apache" );
        assertThat( namespaces ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "maven" );

        namespaces = repository.getNamespaces( TEST_REPO_ID, "org.apache.maven" );
        assertThat( namespaces ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "shared" );
    }

    @Test
    public void testGetNamespacesWithProjectsPresent()
        throws Exception
    {
        String namespace = "org.apache.maven.shared";
        repository.updateNamespace( TEST_REPO_ID, namespace );

        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, namespace, TEST_PROJECT, metadata );

        Collection<String> namespaces = repository.getNamespaces( TEST_REPO_ID, namespace );

        assertThat( namespaces ).isNotNull().isEmpty();

    }

    @Test
    public void testGetProjectsWithOtherNamespacesPresent()
        throws Exception
    {
        ProjectMetadata projectMetadata = new ProjectMetadata();
        projectMetadata.setId( TEST_PROJECT );
        projectMetadata.setNamespace( "org.apache.maven" );
        repository.updateProject( TEST_REPO_ID, projectMetadata );

        repository.updateNamespace( TEST_REPO_ID, "org.apache.maven.shared" );

        Collection<String> projects = repository.getProjects( TEST_REPO_ID, "org.apache.maven" );

        assertThat( projects ).isNotNull().isNotEmpty().hasSize( 1 ).contains( TEST_PROJECT );
    }

    @Test
    public void testGetProjectVersionsWithOtherNamespacesPresent()
        throws Exception
    {
        // an unusual case but technically possible where a project namespace matches another project's name

        ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata();
        versionMetadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, "org.apache.maven", TEST_PROJECT, versionMetadata );

        repository.updateProjectVersion( TEST_REPO_ID, "org.apache.maven." + TEST_PROJECT, "other-project",
                                         versionMetadata );

        Collection<String> versions =
            repository.getProjectVersions( TEST_REPO_ID, "org.apache.maven." + TEST_PROJECT, "other-project" );
        assertThat( versions ).isNotNull().isNotEmpty().contains( TEST_PROJECT_VERSION );

        versions = repository.getProjectVersions( TEST_REPO_ID, "org.apache.maven", TEST_PROJECT );

        assertThat( versions ).isNotNull().isNotEmpty().contains( TEST_PROJECT_VERSION );
    }

    @Test
    public void testGetArtifactsByChecksumSingleResultMd5()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_MD5 ) ) );
    }

    @Test
    public void testGetArtifactsByChecksumSingleResultSha1()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 ) ) );
    }

    @Test
    public void testGetArtifactsByChecksumDeepNamespace()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        String namespace = "multi.level.ns";
        artifact.setNamespace( namespace );
        repository.updateArtifact( TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        repository.save();

        tryAssert(()->assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 ) ) ));
        tryAssert(()->assertEquals( Collections.singletonList( artifact ),
                      new ArrayList<>( repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_MD5 ) ) ) );
    }

    @Test
    public void testGetArtifactsByChecksumMultipleResult()
        throws Exception
    {
        ArtifactMetadata artifact1 = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );

        String newProjectId = "another-project";
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setProject( newProjectId );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, newProjectId, TEST_PROJECT_VERSION, artifact2 );
        repository.save();

        tryAssert(()-> {
                    List<ArtifactMetadata> artifacts =
                            new ArrayList<>(repository.getArtifactsByChecksum(TEST_REPO_ID, TEST_SHA1));
                    Collections.sort(artifacts, new ArtifactMetadataComparator());
                    assertEquals(Arrays.asList(artifact2, artifact1), artifacts);
                });

        tryAssert(() -> {
            ArrayList<ArtifactMetadata> artifacts = new ArrayList<>(repository.getArtifactsByChecksum(TEST_REPO_ID, TEST_MD5));
            Collections.sort(artifacts, new ArtifactMetadataComparator());
            assertEquals(Arrays.asList(artifact2, artifact1), artifacts);
        });
    }

    @Test
    public void testGetArtifactsByChecksumNoResult()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Collection<ArtifactMetadata> artifactsByChecksum =
            repository.getArtifactsByChecksum( TEST_REPO_ID, "not checksum" );
        assertThat( artifactsByChecksum ).isNotNull().isEmpty();
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadata()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet( 10 );
        Collection<ArtifactMetadata> artifactsByMetadata =
            repository.getArtifactsByProjectVersionMetadata( TEST_METADATA_KEY, TEST_METADATA_VALUE, TEST_REPO_ID );
        assertThat( artifactsByMetadata ).hasSize( 1 );
        ArtifactMetadata artifactMetadata = artifactsByMetadata.iterator().next();
        assertThat( artifactMetadata.getId() ).isEqualTo( "projectId-1.0.jar" );
        assertThat( artifactMetadata.getSha1() ).isEqualTo( TEST_SHA1 );
        assertThat( artifactMetadata.getRepositoryId() ).isEqualTo( TEST_REPO_ID );
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadataNoRepository()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        Collection<ArtifactMetadata> artifactsByMetadata =
            repository.getArtifactsByProjectVersionMetadata( TEST_METADATA_KEY, TEST_METADATA_VALUE, null );
        assertThat( artifactsByMetadata ).hasSize( 1 );
        assertThat( artifactsByMetadata.iterator().next().getRepositoryId() ).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetArtifactsByProjectVersionMetadataAllRepositories()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        Collection<ArtifactMetadata> artifactsByMetadata =
            repository.getArtifactsByProjectVersionMetadata( TEST_METADATA_KEY, TEST_METADATA_VALUE, null );
        assertThat( artifactsByMetadata ).hasSize( 1 );
    }

    @Test
    public void testGetArtifactsByMetadataAllRepositories()
        throws Exception
    {
        createArtifactWithMavenArtifactFacet();
        tryAssert(()-> {
            Collection<ArtifactMetadata> artifactsByMetadata =
                    repository.getArtifactsByMetadata("foo", TEST_METADATA_VALUE, null);
            assertThat(artifactsByMetadata).hasSize(1);
            ArtifactMetadata artifactMetadata = artifactsByMetadata.iterator().next();
            assertThat(artifactMetadata.getId()).isEqualTo("projectId-1.0.jar");
            assertThat(artifactMetadata.getSha1()).isEqualTo(TEST_SHA1);
            assertThat(artifactMetadata.getRepositoryId()).isEqualTo(TEST_REPO_ID);
            MetadataFacet facet = artifactMetadata.getFacet(TEST_FACET_ID);
            assertThat(facet).isNotNull();
            assertThat(facet.toProperties()).isEqualTo(Collections.singletonMap("foo", TEST_METADATA_VALUE));
        });
    }

    @Test
    public void testGetArtifactsByPropertySingleResult()
        throws Exception
    {
        createArtifactWithData();
        // only works on JCR implementation
        // Collection<ArtifactMetadata> artifactsByProperty = repository.getArtifactsByProperty( "org.name", TEST_ORGANIZATION.getName(), TEST_REPO_ID );
        Collection<ArtifactMetadata> artifactsByProperty = repository.getArtifactsByProperty( "url", TEST_URL, TEST_REPO_ID );
        assertThat( artifactsByProperty ).hasSize( 1 );
        ArtifactMetadata artifactMetadata = artifactsByProperty.iterator().next();
        assertThat( artifactMetadata.getId() ).isEqualTo( "projectId-1.0.jar" );
        assertThat( artifactMetadata.getSha1() ).isEqualTo( TEST_SHA1 );
        assertThat( artifactMetadata.getRepositoryId() ).isEqualTo( TEST_REPO_ID );
    }

    @Test
    public void testDeleteRepository()
        throws Exception
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );

        ProjectMetadata project1 = new ProjectMetadata();
        project1.setNamespace( TEST_NAMESPACE );
        project1.setId( "project1" );
        repository.updateProject( TEST_REPO_ID, project1 );
        ProjectMetadata project2 = new ProjectMetadata();
        project2.setNamespace( TEST_NAMESPACE );
        project2.setId( "project2" );
        repository.updateProject( TEST_REPO_ID, project2 );

        ArtifactMetadata artifact1 = createArtifact();
        artifact1.setProject( "project1" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, "project1", TEST_PROJECT_VERSION, artifact1 );
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setProject( "project2" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, "project2", TEST_PROJECT_VERSION, artifact2 );
        repository.save();

        List<ArtifactMetadata> expected = Arrays.asList( artifact1, artifact2 );
        Collections.sort( expected, new ArtifactMetadataComparator() );


        tryAssert(()-> {
            List<ArtifactMetadata> actual =
                    new ArrayList<>( repository.getArtifactsByDateRange( TEST_REPO_ID, null, null ) );
            Collections.sort( actual, new ArtifactMetadataComparator() );
            assertEquals(expected, actual);
        });

        repository.removeRepository( TEST_REPO_ID );

        assertTrue( repository.getArtifacts( TEST_REPO_ID ).isEmpty() );
        assertTrue( repository.getRootNamespaces( TEST_REPO_ID ).isEmpty() );
    }


    @Test
    public void testDeleteArtifact()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        artifact.addFacet( new TestMetadataFacet( "value" ) );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertThat( repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                             TEST_PROJECT_VERSION ) ).containsExactly( artifact );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION_2_0, artifact );

        Collection<String> versions = repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        log.info( "versions {}", versions );

        assertThat( versions ).isNotNull().isNotEmpty().hasSize( 2 ).contains( "1.0", "2.0" );

        repository.removeArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact.getId() );

        versions = repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        log.info( "versions {}", versions );

        assertThat( versions ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "2.0" );

        assertThat( repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                             TEST_PROJECT_VERSION ) ).isNotNull().isEmpty();

        assertThat( repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                             TEST_PROJECT_VERSION_2_0 ) ).isNotEmpty().hasSize( 1 );
    }

    @Test
    public void deleteArtifact()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        artifact.addFacet( new TestMetadataFacet( "value" ) );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Collection<ArtifactMetadata> artifacts =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertEquals( Collections.singletonList( artifact ), new ArrayList<>( artifacts ) );

        repository.removeArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact.getId() );

        artifacts = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat( artifacts ).isNotNull().isEmpty();
    }

    @Test
    public void deleteVersion()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        artifact.addFacet( new TestMetadataFacet( "value" ) );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Collection<String> versions = repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        assertThat( versions ).isNotNull().isNotEmpty().hasSize( 1 );

        repository.removeProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        versions = repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        assertThat( versions ).isNotNull().isEmpty();
    }

    @Test
    public void deleteProject()
        throws Exception
    {
        ArtifactMetadata artifact = createArtifact();
        artifact.addFacet( new TestMetadataFacet( "value" ) );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( 1, repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ).size() );

        repository.removeProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        Collection<String> versions = repository.getProjectVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );

        assertThat( versions ).isNotNull().isEmpty();
    }


    @Test
    public void deleteSnapshotVersion()
        throws Exception
    {
        ArtifactMetadata artifactOne = createArtifact();
        artifactOne.setVersion( "2.0-20120618.214127-1" );
        artifactOne.setProjectVersion( "2.0-SNAPSHOT" );
        artifactOne.addFacet( new TestMetadataFacet( "value" ) );
        artifactOne.setId( TEST_PROJECT + "-" + "2.0-20120618.214127-1" + "." + "jar" );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT", artifactOne );

        ArtifactMetadata artifactTwo = createArtifact();
        artifactTwo.setVersion( "2.0-20120618.214135-2" );
        artifactTwo.setProjectVersion( "2.0-SNAPSHOT" );
        artifactTwo.addFacet( new TestMetadataFacet( "value" ) );
        artifactTwo.setId( TEST_PROJECT + "-" + "2.0-20120618.214135-2" + "." + "jar" );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT", artifactTwo );

        Collection<ArtifactMetadata> artifactMetadatas =
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

        assertThat( artifactMetadatas ).isNotNull().isNotEmpty().hasSize( 2 );

        log.info( "artifactMetadatas: {}", artifactMetadatas );

        repository.removeArtifact( artifactOne, "2.0-SNAPSHOT" );

        artifactMetadatas = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

        assertThat( artifactMetadatas ).isNotNull().isNotEmpty().hasSize( 1 );

        repository.removeArtifact( artifactTwo, "2.0-SNAPSHOT" );

        artifactMetadatas = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, "2.0-SNAPSHOT" );

        assertThat( artifactMetadatas ).isNotNull().isEmpty();
    }


    @Test
    public void testgetProjectReferences()
        throws Exception
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        metadata.setName( "project name" );
        metadata.setDescription( "project description" );
        metadata.setUrl( "the url" );

        Dependency d = new Dependency();
        d.setArtifactId( "artifactId" );
        d.setClassifier( "classifier" );
        d.setGroupId( "groupId" );
        d.setScope( "scope" );
        d.setSystemPath( "system path" );
        d.setType( "type" );
        d.setVersion( "version" );
        d.setOptional( true );
        metadata.addDependency( d );

        d = new Dependency();
        d.setArtifactId( "artifactId1" );
        d.setClassifier( "classifier" );
        d.setGroupId( "groupId" );
        d.setScope( "scope" );
        d.setSystemPath( "system path" );
        d.setType( "type" );
        d.setVersion( "version1" );
        d.setOptional( true );
        metadata.addDependency( d );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        repository.save();

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        Collection<ProjectVersionReference> references =
            repository.getProjectReferences( TEST_REPO_ID, d.getGroupId(), d.getArtifactId(), d.getVersion() );

        log.info( "references: {}", references );

        assertThat( references ).isNotNull().hasSize( 1 ).contains(
            new ProjectVersionReference( ProjectVersionReference.ReferenceType.DEPENDENCY, TEST_PROJECT, TEST_NAMESPACE,
                                         TEST_PROJECT_VERSION ) );

    }

    @Test
    public void testSearchArtifactsByKey()
        throws Exception
    {
        createArtifactWithData();
        Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( "url", TEST_URL, TEST_REPO_ID, false );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
    }

    @Test
    public void testSearchArtifactsByKeyExact()
        throws Exception
    {
        createArtifactWithData();
        Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( "url", TEST_URL, TEST_REPO_ID, true );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
        artifactsByProperty = repository.searchArtifacts( "org.name", "pache", TEST_REPO_ID, true );
        assertThat( artifactsByProperty ).isNotNull().isEmpty();
    }

    @Test
    public void testSearchArtifactsByFacetKey()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( TEST_METADATA_KEY, TEST_METADATA_VALUE, TEST_REPO_ID, false );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
    }

    @Test
    public void testSearchArtifactsByFacetKeyAllRepos()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        tryAssert(()-> {
            Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( TEST_METADATA_KEY, TEST_METADATA_VALUE, null, false );
            assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
        });
    }

    @Test
    public void testSearchArtifactsFullText()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        // only works in JCR
        // Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( TEST_URL, TEST_REPO_ID, false );
        Collection<ArtifactMetadata> artifactsByProperty =
            repository.searchArtifacts( TEST_METADATA_VALUE, TEST_REPO_ID, false );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
    }

    @Test
    public void testSearchArtifactsFullTextExact()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        // only works in JCR
        // Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( TEST_URL, TEST_REPO_ID, true );
        Collection<ArtifactMetadata> artifactsByProperty =
            repository.searchArtifacts( TEST_METADATA_VALUE, TEST_REPO_ID, true );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
        artifactsByProperty = repository.searchArtifacts( TEST_METADATA_VALUE.substring( 2 ), TEST_REPO_ID, true );
        assertThat( artifactsByProperty ).isNotNull().isEmpty();
    }

    @Test
    public void testSearchArtifactsFullTextByFacet()
        throws Exception
    {
        createArtifactWithGenericMetadataFacet();
        Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( TEST_METADATA_VALUE, TEST_REPO_ID, false );
        assertThat( artifactsByProperty ).isNotNull().isNotEmpty();
    }

    private static ProjectMetadata createProject()
    {
        return createProject( TEST_NAMESPACE );
    }

    private static ProjectMetadata createProject( String ns )
    {
        ProjectMetadata project = new ProjectMetadata();
        project.setId( TEST_PROJECT );
        project.setNamespace( ns );
        return project;
    }

    private void createArtifactWithGenericMetadataFacet()
        throws MetadataRepositoryException, MetadataResolutionException
    {
        createArtifactWithGenericMetadataFacet( 1 );
    }

    private void createArtifactWithGenericMetadataFacet( int artifacts )
        throws MetadataRepositoryException, MetadataResolutionException
    {
        MetadataFacet metadataFacet = new GenericMetadataFacet();
        Map<String, String> properties = new HashMap<>();
        properties.put( TEST_METADATA_KEY, TEST_METADATA_VALUE );
        metadataFacet.fromProperties( properties );
        createArtifactWithFacet( artifacts, null, metadataFacet );
    }

    private void createArtifactWithMavenArtifactFacet()
        throws MetadataRepositoryException, MetadataResolutionException
    {
        createArtifactWithMavenArtifactFacet( 1 );
    }

    private void createArtifactWithMavenArtifactFacet( int artifacts )
        throws MetadataRepositoryException, MetadataResolutionException
    {
        TestMetadataFacet facet = new TestMetadataFacet( TEST_METADATA_VALUE );
        createArtifactWithFacet( artifacts, facet, null );
    }

    private void createArtifactWithFacet( int artifacts, MetadataFacet artifactFacet,
                                          MetadataFacet projectVersionMetadataFacet )
        throws MetadataRepositoryException, MetadataResolutionException
    {
        for ( int i = 0; i < artifacts; i++ )
        {
            ArtifactMetadata artifact = createArtifact();
            if ( artifactFacet != null )
            {
                artifact.addFacet( artifactFacet );
            }
            repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        }
        if ( projectVersionMetadataFacet != null )
        {
            ProjectVersionMetadata metadata =
                repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
            metadata.addFacet( projectVersionMetadataFacet );
            metadata.setOrganization( TEST_ORGANIZATION );
            metadata.setUrl( TEST_URL );
            repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
        }
        repository.save();
    }

    private void createArtifactWithData()
        throws MetadataRepositoryException, MetadataResolutionException
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        ProjectVersionMetadata metadata =
            repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        metadata.setOrganization( TEST_ORGANIZATION );
        metadata.setUrl( TEST_URL );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
        repository.save();
    }

    private static ArtifactMetadata createArtifact()
    {
        return createArtifact( "jar" );
    }

    private static ArtifactMetadata createArtifact( String type )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "." + type );
        artifact.setWhenGathered( new Date() );
        artifact.setNamespace( TEST_NAMESPACE );
        artifact.setProject( TEST_PROJECT );
        artifact.setRepositoryId( TEST_REPO_ID );
        artifact.setFileLastModified( System.currentTimeMillis() );
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
            return a.getProject().compareTo( b.getProject() );
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
        public String getName()
        {
            return createNameFormat().format( date );
        }

        private static SimpleDateFormat createNameFormat()
        {
            SimpleDateFormat fmt = new SimpleDateFormat( SCAN_TIMESTAMP_FORMAT );
            fmt.setTimeZone( UTC_TIME_ZONE );
            return fmt;
        }

        @Override
        public String getFacetId()
        {
            return KindOfRepositoryStatistics.class.getName();
        }

        @Override
        public Map<String, String> toProperties()
        {
            return Collections.emptyMap();
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

        private TestMetadataFacet( String facetId, String value, Map<String, String> additionalProps )
        {
            this( facetId, value );
            this.additionalProps = additionalProps;
        }

        @Override
        public String getFacetId()
        {
            return testFacetId;
        }

        @Override
        public String getName()
        {
            return TEST_NAME;
        }

        @Override
        public Map<String, String> toProperties()
        {
            if ( value != null )
            {
                if ( additionalProps == null )
                {
                    return Collections.singletonMap( "foo", value );
                }
                else
                {
                    Map<String, String> props = new HashMap<>();
                    props.put( "foo", value );

                    for ( String key : additionalProps.keySet() )
                    {
                        props.put( key, additionalProps.get( key ) );
                    }
                    return props;
                }
            }
            else
            {
                return Collections.emptyMap();
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
                additionalProps = new HashMap<>();
            }

            for ( String key : properties.keySet() )
            {
                additionalProps.put( key, properties.get( key ) );
            }
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return "TestMetadataFacet{" + "value='" + value + '\'' + '}';
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
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
        public int hashCode()
        {
            return value != null ? value.hashCode() : 0;
        }
    }
}
