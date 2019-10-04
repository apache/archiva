package org.apache.archiva.indexer.maven.search;
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
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class MavenRepositorySearchPaginateTest
    extends TestCase
{

    @Autowired
    ArchivaRepositoryRegistry repositoryRegistry;

    @After
    public void endTests() {
        assert repositoryRegistry!=null;
        repositoryRegistry.destroy();
    }

    @Test
    public void nonPaginatedResult()
        throws Exception
    {
        MavenRepositorySearch search = new MavenRepositorySearch();

        SearchResults searchResults = build( 10, new SearchResultLimits( 0 ) );

        searchResults = search.paginate( searchResults );

        assertEquals( 10, searchResults.getReturnedHitsCount() );

    }

    @Test
    public void nonPaginatedHugeResult()
        throws Exception
    {
        MavenRepositorySearch search = new MavenRepositorySearch();

        SearchResults origSearchResults = build( 63, new SearchResultLimits( 0 ) );

        SearchResults searchResults = search.paginate( origSearchResults );

        assertEquals( 30, searchResults.getReturnedHitsCount() );

        origSearchResults = build( 63, new SearchResultLimits( 1 ) );

        searchResults = search.paginate( origSearchResults );

        assertEquals( 30, searchResults.getReturnedHitsCount() );

    }

    @Test
    public void paginatedResult()
        throws Exception
    {
        MavenRepositorySearch search = new MavenRepositorySearch();

        SearchResults searchResults = build( 32, new SearchResultLimits( 1 ) );

        searchResults = search.paginate( searchResults );

        assertEquals( 2, searchResults.getReturnedHitsCount() );

    }


    SearchResults build( int number, SearchResultLimits limits )
    {
        SearchResults searchResults = new SearchResults();
        searchResults.setLimits( limits );
        for ( int i = 0; i < number; i++ )
        {
            SearchResultHit hit = new SearchResultHit();
            hit.setGroupId( "commons-foo" );
            hit.setArtifactId( "commons-bar-" + i );
            hit.setPackaging( "jar" );
            hit.setVersions( Arrays.asList( "1.0" ) );
            String id =
                SearchUtil.getHitId( hit.getGroupId(), hit.getArtifactId(), hit.getClassifier(), hit.getPackaging() );
            searchResults.addHit( id, hit );
        }

        searchResults.setTotalHits( number );
        return searchResults;

    }
}
