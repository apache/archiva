package org.apache.archiva.indexer.maven;

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

import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.maven2.MavenManagedRepository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class MavenIndexManagerTest {

    @Inject
    MavenIndexManager mavenIndexManager;



    @Test
    public void pack() throws Exception {
    }

    @Test
    public void scan() throws Exception {
    }

    @Test
    public void update() throws Exception {
    }

    @Test
    public void addArtifactsToIndex() throws Exception {
    }

    @Test
    public void removeArtifactsFromIndex() throws Exception {
    }

    @Test
    public void supportsRepository() throws Exception {
    }

    @Test
    public void createContext() throws Exception {
        MavenManagedRepository repository = new MavenManagedRepository("test-repo", "Test Repo", Paths.get("target/repositories"));
        repository.setLocation(new URI("test-repo"));
        IndexCreationFeature icf = repository.getFeature(IndexCreationFeature.class).get();
        icf.setIndexPath(new URI(".index-test"));
        ArchivaIndexingContext ctx = mavenIndexManager.createContext(repository);
        assertNotNull(ctx);
        assertEquals(repository, ctx.getRepository());
        assertEquals("test-repo", ctx.getId());
        Path indexPath = Paths.get("target/repositories/test-repo/.index-test");
        assertEquals(indexPath.toAbsolutePath(), Paths.get(ctx.getPath()).toAbsolutePath());
        assertTrue(Files.exists(indexPath));

    }

}