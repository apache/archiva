package org.apache.archiva.repository.base;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test for RepositoryRegistry
 */
@RunWith(ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
public class ArchivaRepositoryRegistryTest
{

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    private static final Path userCfg = Paths.get(System.getProperty( "user.home" ), ".m2/archiva.xml");

    private static Path cfgCopy;
    private static Path archivaCfg;

    @BeforeClass
    public static void classSetup() throws IOException, URISyntaxException
    {
        URL archivaCfgUri = Thread.currentThread().getContextClassLoader().getResource( "archiva.xml" );
        if (archivaCfgUri!=null) {
            archivaCfg = Paths.get(archivaCfgUri.toURI());
            cfgCopy = Files.createTempFile( "archiva-backup", ".xml" );
            Files.copy( archivaCfg, cfgCopy, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterClass
    public static void classTearDown() throws IOException
    {
        if (cfgCopy!=null) {
            Files.deleteIfExists( cfgCopy );
        }
    }

    @Before
    public void setUp( ) throws Exception
    {
        assertNotNull( repositoryRegistry );
        Files.deleteIfExists( userCfg );
        URL archivaCfgUri = Thread.currentThread().getContextClassLoader().getResource( "archiva.xml" );
        if (archivaCfgUri!=null) {
            archivaCfg = Paths.get(archivaCfgUri.toURI());
            if (Files.exists(cfgCopy))
            {
                Files.copy( cfgCopy, archivaCfg , StandardCopyOption.REPLACE_EXISTING);
            }
        }
        archivaConfiguration.reload();
        repositoryRegistry.reload();
    }

    @After
    public void tearDown( ) throws Exception
    {
        Files.deleteIfExists( userCfg );
        if (cfgCopy!=null && Files.exists(cfgCopy)) {
            Files.copy(cfgCopy, archivaCfg, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void getRepositories( ) throws Exception
    {
        Collection<Repository> repos = repositoryRegistry.getRepositories( );
        assertEquals( 5, repos.size( ) );
        assertTrue(repos.stream().anyMatch( rep -> rep.getId().equals("internal") ));
        assertTrue( repos.stream( ).anyMatch( rep -> rep.getId( ).equals( "snapshots") ) );
        assertTrue(repos.stream().anyMatch( rep -> rep.getId().equals( "central") ));
    }

    @Test
    public void getManagedRepositories( ) throws Exception
    {
        Collection<ManagedRepository> repos = repositoryRegistry.getManagedRepositories();
        assertEquals( 4, repos.size( ) );
        assertTrue(repos.stream().anyMatch( rep -> rep.getId().equals("internal") ));
        assertTrue( repos.stream( ).anyMatch( rep -> rep.getId( ).equals( "snapshots") ) );
    }

    @Test
    public void getRemoteRepositories( ) throws Exception
    {
        Collection<RemoteRepository> repos = repositoryRegistry.getRemoteRepositories( );
        assertEquals( 1, repos.size( ) );
        assertTrue(repos.stream().anyMatch( rep -> rep.getId().equals( "central") ));
    }

    @Test
    public void getRepository( ) throws Exception
    {
        Repository repo = repositoryRegistry.getRepository( "internal" );
        assertNotNull(repo);
        assertEquals("internal", repo.getId());
        assertEquals("Archiva Managed Internal Repository", repo.getName());
        assertEquals("This is internal repository.", repo.getDescription());
        assertEquals( "default", repo.getLayout( ) );
        assertEquals("0 0 * * * ?", repo.getSchedulingDefinition());
        assertTrue(repo instanceof ManagedRepository);
        assertTrue( repo.hasIndex( ) );
        assertTrue(repo.isScanned());
        Assert.assertEquals( RepositoryType.MAVEN, repo.getType());
    }

    @Test
    public void getManagedRepository( ) throws Exception
    {
        ManagedRepository repo = repositoryRegistry.getManagedRepository( "internal" );
        assertNotNull(repo);
        assertEquals("internal", repo.getId());
        assertEquals("Archiva Managed Internal Repository", repo.getName());
        assertEquals("This is internal repository.", repo.getDescription());
        assertEquals( "default", repo.getLayout( ) );
        assertEquals("0 0 * * * ?", repo.getSchedulingDefinition());
        assertTrue( repo.hasIndex( ) );
        assertTrue(repo.isScanned());
        assertEquals(RepositoryType.MAVEN, repo.getType());
        assertTrue(repo.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE));
        assertFalse( repo.getActiveReleaseSchemes( ).contains( ReleaseScheme.SNAPSHOT ) );
        assertNotNull(repo.getContent());

        assertNull(repositoryRegistry.getManagedRepository( "xyu" ));

    }

    @Test
    public void getRemoteRepository( ) throws Exception
    {
        RemoteRepository repo = repositoryRegistry.getRemoteRepository( "central" );
        assertNotNull(repo);
        assertEquals("central", repo.getId());
        assertEquals("Central Repository", repo.getName());
        assertEquals("", repo.getDescription());
        assertEquals( "default", repo.getLayout( ) );
        assertEquals("0 0 08 ? * SUN", repo.getSchedulingDefinition());
        assertTrue( repo.hasIndex( ) );
        assertTrue(repo.isScanned());
        assertEquals(RepositoryType.MAVEN, repo.getType());

        assertEquals(35, repo.getTimeout().getSeconds());
    }

    @Test
    public void putManagedRepository( ) throws Exception
    {
        BasicManagedRepository managedRepository = BasicManagedRepository.newFilesystemInstance("test001", "Test repo", archivaConfiguration.getRepositoryBaseDir().resolve("test001"));
        managedRepository.setDescription( managedRepository.getPrimaryLocale(), "This is just a test" );
        repositoryRegistry.putRepository(managedRepository);

        assertNotNull(managedRepository.getContent());
        assertEquals(6, repositoryRegistry.getRepositories().size());

        managedRepository = BasicManagedRepository.newFilesystemInstance("central", "Test repo", archivaConfiguration.getRepositoryBaseDir().resolve("central"));
        managedRepository.setDescription( managedRepository.getPrimaryLocale(), "This is just a test" );
        ManagedRepository updatedRepo = null;
        try {
            repositoryRegistry.putRepository( managedRepository );
            throw new RuntimeException("Repository exception should be thrown, if there exists a remote repository already with that id");
        } catch ( RepositoryException e) {
            // OK
        }
        managedRepository = BasicManagedRepository.newFilesystemInstance("internal", "Test repo", archivaConfiguration.getRepositoryBaseDir().resolve("internal"));
        managedRepository.setDescription( managedRepository.getPrimaryLocale(), "This is just a test" );
        updatedRepo = repositoryRegistry.putRepository( managedRepository );

        assertTrue(updatedRepo==managedRepository);
        assertNotNull(managedRepository.getContent());
        assertEquals(6, repositoryRegistry.getRepositories().size());
        ManagedRepository managedRepository1 = repositoryRegistry.getManagedRepository( "internal" );
        assertEquals("Test repo", managedRepository1.getName());
        assertTrue(managedRepository1==managedRepository);

    }

    @Test
    public void putManagedRepositoryFromConfig( ) throws Exception
    {
        ManagedRepositoryConfiguration cfg = new ManagedRepositoryConfiguration();
        cfg.setId("test002");
        cfg.setName("This is test 002");
        ManagedRepository repo = repositoryRegistry.putRepository( cfg );
        assertNotNull(repo);
        assertEquals("test002", repo.getId());
        assertEquals("This is test 002", repo.getName());
        assertNotNull(repo.getContent());
        archivaConfiguration.reload();
        Collection<ManagedRepository> repos = repositoryRegistry.getManagedRepositories();
        assertEquals(5, repos.size());

        ManagedRepository internalRepo = repositoryRegistry.getManagedRepository( "internal" );
        cfg = new ManagedRepositoryConfiguration();
        cfg.setId("internal");
        cfg.setName("This is internal test 002");
        repo = repositoryRegistry.putRepository( cfg );
        assertTrue(internalRepo==repo);
        assertEquals("This is internal test 002",repo.getName());
        assertEquals(5, repositoryRegistry.getManagedRepositories().size());

        repositoryRegistry.reload();
        assertEquals(5, repositoryRegistry.getManagedRepositories().size());

    }

    @Test
    public void putManagedRepositoryFromConfigWithoutSave( ) throws Exception
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration cfg = new ManagedRepositoryConfiguration();
        cfg.setId("test002");
        cfg.setName("This is test 002");
        ManagedRepository repo = repositoryRegistry.putRepository( cfg, configuration );
        assertNotNull(repo);
        assertEquals("test002", repo.getId());
        assertEquals("This is test 002", repo.getName());
        assertNotNull(repo.getContent());
        archivaConfiguration.reload();
        assertEquals(3, archivaConfiguration.getConfiguration().getManagedRepositories().size());
        Collection<ManagedRepository> repos = repositoryRegistry.getManagedRepositories();
        assertEquals(5, repos.size());

        ManagedRepository internalRepo = repositoryRegistry.getManagedRepository( "internal" );
        cfg = new ManagedRepositoryConfiguration();
        cfg.setId("internal");
        cfg.setName("This is internal test 002");
        repo = repositoryRegistry.putRepository( cfg, configuration );
        assertTrue(internalRepo==repo);
        assertEquals("This is internal test 002",repo.getName());
        assertEquals(5, repositoryRegistry.getManagedRepositories().size());

        repositoryRegistry.reload();
        assertEquals(4, repositoryRegistry.getManagedRepositories().size());
    }

    @Test
    public void putRemoteRepository( ) throws Exception
    {
        BasicRemoteRepository remoteRepository = BasicRemoteRepository.newFilesystemInstance( "test001", "Test repo", archivaConfiguration.getRemoteRepositoryBaseDir() );
        remoteRepository.setDescription( remoteRepository.getPrimaryLocale(), "This is just a test" );
        RemoteRepository newRepo = repositoryRegistry.putRepository(remoteRepository);

        assertTrue(remoteRepository==newRepo);
        assertNotNull(remoteRepository.getContent());
        assertEquals(6, repositoryRegistry.getRepositories().size());

        remoteRepository = BasicRemoteRepository.newFilesystemInstance( "internal", "Test repo", archivaConfiguration.getRemoteRepositoryBaseDir() );
        remoteRepository.setDescription( remoteRepository.getPrimaryLocale(), "This is just a test" );
        RemoteRepository updatedRepo = null;
        try
        {
            updatedRepo = repositoryRegistry.putRepository( remoteRepository );
            throw new RuntimeException("Should throw repository exception, if repository exists already and is not the same type.");
        } catch (RepositoryException e) {
            // OK
        }

        remoteRepository = BasicRemoteRepository.newFilesystemInstance( "central", "Test repo", archivaConfiguration.getRemoteRepositoryBaseDir() );
        remoteRepository.setDescription( remoteRepository.getPrimaryLocale(), "This is just a test" );
        updatedRepo = repositoryRegistry.putRepository( remoteRepository );

        assertTrue(updatedRepo==remoteRepository);
        assertNotNull(remoteRepository.getContent());
        assertEquals(6, repositoryRegistry.getRepositories().size());
        RemoteRepository remoteRepository1 = repositoryRegistry.getRemoteRepository( "central" );
        assertEquals("Test repo", remoteRepository1.getName());
        assertTrue(remoteRepository1==remoteRepository);
    }

    @Test
    public void putRemoteRepositoryFromConfig( ) throws Exception
    {
        RemoteRepositoryConfiguration cfg = new RemoteRepositoryConfiguration();
        cfg.setId("test002");
        cfg.setName("This is test 002");
        RemoteRepository repo = repositoryRegistry.putRepository( cfg );
        assertNotNull(repo);
        assertEquals("test002", repo.getId());
        assertEquals("This is test 002", repo.getName());
        assertNotNull(repo.getContent());
        archivaConfiguration.reload();
        Collection<RemoteRepository> repos = repositoryRegistry.getRemoteRepositories();
        assertEquals(2, repos.size());

        RemoteRepository internalRepo = repositoryRegistry.getRemoteRepository( "central" );
        cfg = new RemoteRepositoryConfiguration();
        cfg.setId("central");
        cfg.setName("This is central test 002");
        repo = repositoryRegistry.putRepository( cfg );
        assertTrue(internalRepo==repo);
        assertEquals("This is central test 002",repo.getName());
        assertEquals(2, repositoryRegistry.getRemoteRepositories().size());

        repositoryRegistry.reload();
        assertEquals(2, repositoryRegistry.getRemoteRepositories().size());
    }

    @Test
    public void putRemoteRepositoryFromConfigWithoutSave( ) throws Exception
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        RemoteRepositoryConfiguration cfg = new RemoteRepositoryConfiguration();
        cfg.setId("test002");
        cfg.setName("This is test 002");
        RemoteRepository repo = repositoryRegistry.putRepository( cfg, configuration );
        assertNotNull(repo);
        assertEquals("test002", repo.getId());
        assertEquals("This is test 002", repo.getName());
        assertNotNull(repo.getContent());
        archivaConfiguration.reload();
        assertEquals(1, archivaConfiguration.getConfiguration().getRemoteRepositories().size());
        Collection<RemoteRepository> repos = repositoryRegistry.getRemoteRepositories();
        assertEquals(2, repos.size());

        RemoteRepository internalRepo = repositoryRegistry.getRemoteRepository( "central" );
        cfg = new RemoteRepositoryConfiguration();
        cfg.setId("central");
        cfg.setName("This is central test 002");
        repo = repositoryRegistry.putRepository( cfg, configuration );
        assertTrue(internalRepo==repo);
        assertEquals("This is central test 002",repo.getName());
        assertEquals(2, repositoryRegistry.getRemoteRepositories().size());

        repositoryRegistry.reload();
        assertEquals(1, repositoryRegistry.getRemoteRepositories().size());
    }

    @Test
    public void removeRepository( ) throws Exception
    {
        assertEquals(5, repositoryRegistry.getRepositories().size());
        Repository repo = repositoryRegistry.getRepository( "snapshots" );
        repositoryRegistry.removeRepository( repo );
        assertEquals(4, repositoryRegistry.getRepositories().size());
        assertTrue( repositoryRegistry.getRepositories( ).stream( ).noneMatch( rep -> rep.getId( ).equals( "snapshots" ) ) );
        archivaConfiguration.reload();
        repositoryRegistry.reload();
        assertEquals(4, repositoryRegistry.getRepositories().size());
    }

    @Test
    public void removeManagedRepository( ) throws Exception
    {

        assertEquals(4, repositoryRegistry.getManagedRepositories().size());
        ManagedRepository repo = repositoryRegistry.getManagedRepository( "snapshots" );
        repositoryRegistry.removeRepository( repo );
        assertEquals(3, repositoryRegistry.getManagedRepositories().size());
        assertTrue( repositoryRegistry.getManagedRepositories( ).stream( ).noneMatch( rep -> rep.getId( ).equals( "snapshots" ) ) );
        archivaConfiguration.reload();
        repositoryRegistry.reload();
        assertEquals(3, repositoryRegistry.getManagedRepositories().size());
    }

    @Test
    public void removeManagedRepositoryWithoutSave( ) throws Exception
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertEquals(4, repositoryRegistry.getManagedRepositories().size());
        ManagedRepository repo = repositoryRegistry.getManagedRepository( "snapshots" );
        repositoryRegistry.removeRepository( repo, configuration );
        assertEquals(3, repositoryRegistry.getManagedRepositories().size());
        assertTrue( repositoryRegistry.getManagedRepositories( ).stream( ).noneMatch( rep -> rep.getId( ).equals( "snapshots" ) ) );
        archivaConfiguration.reload();
        repositoryRegistry.reload();
        assertEquals(4, repositoryRegistry.getManagedRepositories().size());
    }


    @Test
    public void removeRemoteRepository( ) throws Exception
    {
        assertEquals(1, repositoryRegistry.getRemoteRepositories().size());
        RemoteRepository repo = repositoryRegistry.getRemoteRepository( "central" );
        repositoryRegistry.removeRepository( repo );
        assertEquals(0, repositoryRegistry.getRemoteRepositories().size());
        assertTrue( repositoryRegistry.getRemoteRepositories( ).stream( ).noneMatch( rep -> rep.getId( ).equals( "central" ) ) );
        archivaConfiguration.reload();
        repositoryRegistry.reload();
        assertEquals(0, repositoryRegistry.getRemoteRepositories().size());
    }

    @Test
    public void removeRemoteRepositoryWithoutSave( ) throws Exception
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertEquals(1, repositoryRegistry.getRemoteRepositories().size());
        RemoteRepository repo = repositoryRegistry.getRemoteRepository( "central" );
        repositoryRegistry.removeRepository( repo, configuration );
        assertEquals(0, repositoryRegistry.getRemoteRepositories().size());
        assertTrue( repositoryRegistry.getRemoteRepositories( ).stream( ).noneMatch( rep -> rep.getId( ).equals( "central" ) ) );
        archivaConfiguration.reload();
        repositoryRegistry.reload();
        assertEquals(1, repositoryRegistry.getRemoteRepositories().size());
    }


    @Test
    public void cloneManagedRepo( ) throws Exception
    {
        ManagedRepository managedRepository = repositoryRegistry.getManagedRepository( "internal" );

        try
        {
            repositoryRegistry.clone(managedRepository, "snapshots");
            throw new RuntimeException("RepositoryRegistry exception should be thrown if id exists already.");
        }
        catch ( RepositoryException e )
        {
            // OK
        }

        try
        {
            repositoryRegistry.clone(managedRepository, "central");
            throw new RuntimeException("RepositoryRegistry exception should be thrown if id exists already.");
        }
        catch ( RepositoryException e )
        {
            // OK
        }

        ManagedRepository clone = repositoryRegistry.clone( managedRepository, "newinternal" );
        assertNotNull(clone);
        assertNull(clone.getContent());
        assertEquals("Archiva Managed Internal Repository", clone.getName());
        assertFalse(managedRepository==clone);

    }

    @Test
    public void cloneRemoteRepo( ) throws Exception
    {
        RemoteRepository remoteRepository = repositoryRegistry.getRemoteRepository( "central" );

        try
        {
            repositoryRegistry.clone(remoteRepository, "snapshots");
            throw new RuntimeException("RepositoryRegistry exception should be thrown if id exists already.");
        }
        catch ( RepositoryException e )
        {
            // OK
        }

        try
        {
            repositoryRegistry.clone(remoteRepository, "central");
            throw new RuntimeException("RepositoryRegistry exception should be thrown if id exists already.");
        }
        catch ( RepositoryException e )
        {
            // OK
        }

        RemoteRepository clone = repositoryRegistry.clone( remoteRepository, "newCentral" );
        assertNotNull(clone);
        assertNull(clone.getContent());
        assertEquals("Central Repository", clone.getName());
        assertFalse(remoteRepository==clone);

    }

}