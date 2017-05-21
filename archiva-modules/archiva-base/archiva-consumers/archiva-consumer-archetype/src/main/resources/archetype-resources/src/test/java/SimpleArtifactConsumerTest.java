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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * <code>SimpleArtifactConsumerTest</code>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
public class SimpleArtifactConsumerTest
{
    @Inject
    private SimpleArtifactConsumer consumer;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private ManagedRepository testRepository;

    private Logger log = LoggerFactory.getLogger( SimpleArtifactConsumer.class );

    private MetadataRepository metadataRepository;

    @Before
    public void setUp()
        throws Exception
    {
        setUpMockRepository();
    }

    private void setUpMockRepository()
        throws RepositoryAdminException
    {
        File repoDir = new File( "target/test-consumer-repo" );
        repoDir.mkdirs();
        repoDir.deleteOnExit();

        testRepository = new ManagedRepository();
        testRepository.setName( "Test-Consumer-Repository" );
        testRepository.setId( "test-consumer-repository" );
        testRepository.setLocation( repoDir.getAbsolutePath() );

        when( managedRepositoryAdmin.getManagedRepository( testRepository.getId() ) ).thenReturn( testRepository );
    }

    @Test
    public void testBeginScan()
        throws Exception
    {
        log.info( "Beginning scan of repository [test-consumer-repository]" );

        consumer.beginScan( testRepository, new Date() );
    }

    @Test
    public void testProcessFile()
        throws Exception
    {
        consumer.beginScan( testRepository, new Date() );
        consumer.processFile( "org/simple/test/testartifact/testartifact/1.0/testartifact-1.0.pom" );
        consumer.processFile( "org/simple/test/testartifact/testartifact/1.1/testartifact-1.1.pom" );

    }

}
