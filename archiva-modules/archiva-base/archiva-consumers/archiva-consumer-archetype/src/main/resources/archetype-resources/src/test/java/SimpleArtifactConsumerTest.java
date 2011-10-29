package $package;

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

import java.io.File;

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.easymock.EasyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import junit.framework.TestCase;

/**
 * <code>SimpleArtifactConsumerTest</code>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath:/spring-context.xml"} )
public class SimpleArtifactConsumerTest
    extends TestCase
{
    @Inject
    private SimpleArtifactConsumer consumer;

    private File repoDir;

    private ManagedRepository testRepository;

    private Logger log = LoggerFactory.getLogger( SimpleArtifactConsumer.class );

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        String consumerRole = KnownRepositoryContentConsumer.class.getName();

        setUpMockRepository();

    }



    private void setUpMockRepository()
    {
        repoDir = new java.io.File( "target/test-consumer-repo" );
        repoDir.mkdirs();
        repoDir.deleteOnExit();

        testRepository = new ManagedRepository();
        testRepository.setName( "Test-Consumer-Repository" );
        testRepository.setId( "test-consumer-repository" );
        testRepository.setLocation( repoDir.getAbsolutePath() );
    }

    @Test
    public void testBeginScan()
        throws Exception
    {
        log.info( "Beginning scan of repository [test-consumer-repository]" );

        consumer.beginScan( testRepository );

    }

    @Test
    public void testProcessFile()
        throws Exception
    {
        consumer.beginScan( testRepository );
        consumer.processFile( "org/simple/test/testartifact/testartifact/1.0/testartifact-1.0.pom" );
        consumer.processFile( "org/simple/test/testartifact/testartifact/1.1/testartifact-1.1.pom" );
    }

}
