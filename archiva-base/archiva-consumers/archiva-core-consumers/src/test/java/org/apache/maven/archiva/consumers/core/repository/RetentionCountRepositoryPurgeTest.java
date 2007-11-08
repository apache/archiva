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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.archiva.consumers.core.repository.stubs.LuceneRepositoryContentIndexStub;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;

/**
 * Test RetentionsCountRepositoryPurgeTest
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RetentionCountRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{

    protected void setUp()
        throws Exception
    {
        super.setUp();

        Map<String, RepositoryContentIndex> map = new HashMap<String, RepositoryContentIndex>();
        map.put( "filecontent", new LuceneRepositoryContentIndexStub( 2 ) );
        map.put( "hashcodes", new LuceneRepositoryContentIndexStub( 2 ) );
        map.put( "bytecode", new LuceneRepositoryContentIndexStub( 2 ) );
        
        repoPurge = new RetentionCountRepositoryPurge( getRepository(), dao,
                                                       getRepoConfiguration().getRetentionCount(), map );
    }

    /**
     * Test if the artifact to be processed was a jar.
     *
     * @throws Exception
     */
    public void testIfAJarWasFound()
        throws Exception
    {
        populateIfJarWasFoundDb();

        String repoRoot = prepareTestRepo();

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );
        
        String versionRoot = repoRoot + "/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT";

        // assert if removed from repo
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" );

        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.sha1" );

        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.sha1" );
    }

    /**
     * Test if the artifact to be processed is a pom
     *
     * @throws Exception
     */
    public void testIfAPomWasFound()
        throws Exception
    {
        populateIfPomWasFoundDb();

        String repoRoot = prepareTestRepo();

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_POM );

        String versionRoot = repoRoot + "/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT";
        
        // assert if removed from repo
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom.md5" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.sha1" );

        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.sha1" );
    }

    public void populateIfJarWasFoundDb()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0RC1-20070504.153317-1" );
        versions.add( "1.0RC1-20070504.160758-2" );
        versions.add( "1.0RC1-20070505.090015-3" );
        versions.add( "1.0RC1-20070506.090132-4" );

        populateDb( "org.jruby.plugins", "jruby-rake-plugin", versions );
    }

    public void populateIfPomWasFoundDb()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "1.1.2-20070427.065136-1" );
        versions.add( "1.1.2-20070615.105019-3" );
        versions.add( "1.1.2-20070506.163513-2" );

        populateDb( "org.codehaus.castor", "castor-anttasks", versions );
    }
}
