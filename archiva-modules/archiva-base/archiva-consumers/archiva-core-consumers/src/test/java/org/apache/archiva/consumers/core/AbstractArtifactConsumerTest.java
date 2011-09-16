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

import junit.framework.TestCase;
import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath:/spring-context.xml"} )
public abstract class AbstractArtifactConsumerTest
    extends TestCase
{
    private File repoLocation;

    protected KnownRepositoryContentConsumer consumer;

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        FileType fileType =
            (FileType) archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );
        fileType.addPattern( "**/*.xml" );

        repoLocation = new File( "target/test-" + getName() + "/test-repo" );
    }

    @Test
    public void testConsumption()
    {
        File localFile =
            new File( repoLocation, "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata.xml" );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation, localFile );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( consumer ) );
    }

    @Test
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
