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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

// import static org.mockito.Mockito.*;

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
    private RepositoryRegistry repositoryRegistry;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private BasicManagedRepository testRepository;

    private Logger log = LoggerFactory.getLogger( SimpleArtifactConsumer.class );

    private MetadataRepository metadataRepository;

    @Before
    public void setUp()
        throws Exception
    {
        setUpMockRepository();
    }

    private void setUpMockRepository()
        throws IOException, RepositoryException
    {
        Path repoDir = Paths.get( "target/test-consumer-repo" );
        Files.createDirectories( repoDir );
        repoDir.toFile().deleteOnExit();

        testRepository = BasicManagedRepository.newFilesystemInstance("test-consumer-repository","Test-Consumer-Repository", Paths.get("target/repositories") );
        testRepository.setLocation( repoDir.toAbsolutePath().toUri() );

        repositoryRegistry.putRepository(testRepository);

        // when( repositoryRegistry.getManagedRepository( testRepository.getId() ) ).thenReturn( testRepository );
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
