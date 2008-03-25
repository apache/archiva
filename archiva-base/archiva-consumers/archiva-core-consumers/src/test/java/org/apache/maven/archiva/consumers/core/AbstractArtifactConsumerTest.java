package org.apache.maven.archiva.consumers.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerWantsFilePredicate;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

public abstract class AbstractArtifactConsumerTest
    extends PlexusTestCase
{
    private File repoLocation;

    protected KnownRepositoryContentConsumer consumer;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaConfiguration archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.ROLE );
        FileType fileType =
            (FileType) archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );
        fileType.addPattern( "**/*.xml" );

        repoLocation = getTestFile( "target/test-" + getName() + "/test-repo" );
    }

    public void testConsumption()
    {
        File localFile =
            new File( repoLocation, "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata.xml" );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation, localFile );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( consumer ) );
    }

    public void testConsumptionOfOtherMetadata()
    {
        File localFile =
            new File( repoLocation, "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata-central.xml" );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation, localFile );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( consumer ) );
    }
}
