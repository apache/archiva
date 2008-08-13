package org.apache.maven.archiva.repository.metadata;

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
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.custommonkey.xmlunit.XMLAssert;

import java.io.File;
import java.io.StringWriter;

/**
 * RepositoryMetadataWriterTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryMetadataWriterTest
    extends PlexusInSpringTestCase
{
    public void testWriteSimple()
        throws Exception
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/default-repository" );
        File expectedFile = new File( defaultRepoDir, "org/apache/maven/shared/maven-downloader/maven-metadata.xml" );
        String expectedContent = FileUtils.readFileToString( expectedFile, null );

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();

        metadata.setGroupId( "org.apache.maven.shared" );
        metadata.setArtifactId( "maven-downloader" );
        metadata.setVersion( "1.0" );
        metadata.setReleasedVersion( "1.1" );
        metadata.getAvailableVersions().add( "1.0" );
        metadata.getAvailableVersions().add( "1.1" );
        metadata.setLastUpdated( "20061212214311" );

        StringWriter actual = new StringWriter();
        RepositoryMetadataWriter.write( metadata, actual );

        XMLAssert.assertXMLEqual( "XML Contents", expectedContent, actual.toString() );
    }
}
