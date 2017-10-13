package org.apache.archiva.repository.maven2;

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
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedFeatureException;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Period;

import static org.junit.Assert.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-no-mock-conf.xml" }  )
public class MavenRepositoryProviderTest
{

    @Inject
    @Named( "archivaConfiguration#default" )
    ArchivaConfiguration archivaConfiguration;

    MavenRepositoryProvider provider;


    @Before
    public void setUp()
        throws Exception
    {
        provider = new MavenRepositoryProvider();
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
        assertNotNull(archivaConfiguration);
        assertNotNull(archivaConfiguration.getConfiguration());
        ManagedRepositoryConfiguration repo = archivaConfiguration.getConfiguration().getManagedRepositories().get(0);
        ManagedRepository mr = provider.createManagedInstance( repo );
        assertNotNull(mr.getLocation());
        assertTrue(mr.getLocation().toString().endsWith( "/repositories/internal" ));
        assertEquals("Archiva Managed Internal Repository", mr.getName());
        assertEquals(1, mr.getActiveReleaseSchemes().size());
        assertEquals( ReleaseScheme.RELEASE, mr.getActiveReleaseSchemes().iterator().next());
        assertEquals("internal", mr.getId());
        assertTrue(mr.blocksRedeployments());
        assertEquals("0 0 * * * ?", mr.getSchedulingDefinition());
        assertTrue(mr.isScanned());
        ArtifactCleanupFeature artifactCleanupFeature = mr.getFeature( ArtifactCleanupFeature.class ).get();
        assertEquals( Period.ofDays( 30), artifactCleanupFeature.getRetentionTime());
        assertFalse(artifactCleanupFeature.isDeleteReleasedSnapshots());
        assertEquals(2, artifactCleanupFeature.getRetentionCount());

        IndexCreationFeature indexCreationFeature = mr.getFeature( IndexCreationFeature.class ).get();
        assertNotNull(indexCreationFeature.getIndexPath());
        assertTrue(indexCreationFeature.getIndexPath().toString().endsWith("/repositories/internal/.indexer"));
        assertTrue(indexCreationFeature.getIndexPath().isAbsolute());
        assertFalse(indexCreationFeature.isSkipPackedIndexCreation());

        StagingRepositoryFeature stagingRepositoryFeature = mr.getFeature( StagingRepositoryFeature.class ).get();
        assertFalse(stagingRepositoryFeature.isStageRepoNeeded());
        assertNull(stagingRepositoryFeature.getStagingRepository());


    }

    @Test
    public void createRemoteInstance( ) throws Exception
    {
        assertNotNull(archivaConfiguration);
        assertNotNull(archivaConfiguration.getConfiguration());
        RemoteRepositoryConfiguration repo = archivaConfiguration.getConfiguration().getRemoteRepositories().get(0);
        RemoteRepository mr = provider.createRemoteInstance( repo );
        assertNotNull(mr.getLocation());
        assertEquals("https://repo.maven.apache.org/maven2", mr.getLocation().toString());
        assertEquals("Central Repository", mr.getName());
        assertEquals("central", mr.getId());
        assertEquals("0 0 08 ? * SUN", mr.getSchedulingDefinition());
        assertTrue(mr.isScanned());
        assertNull(mr.getLoginCredentials());
        try
        {
            ArtifactCleanupFeature artifactCleanupFeature = mr.getFeature( ArtifactCleanupFeature.class ).get( );
            throw new Exception("artifactCleanupFeature should not be available");
        } catch ( UnsupportedFeatureException e ) {
            // correct
        }

        try
        {
            IndexCreationFeature indexCreationFeature = mr.getFeature( IndexCreationFeature.class ).get( );
            throw new Exception("indexCreationFeature should not be available");
        } catch (UnsupportedFeatureException e) {
            // correct
        }
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

}