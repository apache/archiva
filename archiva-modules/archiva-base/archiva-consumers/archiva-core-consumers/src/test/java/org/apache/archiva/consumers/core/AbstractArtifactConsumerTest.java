package org.apache.archiva.consumers.core;

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

import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public abstract class AbstractArtifactConsumerTest
{
    private Path repoLocation;

    protected KnownRepositoryContentConsumer consumer;

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    ArchivaConfiguration archivaConfiguration;


    @Before
    public void setUp()
        throws Exception
    {
        FileType fileType =
            (FileType) archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );
        fileType.addPattern( "**/*.xml" );
        archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration().addChecksumType("MD5");
        archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration().addChecksumType("SHA1");
        archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration().addChecksumType("SHA256");

        repoLocation = Paths.get( "target/test-" + getName() + "/test-repo" );
    }


    @SuppressWarnings( "deprecation" )
    @Test
    public void testConsumption()
    {
        Path localFile =
            repoLocation.resolve( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata.xml" );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation.toFile(), localFile.toFile() );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( consumer ) );
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void testConsumptionOfOtherMetadata()
    {
        Path localFile =
            repoLocation.resolve( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata-central.xml" );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation.toFile(), localFile.toFile() );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( consumer ) );
    }
    
    public String getName()
    {
        return StringUtils.substringAfterLast( getClass().getName(), "." );
    }
}
