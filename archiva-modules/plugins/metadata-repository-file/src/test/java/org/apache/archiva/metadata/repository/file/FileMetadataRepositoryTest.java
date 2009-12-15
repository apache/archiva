package org.apache.archiva.metadata.repository.file;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class FileMetadataRepositoryTest
    extends PlexusInSpringTestCase
{
    private FileMetadataRepository repository;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_PROJECT = "projectId";

    private static final String TEST_NAMESPACE = "namespace";

    private static final String TEST_PROJECT_VERSION = "1.0";

    private static final String TEST_FACET_ID = "test-facet-id";

    private static final String TEST_NAME = "test/name";

    private static final String TEST_VALUE = "test-value";

    private static final String UNKNOWN = "unknown";

    private static final String OTHER_REPO = "other-repo";

    private static final String TEST_MD5 = "bd4a9b642562547754086de2dab26b7d";

    private static final String TEST_SHA1 = "2e5daf0201ddeb068a62d5e08da18657ab2c6be9";

    public void setUp()
        throws Exception
    {
        super.setUp();

        repository = new FileMetadataRepository();
        File directory = getTestFile( "target/test-repository" );
        FileUtils.deleteDirectory( directory );
        repository.setDirectory( directory );

        repository.setMetadataFacetFactories(
            Collections.<String, MetadataFacetFactory>singletonMap( TEST_FACET_ID, new MetadataFacetFactory()
            {
                public MetadataFacet createMetadataFacet()
                {
                    return new TestMetadataFacet( "test-metadata" );
                }
            } ) );
    }

    public void testRootNamespaceWithNoMetadataRepository()
    {
        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );
        assertEquals( Collections.<String>emptyList(), namespaces );
    }

    public void testUpdateProjectVersionMetadataWithNoOtherArchives()
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT_VERSION );
        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setOtherArchives( Collections.<String>emptyList() );
        metadata.setMailingLists( Collections.singletonList( mailingList ) );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
    }

    public void testUpdateProjectVersionMetadataWithExistingFacets()
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

    public void testGetMetadataFacet()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( new TestMetadataFacet( TEST_VALUE ),
                      repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenEmpty()
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownName()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, UNKNOWN ) );
    }

    public void testGetMetadataFacetWhenDefaultValue()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( null ) );

        assertEquals( new TestMetadataFacet( "test-metadata" ),
                      repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownFacetId()
    {
        repository.addMetadataFacet( TEST_REPO_ID, UNKNOWN, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME ) );
    }

    public void testGetMetadataFacets()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( Collections.singletonList( TEST_NAME ),
                      repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID ) );
    }

    public void testGetMetadataFacetsWhenEmpty()
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
    }

    public void testRemoveFacets()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );

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
        repository.removeMetadataFacets( TEST_REPO_ID, UNKNOWN );
    }

    public void testRemoveFacet()
    {
        TestMetadataFacet metadataFacet = new TestMetadataFacet( TEST_VALUE );
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, metadataFacet );

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

    public void testRemoveFacetWhenUnknown()
    {
        repository.removeMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME );
    }

    public void testGetArtifacts()
    {
        ArtifactMetadata artifact1 = createArtifact();
        ArtifactMetadata artifact2 = createArtifact( "pom" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        assertEquals( Arrays.asList( artifact2, artifact1 ), new ArrayList<ArtifactMetadata>(
            repository.getArtifacts( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ) );
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

        assertEquals( new HashSet<String>( Arrays.asList( version2, version1 ) ),
                      repository.getArtifactVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                                      TEST_PROJECT_VERSION ) );
    }

    public void testGetArtifactVersionsMultipleArtifactsSingleVersion()
    {
        ArtifactMetadata artifact1 = createArtifact();
        artifact1.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + ".jar" );
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "-sources.jar" );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact2 );

        assertEquals( Collections.singleton( TEST_PROJECT_VERSION ),
                      repository.getArtifactVersions( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT,
                                                      TEST_PROJECT_VERSION ) );
    }

    public void testRepositories()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );
        repository.addMetadataFacet( OTHER_REPO, TEST_FACET_ID, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( Arrays.asList( OTHER_REPO, TEST_REPO_ID ), repository.getRepositories() );
    }

    public void testRepositoriesWhenEmpty()
    {
        assertTrue( repository.getRepositories().isEmpty() );
    }

    public void testGetArtifactsByDateRangeOpen()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, null, null ) );
    }

    public void testGetArtifactsByDateRangeSparseNamespace()
    {
        String namespace = "org.apache.archiva";
        repository.updateNamespace( TEST_REPO_ID, namespace );
        repository.updateProject( TEST_REPO_ID, createProject( namespace ) );
        ArtifactMetadata artifact = createArtifact();
        artifact.setNamespace( namespace );
        repository.updateArtifact( TEST_REPO_ID, namespace, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, null, null ) );
    }

    public void testGetArtifactsByDateRangeLowerBound()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date date = new Date( artifact.getWhenGathered().getTime() - 10000 );
        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, date, null ) );
    }

    public void testGetArtifactsByDateRangeLowerBoundOutOfRange()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date date = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertTrue( repository.getArtifactsByDateRange( TEST_REPO_ID, date, null ).isEmpty() );
    }

    public void testGetArtifactsByDateRangeLowerAndUpperBound()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date lower = new Date( artifact.getWhenGathered().getTime() - 10000 );
        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, lower, upper ) );
    }

    public void testGetArtifactsByDateRangeUpperBound()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date upper = new Date( artifact.getWhenGathered().getTime() + 10000 );
        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByDateRange( TEST_REPO_ID, null, upper ) );
    }

    public void testGetArtifactsByDateRangeUpperBoundOutOfRange()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        Date upper = new Date( artifact.getWhenGathered().getTime() - 10000 );
        assertTrue( repository.getArtifactsByDateRange( TEST_REPO_ID, null, upper ).isEmpty() );
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
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_MD5 ) );
    }

    public void testGetArtifactsByChecksumSingleResultSha1()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.singletonList( artifact ),
                      repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 ) );
    }

    public void testGetArtifactsByChecksumMultipleResult()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );

        ProjectMetadata projectMetadata = createProject();
        repository.updateProject( TEST_REPO_ID, projectMetadata );
        ArtifactMetadata artifact1 = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact1 );

        projectMetadata = createProject();
        String newProjectId = "another-project";
        projectMetadata.setId( newProjectId );
        repository.updateProject( TEST_REPO_ID, projectMetadata );
        ArtifactMetadata artifact2 = createArtifact();
        artifact2.setProject( newProjectId );
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, newProjectId, TEST_PROJECT_VERSION, artifact2 );

        assertEquals( Arrays.asList( artifact2, artifact1 ),
                      repository.getArtifactsByChecksum( TEST_REPO_ID, TEST_SHA1 ) );
    }

    public void testGetArtifactsByChecksumNoResult()
    {
        repository.updateNamespace( TEST_REPO_ID, TEST_NAMESPACE );
        repository.updateProject( TEST_REPO_ID, createProject() );
        ArtifactMetadata artifact = createArtifact();
        repository.updateArtifact( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION, artifact );

        assertEquals( Collections.<ArtifactMetadata>emptyList(),
                      repository.getArtifactsByChecksum( TEST_REPO_ID, "not a checksum" ) );
    }

    private ProjectMetadata createProject()
    {
        return createProject( TEST_NAMESPACE );
    }

    private ProjectMetadata createProject( String ns )
    {
        ProjectMetadata project = new ProjectMetadata();
        project.setId( TEST_PROJECT );
        project.setNamespace( ns );
        return project;
    }

    private ArtifactMetadata createArtifact()
    {
        return createArtifact( "jar" );
    }

    private ArtifactMetadata createArtifact( String type )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( TEST_PROJECT + "-" + TEST_PROJECT_VERSION + "." + type );
        artifact.setWhenGathered( new Date() );
        artifact.setNamespace( TEST_NAMESPACE );
        artifact.setProject( TEST_PROJECT );
        artifact.setRepositoryId( TEST_REPO_ID );
        artifact.setFileLastModified( System.currentTimeMillis() );
        artifact.setVersion( TEST_PROJECT_VERSION );
        artifact.setMd5( TEST_MD5 );
        artifact.setSha1( TEST_SHA1 );
        return artifact;
    }

    private static class TestMetadataFacet
        implements MetadataFacet
    {
        private TestMetadataFacet( String value )
        {
            this.value = value;
        }

        private String value;

        public String getFacetId()
        {
            return TEST_FACET_ID;
        }

        public String getName()
        {
            return TEST_NAME;
        }

        public Map<String, String> toProperties()
        {
            if ( value != null )
            {
                return Collections.singletonMap( TEST_FACET_ID + ":foo", value );
            }
            else
            {
                return Collections.emptyMap();
            }
        }

        public void fromProperties( Map<String, String> properties )
        {
            String value = properties.get( TEST_FACET_ID + ":foo" );
            if ( value != null )
            {
                this.value = value;
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
