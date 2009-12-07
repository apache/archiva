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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
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

    private static final String TEST_NAME = "test-name";

    private static final String TEST_VALUE = "test-value";

    private static final String UNKNOWN = "unknown";

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
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( new TestMetadataFacet( TEST_VALUE ),
                      repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenEmpty()
    {
        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownName()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, UNKNOWN ) );
    }

    public void testGetMetadataFacetWhenDefaultValue()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME, new TestMetadataFacet( null ) );

        assertEquals( new TestMetadataFacet( "test-metadata" ),
                      repository.getMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME ) );
    }

    public void testGetMetadataFacetWhenUnknownFacetId()
    {
        repository.addMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME, new TestMetadataFacet( TEST_VALUE ) );

        assertNull( repository.getMetadataFacet( TEST_REPO_ID, UNKNOWN, TEST_NAME ) );
    }

    public void testGetMetadataFacets()
    {
        repository.addMetadataFacet( TEST_REPO_ID, TEST_FACET_ID, TEST_NAME, new TestMetadataFacet( TEST_VALUE ) );

        assertEquals( Collections.singletonList( TEST_NAME ),
                      repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID ) );
    }

    public void testGetMetadataFacetsWhenEmpty()
    {
        List<String> facets = repository.getMetadataFacets( TEST_REPO_ID, TEST_FACET_ID );
        assertTrue( facets.isEmpty() );
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
