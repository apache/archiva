package org.apache.archiva.webdav;

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


import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * RepositoryServletTest
 */
public class RepositoryServletNoProxyTest
    extends AbstractRepositoryServletTestCase
{

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        archivaConfiguration.getConfiguration().setProxyConnectors( new ArrayList<ProxyConnectorConfiguration>() );
        startRepository();
    }

    @Test
    public void testLastModifiedHeaderExists()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangSha1 );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangSha1 );
        assertNotNull( response.getResponseHeaderValue( "Last-Modified" ) );
    }

    @Test
    public void testGetNoProxyChecksumDefaultLayout()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", Charset.defaultCharset() );

        //WebRequest request = new WebRequest( "http://machine.com/repository/internal/" + commonsLangSha1 );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangSha1 );
        assertResponseOK( response );

        assertEquals( "Expected file contents", "dummy-checksum", response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyChecksumLegacyLayout()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootInternal, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest(
        //    "http://machine.com/repository/internal/" + "commons-lang/jars/commons-lang-2.1.jar.sha1" );
        WebResponse response =
            getWebResponse( "/repository/internal/" + "commons-lang/jars/commons-lang-2.1.jar.sha1" );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyVersionedMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "dummy-versioned-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangMetadata );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyProjectMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-project-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangMetadata );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyGroupMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-group-metadata";

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangMetadata );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangJar );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest(
        //    "http://machine.com/repository/internal/" + "commons-lang/jars/commons-lang-2.1.jar" );
        WebResponse response = getWebResponse( "/repository/internal/" + "commons-lang/jars/commons-lang-2.1.jar" );
        assertResponseNotFound( response );

    }

    @Test
    public void testGetNoProxySnapshotArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangJar );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxySnapshotArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest(
        //    "http://machine.com/repository/internal/" + "commons-lang/jars/commons-lang-2.1-SNAPSHOT.jar" );
        WebResponse response = getWebResponse( "/repository/internal/commons-lang/jars/commons-lang-2.1-SNAPSHOT.jar" );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyTimestampedSnapshotArtifactDefaultLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        //WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = getWebResponse( "/repository/internal/" + commonsLangJar );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyTimestampedSnapshotArtifactLegacyLayout()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/" + "commons-lang/jars/commons-lang-2.1-20050821.023400-1.jar" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    @Test
    public void testGetNoProxyDualExtensionDefaultLayout()
        throws Exception
    {
        String expectedContents = "the-contents-of-the-dual-extension";
        String dualExtensionPath = "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip";

        File checksumFile = new File( repoRootInternal, dualExtensionPath );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + dualExtensionPath );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedContents, response.getContentAsString() );
    }

    @Test
    public void testGetNoProxyDistributionLegacyLayout()
        throws Exception
    {
        String expectedContents = "the-contents-of-the-dual-extension";
        String dualExtensionPath = "org/project/example-presentation/3.2/example-presentation-3.2.zip";

        File checksumFile = new File( repoRootInternal, dualExtensionPath );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/" + "org.project/distributions/example-presentation-3.2.zip" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );

    }

    @Test
    public void testGetNoProxyChecksumDefaultLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repoRootLegacy, "commons-lang/jars/commons-lang-2.1.jar.sha1" );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangSha1 );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyChecksumLegacyLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangSha1 = "commons-lang/jars/commons-lang-2.1.jar.sha1";
        File checksumFile = new File( repoRootLegacy, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangSha1 );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyVersionedMetadataDefaultLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "dummy-versioned-metadata";

        // TODO: find out what this should be from maven-artifact
        File metadataFile = new File( repoRootLegacy, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyProjectMetadataDefaultLayoutManagedLegacy()
        throws Exception
    {
        // TODO: find out what it is meant to be from maven-artifact
        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-project-metadata";

        File metadataFile = new File( repoRootLegacy, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyGroupMetadataDefaultLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-group-metadata";

        File metadataFile = new File( repoRootLegacy, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyArtifactDefaultLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootLegacy, "commons-lang/jars/commons-lang-2.1.jar" );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyArtifactLegacyLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangJar = "commons-lang/jars/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootLegacy, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxySnapshotArtifactDefaultLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootLegacy, "commons-lang/jars/commons-lang-2.1-SNAPSHOT.jar" );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxySnapshotArtifactLegacyLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangJar = "commons-lang/jars/commons-lang-2.1-SNAPSHOT.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootLegacy, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyTimestampedSnapshotArtifactDefaultLayoutManagedLegacy()
        throws Exception
    {
        String filename = "commons-lang-2.1-20050821.023400-1.jar";
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/" + filename;
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootLegacy, "commons-lang/jars/" + filename );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyTimestampedSnapshotArtifactLegacyLayoutManagedLegacy()
        throws Exception
    {
        String commonsLangJar = "commons-lang/jars/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        File artifactFile = new File( repoRootLegacy, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + commonsLangJar );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    @Test
    public void testGetNoProxyDualExtensionDefaultLayoutManagedLegacy()
        throws Exception
    {
        String expectedContents = "the-contents-of-the-dual-extension";
        String dualExtensionPath = "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip";

        File checksumFile = new File( repoRootLegacy, "org.project/distributions/example-presentation-3.2.xml.zip" );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + dualExtensionPath );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxyDistributionLegacyLayoutManagedLegacy()
        throws Exception
    {
        String expectedContents = "the-contents-of-the-dual-extension";
        String dualExtensionPath = "org.project/distributions/example-presentation-3.2.zip";

        File checksumFile = new File( repoRootLegacy, dualExtensionPath );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedContents, Charset.defaultCharset() );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/legacy/" + dualExtensionPath );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
    }

    @Test
    public void testGetNoProxySnapshotRedirectToTimestampedSnapshot()
        throws Exception
    {
        String commonsLangQuery = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-SNAPSHOT.jar";
        String commonsLangMetadata = "commons-lang/commons-lang/2.1-SNAPSHOT/maven-metadata.xml";
        String commonsLangJar = "commons-lang/commons-lang/2.1-SNAPSHOT/commons-lang-2.1-20050821.023400-1.jar";
        String expectedArtifactContents = "dummy-commons-lang-snapshot-artifact";

        archivaConfiguration.getConfiguration().getWebapp().getUi().setApplicationUrl("http://localhost");

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        File metadataFile = new File( repoRootInternal, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( metadataFile, createVersionMetadata("commons-lang", "commons-lang",
                "2.1-SNAPSHOT", "20050821.023400", "1", "20050821.023400"));

        WebRequest webRequest = new GetMethodWebRequest(
                "http://localhost/repository/internal/" + commonsLangQuery );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( webRequest.getUrl().getPath() );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( webRequest.getHttpMethod().name() );

        final MockHttpServletResponse response = execute( request );

        assertEquals( HttpServletResponse.SC_MOVED_TEMPORARILY,
                      response.getStatus() );

        assertEquals( "http://localhost/repository/internal/" + commonsLangJar,
                      response.getHeader("Location") );
    }

}
