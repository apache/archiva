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

import org.apache.commons.io.FileUtils;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

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

        repoPurge = new DaysOldRepositoryPurge( getRepository(), getLayout(), dao, getRepoConfiguration() );
    }

    private void setLastModified()
    {
        File dir =
            new File( "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/" );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 );
        }
    }

    public void testIfAJarIsFound()
        throws Exception
    {
        populateDb();

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        setLastModified();

        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" ).exists() );

        FileUtils.deleteDirectory( testDir );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        repoPurge = null;
    }

    private void populateDb()
        throws Exception
    {
        List versions = new ArrayList();
        versions.add( "2.2-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-install-plugin", versions );
    }
}
