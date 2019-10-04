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

import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MavenRepositorySearchOSGITest
    extends AbstractMavenRepositorySearch
{

    @Inject
    ArchivaRepositoryRegistry repositoryRegistry;

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        repositoryRegistry.destroy();
    }

    @Test
    public void searchFelixWithSymbolicName()
        throws Exception
    {

        createIndex( TEST_REPO_1, Collections.<Path>emptyList(), true );

        List<String> selectedRepos = Arrays.asList( TEST_REPO_1 );

        // search artifactId
        // EasyMock.expect( archivaConfig.getDefaultLocale() ).andReturn( Locale.getDefault( ) ).anyTimes();
        EasyMock.expect( archivaConfig.getConfiguration()).andReturn(config).anyTimes();

        archivaConfigControl.replay();

        SearchFields searchFields = new SearchFields();
        searchFields.setBundleSymbolicName( "org.apache.felix.bundlerepository" );
        searchFields.setBundleVersion( "1.6.6" );
        searchFields.setRepositories( selectedRepos );

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.felix", hit.getGroupId() );
        assertEquals( "org.apache.felix.bundlerepository", hit.getArtifactId() );
        assertEquals( "1.6.6", hit.getVersions().get( 0 ) );

        assertEquals( "org.apache.felix.bundlerepository;uses:=\"org.osgi.framework\";version=\"2.0\"",
                      hit.getBundleExportPackage() );
        assertEquals( "org.apache.felix.bundlerepository.RepositoryAdmin,org.osgi.service.obr.RepositoryAdmin",
                      hit.getBundleExportService() );
        assertEquals( "org.apache.felix.bundlerepository", hit.getBundleSymbolicName() );
        assertEquals( "1.6.6", hit.getBundleVersion() );
    }

}
