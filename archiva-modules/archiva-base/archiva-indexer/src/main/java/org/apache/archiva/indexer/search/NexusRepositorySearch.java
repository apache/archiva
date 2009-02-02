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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.search.SearchResultHit;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexContextInInconsistentStateException;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;

/**
 * RepositorySearch implementation which uses the Nexus Indexer for searching.
 */
public class NexusRepositorySearch
    implements RepositorySearch
{
    private static final Logger log = LoggerFactory.getLogger( NexusRepositorySearch.class ); 
                                                              
    private NexusIndexer indexer;
    
    private ArchivaConfiguration archivaConfig;
    
    public NexusRepositorySearch( NexusIndexer indexer, ArchivaConfiguration archivaConfig )
    {
        this.indexer = indexer;
        this.archivaConfig = archivaConfig;
    }

    /**
     * @see RepositorySearch#search(String, List, String, SearchResultLimits, List)
     */
    public SearchResults search( String principal, List<String> selectedRepos, String term, SearchResultLimits limits,
                                 List<String> previousSearchTerms )
        throws RepositorySearchException
    {   
        addIndexingContexts( selectedRepos );
        
        // TODO: 
        // 1. construct query for:
        //    - regular search
        //    - searching within search results
        // 3. multiple repositories
        
        BooleanQuery q = new BooleanQuery();
        if( previousSearchTerms == null || previousSearchTerms.isEmpty() )
        {            
            constructQuery( term, q );
        }
        else
        {   
            for( String previousTerm : previousSearchTerms )
            {
                BooleanQuery iQuery = new BooleanQuery();
                constructQuery( previousTerm, iQuery );
                
                q.add( iQuery, Occur.MUST );
            }
            
            BooleanQuery iQuery = new BooleanQuery();
            constructQuery( term, iQuery );
            q.add( iQuery, Occur.MUST );
        }        
                    
        try
        {
            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = indexer.searchFlat( request );
            
            if( response == null || response.getTotalHits() == 0 )
            {
                return new SearchResults();
            }
            
            return convertToSearchResults( response, limits );
        }
        catch ( IndexContextInInconsistentStateException e )
        {
            throw new RepositorySearchException( e );
        }
        catch ( IOException e )
        {
            throw new RepositorySearchException( e );
        }
        finally
        {
            Map<String, IndexingContext> indexingContexts = indexer.getIndexingContexts();
            Set<String> keys = indexingContexts.keySet();
            for( String key : keys )
            {
                try                
                {   
                    indexer.removeIndexingContext( indexingContexts.get( key ), false );
                    log.debug( "Indexing context '" + key + "' removed from search." );
                }
                catch ( IOException e )
                {
                    log.warn( "IOException occurred while removing indexing content '" + key  + "'." );
                    continue;
                }
            }            
        }
    }

    private void constructQuery( String term, BooleanQuery q )
    {
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, term ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, term ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.VERSION, term ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.PACKAGING, term ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.NAMES, term ), Occur.SHOULD );        
    }
       
    /**
     * @see RepositorySearch#search(String, SearchFields, SearchResultLimits)
     */
    public SearchResults search( String principal, SearchFields searchFields, SearchResultLimits limits )
        throws RepositorySearchException
    {
        // TODO Auto-generated method stub
        return null;
    }

    private void addIndexingContexts( List<String> selectedRepos )
    {
        for( String repo : selectedRepos )
        {
            try
            {
                Configuration config = archivaConfig.getConfiguration();
                ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( repo );
                
                if( repoConfig != null )
                {
                    String indexDir = repoConfig.getIndexDir();
                    File indexDirectory = null;
                    if( indexDir != null && !"".equals( indexDir ) )
                    {
                        indexDirectory = new File( repoConfig.getIndexDir() );
                    }
                    else
                    {
                        indexDirectory = new File( repoConfig.getLocation(), ".indexer" );
                    }
                    
                    IndexingContext context =
                        indexer.addIndexingContext( repoConfig.getId(), repoConfig.getId(), new File( repoConfig.getLocation() ),
                                                    indexDirectory, null, null, NexusIndexer.FULL_INDEX );
                    context.setSearchable( repoConfig.isScanned() );
                }
                else
                {
                    log.warn( "Repository '" + repo + "' not found in configuration." );
                }
            }
            catch ( UnsupportedExistingLuceneIndexException e )
            {                
                log.warn( "Error accessing index of repository '" + repo + "' : " + e.getMessage() );
                continue;
            }
            catch ( IOException e )
            {                
                log.warn( "IO error occured while accessing index of repository '" + repo + "' : " + e.getMessage() );
                continue;
            }
        }
    }

    private SearchResults convertToSearchResults( FlatSearchResponse response, SearchResultLimits limits )
    {   
        SearchResults results = new SearchResults();
        Set<ArtifactInfo> artifactInfos = response.getResults();
        
        for ( ArtifactInfo artifactInfo : artifactInfos )
        {
            String id = SearchUtil.getHitId( artifactInfo.groupId, artifactInfo.artifactId );            
            Map<String, SearchResultHit> hitsMap = results.getHitsMap();

            SearchResultHit hit = hitsMap.get( id );
            if ( hit != null )
            {
                hit.addVersion( artifactInfo.version );
            }
            else
            {
                hit = new SearchResultHit();
                hit.setArtifactId( artifactInfo.artifactId );
                hit.setGroupId( artifactInfo.groupId );
                // do we still need to set the repository id even though we're merging everything?
                //hit.setRepositoryId( artifactInfo.repository );
                hit.setUrl( artifactInfo.repository + "/" + artifactInfo.fname );
                if( !hit.getVersions().contains( artifactInfo.version ) )
                {
                    hit.addVersion( artifactInfo.version );
                }
            }

            results.addHit( id, hit );
        }
        
        results.setTotalHits( results.getHitsMap().size() );
        
        if( limits == null || limits.getSelectedPage() == SearchResultLimits.ALL_PAGES )
        {   
            return results;
        }
        else
        {
            return paginate( limits, results );            
        }        
    }

    private SearchResults paginate( SearchResultLimits limits, SearchResults results )
    {
        SearchResults paginated = new SearchResults();        
        int fetchCount = limits.getPageSize();
        int offset = ( limits.getSelectedPage() * limits.getPageSize() );
        
        if( fetchCount > results.getTotalHits() )
        {
            fetchCount = results.getTotalHits();
        }
        
        // Goto offset.
        if ( offset < results.getTotalHits() )
        {
            // only process if the offset is within the hit count.
            for ( int i = 0; i < fetchCount; i++ )
            {
                // Stop fetching if we are past the total # of available hits.
                if ( offset + i > results.getTotalHits() )
                {
                    break;
                }
                
                SearchResultHit hit = results.getHits().get( ( offset + i ) );
                if( hit != null )
                {
                    String id = SearchUtil.getHitId( hit.getGroupId(), hit.getArtifactId() );
                    paginated.addHit( id, hit );
                }
                else
                {
                    break;
                }
            }
        }            
        paginated.setTotalHits( paginated.getHitsMap().size() );
        
        return paginated;
    }

}
