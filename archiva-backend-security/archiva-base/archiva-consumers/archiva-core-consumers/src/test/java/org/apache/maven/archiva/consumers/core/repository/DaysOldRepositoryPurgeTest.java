package org.apache.maven.archiva.consumers.core.repository;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.archiva.consumers.core.repository.stubs.LuceneRepositoryContentIndexStub;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DaysOldRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{

    protected void setUp()
        throws Exception
    {
        super.setUp();

        Map<String, RepositoryContentIndex> map = new HashMap<String, RepositoryContentIndex>();
        map.put( "filecontent", new LuceneRepositoryContentIndexStub() );
        map.put( "hashcodes", new LuceneRepositoryContentIndexStub() );
        map.put( "bytecode", new LuceneRepositoryContentIndexStub() );
        
        repoPurge =
            new DaysOldRepositoryPurge( getRepository(), dao, getRepoConfiguration().getDaysOlder(), map );
    }

    private void setLastModified( String dirPath )
    {
        File dir = new File( dirPath );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 );
        }
    }

    public void testByLastModified()
        throws Exception
    {
        populateDbForTestByLastModified();

        String repoRoot = prepareTestRepo();

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-install-plugin";
        
        setLastModified( projectRoot + "/2.2-SNAPSHOT/" );

        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" );
    }

    public void testMetadataDrivenSnapshots()
        throws Exception
    {
        populateDbForTestMetadataDrivenSnapshots();

        String repoRoot = prepareTestRepo();

        repoPurge.process( PATH_TO_BY_DAYS_OLD_METADATA_DRIVEN_ARTIFACT );

        String versionRoot = repoRoot + "/org/codehaus/plexus/plexus-utils/1.4.3-SNAPSHOT";
        
        // this should be deleted since the filename version (timestamp) is older than
        // 100 days even if the last modified date was <100 days ago
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar.sha1" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom.sha1" );

        // musn't be deleted since the filename version (timestamp) is not older than 100 days
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070618.102615-5.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070618.102615-5.jar.sha1" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070618.102615-5.pom" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070618.102615-5.pom.sha1" );

        assertExists( versionRoot + "/plexus-utils-1.4.3-20070630.113158-6.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070630.113158-6.jar.sha1" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070630.113158-6.pom" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070630.113158-6.pom.sha1" );

        assertExists( versionRoot + "/plexus-utils-1.4.3-20070707.122114-7.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070707.122114-7.jar.sha1" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070707.122114-7.pom" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-20070707.122114-7.pom.sha1" );

        // mustn't be deleted since the last modified date is <100 days (this is not a timestamped version)
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.pom" );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        repoPurge = null;
    }

    private void populateDbForTestByLastModified()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "2.2-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-install-plugin", versions );
    }

    private void populateDbForTestMetadataDrivenSnapshots()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "1.4.3-20070113.163208-4" );
        versions.add( "1.4.3-20070618.102615-5" );
        versions.add( "1.4.3-20070630.113158-6" );
        versions.add( "1.4.3-20070707.122114-7" );
        versions.add( "1.4.3-SNAPSHOT" );

        populateDb( "org.codehaus.plexus", "plexus-utils", versions );
    }
}
