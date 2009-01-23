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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.sonatype.nexus.index.NexusIndexer;

public class NexusRepositorySearchTest
    extends PlexusInSpringTestCase
{
    private RepositorySearch search;
    
    private ArchivaConfiguration archivaConfig;
    
    private NexusIndexer indexer;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        indexer = ( NexusIndexer )lookup( NexusIndexer.class );
        
        search = new NexusRepositorySearch( indexer, archivaConfig );
    }
    
    public void testQuickSearch()
        throws Exception
    {
    
    }
    
    public void testNoIndexFound()
        throws Exception
    {
    
    }
    
    public void testSearchWithinSearchResults()
        throws Exception
    {
    
    }
    
    public void testAdvancedSearch()
        throws Exception
    {
    
    }
    
    public void testPagination()
        throws Exception
    {
    
    }
    
}
