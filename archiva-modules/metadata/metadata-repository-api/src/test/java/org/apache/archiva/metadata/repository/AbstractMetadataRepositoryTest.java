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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMetadataRepositoryTest
    extends PlexusInSpringTestCase
{
    protected static final String OTHER_REPO_ID = "other-repo";

    protected MetadataRepository repository;

    protected static final String TEST_REPO_ID = "test";

    private static final String TEST_PROJECT = "projectId";

    private static final String TEST_NAMESPACE = "namespace";

    private static final String TEST_PROJECT_VERSION = "1.0";

    private static final String TEST_FACET_ID = "test-facet-id";

    private static final String TEST_NAME = "test/name";

    private static final String TEST_VALUE = "test-value";

    private static final String UNKNOWN = "unknown";

    private static final String TEST_MD5 = "bd4a9b642562547754086de2dab26b7d";

    private static final String TEST_SHA1 = "2e5daf0201ddeb068a62d5e08da18657ab2c6be9";

    private static final String TEST_METADATA_VALUE = "test-metadata";

    protected static Map<String, MetadataFacetFactory> createTestMetadataFacetFactories()
    {
        Map<String, MetadataFacetFactory> factories = new HashMap<String, MetadataFacetFactory>();
        factories.put( TEST_FACET_ID, new MetadataFacetFactory()
        {
            public MetadataFacet createMetadataFacet()
            {
                return new TestMetadataFacet( TEST_METADATA_VALUE );
            }

            public MetadataFacet createMetadataFacet( String repositoryId, String name )
            {
                return new TestMetadataFacet( TEST_METADATA_VALUE );
            }
        } );

        // add to ensure we don't accidentally create an empty facet ID.
        factories.put( "", new MetadataFacetFactory()
        {
            public MetadataFacet createMetadataFacet()
            {
                return new TestMetadataFacet( "", TEST_VALUE );
            }

            public MetadataFacet createMetadataFacet( String repositoryId, String name )
            {
                return new TestMetadataFacet( "", TEST_VALUE );
            }
        } );
        return factories;
    }

    public void testRootNamespaceWithNoMetadataRepository()
    {
        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );
        assertEquals( Collections.<String>emptyList(), namespaces );
    }

    public void testGetNamespaceOnly()
    {
        assertEquals( Collections.emptyList(), repository.getRootNamespaces( TEST_REPO_ID ) );

        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );

        assertEquals( Collections.singletonList( TEST_NAMESPACE ), repository.getRootNamespaces( TEST_REPO_ID ) );
    }

    public void testGetProjectOnly()
    {
        assertNull( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
        assertEquals( Collections.emptyList(), repository.getRootNamespaces( TEST_REPO_ID ) );

        ProjectMetadata project = new ProjectMetadata();
        project.setId( TEST_PROJECT );
        project.setNamespace( TEST_NAMESPACE );

        repository.updateProject( TEST_REPO_ID, project );

        project = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertEquals( TEST_PROJECT, project.getId() );
        assertEquals( TEST_NAMESPACE, project.getNamespace() );

        // test that namespace is also constructed
        assertEquals( Collections.singletonList( TEST_NAMESPACE ), repository.getRootNamespaces( TEST_REPO_ID ) );
    }

    public void testGetProjectVersionOnly()
        throws MetadataResolutionException
    {
        assertNull( repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) );
        assertNull( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
        assertEquals( Collections.<String>emptyList(), repository.getRootNamespaces( TEST_REPO_ID ) );

        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );

        // test that namespace and project is also constructed
        assertEquals( Collections.singletonList( TEST_NAMESPACE ), repository.getRootNamespaces( TEST_REPO_ID ) );
        ProjectMetadata projectMetadata = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertEquals( TEST_PROJECT, projectMetadata.getId() );
        assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace() );
    }

    public void testGetArtifactOnly()
        throws MetadataResolutionException
    {
        assertEquals( Collections.<ArtifactMetadata>emptyList(), new ArrayList<ArtifactMetadata>(
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ) );
        assertNull( repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) );
        assertNull( repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT ) );
        assertEquals( Collections.<String>emptyList(), repository.getRootNamespaces( TEST_REPO_ID ) );

        ArtifactMetadata metadata = createArtifact();

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, metadata );

        Collection<ArtifactMetadata> artifacts = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                                                          TEST_PROJECT_VERSION );
        assertEquals( Collections.singletonList( metadata ), new ArrayList<ArtifactMetadata>( artifacts ) );

        // test that namespace, project and project version is also constructed
        assertEquals( Collections.singletonList( TEST_NAMESPACE ), repository.getRootNamespaces( TEST_REPO_ID ) );

        ProjectMetadata projectMetadata = repository.getProject( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT );
        assertEquals( TEST_PROJECT, projectMetadata.getId() );
        assertEquals( TEST_NAMESPACE, projectMetadata.getNamespace() );

        ProjectVersionMetadata projectVersionMetadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE,
                                                                                      TEST_PROJECT,
                                                                                      TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, projectVersionMetadata.getId() );
    }

    public void testUpdateProjectVersionMetadataWithNoOtherArchives()
        throws MetadataResolutionException
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setOtherArchives( Collections.<String>emptyList() );
        metadata.setMailingLists( Collections.singletonList( mailingList ) );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );
        assertEquals( 1, metadata.getMailingLists().size() );
        mailingList = metadata.getMailingLists().get( 0 );
        assertEquals( "Foo List", mailingList.getName() );
        assertEquals( Collections.<String>emptyList(), mailingList.getOtherArchives() );
    }

    public void testUpdateProjectVersionMetadataWithAllElements()
        throws MetadataResolutionException
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        metadata.setName( "project name" );
        metadata.setDescription( "project description" );

        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setOtherArchives( Collections.singletonList( "other archive" ) );
        metadata.setMailingLists( Collections.singletonList( mailingList ) );

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

        Organization org = new Organization();
        org.setName( "org name" );
        org.setUrl( "url" );
        metadata.setOrganization( org );

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
        metadata.addDependency( d );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( TEST_PROJECT_VERSION, metadata.getId() );
        assertEquals( TEST_PROJECT_VERSION, metadata.getVersion() );
        assertEquals( "project name", metadata.getName() );
        assertEquals( "project description", metadata.getDescription() );

        assertEquals( 1, metadata.getMailingLists().size() );
        mailingList = metadata.getMailingLists().get( 0 );
        assertEquals( "Foo List", mailingList.getName() );
        assertEquals( Collections.singletonList( "other archive" ), mailingList.getOtherArchives() );

        assertEquals( "connection", metadata.getScm().getConnection() );
        assertEquals( "dev conn", metadata.getScm().getDeveloperConnection() );
        assertEquals( "url", metadata.getScm().getUrl() );

        assertEquals( "system", metadata.getCiManagement().getSystem() );
        assertEquals( "ci url", metadata.getCiManagement().getUrl() );

        assertEquals( "system", metadata.getIssueManagement().getSystem() );
        assertEquals( "issue tracker url", metadata.getIssueManagement().getUrl() );

        assertEquals( "org name", metadata.getOrganization().getName() );
        assertEquals( "url", metadata.getOrganization().getUrl() );

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
    }

    public void testUpdateProjectReference()
    {
        ProjectVersionReference reference = new ProjectVersionReference();
        reference.setNamespace( "another.namespace" );
        reference.setProjectId( "another-project-id" );
        reference.setProjectVersion( "1.1" );
        reference.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );

        repository.updateProjectReference( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION,
                                           reference );

        Collection<ProjectVersionReference> references = repository.getProjectReferences( TEST_REPO_ID, TEST_NAMESPACE,
                                                                                          TEST_PROJECT,
                                                                                          TEST_PROJECT_VERSION );
        assertEquals( 1, references.size() );
        reference = references.iterator().next();
        assertEquals( "another.namespace", reference.getNamespace() );
        assertEquals( "another-project-id", reference.getProjectId() );
        assertEquals( "1.1", reference.getProjectVersion() );
        assertEquals( ProjectVersionReference.ReferenceType.DEPENDENCY, reference.getReferenceType() );
    }

    public void testGetRepositories()
    {
        // currently set up this way so the behaviour of both the test and the mock config return the same repository
        // set as the File implementation just uses the config rather than the content
        repository.updateNamespace( TEST_REPO_ID, "namespace" );
        repository.updateNamespace( OTHER_REPO_ID, "namespace" );

        assertEquals( Arrays.asList( TEST_REPO_ID, OTHER_REPO_ID ), new ArrayList<String>(
            repository.getRepositories() ) );
    }

    public void testUpdateProjectVersionMetadataIncomplete()
        throws MetadataResolutionException
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

    public void testUpdateProjectVersionMetadataWithExistingFacets()
        throws MetadataResolutionException
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

    public void testUpdateProjectVersionMetadataWithNoExistingFacets()
        throws MetadataResolutionException
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.<String>emptyList(), new ArrayList<String>( metadata.getFacetIds() ) );

        metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.<String>emptyList(), new ArrayList<String>( metadata.getFacetIds() ) );
    }

    public void testUpdateProjectVersionMetadataWithExistingFacetsFacetPropertyWasRemoved()
        throws MetadataResolutionException
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );

        Map<String, String> additionalProps = new HashMap<String, String>();
        additionalProps.put( "deleteKey", "deleteValue" );

        MetadataFacet facet = new TestMetadataFacet( TEST_FACET_ID, "baz", additionalProps );
        metadata.addFacet( facet );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );

        TestMetadataFacet testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        Map<String, String> facetProperties = testFacet.toProperties();

        assertEquals( "deleteValue", facetProperties.get( "deleteKey" ) );

        facetProperties.remove( "deleteKey" );

        TestMetadataFacet newTestFacet = new TestMetadataFacet( TEST_FACET_ID, testFacet.getValue(), facetProperties );
        metadata.addFacet( newTestFacet );

        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );

        metadata = repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );
        assertEquals( Collections.singleton( TEST_FACET_ID ), metadata.getFacetIds() );
        testFacet = (TestMetadataFacet) metadata.getFacet( TEST_FACET_ID );
        assertFalse( testFacet.toProperties().containsKey( "deleteKey" ) );
    }

    public void testUpdateArtifactMetadataWithExistingFacets()
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

    public void testUpdateArtifactMetadataWithNoExistingFacets()
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

    public void testGetMetadataFacet()
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( new TestMetadataFacet( TEST_VALUE ), repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID,
                                                                                        TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenEmpty()
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownName()
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, UNKNOWN ) );
    }

    public void testGetMetadataFacetWhenDefaultValue()
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( null ) );

        assertEquals( new TestMetadataFacet( TEST_METADATA_VALUE ), repository.getMetadataFacet( TEST_REPO_ID,
                                                                                                 TEST_FACET_ID,
                                                                                                 TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownFacetId()
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME ) );
    }

    public void testGetMetadataFacets()
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( Collections.singletonList( TEST_NAME ), repository.getMetadataFacets( TEST_REPO_ID,
                                                                                            TEST_FACET_ID ) );
    }

    public void testGetMetadataFacetsWhenEmpty()
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    public void testRemoveFacets()
    {
        repository.addMetadataFacet( TEST_REPO_ID, new TestMetadataFacet( TEST_VALUE ) );

        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertFalse( facets.isEmpty() );

        repository.removeMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    public void testRemoveFacetsWhenEmpty()
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );

        repository.removeMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    public void testRemoveFacetsWhenUnknown()
    {
        // testing no exception
        repository.removeMetadataFacets( TEST_REPO_ID, UNKNOWN );
    }

    public void testRemoveFacetWhenUnknown()
    {
        // testing no exception
        repository.removeMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME );
    }

    public void testRemoveFacet()
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

    public void testRemoveFacetWhenEmpty()
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );

        repository.removeMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME );

        facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetArtifacts()
    {
        ArtifactMetadata artifact1 = createArtifact();
        ArtifactMetadata artifact2 = createArtifact( "pom" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        Collection<ArtifactMetadata> artifacts = repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                                                          TEST_PROJECT_VERSION );
        ArrayList<ArtifactMetadata> actual = new ArrayList<ArtifactMetadata>( artifacts );
        Collections.sort( actual, new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
        assertEquals( Arrays.asList( artifact1, artifact2 ), actual );
    }

    public void testGetArtifactVersions()
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

        List<String> versions = new ArrayList<String>( repository.getArtifactVersions( TEST_REPO_ID, TEST_NAMESPACE,
                                                                                       TEST_PROJECT,
                                                                                       TEST_PROJECT_VERSION ) );
        Collections.sort( versions );
        assertEquals( Arrays.asList( version1, version2 ), versions );
    }

    public void testGetArtifactVersionsMultipleArtifactsSingleVersion()
    {
        ArtifactMetadata artifact1 = createArtifact();
        artifact1.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "-sources.jar" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        assertEquals( Collections.singleton( TEST_PROJECT_VERSION ), repository.getArtifactVersions( TEST_REPO_ID,
                                                                                                     TEST_NAMESPACE,
                                                                                                     TEST_PROJECT,
                                                                                                     TEST_PROJECT_VERSION ) );
    }

    public void testGetArtifactsByDateRangeOpen()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByDateRange( TEST_REPO_ID, null,
                                                                                                 null ) );
    }

    public void testGetArtifactsByDateRangeSparseNamespace()
    {
        String namespace = "org.apache.archiva";
        ArtifactMetadata artifact = createArtifact();
        artifact.setNamespace( namespace );
        repository.updateArtifact( TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByDateRange( TEST_REPO_ID, null,
                                                                                                 null ) );
    }

    public void testGetArtifactsByDateRangeLowerBound()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date date = new Date( artifact.getWhenGathered().getTime() - 10000 );
        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByDateRange( TEST_REPO_ID, date,
                                                                                                 null ) );
    }

    public void testGetArtifactsByDateRangeLowerBoundOutOfRange()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date date = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertTrue( repository.getArtifactsByDateRange( TEST_REPO_ID, date, null ).isEmpty() );
    }

    public void testGetArtifactsByDateRangeLowerAndUpperBound()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date lower = new Date( artifact.getWhenGathered().getTime() - 10000 );
        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByDateRange( TEST_REPO_ID, lower,
                                                                                                 upper ) );
    }

    public void testGetArtifactsByDateRangeUpperBound()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByDateRange( TEST_REPO_ID, null,
                                                                                                 upper ) );
    }

    public void testGetArtifactsByDateRangeUpperBoundOutOfRange()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date upper = new Date( artifact.getWhenGathered().getTime() - 10000 );
        assertTrue( repository.getArtifactsByDateRange( TEST_REPO_ID, null, upper ).isEmpty() );
    }

    public void testGetArtifactsByRepoId()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );
        assertEquals( Collections.singletonList( artifact ), repository.getArtifacts( TEST_REPO_ID ) );
    }

    public void testGetNamespacesWithSparseDepth()
    {
        repository.updateNamespace( TEST_REPO_ID, "org.apache.maven.shared" );

        assertEquals( Arrays.asList( "org" ), repository.getRootNamespaces( TEST_REPO_ID ) );
        assertEquals( Arrays.asList( "apache" ), repository.getNamespaces( TEST_REPO_ID, "org" ) );
        assertEquals( Arrays.asList( "maven" ), repository.getNamespaces( TEST_REPO_ID, "org.apache" ) );
        assertEquals( Arrays.asList( "shared" ), repository.getNamespaces( TEST_REPO_ID, "org.apache.maven" ) );
    }

    public void testGetArtifactsByChecksumSingleResultMd5()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByChecksum( TEST_REPO_ID,
                                                                                                TEST_MD5 ) );
    }

    public void testGetArtifactsByChecksumSingleResultSha1()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByChecksum( TEST_REPO_ID,
                                                                                                TEST_SHA1 ) );
    }

    public void testGetArtifactsByChecksumDeepNamespace()
    {
        ArtifactMetadata artifact = createArtifact();
        String namespace = "multi.level.ns";
        artifact.setNamespace( namespace );
        repository.updateArtifact( TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), repository.getArtifactsByChecksum( TEST_REPO_ID,
                                                                                                TEST_SHA1 ) );
    }

    public void testGetArtifactsByChecksumMultipleResult()
    {
        ArtifactMetadata artifact1 = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );

        String newProjectId = "another-project";
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setProject( newProjectId );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, newProjectId, TEST_PROJECT_VERSION, artifact2 );

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( repository.getArtifactsByChecksum(
            TEST_REPO_ID, TEST_SHA1 ) );
        Collections.sort( artifacts, new ArtifactMetadataComparator() );
        assertEquals( Arrays.asList( artifact2, artifact1 ), artifacts );
    }

    public void testGetArtifactsByChecksumNoResult()
    {
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.<ArtifactMetadata>emptyList(), repository.getArtifactsByChecksum( TEST_REPO_ID,
                                                                                                    "not a checksum" ) );
    }

    public void testDeleteArtifact()
    {
        ArtifactMetadata artifact = createArtifact();
        artifact.addFacet( new TestMetadataFacet( "value" ) );

        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ), new ArrayList<ArtifactMetadata>( repository.getArtifacts(
            TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ) );

        repository.deleteArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact.getId() );

        assertTrue( repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                             TEST_PROJECT_VERSION ).isEmpty() );
    }

    public void testDeleteRepository()
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

        List<ArtifactMetadata> expected = Arrays.asList( artifact1, artifact2 );
        Collections.sort( expected, new ArtifactMetadataComparator() );

        List<ArtifactMetadata> actual = new ArrayList<ArtifactMetadata>( repository.getArtifactsByDateRange(
            TEST_REPO_ID, null, null ) );
        Collections.sort( actual, new ArtifactMetadataComparator() );

        assertEquals( expected, actual );

        repository.deleteRepository( TEST_REPO_ID );

        assertTrue( repository.getArtifacts( TEST_REPO_ID ).isEmpty() );
        assertTrue( repository.getRootNamespaces( TEST_REPO_ID ).isEmpty() );
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
        public final int compare( ArtifactMetadata a, ArtifactMetadata b )
        {
            return a.getProject().compareTo( b.getProject() );
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

        public String getFacetId()
        {
            return testFacetId;
        }

        public String getName()
        {
            return TEST_NAME;
        }

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
                    Map<String, String> props = new HashMap<String, String>();
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
                additionalProps = new HashMap<String, String>();
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
