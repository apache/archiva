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
import org.custommonkey.xmlunit.XMLAssert;

import java.io.File;
import java.util.ArrayList;
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

        String repoRoot = prepareTestRepo();

        repoPurge.process( PATH_TO_RELEASED_SNAPSHOT );

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";
        
        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if the released version was not removed
        assertExists( projectRoot + "/2.3" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar.sha1" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar.sha1" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom.sha1" );

        // check if metadata file was updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );
        
        String expectedVersions = "<expected><versions><version>2.2</version>" +
        		"<version>2.3</version></versions></expected>";
        
        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/release", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        // FIXME [MRM-535]: XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );
    }

    public void testHigherSnapshotExists()
        throws Exception
    {
        populateHigherSnapshotExistsTest();

        String repoRoot = prepareTestRepo();

        repoPurge.process( PATH_TO_HIGHER_SNAPSHOT_EXISTS );
        
        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-source-plugin";

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.sha1" );

        // check if the released version was not removed
        assertExists( projectRoot + "/2.0.4-SNAPSHOT" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.sha1" );

        // check if metadata file was updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );
        
        String expectedVersions = "<expected><versions><version>2.0.2</version>" +
        		"<version>2.0.4-SNAPSHOT</version></versions></expected>";
        
        XMLAssert.assertXpathEvaluatesTo( "2.0.4-SNAPSHOT", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        // FIXME [MRM-535]: XMLAssert.assertXpathEvaluatesTo( "20070427033345", "//metadata/versioning/lastUpdated", metadataXml );
    }

    private void populateReleasedSnapshotsTest()
        throws ArchivaDatabaseException
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "2.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-plugin-plugin", versions );
    }

    private void populateHigherSnapshotExistsTest()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "2.0.3-SNAPSHOT" );

        populateDb( "org.apache.maven.plugins", "maven-source-plugin", versions );
    }

}
