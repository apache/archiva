package org.apache.archiva.repository.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.RepositoryContentProvider;
import org.apache.archiva.repository.maven.MavenManagedRepository;
import org.apache.archiva.repository.maven.MavenRemoteRepository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AbstractRepositoryLayerTestCase
 *
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-repository-conf.xml" } )
public abstract class AbstractRepositoryLayerTestCase
{
    @Rule
    public TestName name = new TestName();

    @Inject
    protected ApplicationContext applicationContext;

    protected MavenManagedRepository createRepository( String id, String name, Path location ) throws IOException {
        MavenManagedRepository repo = MavenManagedRepository.newLocalInstance( id, name, location.getParent().toAbsolutePath());
        repo.setLocation( location.toAbsolutePath().toUri() );
        return repo;
    }

    protected MavenRemoteRepository createRemoteRepository( String id, String name, String url ) throws URISyntaxException, IOException {
        MavenRemoteRepository repo = MavenRemoteRepository.newLocalInstance(id, name, Paths.get("target/remotes"));
        repo.setLocation( new URI( url ) );
        return repo;
    }

    protected ManagedRepositoryContent createManagedRepositoryContent( String id, String name, Path location,
                                                                       String layout )
        throws Exception
    {
        MavenManagedRepository repo = MavenManagedRepository.newLocalInstance( id, name, location.getParent() );
        repo.setLocation( location.toAbsolutePath().toUri() );
        repo.setLayout( layout );

        RepositoryContentProvider provider = applicationContext.getBean( "repositoryContentProvider#maven", RepositoryContentProvider.class );
        ManagedRepositoryContent repoContent =
            provider.createManagedContent( repo );

        return repoContent;
    }

    protected RemoteRepositoryContent createRemoteRepositoryContent( String id, String name, String url, String layout )
        throws Exception
    {
        MavenRemoteRepository repo = MavenRemoteRepository.newLocalInstance(id, name, Paths.get("target/remotes"));
        repo.setLocation( new URI( url ) );
        repo.setLayout( layout );

        RepositoryContentProvider provider = applicationContext.getBean( "repositoryContentProvider#maven", RepositoryContentProvider.class );
        RemoteRepositoryContent repoContent =
            provider.createRemoteContent( repo );

        return repoContent;
    }
}
