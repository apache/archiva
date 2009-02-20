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
import java.util.Date;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.web.xmlrpc.api.SearchService;
import org.apache.archiva.web.xmlrpc.api.beans.Artifact;
import org.apache.archiva.web.xmlrpc.api.beans.Dependency;
import org.apache.archiva.web.xmlrpc.security.XmlRpcUserRepositories;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.browsing.BrowsingResults;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;

/**
 * SearchServiceImpl
 * 
 * @version $Id: SearchServiceImpl.java
 */
public class SearchServiceImpl
    implements SearchService
{ 
    private RepositorySearch search;
    
    private XmlRpcUserRepositories xmlRpcUserRepositories;
    
    private ArchivaDAO archivaDAO;
    
    private RepositoryBrowsing repoBrowsing;
    
    public SearchServiceImpl( XmlRpcUserRepositories xmlRpcUserRepositories, ArchivaDAO archivaDAO,
                              RepositoryBrowsing repoBrowsing, RepositorySearch search )
    {
        this.xmlRpcUserRepositories = xmlRpcUserRepositories;
        this.archivaDAO = archivaDAO;        
        this.repoBrowsing = repoBrowsing;
        this.search = search;
    }
    
    /*
     * quick/general text search which returns a list of artifacts
     * query for an artifact based on a checksum
     * query for all available versions of an artifact, sorted in version significance order
     * query for all available versions of an artifact since a given date
     * query for an artifact's direct dependencies
     * query for an artifact's dependency tree (as with mvn dependency:tree - no duplicates should be included)
     * query for all artifacts that depend on a given artifact
     */
    
    public List<Artifact> quickSearch( String queryString )
        throws Exception
    {   
        // 1. check whether bytecode search or ordinary search
        // 2. get observable repos
        // 3. convert results to a list of Artifact objects
        
        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();
        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );
        SearchResults results = null;
        
        results = search.search( "", observableRepos, queryString, limits, null );
                
        List<SearchResultHit> hits = results.getHits();
        for( SearchResultHit hit : hits )
        {   
            ArtifactDAO artifactDAO = archivaDAO.getArtifactDAO(); 
            try
            {
                ArchivaArtifact pomArtifact = artifactDAO.getArtifact( 
                           hit.getGroupId(), hit.getArtifactId(), hit.getVersion(), "", "pom", hit.getRepositoryId() );
                
                if( pomArtifact != null )
                {
                    Artifact artifact = new Artifact( pomArtifact.getModel().getRepositoryId(), pomArtifact.getGroupId(), pomArtifact.getArtifactId(), pomArtifact.getVersion(),
                                                      pomArtifact.getType() );
                                                      //pomArtifact.getType(), pomArtifact.getModel().getWhenGathered() );
                    artifacts.add( artifact );
                }
                else
                {
                    continue;
                }
            }
            catch ( ObjectNotFoundException e )
            {
                continue;
            }
            catch ( ArchivaDatabaseException e )
            {
                continue;
            }
        }
        
        return artifacts;
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
        
        for( ArchivaArtifact archivaArtifact : artifacts )
        {
            Artifact artifact = new Artifact( archivaArtifact.getModel().getRepositoryId(), archivaArtifact.getModel().getGroupId(),
                          archivaArtifact.getModel().getArtifactId(), archivaArtifact.getModel().getVersion(), archivaArtifact.getType() ); 
                          //archivaArtifact.getModel().getWhenGathered() );
            results.add( artifact );
        }
        
        return results;
    }
    
    public List<Artifact> getArtifactVersions( String groupId, String artifactId ) 
        throws Exception
    {
        final List<Artifact> artifacts = new ArrayList<Artifact>();        
        final List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();
        
        final BrowsingResults results = repoBrowsing.selectArtifactId( "", observableRepos, groupId, artifactId );
        final ArtifactDAO artifactDAO = archivaDAO.getArtifactDAO();
        
        for( final String version : results.getVersions() )
        {
            final Artifact artifact = new Artifact( "", groupId, artifactId, version, "pom" ); 
            //ArchivaArtifact pomArtifact = artifactDAO.getArtifact( groupId, artifactId, version, "", "pom",  );
            //Artifact artifact = new Artifact( "", groupId, artifactId, version, pomArtifact.getType() ); 
                          //pomArtifact.getModel().getWhenGathered() );
            
            artifacts.add( artifact );
        }
        
        // 1. get observable repositories
        // 2. use RepositoryBrowsing method to query uniqueVersions?
        return artifacts;
    }
    
    public List<Artifact> getArtifactVersionsByDate( String groupId, String artifactId, String version, Date since )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        
        // 1. get observable repositories
        // 2. use RepositoryBrowsing method to query uniqueVersions? (but with date)
        
        return artifacts;
    }
    
    public List<Dependency> getDependencies( String groupId, String artifactId, String version ) 
        throws Exception
    {  
        List<Dependency> dependencies = new ArrayList<Dependency>();        
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();
        
        try
        {
            ArchivaProjectModel model = repoBrowsing.selectVersion( "", observableRepos, groupId, artifactId, version );
            List<org.apache.maven.archiva.model.Dependency> modelDeps = model.getDependencies();
            for( org.apache.maven.archiva.model.Dependency dep : modelDeps )
            {
                Dependency dependency = new Dependency( 
                    dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier(), dep.getType(), dep.getScope() );
                dependencies.add( dependency );
            }
        }
        catch ( ObjectNotFoundException oe )
        {
            throw new Exception( "Artifact does not exist." );
        }
        
        return dependencies;
    }
    
    public List<Artifact> getDependencyTree( String groupId, String artifactId, String version ) 
        throws Exception
    {
        List<Artifact> a = new ArrayList<Artifact>();
        
        return a;
    }
    
  //get artifacts that depend on a given artifact
    public List<Artifact> getDependees( String groupId, String artifactId, String version )
        throws Exception
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<String> observableRepos = xmlRpcUserRepositories.getObservableRepositories();
        
        List<ArchivaProjectModel> dependees = repoBrowsing.getUsedBy( "", observableRepos, "org.apache.archiva", "archiva-test", "1.0" );
        for( ArchivaProjectModel model : dependees )
        {
            Artifact artifact =
                new Artifact( "", model.getGroupId(), model.getArtifactId(), model.getVersion(), "" );
                              //model.getWhenIndexed() );
            artifacts.add( artifact );
        }
        
        return artifacts;
    }
}
