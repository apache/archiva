package org.apache.archiva.repository;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import javax.inject.Inject;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;

/**
 * AbstractRepositoryLayerTestCase
 *
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-no-mock-conf.xml" } )
public abstract class AbstractRepositoryLayerTestCase
{
    @Rule
    public TestName name = new TestName();

    @Inject
    protected ApplicationContext applicationContext;

    protected ManagedRepository createRepository( String id, String name, File location )
    {
        ManagedRepository repo = new ManagedRepository();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected RemoteRepository createRemoteRepository( String id, String name, String url )
    {
        RemoteRepository repo = new RemoteRepository();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }

    protected ManagedRepositoryContent createManagedRepositoryContent( String id, String name, File location,
                                                                       String layout )
        throws Exception
    {
        ManagedRepository repo = new ManagedRepository();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        repo.setLayout( layout );

        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#" + layout, ManagedRepositoryContent.class );
        repoContent.setRepository( repo );

        return repoContent;
    }

    protected RemoteRepositoryContent createRemoteRepositoryContent( String id, String name, String url, String layout )
        throws Exception
    {
        RemoteRepository repo = new RemoteRepository();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        repo.setLayout( layout );

        RemoteRepositoryContent repoContent =
            applicationContext.getBean( "remoteRepositoryContent#" + layout, RemoteRepositoryContent.class );
        repoContent.setRepository( repo );

        return repoContent;
    }
}
