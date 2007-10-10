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
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
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
public class CleanupReleasedSnapshotsRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        MetadataTools metadataTools = (MetadataTools) lookup( MetadataTools.class );

        repoPurge = new CleanupReleasedSnapshotsRepositoryPurge( getRepository(), dao, metadataTools );
    }

    public void testReleasedSnapshots()
        throws Exception
    {
        populateReleasedSnapshotsTest();

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        repoPurge.process( PATH_TO_RELEASED_SNAPSHOT );

        // check if the snapshot was removed
        assertFalse( new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" )
            .exists() );

        // check if the released version was not removed
        assertTrue( new File( "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3" ).exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3-sources.jar" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3-sources.jar.md5" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3-sources.jar.sha1" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.jar" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.jar.md5" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.jar.sha1" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.pom" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.pom.md5" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/2.3/maven-plugin-plugin-2.3.pom.sha1" )
            .exists() );

        // check if metadata file was updated
        File artifactMetadataFile = new File(
                                              "target/test/test-repo/org/apache/maven/plugins/maven-plugin-plugin/maven-metadata.xml" );

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

    public void testHigherSnapshotExists()
        throws Exception
    {
        populateHigherSnapshotExistsTest();

        File testDir = new File( "target/test" );
        FileUtils.copyDirectoryToDirectory( new File( "target/test-classes/test-repo" ), testDir );

        repoPurge.process( PATH_TO_HIGHER_SNAPSHOT_EXISTS );

        // check if the snapshot was removed
        assertFalse( new File( "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.md5" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.sha1" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.md5" )
            .exists() );
        assertFalse( new File(
                               "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.sha1" )
            .exists() );

        // check if the released version was not removed
        assertTrue( new File( "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.md5" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.sha1" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.md5" )
            .exists() );
        assertTrue( new File(
                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.sha1" )
            .exists() );

        // check if metadata file was updated
        File artifactMetadataFile = new File(
                                              "target/test/test-repo/org/apache/maven/plugins/maven-source-plugin/maven-metadata.xml" );

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
        assertEquals( "2.0.4-SNAPSHOT", el.getValue() );

        el = (Element) xPath.newInstance( "./lastUpdated" ).selectSingleNode( versioning );
        // FIXME: assertFalse( el.getValue().equals( "20070427033345" ) );

        List nodes = xPath.newInstance( "./versions" ).selectNodes( rootElement );

        boolean found = false;
        for ( Iterator iter = nodes.iterator(); iter.hasNext(); )
        {
            el = (Element) iter.next();
            if ( el.getValue().equals( "2.0.3-SNAPSHOT" ) )
            {
                found = true;
            }
        }
        assertFalse( found );

        FileUtils.deleteDirectory( testDir );
    }

    private void populateReleasedSnapshotsTest()
        throws ArchivaDatabaseException
    {
        List versions = new ArrayList();
        versions.add( "2.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-plugin-plugin", versions );
    }

    private void populateHigherSnapshotExistsTest()
        throws Exception
    {
        List versions = new ArrayList();
        versions.add( "2.0.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-source-plugin", versions );
    }

}
