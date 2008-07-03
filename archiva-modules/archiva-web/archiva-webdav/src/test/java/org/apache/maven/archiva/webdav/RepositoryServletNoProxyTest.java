package org.apache.maven.archiva.webdav;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * RepositoryServletTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletNoProxyTest
    extends AbstractRepositoryServletTestCase
{
    public void testLastModifiedHeaderExists()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangSha1 );
        WebResponse response = sc.getResponse( request );
        
        assertNotNull(response.getHeaderField("last-modified"));
    }
    
    public void testGetNoProxyChecksumDefaultLayout()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangSha1 );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", "dummy-checksum", response.getText() );
    }

    public void testGetNoProxyChecksumLegacyLayout()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/"
            + "commons-lang/jars/commons-lang-2.1.jar.sha1" );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", "dummy-checksum", response.getText() );
    }

    public void testGetNoProxyVersionedMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "dummy-versioned-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetNoProxyProjectMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-project-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetNoProxyGroupMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-group-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetNoProxyArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    public void testGetNoProxyArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/"
            + "commons-lang/jars/commons-lang-2.1.jar" );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    public void testGetNoProxySnapshotArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    public void testGetNoProxySnapshotArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/"
            + "commons-lang/jars/commons-lang-2.1-SNAPSHOT.jar" );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    public void testGetNoProxyTimestampedSnapshotArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }

    public void testGetNoProxyTimestampedSnapshotArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/"
            + "commons-lang/jars/commons-lang-2.1-20050821.023400-1.jar" );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }
    
    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    public void testGetNoProxyDualExtensionDefaultLayout()
        throws Exception
    {
        String expectedContents = "the-contents-of-the-dual-extension";
        String dualExtensionPath = "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip";

        File checksumFile = new File( repoRootInternal, dualExtensionPath );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + dualExtensionPath );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedContents, response.getText() );
    }
}
