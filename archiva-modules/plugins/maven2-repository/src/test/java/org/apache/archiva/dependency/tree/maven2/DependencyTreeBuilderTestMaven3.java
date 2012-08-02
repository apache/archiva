package org.apache.archiva.dependency.tree.maven2;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.impl.DependencyCollector;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class DependencyTreeBuilderTestMaven3
    extends TestCase
{
    @Inject
    @Named( value = "dependencyTreeBuilder#maven3" )
    private Maven3DependencyTreeBuilder builder;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_VERSION = "1.2.1";

    private static final String TEST_ARTIFACT_ID = "archiva-common";

    private static final String TEST_GROUP_ID = "org.apache.archiva";

    private DefaultRepositorySystem defaultRepositorySystem;


    final Map<String, DependencyNode> nodes = new HashMap<String, DependencyNode>();

    @Inject
    @Named( value = "archivaConfiguration#test" )
    ArchivaConfiguration config;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        defaultRepositorySystem = (DefaultRepositorySystem) plexusSisuBridge.lookup( RepositorySystem.class );

        DefaultDependencyNode springContext = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.springframework", "spring-context", "2.5.6" ), "compile" ) );

        springContext.setPremanagedVersion( "2.5.5" );

        nodes.put( getId( springContext.getDependency().getArtifact() ), springContext );

        DefaultDependencyNode springTest = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.springframework", "spring-test", "2.5.5" ), "test" ) );

        nodes.put( getId( springTest.getDependency().getArtifact() ), springTest );

        DefaultDependencyNode plexusUtils = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-utils", "1.4.5" ), "compile" ) );

        plexusUtils.setPremanagedVersion( "1.5.1" );

        nodes.put( getId( plexusUtils.getDependency().getArtifact() ), plexusUtils );

        DefaultDependencyNode slf4jLog4j12 = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.slf4j", "slf4j-log4j12", "1.5.0" ), "runtime" ) );

        slf4jLog4j12.setPremanagedScope( "test" );

        nodes.put( getId( slf4jLog4j12.getDependency().getArtifact() ), slf4jLog4j12 );

        DefaultDependencyNode plexusLog4j = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-log4j-logging", "1.1-alpha-3" ), "test" ) );

        nodes.put( getId( plexusLog4j.getDependency().getArtifact() ), plexusLog4j );

        DefaultDependencyNode log4j =
            new DefaultDependencyNode( new Dependency( createArtifact( "log4j", "log4j", "1.2.14" ), "test" ) );

        nodes.put( getId( log4j.getDependency().getArtifact() ), log4j );

        DefaultDependencyNode mavenArtifact = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-artifact", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenArtifact.getDependency().getArtifact() ), mavenArtifact );

        DefaultDependencyNode mavenProject = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-project", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenProject.getDependency().getArtifact() ), mavenProject );

        DefaultDependencyNode mavenCore = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-core", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenCore.getDependency().getArtifact() ), mavenCore );

        DefaultDependencyNode mavenSettings = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-settings", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenSettings.getDependency().getArtifact() ), mavenSettings );

        DefaultDependencyNode mavenModel = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-model", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenModel.getDependency().getArtifact() ), mavenModel );

        DefaultDependencyNode plexusCommandLine = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-command-line", "1.0-alpha-2" ), "test" ) );

        nodes.put( getId( plexusCommandLine.getDependency().getArtifact() ), plexusCommandLine );

        DefaultDependencyNode plexusRegistryCommons = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus.registry", "plexus-registry-commons", "1.0-alpha-2" ),
                            "test" ) );

        nodes.put( getId( plexusRegistryCommons.getDependency().getArtifact() ), plexusRegistryCommons );

        plexusRegistryCommons.setPremanagedVersion( "1.0-alpha-3" );

        DefaultDependencyNode plexusRegistryApi = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus.registry", "plexus-registry-api", "1.0-alpha-2" ),
                            "test" ) );

        nodes.put( getId( plexusRegistryApi.getDependency().getArtifact() ), plexusRegistryApi );

        plexusRegistryApi.setPremanagedVersion( "1.0-alpha-3" );

        DefaultDependencyNode plexusSpring = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2" ), "test" ) );

        nodes.put( getId( plexusSpring.getDependency().getArtifact() ), plexusSpring );

        plexusSpring.getChildren().add( springContext );
        plexusSpring.getChildren().add( springTest );
        plexusSpring.getChildren().add( plexusUtils );
        plexusSpring.getChildren().add( slf4jLog4j12 );
        plexusSpring.getChildren().add( plexusLog4j );
        plexusSpring.getChildren().add( log4j );
        plexusSpring.getChildren().add( mavenArtifact );
        plexusSpring.getChildren().add( mavenProject );
        plexusSpring.getChildren().add( mavenCore );
        plexusSpring.getChildren().add( mavenSettings );
        plexusSpring.getChildren().add( mavenModel );
        plexusSpring.getChildren().add( plexusCommandLine );
        plexusSpring.getChildren().add( plexusRegistryCommons );
        plexusSpring.getChildren().add( plexusRegistryApi );

        DefaultDependencyNode commonsLang = new DefaultDependencyNode(
            new Dependency( createArtifact( "commons-lang", "commons-lang", "2.2" ), "compile" ) );

        nodes.put( getId( commonsLang.getDependency().getArtifact() ), commonsLang );

        DefaultDependencyNode commonsIO = new DefaultDependencyNode(
            new Dependency( createArtifact( "commons-io", "commons-io", "1.4" ), "compile" ) );

        nodes.put( getId( commonsIO.getDependency().getArtifact() ), commonsIO );

        DefaultDependencyNode slf4j = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.slf4j", "slf4j-api", "1.5.0" ), "compile" ) );

        nodes.put( getId( slf4j.getDependency().getArtifact() ), slf4j );

        DefaultDependencyNode plexusAPI = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-component-api", "1.0-alpha-22" ),
                            "compile" ) );

        nodes.put( getId( plexusAPI.getDependency().getArtifact() ), plexusAPI );

        DefaultDependencyNode xalan =
            new DefaultDependencyNode( new Dependency( createArtifact( "xalan", "xalan", "2.7.0" ), "compile" ) );

        nodes.put( getId( xalan.getDependency().getArtifact() ), xalan );

        DefaultDependencyNode dom4j =
            new DefaultDependencyNode( new Dependency( createArtifact( "dom4j", "dom4j", "1.6.1" ), "test" ) );

        nodes.put( getId( dom4j.getDependency().getArtifact() ), dom4j );

        //dom4j.setFailedUpdateScope("compile");

        DefaultDependencyNode junit =
            new DefaultDependencyNode( new Dependency( createArtifact( "junit", "junit", "3.8.1" ), "test" ) );

        nodes.put( getId( junit.getDependency().getArtifact() ), junit );

        DefaultDependencyNode easymock = new DefaultDependencyNode(
            new Dependency( createArtifact( "easymock", "easymock", "1.2_Java1.3" ), "test" ) );

        nodes.put( getId( easymock.getDependency().getArtifact() ), easymock );

        DefaultDependencyNode easymockExt = new DefaultDependencyNode(
            new Dependency( createArtifact( "easymock", "easymockclassextension", "1.2" ), "test" ) );

        nodes.put( getId( easymockExt.getDependency().getArtifact() ), easymockExt );

        DependencyNode mainNode = new DefaultDependencyNode(
            new Dependency( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ), "compile" ) );

        nodes.put( getId( mainNode.getDependency().getArtifact() ), mainNode );

        mainNode.getChildren().add( commonsLang );
        mainNode.getChildren().add( commonsIO );
        mainNode.getChildren().add( slf4j );
        mainNode.getChildren().add( plexusAPI );
        mainNode.getChildren().add( plexusSpring );
        mainNode.getChildren().add( xalan );
        mainNode.getChildren().add( dom4j );
        mainNode.getChildren().add( junit );
        mainNode.getChildren().add( easymock );
        mainNode.getChildren().add( easymockExt );

        defaultRepositorySystem.setDependencyCollector( new DependencyCollector()
        {

            public CollectResult collectDependencies( RepositorySystemSession session, CollectRequest request )
                throws DependencyCollectionException
            {
                CollectResult collectResult = new CollectResult( request );
                collectResult.setRoot( new DefaultDependencyNode() );
                for ( Dependency dependency : request.getDependencies() )
                {
                    DependencyNode node = nodes.get( getId( dependency.getArtifact() ) );
                    if ( node != null )
                    {
                        collectResult.getRoot().getChildren().add( node );
                    }
                }
                return collectResult;
            }
        } );

        Configuration configuration = new Configuration();
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_REPO_ID );
        repoConfig.setLocation( new File( "target/test-repository" ).getAbsolutePath() );
        configuration.addManagedRepository( repoConfig );
        config.save( configuration );

        //artifactFactory = ((DefaultDependencyTreeBuilder)this.builder).getFactory();
    }


    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return new DefaultArtifact( groupId, artifactId, null, version );
    }

    private String getId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    @Test
    public void testBuilderDependencies()
        throws Exception
    {

        builder.buildDependencyTree( Collections.singletonList( TEST_REPO_ID ), TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                     TEST_VERSION, new DependencyVisitor()
        {
            public boolean visitEnter( DependencyNode dependencyNode )
            {
                return true;
            }

            public boolean visitLeave( DependencyNode dependencyNode )
            {
                return true;
            }
        } );


    }
}
