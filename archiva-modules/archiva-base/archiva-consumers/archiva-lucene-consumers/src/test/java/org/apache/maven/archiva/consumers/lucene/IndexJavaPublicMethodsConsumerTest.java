package org.apache.maven.archiva.consumers.lucene;

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
import java.util.List;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.updater.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * 
 * @version
 *
 */
public class IndexJavaPublicMethodsConsumerTest
    extends PlexusInSpringTestCase
{
    DatabaseUnprocessedArtifactConsumer indexMethodsConsumer;
    
    IndexJavaPublicMethodsCrossRepositorySearch searcher;
    
    private RepositoryContentIndexFactory indexFactory;
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        indexMethodsConsumer =
            (DatabaseUnprocessedArtifactConsumer) lookup( DatabaseUnprocessedArtifactConsumer.class,
                                                          "index-public-methods" );
                
        ManagedRepositoryConfiguration config = new ManagedRepositoryConfiguration();
        config.setId( "test-repo" );
        config.setLayout( "default" );
        config.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        config.setName( "Test Repository" );
        
        addRepoToConfiguration( "index-public-methods", config );
        
        indexFactory = (RepositoryContentIndexFactory) lookup (RepositoryContentIndexFactory.class, "lucene" );
        searcher = new IndexJavaPublicMethodsCrossRepositorySearch( config, indexFactory );
    }
    
    private void addRepoToConfiguration( String configHint, ManagedRepositoryConfiguration repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class, configHint );
        Configuration configuration = archivaConfiguration.getConfiguration();
        configuration.removeManagedRepository( configuration.findManagedRepositoryById( repoConfiguration.getId() ) );
        configuration.addManagedRepository( repoConfiguration );
    }
    
    public void testJarPublicMethods()
        throws Exception
    {
        ArchivaArtifact artifact =
            createArtifact( "org.apache.archiva", "archiva-index-methods-jar-test", "1.0", "jar" ); 
        indexMethodsConsumer.processArchivaArtifact( artifact );      
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "test-repo" );
        
        // search for class names
        SearchResults results = searcher.searchForBytecode( "", selectedRepos, "FirstPackageApp", new SearchResultLimits( 0 ) );
        assertEquals( 1, results.getTotalHits() );
        
        results = searcher.searchForBytecode( "", selectedRepos, "SecondPackageApp", new SearchResultLimits( 0 ) );
        assertEquals( 1, results.getTotalHits() );
       
        // search for public methods
        results = searcher.searchForBytecode( "", selectedRepos, "appMethodOne", new SearchResultLimits( 0 ) );
        assertEquals( 1, results.getTotalHits() );
        
        // should return only the overridding public method in SecondPackageApp
        results = searcher.searchForBytecode( "", selectedRepos, "protectedMethod", new SearchResultLimits( 0 ) );
        assertEquals( 1, results.getTotalHits() );
               
        // should not return any private methods
        results = searcher.searchForBytecode( "", selectedRepos, "privMethod", new SearchResultLimits( 0 ) );
        assertEquals( 0, results.getTotalHits() );
        
        // test for public variables?
    }
    
    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        ArchivaArtifactModel model = new ArchivaArtifactModel();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setType( type );
        model.setRepositoryId( "test-repo" );

        return new ArchivaArtifact( model );
    }
}
