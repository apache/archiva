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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.archiva.common.utils.ArchivaNexusIndexerUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RepositorySearch implementation which uses the Nexus Indexer for searching.
 */
@Service( "nexusSearch" )
public class NexusRepositorySearch
    implements RepositorySearch
{
    private Logger log = LoggerFactory.getLogger( NexusRepositorySearch.class );

    private NexusIndexer indexer;

    private ArchivaConfiguration archivaConfig;

    @Inject
    public NexusRepositorySearch( PlexusSisuBridge plexusSisuBridge, ArchivaConfiguration archivaConfig )
        throws PlexusSisuBridgeException
    {
        this.indexer = plexusSisuBridge.lookup( NexusIndexer.class );
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

        // since upgrade to nexus 2.0.0, query has changed from g:[QUERIED TERM]* to g:*[QUERIED TERM]*
        //      resulting to more wildcard searches so we need to increase max clause count
        BooleanQuery.setMaxClauseCount( Integer.MAX_VALUE );
        BooleanQuery q = new BooleanQuery();

        if ( previousSearchTerms == null || previousSearchTerms.isEmpty() )
        {
            constructQuery( term, q );
        }
        else
        {
            for ( String previousTerm : previousSearchTerms )
            {
                BooleanQuery iQuery = new BooleanQuery();
                constructQuery( previousTerm, iQuery );

                q.add( iQuery, Occur.MUST );
            }

            BooleanQuery iQuery = new BooleanQuery();
            constructQuery( term, iQuery );
            q.add( iQuery, Occur.MUST );
        }

        return search( limits, q );
    }

    /**
     * @see RepositorySearch#search(String, SearchFields, SearchResultLimits)
     */
    public SearchResults search( String principal, SearchFields searchFields, SearchResultLimits limits )
        throws RepositorySearchException
    {
        if ( searchFields.getRepositories() == null )
        {
            throw new RepositorySearchException( "Repositories cannot be null." );
        }

        addIndexingContexts( searchFields.getRepositories() );

        BooleanQuery q = new BooleanQuery();
        if ( searchFields.getGroupId() != null && !"".equals( searchFields.getGroupId() ) )
        {
            q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, searchFields.getGroupId() ), Occur.MUST );
        }

        if ( searchFields.getArtifactId() != null && !"".equals( searchFields.getArtifactId() ) )
        {
            q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, searchFields.getArtifactId() ), Occur.MUST );
        }

        if ( searchFields.getVersion() != null && !"".equals( searchFields.getVersion() ) )
        {
            q.add( indexer.constructQuery( ArtifactInfo.VERSION, searchFields.getVersion() ), Occur.MUST );
        }

        if ( searchFields.getPackaging() != null && !"".equals( searchFields.getPackaging() ) )
        {
            q.add( indexer.constructQuery( ArtifactInfo.PACKAGING, searchFields.getPackaging() ), Occur.MUST );
        }

        if ( searchFields.getClassName() != null && !"".equals( searchFields.getClassName() ) )
        {
            q.add( indexer.constructQuery( ArtifactInfo.NAMES, searchFields.getClassName() ), Occur.MUST );
        }

        if ( q.getClauses() == null || q.getClauses().length <= 0 )
        {
            throw new RepositorySearchException( "No search fields set." );
        }

        return search( limits, q );
    }

    private SearchResults search( SearchResultLimits limits, BooleanQuery q )
        throws RepositorySearchException
    {
        try
        {
            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = indexer.searchFlat( request );

            if ( response == null || response.getTotalHits() == 0 )
            {
                SearchResults results = new SearchResults();
                results.setLimits( limits );
                return results;
            }

            return convertToSearchResults( response, limits );
        }
        catch ( IOException e )
        {
            throw new RepositorySearchException( e );
        }
        finally
        {
            Map<String, IndexingContext> indexingContexts = indexer.getIndexingContexts();

            for ( Map.Entry<String, IndexingContext> entry : indexingContexts.entrySet() )
            {
                try
                {
                    indexer.removeIndexingContext( entry.getValue(), false );
                    log.debug( "Indexing context '" + entry.getKey() + "' removed from search." );
                }
                catch ( IOException e )
                {
                    log.warn( "IOException occurred while removing indexing content '" + entry.getKey() + "'." );
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


    private void addIndexingContexts( List<String> selectedRepos )
    {
        for ( String repo : selectedRepos )
        {
            try
            {
                Configuration config = archivaConfig.getConfiguration();
                ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( repo );

                if ( repoConfig != null )
                {
                    String indexDir = repoConfig.getIndexDir();
                    File indexDirectory = null;
                    if ( indexDir != null && !"".equals( indexDir ) )
                    {
                        indexDirectory = new File( repoConfig.getIndexDir() );
                    }
                    else
                    {
                        indexDirectory = new File( repoConfig.getLocation(), ".indexer" );
                    }

                    IndexingContext context = indexer.addIndexingContext( repoConfig.getId(), repoConfig.getId(),
                                                                          new File( repoConfig.getLocation() ),
                                                                          indexDirectory, null, null,
                                                                          ArchivaNexusIndexerUtil.FULL_INDEX );
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
                if ( !hit.getVersions().contains( artifactInfo.version ) )
                {
                    hit.addVersion( artifactInfo.version );
                }
            }

            results.addHit( id, hit );
        }

        results.setTotalHits( results.getHitsMap().size() );
        results.setLimits( limits );

        if ( limits == null || limits.getSelectedPage() == SearchResultLimits.ALL_PAGES )
        {
            return results;
        }
        else
        {
            return paginate( results );
        }
    }

    private SearchResults paginate( SearchResults results )
    {
        SearchResultLimits limits = results.getLimits();
        SearchResults paginated = new SearchResults();

        int fetchCount = limits.getPageSize();
        int offset = ( limits.getSelectedPage() * limits.getPageSize() );

        if ( fetchCount > results.getTotalHits() )
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
                if ( offset + i >= results.getHits().size() )
                {
                    break;
                }

                SearchResultHit hit = results.getHits().get( ( offset + i ) );
                if ( hit != null )
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
        paginated.setTotalHits( results.getTotalHits() );
        paginated.setLimits( limits );

        return paginated;
    }
}
