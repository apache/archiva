package org.apache.archiva.indexer.search;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.common.utils.ArchivaNexusIndexerUtil;
import org.apache.maven.archiva.common.utils.FileUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.artifact.IllegalArtifactCoordinateException;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.easymock.MockControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;


@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class NexusRepositorySearchTest
    extends AbstractNexusRepositorySearch
{


    private void createSimpleIndex( boolean scan )
        throws IOException, UnsupportedExistingLuceneIndexException, IllegalArtifactCoordinateException
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-test/1.0/archiva-test-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-test/2.0/archiva-test-2.0.jar" ) );

        createIndex( TEST_REPO_1, files, scan );
    }

    private void createIndexContainingMoreArtifacts( boolean scan )
        throws IOException, UnsupportedExistingLuceneIndexException, IllegalArtifactCoordinateException
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-test/1.0/archiva-test-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-test/2.0/archiva-test-2.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/org/apache/archiva/archiva-webapp/1.0/archiva-webapp-1.0.war" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/com/artifactid-numeric/1.0/artifactid-numeric-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/com/artifactid-numeric123/1.0/artifactid-numeric123-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/com/classname-search/1.0/classname-search-1.0.jar" ) );

        createIndex( TEST_REPO_1, files, scan );
    }
    
    private void createIndexContainingMultipleArtifactsSameVersion( boolean scan )
        throws IOException, UnsupportedExistingLuceneIndexException, IllegalArtifactCoordinateException
    {
        List<File> files = new ArrayList<File>();
        
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0-sources.jar" ) );
        
        createIndex( TEST_REPO_1, files, scan );
    }

    @Test
    public void testQuickSearch()
        throws Exception
    {
        createSimpleIndex( false );

        List<String> selectedRepos = Arrays.asList( TEST_REPO_1 );

        // search artifactId
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );

        archivaConfigControl.reset();

        // search groupId
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        results = search.search( "user", selectedRepos, "org.apache.archiva", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( "total hints not 1", 1, results.getTotalHits() );

        //TODO: search for class & package names
    }
    
    @Test
    public void testQuickSearchMultipleArtifactsSameVersion() 
        throws Exception
    {
        createIndexContainingMultipleArtifactsSameVersion( false );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        // search artifactId
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
        
        //only 1 version of 1.0 is retrieved
        assertEquals( 1, hit.getVersions().size() );
    }

    // search for existing artifact using multiple keywords
    @Test
    public void testQuickSearchWithMultipleKeywords()
        throws Exception
    {
        createSimpleIndex( false );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );
    }

    @Test
    public void testQuickSearchWithPagination()
        throws Exception
    {
        createSimpleIndex( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        // page 1
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "org", limits, new ArrayList<String>() );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getHits().size() );
        assertEquals( "total hits not 6 for page1 " + results, 6, results.getTotalHits() );
        assertEquals( "returned hits not 1 for page1 " + results, 1, results.getReturnedHitsCount() );
        assertEquals( limits, results.getLimits() );

        archivaConfigControl.reset();

        // page 2
        limits = new SearchResultLimits( 1 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        results = search.search( "user", selectedRepos, "org", limits, null );

        archivaConfigControl.verify();

        assertNotNull( results );

        assertEquals( "hits not 1", 1, results.getHits().size() );
        assertEquals( "total hits not 6 for page 2 " + results, 6, results.getTotalHits() );
        assertEquals( "returned hits not 1 for page2 " + results, 1, results.getReturnedHitsCount() );
        assertEquals( limits, results.getLimits() );
    }

    @Test
    public void testArtifactFoundInMultipleRepositories()
        throws Exception
    {
        createSimpleIndex( true );

        List<File> files = new ArrayList<File>();
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_2
            + "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_2
            + "/org/apache/archiva/archiva-search/1.1/archiva-search-1.1.jar" ) );
        createIndex( TEST_REPO_2, files, true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        selectedRepos.add( TEST_REPO_2 );

        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 4, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( 2, hit.getVersions().size() );
        assertTrue( hit.getVersions().contains( "1.0" ) );
        assertTrue( hit.getVersions().contains( "1.1" ) );

        archivaConfigControl.reset();

        // TODO: [BROWSE] in artifact info from browse, display all the repositories where the artifact is found
    }

    @Test
    public void testNoMatchFound()
        throws Exception
    {
        createSimpleIndex( false );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "dfghdfkweriuasndsaie", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );
    }

    @Test
    public void testNoIndexFound()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "org.apache.archiva", null, null );
        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );

        archivaConfigControl.verify();
    }

    @Test
    public void testRepositoryNotFound()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "non-existing-repo" );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "org.apache.archiva", null, null );
        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );

        archivaConfigControl.verify();
    }

    @Test
    public void testSearchWithinSearchResults()
        throws Exception
    {
        createSimpleIndex( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        List<String> previousSearchTerms = new ArrayList<String>();
        previousSearchTerms.add( "archiva-test" );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "1.0", null, previousSearchTerms );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( "total hints not 1", 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-test", hit.getArtifactId() );
        assertEquals("versions not 1", 1, hit.getVersions().size() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
    }

    // tests for advanced search
    @Test
    public void testAdvancedSearch()
        throws Exception
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_2
            + "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_2
            + "/org/apache/archiva/archiva-search/1.1/archiva-search-1.1.jar" ) );
        createIndex( TEST_REPO_2, files, false );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_2 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setVersion( "1.0" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
    }

    @Test
    public void testAdvancedSearchWithPagination()
        throws Exception
    {
        createIndexContainingMoreArtifacts( false );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setRepositories( selectedRepos );

        // page 1

        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, limits );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 4, results.getTotalHits() );
        assertEquals( 1, results.getHits().size() );

        // page 2
        archivaConfigControl.reset();

        limits = new SearchResultLimits( 1 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        results = search.search( "user", searchFields, limits );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 4, results.getTotalHits() );
        assertEquals( 1, results.getHits().size() );
    }

    // MRM-981 - artifactIds with numeric characters aren't found in advanced search
    @Test
    public void testAdvancedSearchArtifactIdHasNumericChar()
        throws Exception
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/com/artifactid-numeric/1.0/artifactid-numeric-1.0.jar" ) );
        files.add( new File( FileUtil.getBasedir(), "/target/test-classes/" + TEST_REPO_1
            + "/com/artifactid-numeric123/1.0/artifactid-numeric123-1.0.jar" ) );
        createIndex( TEST_REPO_1, files, true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setArtifactId( "artifactid-numeric" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 1 );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );
    }

    @Test
    public void testAdvancedSearchNoRepositoriesConfigured()
        throws Exception
    {
        SearchFields searchFields = new SearchFields();
        searchFields.setArtifactId( "archiva" );
        searchFields.setRepositories( null );

        try
        {
            search.search( "user", searchFields, null );
            fail( "A RepositorySearchExcecption should have been thrown." );
        }
        catch ( RepositorySearchException e )
        {
            assertEquals( "Repositories cannot be null.", e.getMessage() );
        }
    }

    @Test
    public void testAdvancedSearchSearchFieldsAreNull()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setRepositories( selectedRepos );

        try
        {
            archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 1 );

            archivaConfigControl.replay();

            search.search( "user", searchFields, null );

            archivaConfigControl.verify();

            fail( "A RepositorySearchExcecption should have been thrown." );
        }
        catch ( RepositorySearchException e )
        {
            assertEquals( "No search fields set.", e.getMessage() );
        }
    }

    @Test
    public void testAdvancedSearchSearchFieldsAreBlank()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "" );
        searchFields.setArtifactId( "" );
        searchFields.setVersion( "" );
        searchFields.setPackaging( "" );
        searchFields.setClassName( "" );

        searchFields.setRepositories( selectedRepos );

        try
        {
            archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 1 );

            archivaConfigControl.replay();

            search.search( "user", searchFields, null );

            archivaConfigControl.verify();

            fail( "A RepositorySearchExcecption should have been thrown." );
        }
        catch ( RepositorySearchException e )
        {
            assertEquals( "No search fields set.", e.getMessage() );
        }
    }

    @Test
    public void testAdvancedSearchAllSearchCriteriaSpecified()
        throws Exception
    {
        createSimpleIndex( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setArtifactId( "archiva-test" );
        searchFields.setVersion( "2.0" );
        searchFields.setPackaging( "jar" );
        searchFields.setClassName( "org.apache.archiva.test.App" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndDefaultReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );

        assertEquals( "total hints not 1" + results, 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-test", hit.getArtifactId() );
        assertEquals( "version not 2.0", "2.0", hit.getVersions().get( 0 ) );
    }

    @Test
    public void testAdvancedSearchJarArtifacts()
        throws Exception
    {
        createIndexContainingMoreArtifacts( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setPackaging( "jar" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 7, results.getTotalHits() );
    }

    @Test
    public void testAdvancedSearchWithIncorrectPackaging()
        throws Exception
    {
        createSimpleIndex( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setArtifactId( "archiva-test" );
        searchFields.setVersion( "2.0" );
        searchFields.setPackaging( "war" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndDefaultReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );
    }

    @Test
    public void testAdvancedSearchClassname()
        throws Exception
    {
        createIndexContainingMoreArtifacts( true );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setClassName( "com.classname.search.App" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( "totalHits not 1 results " + results, 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "groupId not com", "com", hit.getGroupId() );
        assertEquals( "arttifactId not classname-search", "classname-search", hit.getArtifactId() );
        assertEquals( " hits.version(0) not 1.0", "1.0", hit.getVersions().get( 0 ) );
    }

    @Test
    public void testAdvancedSearchNoIndexFound()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setRepositories( selectedRepos );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );
    }
}
