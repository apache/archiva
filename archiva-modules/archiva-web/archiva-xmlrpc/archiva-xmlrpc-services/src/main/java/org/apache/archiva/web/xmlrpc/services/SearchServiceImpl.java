package org.apache.archiva.web.xmlrpc.services;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.web.xmlrpc.api.SearchService;
import org.apache.archiva.web.xmlrpc.api.beans.Artifact;
import org.apache.archiva.web.xmlrpc.api.beans.Dependency;
import org.apache.archiva.web.xmlrpc.security.XmlRpcUserRepositories;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchServiceImpl
    implements SearchService
{
    private Logger log = LoggerFactory.getLogger( SearchServiceImpl.class );

    private RepositorySearch search;

    private XmlRpcUserRepositories xmlRpcUserRepositories;

    private ArchivaDAO archivaDAO;

    private MetadataResolver metadataResolver;

    public SearchServiceImpl( XmlRpcUserRepositories xmlRpcUserRepositories, ArchivaDAO archivaDAO,
                              MetadataResolver metadataResolver, RepositorySearch search )
    {
        this.xmlRpcUserRepositories = xmlRpcUserRepositories;
        this.archivaDAO = archivaDAO;
        this.search = search;
        this.metadataResolver = metadataResolver;
    }

    @SuppressWarnings("unchecked")
    public List<Artifact> quickSearch( String queryString )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();
        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );
        SearchResults results = null;

        results = search.search( "", observableRepos, queryString, limits, null );

        for ( SearchResultHit resultHit : results.getHits() )
        {
            // double-check all versions as done in SearchAction
            final List<String> versions = (List<String>) archivaDAO.query(
                new UniqueVersionConstraint( observableRepos, resultHit.getGroupId(), resultHit.getArtifactId() ) );
            if ( versions != null && !versions.isEmpty() )
            {
                resultHit.setVersion( null );
                resultHit.setVersions( filterTimestampedSnapshots( versions ) );
            }

            List<String> resultHitVersions = resultHit.getVersions();
            if ( resultHitVersions != null )
            {
                for ( String version : resultHitVersions )
                {
                    Artifact artifact = null;
                    for ( String repoId : observableRepos )
                    {
                        // slight behaviour change to previous implementation: instead of allocating "jar" when not
                        // found in the database, we can rely on the metadata repository to create it on the fly. We
                        // just allocate the default packaging if the Maven facet is not found.
                        ProjectVersionMetadata model =
                            metadataResolver.getProjectVersion( repoId, resultHit.getGroupId(),
                                                                resultHit.getArtifactId(), version );

                        if ( model != null )
                        {
                            String packaging = "jar";

                            MavenProjectFacet facet = (MavenProjectFacet) model.getFacet( MavenProjectFacet.FACET_ID );
                            if ( facet != null && facet.getPackaging() != null )
                            {
                                packaging = facet.getPackaging();
                            }
                            artifact = new Artifact( repoId, resultHit.getGroupId(), resultHit.getArtifactId(), version,
                                                     packaging );
                            break;
                        }
                    }

                    if ( artifact != null )
                    {
                        artifacts.add( artifact );
                    }
                }
            }
        }

        return artifacts;
    }

    /**
     * Remove timestamped snapshots from versions
     */
    private static List<String> filterTimestampedSnapshots( List<String> versions )
    {
        final List<String> filtered = new ArrayList<String>();
        for ( final String version : versions )
        {
            final String baseVersion = VersionUtil.getBaseVersion( version );
            if ( !filtered.contains( baseVersion ) )
            {
                filtered.add( baseVersion );
            }
        }
        return filtered;
    }

    public List<Artifact> getArtifactByChecksum( String checksum )
        throws Exception
    {
        // 1. get ArtifactDAO from ArchivaDAO
        // 2. create ArtifactsByChecksumConstraint( "queryTerm" )
        // 3. query artifacts using constraint
        // 4. convert results to list of Artifact objects

        List<Artifact> results = new ArrayList<Artifact>();
        ArtifactDAO artifactDAO = archivaDAO.getArtifactDAO();

        ArtifactsByChecksumConstraint constraint = new ArtifactsByChecksumConstraint( checksum );
        List<ArchivaArtifact> artifacts = artifactDAO.queryArtifacts( constraint );

        for ( ArchivaArtifact archivaArtifact : artifacts )
        {
            Artifact artifact =
                new Artifact( archivaArtifact.getModel().getRepositoryId(), archivaArtifact.getModel().getGroupId(),
                              archivaArtifact.getModel().getArtifactId(), archivaArtifact.getModel().getVersion(),
                              archivaArtifact.getType() );
            //archivaArtifact.getModel().getWhenGathered() );
            results.add( artifact );
        }

        return results;
    }

    public List<Artifact> getArtifactVersions( String groupId, String artifactId )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();

        for ( String repoId : observableRepos )
        {
            Collection<String> results = metadataResolver.getProjectVersions( repoId, groupId, artifactId );

            for ( final String version : results )
            {
                final Artifact artifact = new Artifact( repoId, groupId, artifactId, version, "pom" );

                artifacts.add( artifact );
            }
        }

        return artifacts;
    }

    public List<Artifact> getArtifactVersionsByDate( String groupId, String artifactId, String version, Date since )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();

        // 1. get observable repositories
        // 2. use RepositoryBrowsing method to query uniqueVersions? (but with date)
        
        throw new UnsupportedOperationException( "getArtifactVersionsByDate not yet implemented" );

//        return artifacts;
    }

    public List<Dependency> getDependencies( String groupId, String artifactId, String version )
        throws Exception
    {
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();

        for ( String repoId : observableRepos )
        {
            ProjectVersionMetadata model = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
            if ( model != null )
            {
                List<Dependency> dependencies = new ArrayList<Dependency>();
                List<org.apache.archiva.metadata.model.Dependency> modelDeps = model.getDependencies();
                for ( org.apache.archiva.metadata.model.Dependency dep : modelDeps )
                {
                    Dependency dependency =
                        new Dependency( dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier(),
                                        dep.getType(), dep.getScope() );
                    dependencies.add( dependency );
                }
                return dependencies;
            }
        }
        throw new Exception( "Artifact does not exist." );
    }

    public List<Artifact> getDependencyTree( String groupId, String artifactId, String version )
        throws Exception
    {
        List<Artifact> a = new ArrayList<Artifact>();
        
        throw new UnsupportedOperationException( "getDependencyTree not yet implemented" );
//        return a;
    }

    public List<Artifact> getDependees( String groupId, String artifactId, String version )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();

        for ( String repoId : observableRepos )
        {
            Collection<ProjectVersionReference> refs =
                metadataResolver.getProjectReferences( repoId, groupId, artifactId, version );
            for ( ProjectVersionReference ref : refs )
            {
                artifacts.add(
                    new Artifact( repoId, ref.getNamespace(), ref.getProjectId(), ref.getProjectVersion(), "" ) );
            }
        }

        return artifacts;
    }
}
