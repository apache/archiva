package org.apache.archiva.repository.maven.dependency.tree;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.maven2.model.TreeEntry;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.maven.dependency.tree.Maven3DependencyTreeBuilder;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class DependencyTreeBuilderTestMaven3
    extends TestCase
{
    @Inject
    @Named( "dependencyTreeBuilder#maven3" )
    private Maven3DependencyTreeBuilder builder;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_VERSION = "1.2.1";

    private static final String TEST_ARTIFACT_ID = "archiva-common";

    private static final String TEST_GROUP_ID = "org.apache.archiva";


    @Inject
    @Named( "archivaConfiguration#test" )
    ArchivaConfiguration config;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        Configuration configuration = new Configuration();
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_REPO_ID );
        repoConfig.setLocation(Paths.get("target/test-repository").toAbsolutePath().toString() );
        configuration.addManagedRepository( repoConfig );

        config.getConfiguration().getProxyConnectors().clear();
        config.save( configuration );

        repositoryRegistry.reload();

        //artifactFactory = ((DefaultDependencyTreeBuilder)this.builder).getFactory();
    }


    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return new Artifact( groupId, artifactId, version );
    }

    private String getId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    @Test
    public void testBuilderDependencies()
        throws Exception
    {

        List<TreeEntry> treeEntries =
            builder.buildDependencyTree( Collections.singletonList( TEST_REPO_ID ), TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                         TEST_VERSION );

        Artifact artifact = new Artifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, "", "" );
        artifact.setFileExtension("jar");
        assertThat( treeEntries ).isNotNull().isNotEmpty().contains(new TreeEntry(artifact) );

        artifact = new Artifact( "commons-lang", "commons-lang", "2.2", "compile", "" );
        artifact.setFileExtension("jar");
        assertThat( treeEntries.get( 0 ).getChilds() ).isNotNull().isNotEmpty().contains(
            new TreeEntry(artifact) );
    }


    public static class TestTreeEntry
        extends TreeEntry
    {
        Artifact a;

        public TestTreeEntry( Artifact a )
        {
            this.a = a;
        }

        @Override
        public int hashCode()
        {
            return this.a.hashCode();
        }

        @Override
        public boolean equals( Object o )
        {
            Artifact artifact = ( (TreeEntry) o ).getArtifact();
            return artifact.equals( this.a );
        }
    }

}
