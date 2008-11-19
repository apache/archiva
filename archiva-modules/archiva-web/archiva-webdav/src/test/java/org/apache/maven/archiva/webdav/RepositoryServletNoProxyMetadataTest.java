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
 * @version $Id$
 */
public class RepositoryServletNoProxyMetadataTest
    extends AbstractRepositoryServletTestCase
{
    public void testGetVersionMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-version-2.1";

        File checksumFile = new File( repoRootInternal, commonsLangMetadata );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetProjectMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-version-for-project";

        File checksumFile = new File( repoRootInternal, commonsLangMetadata );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetGroupMetadataDefaultLayout()
        throws Exception
    {
        String commonsLangMetadata = "commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-commons-lang-group";

        File checksumFile = new File( repoRootInternal, commonsLangMetadata );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

    public void testGetSnapshotVersionMetadataDefaultLayout()
        throws Exception
    {
        String assemblyPluginMetadata = "org/apache/maven/plugins/maven-assembly-plugin/2.2-beta-2-SNAPSHOT/maven-metadata.xml";
        String expectedMetadataContents = "metadata-for-assembly-plugin-version-2.2-beta-2-SNAPSHOT";

        File checksumFile = new File( repoRootInternal, assemblyPluginMetadata );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + assemblyPluginMetadata );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }

}
