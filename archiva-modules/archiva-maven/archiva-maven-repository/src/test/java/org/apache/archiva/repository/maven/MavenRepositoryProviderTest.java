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
import org.apache.archiva.configuration.ArchivaRuntimeConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.repository.maven.metadata.storage.mock.MockConfiguration;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.repository.base.PasswordCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class MavenRepositoryProviderTest
{

    MavenRepositoryProvider provider;
    RepositoryRegistry reg;

    Path repoLocation;


    @Before
    public void setUp()
        throws Exception
    {
        provider = new MavenRepositoryProvider( );
        MockConfiguration mockConfiguration =new MockConfiguration();
        mockConfiguration.getConfiguration().setArchivaRuntimeConfiguration( new ArchivaRuntimeConfiguration() );
        mockConfiguration.getConfiguration().getArchivaRuntimeConfiguration().setRepositoryBaseDirectory( "repositories" );
        provider.setArchivaConfiguration( mockConfiguration );

    }

    @After
    public void cleanUp() {
        if (repoLocation!=null && Files.exists( repoLocation )) {
            FileUtils.deleteQuietly( repoLocation );
        }
    }

    @Test
    public void provides( ) throws Exception
    {
        assertEquals(1, provider.provides().size());
        assertEquals( RepositoryType.MAVEN, provider.provides().iterator().next());
    }

    @Test
    public void createManagedInstance( ) throws Exception
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration( );
        repo.setId("testm001");
        repo.setName("Managed Test Repo 001");
        repo.setDescription( "This is a managed test" );
        repo.setRetentionPeriod( 37 );
        repoLocation = Files.createTempDirectory( "test-repo-001");
        repo.setLocation( repoLocation.toAbsolutePath().toString() );
        repo.setSnapshots( true );
        repo.setReleases( true );
        repo.setRefreshCronExpression( "4 0 0 ? * TUE" );
        repo.setScanned( true );
        repo.setBlockRedeployments( true );
        repo.setDeleteReleasedSnapshots( true );
        repo.setRetentionCount( 33 );
        repo.setSkipPackedIndexCreation( true );
        repo.setStageRepoNeeded( true );
        repo.setIndexDir( "testmanaged/.index" );
        repo.setLayout( "maven2" );
        repo.setType( RepositoryType.MAVEN.toString() );


        ManagedRepository mr = provider.createManagedInstance( repo );
        assertNotNull(mr.getLocation());
        String repoUri = repoLocation.toUri().toString();
        assertTrue(Files.exists(repoLocation));
        repoUri = repoUri.substring( 0, repoUri.length()-1 );
        assertEquals(repoUri, mr.getLocation().toString());
        assertEquals("This is a managed test", mr.getDescription());
        assertEquals("Managed Test Repo 001", mr.getName());
        assertEquals(2, mr.getActiveReleaseSchemes().size());
        assertTrue( mr.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE ));
        assertTrue( mr.getActiveReleaseSchemes().contains( ReleaseScheme.SNAPSHOT));
        assertEquals("testm001", mr.getId());
        assertTrue(mr.blocksRedeployments());
        assertEquals("4 0 0 ? * TUE", mr.getSchedulingDefinition());
        assertTrue(mr.isScanned());
        ArtifactCleanupFeature artifactCleanupFeature = mr.getFeature( ArtifactCleanupFeature.class ).get();
        assertEquals( Period.ofDays( 37), artifactCleanupFeature.getRetentionPeriod());
        assertTrue(artifactCleanupFeature.isDeleteReleasedSnapshots());
        assertEquals(33, artifactCleanupFeature.getRetentionCount());

        IndexCreationFeature indexCreationFeature = mr.getFeature( IndexCreationFeature.class ).get();
        assertNotNull(indexCreationFeature.getIndexPath());
        assertEquals("testmanaged/.index", indexCreationFeature.getIndexPath().toString());
        assertFalse(indexCreationFeature.getIndexPath().isAbsolute());
        assertTrue(indexCreationFeature.isSkipPackedIndexCreation());

        StagingRepositoryFeature stagingRepositoryFeature = mr.getFeature( StagingRepositoryFeature.class ).get();
        assertTrue(stagingRepositoryFeature.isStageRepoNeeded());
        assertNull(stagingRepositoryFeature.getStagingRepository());


    }

    @Test
    public void createRemoteInstance( ) throws Exception
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration( );
        repo.setUsername("testuser001");
        repo.setPassword( "pwd0000abc" );
        repo.setCheckPath( "test/check.html" );
        repo.setTimeout( 50 );
        repo.setUrl( "https://repo.maven.apache.org/maven2/test" );
        repo.setDownloadRemoteIndex( true );
        repo.setDownloadRemoteIndexOnStartup( true );
        Map<String,String> header = new HashMap<>(  );
        header.put("header1","value1");
        header.put("header2","value2");
        repo.setExtraHeaders( header );
        Map<String,String> params = new HashMap<>(  );
        params.put("param1","pval1");
        params.put("param2","pval2");
        repo.setExtraParameters( params );
        repo.setRefreshCronExpression( "0 1 07 ? * MON" );
        repo.setRemoteDownloadTimeout( 333 );
        repo.setRemoteIndexUrl( "testremote/.index" );
        repo.setDescription( "This is a test" );
        repo.setId( "test001" );
        repo.setName( "Remote Test Repo 001" );
        repo.setIndexDir( "testindex/.index" );
        repo.setLayout( "maven2" );
        repo.setType( RepositoryType.MAVEN.toString() );
        repo.setIndexDir( "local/.index" );

        RemoteRepository mr = provider.createRemoteInstance( repo );
        assertEquals("test001", mr.getId());
        assertEquals("This is a test", mr.getDescription());
        assertNotNull(mr.getLocation());
        assertEquals("https://repo.maven.apache.org/maven2/test", mr.getLocation().toString());
        assertEquals("Remote Test Repo 001", mr.getName());
        assertEquals("test001", mr.getId());
        assertEquals("0 1 07 ? * MON", mr.getSchedulingDefinition());
        assertEquals(50, mr.getTimeout().get( ChronoUnit.SECONDS ));
        assertTrue(mr.isScanned());
        assertNotNull(mr.getLoginCredentials());
        assertTrue(mr.getLoginCredentials() instanceof PasswordCredentials );
        PasswordCredentials creds = (PasswordCredentials) mr.getLoginCredentials();
        assertEquals("testuser001", creds.getUsername());
        assertEquals("pwd0000abc", new String(creds.getPassword()));
        assertEquals("value1", mr.getExtraHeaders().get("header1"));
        assertEquals("pval2", mr.getExtraParameters().get("param2"));
        assertEquals( "maven2", mr.getLayout());
        try
        {
            ArtifactCleanupFeature artifactCleanupFeature = mr.getFeature( ArtifactCleanupFeature.class ).get( );
            throw new Exception("artifactCleanupFeature should not be available");
        } catch ( UnsupportedFeatureException e ) {
            // correct
        }

        IndexCreationFeature indexCreationFeature = mr.getFeature( IndexCreationFeature.class ).get( );
        assertEquals("local/.index", indexCreationFeature.getIndexPath().toString());
        try
        {
            StagingRepositoryFeature stagingRepositoryFeature = mr.getFeature( StagingRepositoryFeature.class ).get( );
            throw new Exception("stagingRepositoryFeature should not be available");
        } catch (UnsupportedFeatureException e) {
            // correct
        }
        RemoteIndexFeature remoteIndexFeature = mr.getFeature( RemoteIndexFeature.class ).get();
        assertNull(remoteIndexFeature.getProxyId());
    }

    @Test
    public void getManagedConfiguration() throws Exception {
        MavenManagedRepository repo = MavenManagedRepository.newLocalInstance( "test01", "My Test repo", Paths.get("target/repositories") );

        repo.setLocation( new URI("target/this.is/a/test") );
        repo.setScanned( true );
        repo.setDescription( repo.getPrimaryLocale(), "This is a description" );
        repo.setLayout( "maven2" );
        repo.setBlocksRedeployment( true );
        repo.setName( repo.getPrimaryLocale(), "test0002" );
        repo.setSchedulingDefinition( "0 0 05 ? * WED" );
        repo.addActiveReleaseScheme( ReleaseScheme.RELEASE );
        repo.addActiveReleaseScheme( ReleaseScheme.SNAPSHOT );
        StagingRepositoryFeature stagingFeat = repo.getFeature( StagingRepositoryFeature.class ).get( );
        stagingFeat.setStageRepoNeeded( true );
        IndexCreationFeature indexCreationFeature = repo.getFeature( IndexCreationFeature.class ).get();
        indexCreationFeature.setIndexPath( new URI("test/.indexes") );
        indexCreationFeature.setSkipPackedIndexCreation( true );
        ArtifactCleanupFeature artifactCleanupFeature = repo.getFeature( ArtifactCleanupFeature.class ).get();
        artifactCleanupFeature.setRetentionPeriod( Period.ofDays( 5 ) );
        artifactCleanupFeature.setRetentionCount( 7 );
        artifactCleanupFeature.setDeleteReleasedSnapshots( true );

        ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration( repo );
        assertEquals("target/this.is/a/test", cfg.getLocation());
        assertTrue(cfg.isScanned());
        assertEquals( "This is a description", cfg.getDescription() );
        assertEquals("maven2", cfg.getLayout());
        assertTrue(cfg.isBlockRedeployments());
        assertEquals("test0002", cfg.getName());
        assertEquals("0 0 05 ? * WED", cfg.getRefreshCronExpression());
        assertTrue(cfg.isStageRepoNeeded());
        assertEquals("test/.indexes", cfg.getIndexDir());
        assertTrue(cfg.isSkipPackedIndexCreation());
        assertEquals(5, cfg.getRetentionPeriod());
        assertEquals(7, cfg.getRetentionCount());
        assertTrue(cfg.isDeleteReleasedSnapshots());
        assertTrue(cfg.isReleases());
        assertTrue(cfg.isSnapshots());
        assertTrue(cfg.isScanned());



    }

    @Test
    public void getRemoteConfiguration() throws Exception {
        MavenRemoteRepository repo = MavenRemoteRepository.newLocalInstance( "test01", "My Test repo", Paths.get("target/remotes") );

        repo.setLocation( new URI("https://this.is/a/test") );
        repo.setScanned( true );
        repo.setDescription( repo.getPrimaryLocale(), "This is a description" );
        repo.setLayout( "maven2" );
        repo.setName( repo.getPrimaryLocale(), "test0003" );
        repo.setSchedulingDefinition( "0 0 05 ? * WED" );
        RemoteIndexFeature remoteIndexFeature = repo.getFeature( RemoteIndexFeature.class ).get();
        remoteIndexFeature.setProxyId( "proxyabc" );
        remoteIndexFeature.setDownloadTimeout( Duration.ofSeconds( 54 ) );
        remoteIndexFeature.setDownloadRemoteIndex( false );
        remoteIndexFeature.setIndexUri( new URI("/this/remote/.index") );
        remoteIndexFeature.setDownloadRemoteIndexOnStartup( true );
        IndexCreationFeature indexCreationFeature = repo.getFeature( IndexCreationFeature.class ).get();
        indexCreationFeature.setIndexPath( new URI("/this/local/.index") );

        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration( repo );
        assertEquals("https://this.is/a/test", cfg.getUrl());
        assertEquals( "This is a description", cfg.getDescription() );
        assertEquals("maven2", cfg.getLayout());
        assertEquals("test0003", cfg.getName());
        assertEquals("0 0 05 ? * WED", cfg.getRefreshCronExpression());
        assertEquals("/this/remote/.index", cfg.getRemoteIndexUrl());
        assertEquals("proxyabc", cfg.getRemoteDownloadNetworkProxyId());
        assertEquals(54, cfg.getRemoteDownloadTimeout());
        assertFalse(cfg.isDownloadRemoteIndex());
        assertTrue(cfg.isDownloadRemoteIndexOnStartup());
        assertEquals("/this/local/.index", cfg.getIndexDir());


    }

    @Test
    public void getRepositoryGroupConfiguration() throws RepositoryException, URISyntaxException, IOException {
        MavenRepositoryGroup repositoryGroup = MavenRepositoryGroup.newLocalInstance("group1","group1",Paths.get("target/groups"));
        MavenManagedRepository repo1 = MavenManagedRepository.newLocalInstance( "test01", "My Test repo", Paths.get("target/repositories") );
        MavenManagedRepository repo2 = MavenManagedRepository.newLocalInstance( "test02", "My Test repo", Paths.get("target/repositories") );


        repositoryGroup.setDescription(repositoryGroup.getPrimaryLocale(), "Repository group");
        repositoryGroup.setLayout("non-default");
        IndexCreationFeature indexCreationFeature = repositoryGroup.getFeature( IndexCreationFeature.class ).get();
        indexCreationFeature.setIndexPath( new URI(".index2") );
        repositoryGroup.setName(repositoryGroup.getPrimaryLocale(), "Repo Group 1");
        repositoryGroup.setMergedIndexTTL(1005);
        repositoryGroup.setSchedulingDefinition("0 0 04 ? * THU");
        repositoryGroup.addRepository(repo1);
        repositoryGroup.addRepository(repo2);


        RepositoryGroupConfiguration cfg = provider.getRepositoryGroupConfiguration(repositoryGroup);
        assertEquals("group1", cfg.getId());
        assertEquals(".index2", cfg.getMergedIndexPath());
        assertEquals("0 0 04 ? * THU", cfg.getCronExpression());
        assertEquals("Repo Group 1", cfg.getName());
        assertEquals(1005, cfg.getMergedIndexTtl());
        assertTrue(cfg.getRepositories().contains("test01"));
        assertTrue(cfg.getRepositories().contains("test02"));
        assertEquals(2, cfg.getRepositories().size());
    }


    @Test
    public void createRepositoryGroup() {
        EditableRepositoryGroup gr = provider.createRepositoryGroup("group1", "Group 1");
        assertEquals("group1",gr.getId());
        assertEquals("Group 1", gr.getName());
        assertEquals(MavenRepositoryGroup.class, gr.getClass());
    }

    @Test
    public void createRepositoryGroupWithCfg() throws RepositoryException {

        RepositoryGroupConfiguration cfg = new RepositoryGroupConfiguration();
        cfg.setId("group2");
        cfg.setName("Group 2");
        cfg.setCronExpression("0 0 03 ? * MON");
        cfg.setMergedIndexTtl(504);
        cfg.setMergedIndexPath(".index-abc");
        ArrayList<String> repos = new ArrayList<>();
        repos.add("test01");
        repos.add("test02");
        cfg.setRepositories(repos);

        RepositoryGroup grp = provider.createRepositoryGroup(cfg);

        assertEquals("group2", grp.getId());
        assertEquals("Group 2", grp.getName());
        assertEquals("0 0 03 ? * MON", grp.getSchedulingDefinition());
        IndexCreationFeature indexCreationFeature = grp.getFeature( IndexCreationFeature.class ).get();
        try {
            assertEquals(new URI(".index-abc"), indexCreationFeature.getIndexPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        assertEquals(504, grp.getMergedIndexTTL());
        assertEquals(0, grp.getRepositories().size());
        // assertTrue(grp.getRepositories().stream().anyMatch(r -> "test01".equals(r.getId())));
        // assertTrue(grp.getRepositories().stream().anyMatch(r -> "test02".equals(r.getId())));
    }

}