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

import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.maven.MavenManagedRepository;
import org.apache.archiva.repository.maven.MavenRemoteRepository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.QueryCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.UserInputSearchExpression;
import org.apache.maven.index_shaded.lucene.search.BooleanClause;
import org.apache.maven.index_shaded.lucene.search.BooleanQuery;
import org.apache.maven.index_shaded.lucene.search.Query;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class MavenIndexManagerTest {

    @Inject
    ArchivaRepositoryRegistry repositoryRegistry;


    private Path indexPath;
    private MavenManagedRepository repository;
    private ArchivaIndexingContext ctx;
    private MavenRemoteRepository repositoryRemote;

    @Inject
    MavenIndexManager mavenIndexManager;

    @Inject
    QueryCreator queryCreator;


    @After
    public void tearDown() {
        repositoryRegistry.destroy();
        if (ctx!=null) {
            try {
                ctx.close(true);
            } catch (IOException e) {
                //
            }
        }
        if (indexPath!=null && Files.exists(indexPath)) {
            FileUtils.deleteQuietly(indexPath);
        }

    }

    @Test
    public void pack() throws Exception {
        createTestContext();
        Path destDir = repository.getAsset( "" ).getFilePath().resolve("org/apache/archiva/archiva-webapp/1.0");
        Path srcDir = Paths.get("src/test/maven-search-test-repo/org/apache/archiva/archiva-webapp/1.0");
        org.apache.commons.io.FileUtils.copyDirectory(srcDir.toFile(),destDir.toFile());
        mavenIndexManager.scan(ctx);
        mavenIndexManager.pack(ctx);
        assertTrue(Files.list(indexPath).filter(path -> {
            try {
                return path.getFileName().toString().endsWith(".gz") && Files.size(path) > 0;
            } catch (IOException e) {
                return false;
            }
        }).findAny().isPresent());
    }

    @Test
    public void scan() throws Exception {
        createTestContext();
        Path destDir = repository.getAsset("").getFilePath().resolve("org/apache/archiva/archiva-webapp/1.0");
        Path srcDir = Paths.get("src/test/maven-search-test-repo/org/apache/archiva/archiva-webapp/1.0");
        org.apache.commons.io.FileUtils.copyDirectory(srcDir.toFile(),destDir.toFile());
        mavenIndexManager.scan(ctx);

        IndexingContext mvnCtx = mavenIndexManager.getMvnContext(ctx);
        String term = "org.apache.archiva";
        Query q = new BooleanQuery.Builder().add( queryCreator.constructQuery( MAVEN.GROUP_ID, new UserInputSearchExpression( term ) ),
                BooleanClause.Occur.SHOULD ).build();
        assertEquals(4, mvnCtx.acquireIndexSearcher().count(q));
    }

    /*
     * Does only a index update via file uri, no HTTP uri
     */
    @Test
    public void update() throws Exception {
        createTestContext();
        mavenIndexManager.pack(ctx);
        ctx.close(false);
        createTestContextForRemote();
        mavenIndexManager.update(ctx, true);
    }

    @Test
    public void addArtifactsToIndex() throws Exception {

        ArchivaIndexingContext ctx = createTestContext();
        try {
            Path destDir = repository.getAsset("").getFilePath().resolve("org/apache/archiva/archiva-search/1.0");
            Path srcDir = Paths.get("src/test/maven-search-test-repo/org/apache/archiva/archiva-search/1.0");
            org.apache.commons.io.FileUtils.copyDirectory(srcDir.toFile(), destDir.toFile());
            List<URI> uriList = new ArrayList<>();
            uriList.add(destDir.resolve("archiva-search-1.0.jar").toUri());
            uriList.add(destDir.resolve("archiva-search-1.0-sources.jar").toUri());
            mavenIndexManager.addArtifactsToIndex(ctx, uriList);

            IndexingContext mvnCtx = mavenIndexManager.getMvnContext(ctx);
            String term = "org.apache.archiva";
            Query q = new BooleanQuery.Builder().add(queryCreator.constructQuery(MAVEN.GROUP_ID, new UserInputSearchExpression(term)),
                    BooleanClause.Occur.SHOULD).build();
            assertEquals(2, mvnCtx.acquireIndexSearcher().count(q));
        } finally {
            try {
                ctx.close(true);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    public void removeArtifactsFromIndex() throws Exception {
        ArchivaIndexingContext ctx = createTestContext();
        Path destDir = repository.getAsset( "" ).getFilePath().resolve("org/apache/archiva/archiva-search/1.0");
        Path srcDir = Paths.get("src/test/maven-search-test-repo/org/apache/archiva/archiva-search/1.0");
        org.apache.commons.io.FileUtils.copyDirectory(srcDir.toFile(), destDir.toFile());
        List<URI> uriList = new ArrayList<>();
        uriList.add(destDir.resolve("archiva-search-1.0.jar").toUri());
        uriList.add(destDir.resolve("archiva-search-1.0-sources.jar").toUri());
        mavenIndexManager.addArtifactsToIndex(ctx, uriList);

        IndexingContext mvnCtx = mavenIndexManager.getMvnContext(ctx);
        String term = "org.apache.archiva";
        Query q = new BooleanQuery.Builder().add( queryCreator.constructQuery( MAVEN.GROUP_ID, new UserInputSearchExpression( term ) ),
                BooleanClause.Occur.SHOULD ).build();
        assertEquals(2, mvnCtx.acquireIndexSearcher().count(q));
        uriList.remove(0);
        mavenIndexManager.removeArtifactsFromIndex(ctx, uriList);
        assertEquals(1, mvnCtx.acquireIndexSearcher().count(q));
    }

    @Test
    public void supportsRepository() throws Exception {
        assertTrue(mavenIndexManager.supportsRepository(RepositoryType.MAVEN));
        assertFalse(mavenIndexManager.supportsRepository(RepositoryType.NPM));
    }

    private ArchivaIndexingContext createTestContext() throws URISyntaxException, IndexCreationFailedException, IOException {
        String indexPathName = ".index-test." + System.nanoTime();
        indexPath = Paths.get("target/repositories/test-repo" ).resolve(indexPathName);
        if (Files.exists(indexPath)) {

            try {
                FileUtils.deleteDirectory(indexPath);
            } catch (IOException e) {
                String destName = indexPath.getFileName().toString() + "." + System.currentTimeMillis();
                Files.move(indexPath, indexPath.getParent().resolve(destName));
            }
        }
        repository = MavenManagedRepository.newLocalInstance("test-repo", "Test Repo", Paths.get("target/repositories"));
        // repository.setLocation(new URI("test-repo"));
        IndexCreationFeature icf = repository.getFeature(IndexCreationFeature.class).get();
        icf.setIndexPath(new URI(indexPathName));
        ctx = mavenIndexManager.createContext(repository);
        return ctx;
    }

    private ArchivaIndexingContext createTestContextForRemote() throws URISyntaxException, IndexCreationFailedException, IOException {
        // indexPath = Paths.get("target/repositories/test-repo/.index-test");
        Path repoPath = Paths.get("target/repositories").toAbsolutePath();
        repositoryRemote = MavenRemoteRepository.newLocalInstance("test-repo", "Test Repo", repoPath);
        repositoryRemote.setLocation(repoPath.resolve("test-repo").toUri());
        RemoteIndexFeature icf = repositoryRemote.getFeature(RemoteIndexFeature.class).get();
        icf.setIndexUri(new URI(indexPath.getFileName().toString()));
        ctx = mavenIndexManager.createContext(repositoryRemote);
        return ctx;
    }

    @Test
    public void createContext() throws Exception {
        ArchivaIndexingContext ctx = createTestContext();
        assertNotNull(ctx);
        assertEquals(repository, ctx.getRepository());
        assertEquals("test-repo", ctx.getId());
        assertEquals(indexPath.toAbsolutePath(), ctx.getPath().getFilePath().toAbsolutePath());
        assertTrue(Files.exists(indexPath));
        List<Path> li = Files.list(indexPath).collect(Collectors.toList());
        assertTrue(li.size()>0);

    }

}