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
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
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
        return new DefaultArtifact( groupId, artifactId, "jar", version );
    }

    private String getId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    @Test
    public void testBuilderDependencies()
        throws Exception
    {

        DependencyNode springContext = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.springframework", "spring-context", "2.5.6" ), "compile" ) );

        //springContext.setPremanagedVersion( "2.5.5" );

        nodes.put( getId( springContext.getDependency().getArtifact() ), springContext );

        DependencyNode springTest = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.springframework", "spring-test", "2.5.5" ), "test" ) );

        nodes.put( getId( springTest.getDependency().getArtifact() ), springTest );

        DependencyNode plexusUtils = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-utils", "1.4.5" ), "compile" ) );

        //plexusUtils.setPremanagedVersion( "1.5.1" );

        nodes.put( getId( plexusUtils.getDependency().getArtifact() ), plexusUtils );

        DependencyNode slf4jLog4j12 = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.slf4j", "slf4j-log4j12", "1.5.0" ), "runtime" ) );

        //slf4jLog4j12.setPremanagedScope( "test" );

        nodes.put( getId( slf4jLog4j12.getDependency().getArtifact() ), slf4jLog4j12 );

        DependencyNode plexusLog4j = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-log4j-logging", "1.1-alpha-3" ), "test" ) );

        nodes.put( getId( plexusLog4j.getDependency().getArtifact() ), plexusLog4j );

        DependencyNode log4j =
            new DefaultDependencyNode( new Dependency( createArtifact( "log4j", "log4j", "1.2.14" ), "test" ) );

        nodes.put( getId( log4j.getDependency().getArtifact() ), log4j );

        DependencyNode mavenArtifact = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-artifact", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenArtifact.getDependency().getArtifact() ), mavenArtifact );

        DependencyNode mavenProject = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-project", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenProject.getDependency().getArtifact() ), mavenProject );

        DependencyNode mavenCore = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-core", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenCore.getDependency().getArtifact() ), mavenCore );

        DependencyNode mavenSettings = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-settings", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenSettings.getDependency().getArtifact() ), mavenSettings );

        DependencyNode mavenModel = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.apache.maven", "maven-model", "2.0.8" ), "test" ) );

        nodes.put( getId( mavenModel.getDependency().getArtifact() ), mavenModel );

        DependencyNode plexusCommandLine = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-command-line", "1.0-alpha-2" ), "test" ) );

        nodes.put( getId( plexusCommandLine.getDependency().getArtifact() ), plexusCommandLine );

        DependencyNode plexusRegistryCommons = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus.registry", "plexus-registry-commons", "1.0-alpha-2" ),
                            "test" ) );

        nodes.put( getId( plexusRegistryCommons.getDependency().getArtifact() ), plexusRegistryCommons );

        //plexusRegistryCommons.setPremanagedVersion( "1.0-alpha-3" );

        DependencyNode plexusRegistryApi = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus.registry", "plexus-registry-api", "1.0-alpha-2" ),
                            "test" ) );

        nodes.put( getId( plexusRegistryApi.getDependency().getArtifact() ), plexusRegistryApi );

        //plexusRegistryApi.setPremanagedVersion( "1.0-alpha-3" );

        DependencyNode plexusSpring = new DefaultDependencyNode(
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

        DependencyNode commonsLang = new DefaultDependencyNode(
            new Dependency( createArtifact( "commons-lang", "commons-lang", "2.2" ), "compile" ) );

        nodes.put( getId( commonsLang.getDependency().getArtifact() ), commonsLang );

        DependencyNode commonsIO = new DefaultDependencyNode(
            new Dependency( createArtifact( "commons-io", "commons-io", "1.4" ), "compile" ) );

        nodes.put( getId( commonsIO.getDependency().getArtifact() ), commonsIO );

        DependencyNode slf4j = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.slf4j", "slf4j-api", "1.5.0" ), "compile" ) );

        nodes.put( getId( slf4j.getDependency().getArtifact() ), slf4j );

        DependencyNode plexusAPI = new DefaultDependencyNode(
            new Dependency( createArtifact( "org.codehaus.plexus", "plexus-component-api", "1.0-alpha-22" ),
                            "compile" ) );

        nodes.put( getId( plexusAPI.getDependency().getArtifact() ), plexusAPI );

        DependencyNode xalan =
            new DefaultDependencyNode( new Dependency( createArtifact( "xalan", "xalan", "2.7.0" ), "compile" ) );

        nodes.put( getId( xalan.getDependency().getArtifact() ), xalan );

        DependencyNode dom4j =
            new TestDefaultDependencyNode( new Dependency( createArtifact( "dom4j", "dom4j", "1.6.1" ), "test" ) );

        nodes.put( getId( dom4j.getDependency().getArtifact() ), dom4j );

        //dom4j.setFailedUpdateScope("compile");

        DependencyNode junit =
            new TestDefaultDependencyNode( new Dependency( createArtifact( "junit", "junit", "3.8.1" ), "test" ) );

        nodes.put( getId( junit.getDependency().getArtifact() ), junit );

        DependencyNode easymock = new TestDefaultDependencyNode(
            new Dependency( createArtifact( "easymock", "easymock", "1.2_Java1.3" ), "test" ) );

        nodes.put( getId( easymock.getDependency().getArtifact() ), easymock );

        DependencyNode easymockExt = new TestDefaultDependencyNode(
            new Dependency( createArtifact( "easymock", "easymockclassextension", "1.2" ), "test" ) );

        nodes.put( getId( easymockExt.getDependency().getArtifact() ), easymockExt );

        DependencyNode mainNode = new TestDefaultDependencyNode(
            new Dependency( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ), "" ) );

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

        /*defaultRepositorySystem.setDependencyCollector( new DependencyCollector()
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
        */

        MockControl control = MockControl.createStrictControl( DependencyVisitor.class );
        DependencyVisitor visitor = (DependencyVisitor) control.getMock();

        control.expectAndReturn( visitor.visitEnter( mainNode ), true );

        control.expectAndReturn( visitor.visitEnter( commonsLang ), true );
        control.expectAndReturn( visitor.visitLeave( commonsLang ), true );

        control.expectAndReturn( visitor.visitEnter( commonsIO ), true );
        control.expectAndReturn( visitor.visitLeave( commonsIO ), true );

        control.expectAndReturn( visitor.visitEnter( slf4j ), true );
        control.expectAndReturn( visitor.visitLeave( slf4j ), true );

        control.expectAndReturn( visitor.visitEnter( plexusAPI ), true );
        control.expectAndReturn( visitor.visitLeave( plexusAPI ), true );

        control.expectAndReturn( visitor.visitEnter( plexusSpring ), true );

        control.expectAndReturn( visitor.visitEnter( springContext ), true );
        control.expectAndReturn( visitor.visitLeave( springContext ), true );

        control.expectAndReturn( visitor.visitEnter( springTest ), true );
        control.expectAndReturn( visitor.visitLeave( springTest ), true );

        control.expectAndReturn( visitor.visitEnter( plexusUtils ), true );
        control.expectAndReturn( visitor.visitLeave( plexusUtils ), true );

        control.expectAndReturn( visitor.visitEnter( slf4jLog4j12 ), true );
        control.expectAndReturn( visitor.visitLeave( slf4jLog4j12 ), true );

        control.expectAndReturn( visitor.visitEnter( plexusLog4j ), true );
        control.expectAndReturn( visitor.visitLeave( plexusLog4j ), true );

        control.expectAndReturn( visitor.visitEnter( log4j ), true );
        control.expectAndReturn( visitor.visitLeave( log4j ), true );

        control.expectAndReturn( visitor.visitEnter( mavenArtifact ), true );
        control.expectAndReturn( visitor.visitLeave( mavenArtifact ), true );

        control.expectAndReturn( visitor.visitEnter( mavenProject ), true );
        control.expectAndReturn( visitor.visitLeave( mavenProject ), true );

        control.expectAndReturn( visitor.visitEnter( mavenCore ), true );
        control.expectAndReturn( visitor.visitLeave( mavenCore ), true );

        control.expectAndReturn( visitor.visitEnter( mavenSettings ), true );
        control.expectAndReturn( visitor.visitLeave( mavenSettings ), true );

        control.expectAndReturn( visitor.visitEnter( mavenModel ), true );
        control.expectAndReturn( visitor.visitLeave( mavenModel ), true );

        control.expectAndReturn( visitor.visitEnter( plexusCommandLine ), true );
        control.expectAndReturn( visitor.visitLeave( plexusCommandLine ), true );

        control.expectAndReturn( visitor.visitEnter( plexusRegistryCommons ), true );
        control.expectAndReturn( visitor.visitLeave( plexusRegistryCommons ), true );

        control.expectAndReturn( visitor.visitEnter( plexusRegistryApi ), true );
        control.expectAndReturn( visitor.visitLeave( plexusRegistryApi ), true );

        control.expectAndReturn( visitor.visitLeave( plexusSpring ), true );

        control.expectAndReturn( visitor.visitEnter( xalan ), true );
        control.expectAndReturn( visitor.visitLeave( xalan ), true );

        control.expectAndReturn( visitor.visitEnter( dom4j ), true );
        control.expectAndReturn( visitor.visitLeave( dom4j ), true );

        control.expectAndReturn( visitor.visitEnter( junit ), true );
        control.expectAndReturn( visitor.visitLeave( junit ), true );

        control.expectAndReturn( visitor.visitEnter( easymock ), true );
        control.expectAndReturn( visitor.visitLeave( easymock ), true );

        control.expectAndReturn( visitor.visitEnter( easymockExt ), true );
        control.expectAndReturn( visitor.visitLeave( easymockExt ), true );

        control.expectAndReturn( visitor.visitLeave( mainNode ), true );

        control.replay();

        visitor = new DependencyVisitor()
        {
            public boolean visitEnter( DependencyNode dependencyNode )
            {
                return true;
            }

            public boolean visitLeave( DependencyNode dependencyNode )
            {
                return true;
            }
        };

        builder.buildDependencyTree( Collections.singletonList( TEST_REPO_ID ), TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                     TEST_VERSION, visitor );

        control.verify();

    }

    public static class TestDefaultDependencyNode
        extends DefaultDependencyNode
    {

        private TestDefaultDependencyNode( Dependency dependency )
        {
            super( dependency );
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public boolean equals( Object o )
        {
            DependencyNode node = (DependencyNode) o;
            boolean equals = this.getDependency().getArtifact().getGroupId().equals(
                node.getDependency().getArtifact().getGroupId() ) &&
                this.getDependency().getArtifact().getArtifactId().equals(
                    node.getDependency().getArtifact().getArtifactId() ) &&
                this.getDependency().getArtifact().getVersion().equals(
                    node.getDependency().getArtifact().getVersion() );
            return equals;
        }
    }
}
