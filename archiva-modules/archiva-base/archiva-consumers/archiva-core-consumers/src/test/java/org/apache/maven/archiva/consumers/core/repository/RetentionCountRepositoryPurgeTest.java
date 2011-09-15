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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.repository.events.RepositoryListener;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Test RetentionsCountRepositoryPurgeTest
 */
public class RetentionCountRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        List<RepositoryListener> listeners = Collections.singletonList( listener );
        repoPurge = new RetentionCountRepositoryPurge( getRepository(), repoConfiguration.getRetentionCount(),
                                                       repositorySession, listeners );
    }

    /**
     * Test if the artifact to be processed was a jar.
     */
    @Test
    public void testIfAJarWasFound()
        throws Exception
    {
        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                                 "1.0RC1-20070504.153317-1", "jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                                 "1.0RC1-20070504.153317-1", "jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                                 "1.0RC1-20070504.160758-2", "jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                                 "1.0RC1-20070504.160758-2", "jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" );
        listenerControl.replay();

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        listenerControl.verify();

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
     */
    @Test
    public void testIfAPomWasFound()
        throws Exception
    {
        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                                 "1.1.2-20070427.065136-1", "castor-anttasks-1.1.2-20070427.065136-1.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                                 "1.1.2-20070427.065136-1", "castor-anttasks-1.1.2-20070427.065136-1.pom" );
        listenerControl.replay();

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_POM );

        listenerControl.verify();

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

    @Test
    public void testOrderOfDeletion()
        throws Exception
    {
        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                                 "maven-assembly-plugin", "1.1.2-20070427.065136-1",
                                 "maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                                 "maven-assembly-plugin", "1.1.2-20070427.065136-1",
                                 "maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        listenerControl.replay();

        repoPurge.process( PATH_TO_TEST_ORDER_OF_DELETION );

        listenerControl.verify();

        String versionRoot = repoRoot + "/org/apache/maven/plugins/maven-assembly-plugin/1.1.2-SNAPSHOT";

        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.sha1" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.md5" );

        // the following should not have been deleted
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.md5" );

        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.md5" );
    }
}
