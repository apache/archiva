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

import org.apache.maven.archiva.model.ArchivaArtifact;

import java.io.File;
import java.util.Date;

/**
 * Test RetentionsCountRepositoryPurgeTest
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RetentionCountRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{

    public static final String PATH_TO_BY_RETENTION_COUNT_ARTIFACT =
        "org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar";

    public static final String PATH_TO_BY_RETENTION_COUNT_POM =
        "org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        repoPurge = new RetentionCountRepositoryPurge( getRepository(), getLayout(), dao,
                                                       getRepoConfiguration().getRetentionCount() );
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

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        // assert if removed from repo
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" ).exists() );

        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" ).exists() );

        // assert if not removed from repo
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.sha1" ).exists() );

        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.sha1" ).exists() );
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

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_POM );

        // assert if removed from repo
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.jar" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.pom" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070427.065136-1.pom.sha1" ).exists() );

        // assert if not removed from repo
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.pom" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.pom.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3-sources.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.sha1" ).exists() );

        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2-sources.jar" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test-classes/test-repo/org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.sha1" ).exists() );
    }

    public void populateIfJarWasFoundDb()
        throws Exception
    {
        // Create it
        ArchivaArtifact artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070504.153317-1", "", "jar" );
        assertNotNull( artifact );

        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );

        // Save it.
        ArchivaArtifact savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070504.153317-1", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //JAR
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070504.160758-2", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070504.160758-2", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //JAR
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070505.090015-3", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070505.090015-3", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //JAR
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070506.090132-4", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact =
            dao.createArtifact( "org.jruby.plugins", "jruby-rake-plugin", "1.0RC1-20070506.090132-4", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );
    }

    public void populateIfPomWasFoundDb()
        throws Exception
    {
        // Create it
        ArchivaArtifact artifact =
            dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070427.065136-1", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        ArchivaArtifact savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact = dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070427.065136-1", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //JAR
        artifact = dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070615.105019-3", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact = dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070615.105019-3", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //JAR
        artifact = dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070506.163513-2", "", "jar" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );

        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact = dao.createArtifact( "org.codehaus.castor", "castor-anttasks", "1.1.2-20070506.163513-2", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );
    }
}
