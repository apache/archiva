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

import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.OSGI;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.expr.StringSearchExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private Logger log = LoggerFactory.getLogger( getClass() );

    private NexusIndexer indexer;

    private ArchivaConfiguration archivaConfig;

    private MavenIndexerUtils mavenIndexerUtils;

    @Inject
    public NexusRepositorySearch( PlexusSisuBridge plexusSisuBridge, ArchivaConfiguration archivaConfig,
                                  MavenIndexerUtils mavenIndexerUtils )
        throws PlexusSisuBridgeException
    {
        this.indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.archivaConfig = archivaConfig;
        this.mavenIndexerUtils = mavenIndexerUtils;

    }

    /**
     * @see RepositorySearch#search(String, List, String, SearchResultLimits, List)
     */
    public SearchResults search( String principal, List<String> selectedRepos, String term, SearchResultLimits limits,
                                 List<String> previousSearchTerms )
        throws RepositorySearchException
    {
        List<String> indexingContextIds = addIndexingContexts( selectedRepos );

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

        return search( limits, q, indexingContextIds );
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

        List<String> indexingContextIds = addIndexingContexts( searchFields.getRepositories() );

        BooleanQuery q = new BooleanQuery();
        if ( StringUtils.isNotBlank( searchFields.getGroupId() ) )
        {
            q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( searchFields.getGroupId() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getArtifactId() ) )
        {
            q.add(
                indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( searchFields.getArtifactId() ) ),
                Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getVersion() ) )
        {
            q.add( indexer.constructQuery( MAVEN.VERSION, new StringSearchExpression( searchFields.getVersion() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getPackaging() ) )
        {
            q.add( indexer.constructQuery( MAVEN.PACKAGING, new StringSearchExpression( searchFields.getPackaging() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getClassName() ) )
        {
            q.add(
                indexer.constructQuery( MAVEN.CLASSNAMES, new StringSearchExpression( searchFields.getClassName() ) ),
                Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleSymbolicName() ) )
        {
            q.add( indexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                           new StringSearchExpression( searchFields.getBundleSymbolicName() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleVersion() ) )
        {
            q.add(
                indexer.constructQuery( OSGI.VERSION, new StringSearchExpression( searchFields.getBundleVersion() ) ),
                Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleExportPackage() ) )
        {
            q.add( indexer.constructQuery( OSGI.EXPORT_PACKAGE,
                                           new StringSearchExpression( searchFields.getBundleExportPackage() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleExportService() ) )
        {
            q.add( indexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                           new StringSearchExpression( searchFields.getBundleExportService() ) ),
                   Occur.MUST );
        }

        if ( q.getClauses() == null || q.getClauses().length <= 0 )
        {
            throw new RepositorySearchException( "No search fields set." );
        }

        return search( limits, q, indexingContextIds );
    }

    private SearchResults search( SearchResultLimits limits, BooleanQuery q, List<String> indexingContextIds )
        throws RepositorySearchException
    {
        try
        {
            FlatSearchRequest request = new FlatSearchRequest( q );
            request.setContexts( getIndexingContexts( indexingContextIds ) );
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
        /*
        olamy : don't understand why this ?? it remove content from index ??
        comment until someone explain WTF ?? :-))
        finally
        {
            Map<String, IndexingContext> indexingContexts = indexer.getIndexingContexts();

            for ( Map.Entry<String, IndexingContext> entry : indexingContexts.entrySet() )
            {
                try
                {
                    indexer.removeIndexingContext( entry.getValue(), false );
                    log.debug( "Indexing context '{}' removed from search.", entry.getKey() );
                }
                catch ( IOException e )
                {
                    log.warn( "IOException occurred while removing indexing content '" + entry.getKey() + "'." );
                    continue;
                }
            }
        }*/
    }

    private List<IndexingContext> getIndexingContexts( List<String> ids )
    {
        List<IndexingContext> contexts = new ArrayList<IndexingContext>( ids.size() );

        for ( String id : ids )
        {
            IndexingContext context = indexer.getIndexingContexts().get( id );
            if ( context != null )
            {
                contexts.add( context );
            }
            else
            {
                log.warn( "context with id {} not exists", id );
            }
        }

        return contexts;
    }

    private void constructQuery( String term, BooleanQuery q )
    {
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.VERSION, new StringSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.PACKAGING, new StringSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.CLASSNAMES, new StringSearchExpression( term ) ), Occur.SHOULD );
        // olamy IMHO we could set this option as at least one must match
        //q.setMinimumNumberShouldMatch( 1 );
    }


    /**
     * @param selectedRepos
     * @return indexing contextId used
     */
    private List<String> addIndexingContexts( List<String> selectedRepos )
    {
        List<String> indexingContextIds = new ArrayList<String>();
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

                    IndexingContext context = indexer.getIndexingContexts().get( repoConfig.getId() );
                    if ( context != null )
                    {
                        // alreday here so no need to record it again
                        log.debug( "index with id {} already exists skip adding it", repoConfig.getId() );
                        // set searchable flag
                        context.setSearchable( repoConfig.isScanned() );
                        indexingContextIds.add( context.getId() );
                        continue;
                    }

                    context = indexer.addIndexingContext( repoConfig.getId(), repoConfig.getId(),
                                                          new File( repoConfig.getLocation() ), indexDirectory, null,
                                                          null, getAllIndexCreators() );
                    context.setSearchable( repoConfig.isScanned() );
                    if ( context.isSearchable() )
                    {
                        indexingContextIds.add( context.getId() );
                    }
                    else
                    {
                        log.warn( "indexingContext with id {} not searchable", repoConfig.getId() );
                    }

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
        return indexingContextIds;
    }


    protected List<? extends IndexCreator> getAllIndexCreators()
    {
        return mavenIndexerUtils.getAllIndexCreators();
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
                if ( !hit.getVersions().contains( artifactInfo.version ) )
                {
                    hit.addVersion( artifactInfo.version );
                }
            }
            else
            {
                hit = new SearchResultHit();
                hit.setArtifactId( artifactInfo.artifactId );
                hit.setGroupId( artifactInfo.groupId );
                hit.setRepositoryId( artifactInfo.repository );
                // FIXME archiva url ??
                hit.setUrl( artifactInfo.repository + "/" + artifactInfo.fname );
                hit.addVersion( artifactInfo.version );
                hit.setBundleExportPackage( artifactInfo.bundleExportPackage );
                hit.setBundleExportService( artifactInfo.bundleExportService );
                hit.setBundleSymbolicName( artifactInfo.bundleSymbolicName );
                hit.setBundleVersion( artifactInfo.bundleVersion );
                hit.setBundleDescription( artifactInfo.bundleDescription );
                hit.setBundleDocUrl( artifactInfo.bundleDocUrl );

                hit.setBundleRequireBundle( artifactInfo.bundleRequireBundle );
                hit.setBundleImportPackage( artifactInfo.bundleImportPackage );
                hit.setBundleLicense( artifactInfo.bundleLicense );
                hit.setBundleName( artifactInfo.bundleName );
                hit.setContext( artifactInfo.context );
                hit.setGoals( artifactInfo.goals );
                hit.setPrefix( artifactInfo.prefix );
                hit.setPackaging( artifactInfo.packaging );
                // sure ??
                hit.setUrl( artifactInfo.remoteUrl );
            }

            results.addHit( id, hit );
        }

        results.setTotalHits( response.getTotalHitsCount() );
        results.setReturnedHitsCount( response.getReturnedHitsCount() );
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
        paginated.setReturnedHitsCount( paginated.getHits().size() );
        paginated.setLimits( limits );

        return paginated;
    }
}
