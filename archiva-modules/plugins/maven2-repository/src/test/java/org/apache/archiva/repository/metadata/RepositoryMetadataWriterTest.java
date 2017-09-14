package org.apache.archiva.repository.metadata;

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

import junit.framework.TestCase;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RepositoryMetadataWriterTest
 */
@RunWith ( ArchivaBlockJUnit4ClassRunner.class )
public class RepositoryMetadataWriterTest
    extends TestCase
{

    @Test
    public void testWriteSimple()
        throws Exception
    {
        Path defaultRepoDir = Paths.get( "src/test/repositories/default-repository" );
        Path expectedFile = defaultRepoDir.resolve( "org/apache/maven/shared/maven-downloader/maven-metadata.xml" );
        String expectedContent = org.apache.archiva.common.utils.FileUtils.readFileToString( expectedFile, Charset.defaultCharset() );

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
