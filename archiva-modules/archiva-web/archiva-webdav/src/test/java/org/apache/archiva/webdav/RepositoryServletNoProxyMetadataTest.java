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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RepositoryServletTest
 */
public class RepositoryServletNoProxyMetadataTest
    extends AbstractRepositoryServletTestCase
{

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        startRepository();
    }

    @Override
    @After
    public void tearDown( ) throws Exception
    {
        super.tearDown( );
    }

    @Test
    public void testGetVersionMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-version-2.1";

        Path checksumFile = repoRootInternal.resolve(commonsLangMetadata);
        Files.createDirectories(checksumFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( checksumFile, Charset.defaultCharset(), expectedMetadataContents );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetProjectMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-version-for-project";

        Path checksumFile = repoRootInternal.resolve(commonsLangMetadata);
        Files.createDirectories(checksumFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( checksumFile, Charset.defaultCharset(), expectedMetadataContents );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetGroupMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-group";

        Path checksumFile = repoRootInternal.resolve(commonsLangMetadata);
        Files.createDirectories(checksumFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( checksumFile, Charset.defaultCharset() , expectedMetadataContents);

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

    @Test
    public void testGetSnapshotVersionMetadataDefaultLayout()
        throws Exception
    {
        String assemblyPluginMetadata =
            "org/apache/maven/plugins/maven-assembly-plugin/2.2-beta-2-SNAPSHOT/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-assembly-plugin-version-2.2-beta-2-SNAPSHOT";

        Path checksumFile = repoRootInternal.resolve(assemblyPluginMetadata);
        Files.createDirectories(checksumFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( checksumFile, Charset.defaultCharset(), expectedMetadataContents );

        WebRequest request =
            new GetMethodWebRequest( "http://machine.com/repository/internal/" + assemblyPluginMetadata );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getContentAsString() );
    }

}
