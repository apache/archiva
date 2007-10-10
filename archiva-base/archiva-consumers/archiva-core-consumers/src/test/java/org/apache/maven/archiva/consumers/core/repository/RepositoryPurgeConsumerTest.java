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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.codehaus.plexus.util.IOUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RepositoryPurgeConsumerTest
    extends AbstractRepositoryPurgeTest
{
    private void setLastModified()
    {
        File dir = new File( "target/test/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/" );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 );
        }
    }

    public void testConsumerByRetentionCount()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-retention-count" );

        populateDbForRetentionCountTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDaysOlder( 0 ); // force days older off to allow retention count purge to execute.
        repoConfiguration.setRetentionCount( TEST_RETENTION_COUNT );
        addRepoToConfiguration( "retention-count", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        repoPurgeConsumer.processFile( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );
        
        // assert if removed from repo
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" ).exists() );

        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" ).exists() );

        // assert if not removed from repo
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.sha1" ).exists() );

        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.sha1" ).exists() );

        FileUtils.deleteDirectory( testDir );
    }

    private void addRepoToConfiguration( String configHint, ManagedRepositoryConfiguration repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class,
                                                                                   configHint );
        archivaConfiguration.getConfiguration().addManagedRepository( repoConfiguration );
    }

    public void testConsumerByDaysOld()
        throws Exception
    {
        populateDbForDaysOldTest();

        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-days-old" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDaysOlder( TEST_DAYS_OLDER ); 
        addRepoToConfiguration( "days-old", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration );

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        setLastModified();

        repoPurgeConsumer.processFile( PATH_TO_BY_DAYS_OLD_ARTIFACT );

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

    public void testReleasedSnapshotsWereNotCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-retention-count" );

        populateDbForReleasedSnapshotsTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDeleteReleasedSnapshots( false );
        addRepoToConfiguration( "retention-count", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        repoPurgeConsumer.processFile( PATH_TO_RELEASED_SNAPSHOT );

        // check if the snapshot wasn't removed
        assertTrue(
            new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" ).exists() );
        assertTrue( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" ).exists() );

        // check if metadata file wasn't updated
        File artifactMetadataFile =
            new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/maven-metadata-local.xml" );

        FileReader fileReader = new FileReader( artifactMetadataFile );
        Document document;

        try
        {
            SAXBuilder builder = new SAXBuilder();
            document = builder.build( fileReader );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        // parse the metadata file
        XPath xPath = XPath.newInstance( "//metadata/versioning" );
        Element rootElement = document.getRootElement();

        Element versioning = (Element) xPath.selectSingleNode( rootElement );
        Element el = (Element) xPath.newInstance( "./latest" ).selectSingleNode( versioning );
        assertEquals( "2.3-SNAPSHOT", el.getValue() );

        el = (Element) xPath.newInstance( "./lastUpdated" ).selectSingleNode( versioning );
        assertTrue( el.getValue().equals( "20070315032817" ) );

        List nodes = xPath.newInstance( "./versions" ).selectNodes( versioning );

        boolean found = false;
        for ( Iterator iter = nodes.iterator(); iter.hasNext(); )
        {
            el = (Element) iter.next();
            if ( el.getValue().trim().equals( "2.3-SNAPSHOT" ) )
            {
                found = true;
            }
        }
        assertTrue( found );

        FileUtils.deleteDirectory( testDir );
    }

    public void testReleasedSnapshotsWereCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer = (KnownRepositoryContentConsumer) lookup(
            KnownRepositoryContentConsumer.class, "repo-purge-consumer-by-days-old" );

        populateDbForReleasedSnapshotsTest();

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration();
        repoConfiguration.setDeleteReleasedSnapshots( true );
        addRepoToConfiguration( "days-old", repoConfiguration );
        
        repoPurgeConsumer.beginScan( repoConfiguration );

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        repoPurgeConsumer.processFile( PATH_TO_RELEASED_SNAPSHOT );

        // check if the snapshot was removed
        assertFalse(
            new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" ).exists() );

        // check if metadata file was updated
        File artifactMetadataFile =
            new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/maven-metadata.xml" );

        FileReader fileReader = new FileReader( artifactMetadataFile );
        Document document;

        try
        {
            SAXBuilder builder = new SAXBuilder();
            document = builder.build( fileReader );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        // parse the metadata file
        XPath xPath = XPath.newInstance( "//metadata/versioning" );
        Element rootElement = document.getRootElement();

        Element versioning = (Element) xPath.selectSingleNode( rootElement );
        Element el = (Element) xPath.newInstance( "./latest" ).selectSingleNode( versioning );
        assertEquals( "2.3", el.getValue() );

        el = (Element) xPath.newInstance( "./lastUpdated" ).selectSingleNode( versioning );
        // FIXME: assertFalse( el.getValue().equals( "20070315032817" ) );

        List nodes = xPath.newInstance( "./versions" ).selectNodes( rootElement );

        boolean found = false;
        for ( Iterator iter = nodes.iterator(); iter.hasNext(); )
        {
            el = (Element) iter.next();
            if ( el.getValue().equals( "2.3-SNAPSHOT" ) )
            {
                found = true;
            }
        }
        assertFalse( found );

        FileUtils.deleteDirectory( testDir );
    }

    public void populateDbForRetentionCountTest()
        throws ArchivaDatabaseException
    {
        List versions = new ArrayList();
        versions.add( "1.0RC1-20070504.153317-1" );
        versions.add( "1.0RC1-20070504.160758-2" );
        versions.add( "1.0RC1-20070505.090015-3" );
        versions.add( "1.0RC1-20070506.090132-4" );

        populateDb( "org.jruby.plugins", "jruby-rake-plugin", versions );
    }

    private void populateDbForDaysOldTest()
        throws ArchivaDatabaseException
    {
        List versions = new ArrayList();
        versions.add( "2.2-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-install-plugin", versions );
    }

    public void populateDbForReleasedSnapshotsTest()
        throws ArchivaDatabaseException
    {
        List versions = new ArrayList();
        versions.add( "2.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-plugin-plugin", versions );
    }

}
