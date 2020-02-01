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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.indexer.search.ArtifactInfoFilter;
import org.apache.archiva.indexer.search.NoClassifierArtifactInfoFilter;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.archiva.model.ArchivaArtifactModel;
import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.proxy.model.ProxyConnector;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.OSGI;
import org.apache.maven.index.QueryCreator;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.expr.SearchTyped;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.expr.UserInputSearchExpression;
import org.apache.maven.index_shaded.lucene.search.BooleanClause;
import org.apache.maven.index_shaded.lucene.search.BooleanClause.Occur;
import org.apache.maven.index_shaded.lucene.search.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RepositorySearch implementation which uses the Maven Indexer for searching.
 */
@Service( "repositorySearch#maven" )
public class MavenRepositorySearch
    implements RepositorySearch
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private Indexer indexer;

    private QueryCreator queryCreator;


    private RepositoryRegistry repositoryRegistry;

    private ProxyRegistry proxyRegistry;

    protected MavenRepositorySearch()
    {
        // for test purpose
    }

    @Inject
    public MavenRepositorySearch( Indexer nexusIndexer, RepositoryRegistry repositoryRegistry,
                                  ProxyRegistry proxyRegistry, QueryCreator queryCreator )
    {
        this.indexer = nexusIndexer;
        this.queryCreator = queryCreator;
        this.repositoryRegistry = repositoryRegistry;
        this.proxyRegistry = proxyRegistry;
    }

    /**
     * @see RepositorySearch#search(String, List, String, SearchResultLimits, List)
     */
    @Override
    public SearchResults search(String principal, List<String> selectedRepos, String term, SearchResultLimits limits,
                                List<String> previousSearchTerms )
        throws RepositorySearchException
    {
        List<String> indexingContextIds = addIndexingContexts( selectedRepos );

        // since upgrade to nexus 2.0.0, query has changed from g:[QUERIED TERM]* to g:*[QUERIED TERM]*
        //      resulting to more wildcard searches so we need to increase max clause count
        BooleanQuery.setMaxClauseCount( Integer.MAX_VALUE );
        BooleanQuery.Builder qb = new BooleanQuery.Builder();

        if ( previousSearchTerms == null || previousSearchTerms.isEmpty() )
        {
            constructQuery( term, qb );
        }
        else
        {
            for ( String previousTerm : previousSearchTerms )
            {
                BooleanQuery.Builder iQuery = new BooleanQuery.Builder();
                constructQuery( previousTerm, iQuery );

                qb.add( iQuery.build(), BooleanClause.Occur.MUST );
            }

            BooleanQuery.Builder iQuery = new BooleanQuery.Builder();
            constructQuery( term, iQuery );
            qb.add( iQuery.build(), BooleanClause.Occur.MUST );
        }

        // we retun only artifacts without classifier in quick search, olamy cannot find a way to say with this field empty
        // FIXME  cannot find a way currently to setup this in constructQuery !!!
        return search( limits, qb.build(), indexingContextIds, NoClassifierArtifactInfoFilter.LIST, selectedRepos, true );

    }

    /**
     * @see RepositorySearch#search(String, SearchFields, SearchResultLimits)
     */
    @SuppressWarnings( "deprecation" )
    @Override
    public SearchResults search( String principal, SearchFields searchFields, SearchResultLimits limits )
        throws RepositorySearchException
    {
        if ( searchFields.getRepositories() == null )
        {
            throw new RepositorySearchException( "Repositories cannot be null." );
        }

        List<String> indexingContextIds = addIndexingContexts( searchFields.getRepositories() );

        // if no index found in the specified ones return an empty search result instead of doing a search on all index
        // olamy: IMHO doesn't make sense
        if ( !searchFields.getRepositories().isEmpty() && ( indexingContextIds == null
            || indexingContextIds.isEmpty() ) )
        {
            return new SearchResults();
        }

        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        if ( StringUtils.isNotBlank( searchFields.getGroupId() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.GROUP_ID, searchFields.isExactSearch() ? new SourcedSearchExpression(
                       searchFields.getGroupId() ) : new UserInputSearchExpression( searchFields.getGroupId() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getArtifactId() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.ARTIFACT_ID,
                                           searchFields.isExactSearch()
                                               ? new SourcedSearchExpression( searchFields.getArtifactId() )
                                               : new UserInputSearchExpression( searchFields.getArtifactId() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getVersion() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.VERSION, searchFields.isExactSearch() ? new SourcedSearchExpression(
                       searchFields.getVersion() ) : new SourcedSearchExpression( searchFields.getVersion() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getPackaging() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.PACKAGING, searchFields.isExactSearch() ? new SourcedSearchExpression(
                       searchFields.getPackaging() ) : new UserInputSearchExpression( searchFields.getPackaging() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getClassName() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.CLASSNAMES,
                                           new UserInputSearchExpression( searchFields.getClassName() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleSymbolicName() ) )
        {
            qb.add( indexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                           new UserInputSearchExpression( searchFields.getBundleSymbolicName() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleVersion() ) )
        {
            qb.add( indexer.constructQuery( OSGI.VERSION,
                                           new UserInputSearchExpression( searchFields.getBundleVersion() ) ),
                   BooleanClause.Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleExportPackage() ) )
        {
            qb.add( indexer.constructQuery( OSGI.EXPORT_PACKAGE,
                                           new UserInputSearchExpression( searchFields.getBundleExportPackage() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleExportService() ) )
        {
            qb.add( indexer.constructQuery( OSGI.EXPORT_SERVICE,
                                           new UserInputSearchExpression( searchFields.getBundleExportService() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleImportPackage() ) )
        {
            qb.add( indexer.constructQuery( OSGI.IMPORT_PACKAGE,
                                           new UserInputSearchExpression( searchFields.getBundleImportPackage() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleName() ) )
        {
            qb.add( indexer.constructQuery( OSGI.NAME, new UserInputSearchExpression( searchFields.getBundleName() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleImportPackage() ) )
        {
            qb.add( indexer.constructQuery( OSGI.IMPORT_PACKAGE,
                                           new UserInputSearchExpression( searchFields.getBundleImportPackage() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getBundleRequireBundle() ) )
        {
            qb.add( indexer.constructQuery( OSGI.REQUIRE_BUNDLE,
                                           new UserInputSearchExpression( searchFields.getBundleRequireBundle() ) ),
                   Occur.MUST );
        }

        if ( StringUtils.isNotBlank( searchFields.getClassifier() ) )
        {
            qb.add( indexer.constructQuery( MAVEN.CLASSIFIER, searchFields.isExactSearch() ? new SourcedSearchExpression(
                       searchFields.getClassifier() ) : new UserInputSearchExpression( searchFields.getClassifier() ) ),
                   Occur.MUST );
        }
        else if ( searchFields.isExactSearch() )
        {
            //TODO improvement in case of exact search and no classifier we must query for classifier with null value
            // currently it's done in DefaultSearchService with some filtering
        }

        BooleanQuery qu = qb.build();
        if ( qu.clauses() == null || qu.clauses().size() <= 0 )
        {
            throw new RepositorySearchException( "No search fields set." );
        }
        if (qu.clauses()!=null) {
            log.debug("CLAUSES ", qu.clauses());
            for (BooleanClause cl : qu.clauses()) {
                log.debug("Clause ",cl);
            }
        }

        return search( limits, qu, indexingContextIds, Collections.<ArtifactInfoFilter>emptyList(),
                       searchFields.getRepositories(), searchFields.isIncludePomArtifacts() );
    }

    private static class NullSearch
        implements SearchTyped, SearchExpression
    {
        private static final NullSearch INSTANCE = new NullSearch();

        @Override
        public String getStringValue()
        {
            return "[[NULL_VALUE]]";
        }

        @Override
        public SearchType getSearchType()
        {
            return SearchType.EXACT;
        }
    }

    private SearchResults search( SearchResultLimits limits, BooleanQuery q, List<String> indexingContextIds,
                                  List<? extends ArtifactInfoFilter> filters, List<String> selectedRepos,
                                  boolean includePoms )
        throws RepositorySearchException
    {

        try
        {
            FlatSearchRequest request = new FlatSearchRequest( q );

            request.setContexts( getIndexingContexts( indexingContextIds ) );
            if ( limits != null )
            {
                // we apply limits only when first page asked
                if ( limits.getSelectedPage() == 0 )
                {
                    request.setCount( limits.getPageSize() * ( Math.max( 1, limits.getSelectedPage() ) ) );
                }
            }

            FlatSearchResponse response = indexer.searchFlat( request );

            if ( response == null || response.getTotalHitsCount() == 0 )
            {
                SearchResults results = new SearchResults();
                results.setLimits( limits );
                return results;
            }

            return convertToSearchResults( response, limits, filters, selectedRepos, includePoms );
        }
        catch ( IOException e )
        {
            throw new RepositorySearchException( e.getMessage(), e );
        }

    }

    private IndexingContext getIndexingContext(String id) {
        String repoId;
        if (StringUtils.startsWith(id, "remote-")) {
            repoId = StringUtils.substringAfter(id, "remote-");
        } else {
            repoId = id;
        }
        Repository repo = repositoryRegistry.getRepository(repoId);
        if (repo==null) {
            return null;
        } else {
            if (repo.getIndexingContext()!=null) {
                try {
                    return repo.getIndexingContext().getBaseContext(IndexingContext.class);
                } catch (UnsupportedBaseContextException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private List<IndexingContext> getIndexingContexts( List<String> ids )
    {
        List<IndexingContext> contexts = new ArrayList<>( ids.size() );

        for ( String id : ids )
        {
            IndexingContext context = getIndexingContext(id);
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

    private void constructQuery( String term, BooleanQuery.Builder q )
    {
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new UserInputSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.ARTIFACT_ID, new UserInputSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.VERSION, new UserInputSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.PACKAGING, new UserInputSearchExpression( term ) ), Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.CLASSNAMES, new UserInputSearchExpression( term ) ), Occur.SHOULD );

        //Query query =
        //    new WildcardQuery( new Term( MAVEN.CLASSNAMES.getFieldName(), "*" ) );
        //q.add( query, Occur.MUST_NOT );
        // olamy IMHO we could set this option as at least one must match
        //q.setMinimumNumberShouldMatch( 1 );
    }


    /**
     * @param selectedRepos
     * @return indexing contextId used
     */
    private List<String> addIndexingContexts( List<String> selectedRepos )
    {
        Set<String> indexingContextIds = new HashSet<>();
        for ( String repo : selectedRepos )
        {
            try
            {
                Repository rRepo = repositoryRegistry.getRepository(repo);

                if ( rRepo != null )
                {

                    if (rRepo.getType().equals(RepositoryType.MAVEN)) {
                        assert rRepo.getIndexingContext() != null;
                        IndexingContext context = rRepo.getIndexingContext().getBaseContext(IndexingContext.class);
                        if (context.isSearchable()) {
                            indexingContextIds.addAll(getRemoteIndexingContextIds(repo));
                            indexingContextIds.add(context.getId());
                        } else {
                            log.warn("indexingContext with id {} not searchable", rRepo.getId());
                        }
                    }

                }
                else
                {
                    log.warn( "Repository '{}' not found in configuration.", repo );
                }
            }
            catch ( RepositorySearchException e )
            {
                log.warn( "RepositorySearchException occured while accessing index of repository '{}' : {}", repo,
                    e.getMessage() );
                continue;
            } catch (UnsupportedBaseContextException e) {
                log.error("Fatal situation: Maven repository without IndexingContext found.");
                continue;
            }
        }

        return new ArrayList<>( indexingContextIds );
    }


    @Override
    public Set<String> getRemoteIndexingContextIds( String managedRepoId )
        throws RepositorySearchException
    {
        Set<String> ids = new HashSet<>();

        List<ProxyConnector> proxyConnectors = null;
        proxyConnectors = proxyRegistry.getProxyConnectorAsMap( ).get( managedRepoId );

        if ( proxyConnectors == null || proxyConnectors.isEmpty() )
        {
            return ids;
        }

        for ( ProxyConnector proxyConnector : proxyConnectors )
        {
            String remoteId = "remote-" + proxyConnector.getTargetRepository().getId();
            RemoteRepository repo = repositoryRegistry.getRemoteRepository(proxyConnector.getTargetRepository().getId());
            if (repo.getType()==RepositoryType.MAVEN) {
                try {
                    IndexingContext context = repo.getIndexingContext() != null ? repo.getIndexingContext().getBaseContext(IndexingContext.class) : null;
                    if (context!=null && context.isSearchable()) {
                        ids.add(remoteId);
                    }
                } catch (UnsupportedBaseContextException e) {
                    // Ignore this one
                }
            }
        }

        return ids;
    }

    @Override
    public Collection<String> getAllGroupIds( String principal, List<String> selectedRepos )
        throws RepositorySearchException
    {
        List<IndexingContext> indexContexts = getIndexingContexts( selectedRepos );

        if ( indexContexts == null || indexContexts.isEmpty() )
        {
            return Collections.emptyList();
        }

        try
        {
            Set<String> allGroupIds = new HashSet<>();
            for ( IndexingContext indexingContext : indexContexts )
            {
                allGroupIds.addAll( indexingContext.getAllGroups() );
            }
            return allGroupIds;
        }
        catch ( IOException e )
        {
            throw new RepositorySearchException( e.getMessage(), e );
        }

    }

    private SearchResults convertToSearchResults( FlatSearchResponse response, SearchResultLimits limits,
                                                  List<? extends ArtifactInfoFilter> artifactInfoFilters,
                                                  List<String> selectedRepos, boolean includePoms )
    {
        SearchResults results = new SearchResults();
        Set<ArtifactInfo> artifactInfos = response.getResults();

        for ( ArtifactInfo artifactInfo : artifactInfos )
        {
            if ( StringUtils.equalsIgnoreCase( "pom", artifactInfo.getFileExtension() ) && !includePoms )
            {
                continue;
            }
            String id = SearchUtil.getHitId( artifactInfo.getGroupId(), //
                                             artifactInfo.getArtifactId(), //
                                             artifactInfo.getClassifier(), //
                                             artifactInfo.getPackaging() );
            Map<String, SearchResultHit> hitsMap = results.getHitsMap();


            if ( !applyArtifactInfoFilters( artifactInfo, artifactInfoFilters, hitsMap ) )
            {
                continue;
            }

            SearchResultHit hit = hitsMap.get( id );
            if ( hit != null )
            {
                if ( !hit.getVersions().contains( artifactInfo.getVersion() ) )
                {
                    hit.addVersion( artifactInfo.getVersion() );
                }
            }
            else
            {
                hit = new SearchResultHit();
                hit.setArtifactId( artifactInfo.getArtifactId() );
                hit.setGroupId( artifactInfo.getGroupId() );
                hit.setRepositoryId( artifactInfo.getRepository() );
                hit.addVersion( artifactInfo.getVersion() );
                hit.setBundleExportPackage( artifactInfo.getBundleExportPackage() );
                hit.setBundleExportService( artifactInfo.getBundleExportService() );
                hit.setBundleSymbolicName( artifactInfo.getBundleSymbolicName() );
                hit.setBundleVersion( artifactInfo.getBundleVersion() );
                hit.setBundleDescription( artifactInfo.getBundleDescription() );
                hit.setBundleDocUrl( artifactInfo.getBundleDocUrl() );
                hit.setBundleRequireBundle( artifactInfo.getBundleRequireBundle() );
                hit.setBundleImportPackage( artifactInfo.getBundleImportPackage() );
                hit.setBundleLicense( artifactInfo.getBundleLicense() );
                hit.setBundleName( artifactInfo.getBundleName() );
                hit.setContext( artifactInfo.getContext() );
                hit.setGoals( artifactInfo.getGoals() );
                hit.setPrefix( artifactInfo.getPrefix() );
                hit.setPackaging( artifactInfo.getPackaging() );
                hit.setClassifier( artifactInfo.getClassifier() );
                hit.setFileExtension( artifactInfo.getFileExtension() );
                hit.setUrl( getBaseUrl( artifactInfo, selectedRepos ) );
            }

            results.addHit( id, hit );
        }

        results.setTotalHits( response.getTotalHitsCount() );
        results.setTotalHitsMapSize( results.getHitsMap().values().size() );
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

    /**
     * calculate baseUrl without the context and base Archiva Url
     *
     * @param artifactInfo
     * @return
     */
    protected String getBaseUrl( ArtifactInfo artifactInfo, List<String> selectedRepos )
    {
        StringBuilder sb = new StringBuilder();
        if ( StringUtils.startsWith( artifactInfo.getContext(), "remote-" ) )
        {
            // it's a remote index result we search a managed which proxying this remote and on which
            // current user has read karma
            String managedRepoId =
                getManagedRepoId( StringUtils.substringAfter( artifactInfo.getContext(), "remote-" ), selectedRepos );
            if ( managedRepoId != null )
            {
                sb.append( '/' ).append( managedRepoId );
                artifactInfo.setContext( managedRepoId );
            }
        }
        else
        {
            sb.append( '/' ).append( artifactInfo.getContext() );
        }

        sb.append( '/' ).append( StringUtils.replaceChars( artifactInfo.getGroupId(), '.', '/' ) );
        sb.append( '/' ).append( artifactInfo.getArtifactId() );
        sb.append( '/' ).append( artifactInfo.getVersion() );
        sb.append( '/' ).append( artifactInfo.getArtifactId() );
        sb.append( '-' ).append( artifactInfo.getVersion() );
        if ( StringUtils.isNotBlank( artifactInfo.getClassifier() ) )
        {
            sb.append( '-' ).append( artifactInfo.getClassifier() );
        }
        // maven-plugin packaging is a jar
        if ( StringUtils.equals( "maven-plugin", artifactInfo.getPackaging() ) )
        {
            sb.append( "jar" );
        }
        else
        {
            sb.append( '.' ).append( artifactInfo.getPackaging() );
        }

        return sb.toString();
    }

    /**
     * return a managed repo for a remote result
     *
     * @param remoteRepo
     * @param selectedRepos
     * @return
     */
    private String getManagedRepoId( String remoteRepo, List<String> selectedRepos )
    {
        Map<String, List<ProxyConnector>> proxyConnectorMap = proxyRegistry.getProxyConnectorAsMap();
        if ( proxyConnectorMap == null || proxyConnectorMap.isEmpty() )
        {
            return null;
        }
        if ( selectedRepos != null && !selectedRepos.isEmpty() )
        {
            for ( Map.Entry<String, List<ProxyConnector>> entry : proxyConnectorMap.entrySet() )
            {
                if ( selectedRepos.contains( entry.getKey() ) )
                {
                    for ( ProxyConnector proxyConnector : entry.getValue() )
                    {
                        if ( StringUtils.equals( remoteRepo, proxyConnector.getTargetRepository().getId() ) )
                        {
                            return proxyConnector.getSourceRepository().getId();
                        }
                    }
                }
            }
        }

        // we don't find in search selected repos so return the first one
        for ( Map.Entry<String, List<ProxyConnector>> entry : proxyConnectorMap.entrySet() )
        {

            for ( ProxyConnector proxyConnector : entry.getValue() )
            {
                if ( StringUtils.equals( remoteRepo, proxyConnector.getTargetRepository().getId() ) )
                {
                    return proxyConnector.getSourceRepository().getId();
                }
            }

        }
        return null;
    }

    private boolean applyArtifactInfoFilters( ArtifactInfo artifactInfo,
                                              List<? extends ArtifactInfoFilter> artifactInfoFilters,
                                              Map<String, SearchResultHit> currentResult )
    {
        if ( artifactInfoFilters == null || artifactInfoFilters.isEmpty() )
        {
            return true;
        }

        ArchivaArtifactModel artifact = new ArchivaArtifactModel();
        artifact.setArtifactId( artifactInfo.getArtifactId() );
        artifact.setClassifier( artifactInfo.getClassifier() );
        artifact.setGroupId( artifactInfo.getGroupId() );
        artifact.setRepositoryId( artifactInfo.getRepository() );
        artifact.setVersion( artifactInfo.getVersion() );
        artifact.setChecksumMD5( artifactInfo.getMd5() );
        artifact.setChecksumSHA1( artifactInfo.getSha1() );
        for ( ArtifactInfoFilter filter : artifactInfoFilters )
        {
            if ( !filter.addArtifactInResult( artifact, currentResult ) )
            {
                return false;
            }
        }
        return true;
    }

    protected SearchResults paginate( SearchResults results )
    {
        SearchResultLimits limits = results.getLimits();
        SearchResults paginated = new SearchResults();

        // ( limits.getPageSize() * ( Math.max( 1, limits.getSelectedPage() ) ) );

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
                    String id = SearchUtil.getHitId( hit.getGroupId(), hit.getArtifactId(), hit.getClassifier(),
                                                     hit.getPackaging() );
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
        paginated.setTotalHitsMapSize( results.getTotalHitsMapSize() );
        paginated.setLimits( limits );

        return paginated;
    }


}
