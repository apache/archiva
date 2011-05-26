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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Collections;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath:/spring-context.xml"} )
public class DependencyTreeBuilderTest
    extends TestCase
{
    @Inject
    @Named (value = "dependencyTreeBuilder#maven2")
    private DependencyTreeBuilder builder;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_VERSION = "1.2.1";

    private static final String TEST_ARTIFACT_ID = "archiva-common";

    private static final String TEST_GROUP_ID = "org.apache.archiva";

    private ArtifactFactory artifactFactory;

    @Inject  @Named(value = "archivaConfiguration#test")
    ArchivaConfiguration config;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        Configuration configuration = new Configuration();
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_REPO_ID );
        repoConfig.setLocation( new File( "target/test-repository" ).getAbsolutePath() );
        configuration.addManagedRepository( repoConfig );
        config.save( configuration );

        artifactFactory = ((DefaultDependencyTreeBuilder)this.builder).getFactory();
    }

    @Test
    public void testBuilder()
        throws DependencyTreeBuilderException
    {
        MockControl control = MockControl.createStrictControl( DependencyNodeVisitor.class );
        DependencyNodeVisitor visitor = (DependencyNodeVisitor) control.getMock();

        DependencyNode springContext =
            new DependencyNode( createArtifact( "org.springframework", "spring-context", "2.5.6" ) );
        springContext.setPremanagedVersion( "2.5.5" );
        DependencyNode springTest =
            new DependencyNode( createArtifact( "org.springframework", "spring-test", "2.5.5", "test" ) );
        DependencyNode plexusUtils =
            new DependencyNode( createArtifact( "org.codehaus.plexus", "plexus-utils", "1.4.5" ) );
        plexusUtils.setPremanagedVersion( "1.5.1" );
        DependencyNode slf4jLog4j12 =
            new DependencyNode( createArtifact( "org.slf4j", "slf4j-log4j12", "1.5.0", "runtime" ) );
        slf4jLog4j12.setPremanagedScope( "test" );
        DependencyNode plexusLog4j = new DependencyNode(
            createArtifact( "org.codehaus.plexus", "plexus-log4j-logging", "1.1-alpha-3", "test" ) );
        DependencyNode log4j = new DependencyNode( createArtifact( "log4j", "log4j", "1.2.14", "test" ) );
        DependencyNode mavenArtifact =
            new DependencyNode( createArtifact( "org.apache.maven", "maven-artifact", "2.0.8", "test" ) );
        DependencyNode mavenProject =
            new DependencyNode( createArtifact( "org.apache.maven", "maven-project", "2.0.8", "test" ) );
        DependencyNode mavenCore =
            new DependencyNode( createArtifact( "org.apache.maven", "maven-core", "2.0.8", "test" ) );
        DependencyNode mavenSettings =
            new DependencyNode( createArtifact( "org.apache.maven", "maven-settings", "2.0.8", "test" ) );
        DependencyNode mavenModel =
            new DependencyNode( createArtifact( "org.apache.maven", "maven-model", "2.0.8", "test" ) );
        DependencyNode plexusCommandLine =
            new DependencyNode( createArtifact( "org.codehaus.plexus", "plexus-command-line", "1.0-alpha-2", "test" ) );
        DependencyNode plexusRegistryCommons = new DependencyNode(
            createArtifact( "org.codehaus.plexus.registry", "plexus-registry-commons", "1.0-alpha-2", "test" ) );
        plexusRegistryCommons.setPremanagedVersion( "1.0-alpha-3" );
        DependencyNode plexusRegistryApi = new DependencyNode(
            createArtifact( "org.codehaus.plexus.registry", "plexus-registry-api", "1.0-alpha-2", "test" ) );
        plexusRegistryApi.setPremanagedVersion( "1.0-alpha-3" );

        DependencyNode plexusSpring =
            new DependencyNode( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2", "test" ) );
        plexusSpring.addChild( springContext );
        plexusSpring.addChild( springTest );
        plexusSpring.addChild( plexusUtils );
        plexusSpring.addChild( slf4jLog4j12 );
        plexusSpring.addChild( plexusLog4j );
        plexusSpring.addChild( log4j );
        plexusSpring.addChild( mavenArtifact );
        plexusSpring.addChild( mavenProject );
        plexusSpring.addChild( mavenCore );
        plexusSpring.addChild( mavenSettings );
        plexusSpring.addChild( mavenModel );
        plexusSpring.addChild( plexusCommandLine );
        plexusSpring.addChild( plexusRegistryCommons );
        plexusSpring.addChild( plexusRegistryApi );

        DependencyNode commonsLang = new DependencyNode( createArtifact( "commons-lang", "commons-lang", "2.2" ) );
        DependencyNode commonsIO = new DependencyNode( createArtifact( "commons-io", "commons-io", "1.4" ) );
        DependencyNode slf4j = new DependencyNode( createArtifact( "org.slf4j", "slf4j-api", "1.5.0" ) );
        DependencyNode plexusAPI =
            new DependencyNode( createArtifact( "org.codehaus.plexus", "plexus-component-api", "1.0-alpha-22" ) );
        DependencyNode xalan = new DependencyNode( createArtifact( "xalan", "xalan", "2.7.0" ) );
        DependencyNode dom4j = new DependencyNode( createArtifact( "dom4j", "dom4j", "1.6.1", "test" ) );
        dom4j.setFailedUpdateScope( "compile" );
        DependencyNode junit = new DependencyNode( createArtifact( "junit", "junit", "3.8.1", "test" ) );
        DependencyNode easymock = new DependencyNode( createArtifact( "easymock", "easymock", "1.2_Java1.3", "test" ) );
        DependencyNode easymockExt =
            new DependencyNode( createArtifact( "easymock", "easymockclassextension", "1.2", "test" ) );

        DependencyNode mainNode =
            new DependencyNode( createProjectArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );
        mainNode.addChild( commonsLang );
        mainNode.addChild( commonsIO );
        mainNode.addChild( slf4j );
        mainNode.addChild( plexusAPI );
        mainNode.addChild( plexusSpring );
        mainNode.addChild( xalan );
        mainNode.addChild( dom4j );
        mainNode.addChild( junit );
        mainNode.addChild( easymock );
        mainNode.addChild( easymockExt );

        control.expectAndReturn( visitor.visit( mainNode ), true );

        control.expectAndReturn( visitor.visit( commonsLang ), true );
        control.expectAndReturn( visitor.endVisit( commonsLang ), true );

        control.expectAndReturn( visitor.visit( commonsIO ), true );
        control.expectAndReturn( visitor.endVisit( commonsIO ), true );

        control.expectAndReturn( visitor.visit( slf4j ), true );
        control.expectAndReturn( visitor.endVisit( slf4j ), true );

        control.expectAndReturn( visitor.visit( plexusAPI ), true );
        control.expectAndReturn( visitor.endVisit( plexusAPI ), true );

        control.expectAndReturn( visitor.visit( plexusSpring ), true );

        control.expectAndReturn( visitor.visit( springContext ), true );
        control.expectAndReturn( visitor.endVisit( springContext ), true );

        control.expectAndReturn( visitor.visit( springTest ), true );
        control.expectAndReturn( visitor.endVisit( springTest ), true );

        control.expectAndReturn( visitor.visit( plexusUtils ), true );
        control.expectAndReturn( visitor.endVisit( plexusUtils ), true );

        control.expectAndReturn( visitor.visit( slf4jLog4j12 ), true );
        control.expectAndReturn( visitor.endVisit( slf4jLog4j12 ), true );

        control.expectAndReturn( visitor.visit( plexusLog4j ), true );
        control.expectAndReturn( visitor.endVisit( plexusLog4j ), true );

        control.expectAndReturn( visitor.visit( log4j ), true );
        control.expectAndReturn( visitor.endVisit( log4j ), true );

        control.expectAndReturn( visitor.visit( mavenArtifact ), true );
        control.expectAndReturn( visitor.endVisit( mavenArtifact ), true );

        control.expectAndReturn( visitor.visit( mavenProject ), true );
        control.expectAndReturn( visitor.endVisit( mavenProject ), true );

        control.expectAndReturn( visitor.visit( mavenCore ), true );
        control.expectAndReturn( visitor.endVisit( mavenCore ), true );

        control.expectAndReturn( visitor.visit( mavenSettings ), true );
        control.expectAndReturn( visitor.endVisit( mavenSettings ), true );

        control.expectAndReturn( visitor.visit( mavenModel ), true );
        control.expectAndReturn( visitor.endVisit( mavenModel ), true );

        control.expectAndReturn( visitor.visit( plexusCommandLine ), true );
        control.expectAndReturn( visitor.endVisit( plexusCommandLine ), true );

        control.expectAndReturn( visitor.visit( plexusRegistryCommons ), true );
        control.expectAndReturn( visitor.endVisit( plexusRegistryCommons ), true );

        control.expectAndReturn( visitor.visit( plexusRegistryApi ), true );
        control.expectAndReturn( visitor.endVisit( plexusRegistryApi ), true );

        control.expectAndReturn( visitor.endVisit( plexusSpring ), true );

        control.expectAndReturn( visitor.visit( xalan ), true );
        control.expectAndReturn( visitor.endVisit( xalan ), true );

        control.expectAndReturn( visitor.visit( dom4j ), true );
        control.expectAndReturn( visitor.endVisit( dom4j ), true );

        control.expectAndReturn( visitor.visit( junit ), true );
        control.expectAndReturn( visitor.endVisit( junit ), true );

        control.expectAndReturn( visitor.visit( easymock ), true );
        control.expectAndReturn( visitor.endVisit( easymock ), true );

        control.expectAndReturn( visitor.visit( easymockExt ), true );
        control.expectAndReturn( visitor.endVisit( easymockExt ), true );

        control.expectAndReturn( visitor.endVisit( mainNode ), true );

        control.replay();

        builder.buildDependencyTree( Collections.singletonList( TEST_REPO_ID ), TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                     TEST_VERSION, visitor );

        control.verify();
    }

    private Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return artifactFactory.createProjectArtifact( groupId, artifactId, version );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String scope )
    {
        return artifactFactory.createDependencyArtifact( groupId, artifactId, VersionRange.createFromVersion( version ),
                                                         "jar", null, scope );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE );
    }

    @Test
    public void testBuilderMissingDependency()
        throws DependencyTreeBuilderException
    {
        MockControl control = MockControl.createStrictControl( DependencyNodeVisitor.class );
        DependencyNodeVisitor visitor = (DependencyNodeVisitor) control.getMock();

        // not visited

        control.replay();

        builder.buildDependencyTree( Collections.singletonList( TEST_REPO_ID ), TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                     "not-a-version", visitor );

        control.verify();
    }
}
