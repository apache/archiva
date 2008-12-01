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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

/**
 */
public class DaysOldRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    private static final String[] extensions =
        new String[] { "-5.jar", "-5.pom", "-6.jar", "-6.pom", "-7.jar", "-7.pom" };

    private String year;

    private String mon;

    private String day;

    private String hr;

    private String min;

    private String sec;

    private void setLastModified( String dirPath, long lastModified )
    {
        File dir = new File( dirPath );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( lastModified );
        }
    }

    public void testByLastModified()
        throws Exception
    {
        repoPurge =
            new DaysOldRepositoryPurge( getRepository(),
                                        getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getDaysOlder(),
                                        getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getRetentionCount(),
                                        Collections.singletonList( listener ) );

        String repoRoot = prepareTestRepos();

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-install-plugin";

        setLastModified( projectRoot + "/2.2-SNAPSHOT/", 1179382029 );

        // test listeners for the correct artifacts
        listener.deleteArtifact( getRepository(), createArtifact( "org.apache.maven.plugins", "maven-install-plugin",
                                                                  "2.2-SNAPSHOT", "maven-plugin" ) );
        listener.deleteArtifact( getRepository(), createArtifact( "org.apache.maven.plugins", "maven-install-plugin",
                                                                  "2.2-SNAPSHOT", "pom" ) );
        listenerControl.replay();
        
        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT );
        
        listenerControl.verify();

        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" );

        // shouldn't be deleted because even if older than 30 days (because retention count = 2)
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar.sha1" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom.sha1" );

        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar.sha1" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom.sha1" );
    }

    public void testOrderOfDeletion()
        throws Exception
    {
        repoPurge =
            new DaysOldRepositoryPurge( getRepository(), getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getDaysOlder(),
                                        getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getRetentionCount(), 
                                        Collections.singletonList( listener ) );

        String repoRoot = prepareTestRepos();

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-assembly-plugin";

        setLastModified( projectRoot + "/1.1.2-SNAPSHOT/", 1179382029 );

        // test listeners for the correct artifacts
        listener.deleteArtifact( getRepository(), createArtifact( "org.apache.maven.plugins", "maven-assembly-plugin",
                                                                  "1.1.2-20070427.065136-1", "maven-plugin" ) );
        listener.deleteArtifact( getRepository(), createArtifact( "org.apache.maven.plugins", "maven-assembly-plugin",
                                                                  "1.1.2-20070427.065136-1", "pom" ) );
        listenerControl.replay();
        
        repoPurge.process( PATH_TO_TEST_ORDER_OF_DELETION );

        listenerControl.verify();

        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.sha1" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.md5" );

        // the following should not have been deleted
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.md5" );

        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.md5" );
    }

    public void testMetadataDrivenSnapshots()
        throws Exception
    {
        repoPurge =
            new DaysOldRepositoryPurge( getRepository(),
                                        getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getDaysOlder(),
                                        getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ).getRetentionCount(),
                                        Collections.singletonList( listener ) );

        String repoRoot = prepareTestRepos();

        String versionRoot = repoRoot + "/org/codehaus/plexus/plexus-utils/1.4.3-SNAPSHOT";

        Calendar currentDate = Calendar.getInstance( DateUtils.UTC_TIME_ZONE );
        setLastModified( versionRoot, currentDate.getTimeInMillis() );

        year = String.valueOf( currentDate.get( Calendar.YEAR ) );
        mon = String.valueOf( currentDate.get( Calendar.MONTH ) + 1 );
        day = String.valueOf( currentDate.get( Calendar.DATE ) );
        hr = String.valueOf( currentDate.get( Calendar.HOUR ) );
        min = String.valueOf( currentDate.get( Calendar.MINUTE ) );
        sec = String.valueOf( currentDate.get( Calendar.SECOND ) );

        if ( mon.length() == 1 )
        {
            mon = "0" + mon;
        }

        if ( day.length() == 1 )
        {
            day = "0" + day;
        }

        if ( hr.length() == 1 )
        {
            hr = "0" + hr;
        }

        if ( min.length() == 1 )
        {
            min = "0" + min;
        }

        if ( sec.length() == 1 )
        {
            sec = "0" + sec;
        }

        createFiles( versionRoot );

        List<String> versions = new ArrayList<String>();
        versions.add( "1.4.3-20070113.163208-4" );
        versions.add( "1.4.3-" + year + mon + day + "." + hr + min + sec + "-5" );
        versions.add( "1.4.3-" + year + mon + day + "." + hr + min + sec + "-6" );
        versions.add( "1.4.3-" + year + mon + day + "." + hr + min + sec + "-7" );
        versions.add( "1.4.3-SNAPSHOT" );

        // test listeners for the correct artifacts
        listener.deleteArtifact( getRepository(), createArtifact( "org.codehaus.plexus", "plexus-utils",
                                                                  "1.4.3-20070113.163208-4", "jar" ) );
        listener.deleteArtifact( getRepository(), createArtifact( "org.codehaus.plexus", "plexus-utils",
                                                                  "1.4.3-20070113.163208-4", "pom" ) );
        listenerControl.replay();
        
        repoPurge.process( PATH_TO_BY_DAYS_OLD_METADATA_DRIVEN_ARTIFACT );

        listenerControl.verify();

        // this should be deleted since the filename version (timestamp) is older than
        // 100 days even if the last modified date was <100 days ago
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar.sha1" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom.sha1" );

        // this should not be deleted because last modified date is <100 days ago
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.pom" );

        for ( int i = 0; i < extensions.length; i++ )
        {
            assertExists( versionRoot + "/plexus-utils-1.4.3-" + year + mon + day + "." + hr + min + sec +
                extensions[i] );
        }
    }

    private void createFiles( String versionRoot )
        throws IOException
    {
        for ( int i = 0; i < extensions.length; i++ )
        {
            File file =
                new File( versionRoot, "/plexus-utils-1.4.3-" + year + mon + day + "." + hr + min + sec + extensions[i] );
            file.createNewFile();
        }
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        repoPurge = null;
    }
}
