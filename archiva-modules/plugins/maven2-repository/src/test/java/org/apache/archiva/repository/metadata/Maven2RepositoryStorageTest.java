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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml" } )
public class Maven2RepositoryStorageTest
{
    @Inject
    @Named( "repositoryStorage#maven2" )
    RepositoryStorage repositoryStorage;

    @Test
    public void testGetLogicalPath()
    {
        String href = "/repository/internal/org/apache/maven/someartifact.jar";
        Assert.assertEquals( "/org/apache/maven/someartifact.jar",
                             repositoryStorage.getFilePath( href, new ManagedRepository() ) );

        href = "repository/internal/org/apache/maven/someartifact.jar";
        Assert.assertEquals( "/org/apache/maven/someartifact.jar",
                             repositoryStorage.getFilePath( href, new ManagedRepository() ) );

        href = "repository/internal/org/apache/maven/";
        Assert.assertEquals( "/org/apache/maven/", repositoryStorage.getFilePath( href, new ManagedRepository() ) );

        href = "mypath";
        Assert.assertEquals( "/", repositoryStorage.getFilePath( href, new ManagedRepository() ) );
    }


}

