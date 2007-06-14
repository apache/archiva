package org.apache.maven.archiva.plugins.dev.testgen;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.maven.archiva.plugins.dev.functors.DependencyNodeToArtifactTransformer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * DependencyGraphTestCreator 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphTestCreator
    extends AbstractCreator
{
    private class RootArtifactPredicate
        implements Predicate
    {
        private String rootKey;

        public RootArtifactPredicate( DependencyTree tree )
        {
            this.rootKey = toKey( tree.getRootNode().getArtifact() );
        }

        public boolean evaluate( Object input )
        {
            boolean satisfies = false;
        
            if ( input instanceof Artifact )
            {
                Artifact nodeArtifact = (Artifact) input;
                String key = toKey( nodeArtifact );
                
                if ( key.equals( rootKey ) )
                {
                    satisfies = true;
                }
            }
        
            return satisfies;
        }
    }

    private File outputFile;

    private PrintWriter out;

    private DependencyTreeBuilder dependencyTreeBuilder;

    private ArtifactMetadataSource artifactMetadataSource;

    private ArtifactCollector collector;

    public void create( String classPrefix )
        throws MojoExecutionException
    {
        String classname = classPrefix + "DependencyGraphTest";

        getLog().info( "Generating " + classname + ".java ..." );

        outputFile = new File( outputDir, classname + ".java" );
        try
        {
            out = new PrintWriter( outputFile );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to open file " + outputFile.getName() + " for output: "
                + e.getMessage(), e );
        }

        try
        {
            out.println( "package org.apache.maven.archiva.dependency.graph;" );
            out.println( "" );

            writeLicense( out );

            // Imports
            out.println( "import org.apache.maven.archiva.dependency.DependencyGraphFactory;" );
            out.println( "import org.apache.maven.archiva.model.DependencyScope;" );
            out.println( "import org.apache.maven.archiva.model.VersionedReference;" );
            out.println( "" );
            out.println( "import java.util.ArrayList;" );
            out.println( "import java.util.List;" );
            out.println( "" );

            String projectKey = toKey( project.getModel() );

            writeJavadoc( classname, projectKey );

            // The class itself.
            out.println( "public class " + classname );
            out.println( "   extends AbstractDependencyGraphFactoryTestCase" );
            out.println( "{" );

            DependencyTree dependencyTree = getDependencyTree();

            writeGraphNodesTest( classPrefix, dependencyTree );

            // TODO: enable in future, when resolution between archiva and maven are equal.
            // writeDirectCompileDepsTest( classPrefix, dependencyTree );
            // writeDirectTestDepsTest( classPrefix, dependencyTree );
            // writeTransitiveCompileDepsTest( classPrefix, dependencyTree );
            // writeTransitiveTestDepsTest( classPrefix, dependencyTree );

            out.println( "}" );
        }
        finally
        {
            out.flush();
            IOUtil.close( out );
        }
    }

    public ArtifactMetadataSource getArtifactMetadataSource()
    {
        return artifactMetadataSource;
    }

    public ArtifactCollector getCollector()
    {
        return collector;
    }

    public DependencyTreeBuilder getDependencyTreeBuilder()
    {
        return dependencyTreeBuilder;
    }

    public void setArtifactMetadataSource( ArtifactMetadataSource artifactMetadataSource )
    {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    public void setCollector( ArtifactCollector collector )
    {
        this.collector = collector;
    }

    public void setDependencyTreeBuilder( DependencyTreeBuilder dependencyTreeBuilder )
    {
        this.dependencyTreeBuilder = dependencyTreeBuilder;
    }

    private DependencyTree getDependencyTree()
        throws MojoExecutionException
    {
        try
        {
            return dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                              artifactMetadataSource, collector );
        }
        catch ( DependencyTreeBuilderException e )
        {
            String emsg = "Unable to build dependency tree.";
            getLog().error( emsg, e );
            throw new MojoExecutionException( emsg, e );
        }
    }

    private void writeDirectCompileDepsTest( String classPrefix, DependencyTree tree )
    {
        out.println( "   public void testResolveOfDirectCompileDeps()" );
        out.println( "        throws GraphTaskException" );
        out.println( "   {" );
        writeTestProlog( classPrefix );
        writeDirectDependenciesCheck( tree, "compile" );
        out.println( "   }" );
        out.println( "" );
    }

    private void writeDirectDependenciesCheck( DependencyTree dependencyTree, String scope )
    {
        out.println( "      // Check for direct dependencies on scope " + scope );
        out.println( "      expectedNodes.clear();" );

        List directDeps = new ArrayList();
        directDeps.addAll( dependencyTree.getRootNode().getChildren() );
        CollectionUtils.transform( directDeps, new DependencyNodeToArtifactTransformer() );

        Collections.sort( directDeps );

        writeExpectedNodesAdd( directDeps, scope );

        out.println( "      assertDirectNodes( graph, expectedNodes, \"" + scope + "\" );" );
    }

    private void writeDirectTestDepsTest( String classPrefix, DependencyTree tree )
    {
        out.println( "   public void testResolveOfDirectTestDeps()" );
        out.println( "        throws GraphTaskException" );
        out.println( "   {" );
        writeTestProlog( classPrefix );
        writeDirectDependenciesCheck( tree, "test" );
        out.println( "   }" );
        out.println( "" );
    }

    private void writeExpectedNodesAdd( List deps, String scope )
    {
        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            String depKey = toKey( artifact );
            if ( StringUtils.equals( scope, artifact.getScope() ) )
            {
                out.println( "      expectedNodes.add( \"" + depKey + "\" );" );
            }
        }
    }

    private void writeJavadoc( String classname, String projectKey )
    {
        out.println( "/**" );
        out.println( " * " + classname );
        out.println( " * " );
        out.println( " * DependencyGraphTest for testing <code>" + projectKey + "</code>" );
        out.println( " *" );
        out.println( " * Generated by <code>archivadev:generate-dependency-tests</code> plugin" );
        out.println( " * @version $Id$" );
        out.println( " */" );
    }

    private void writeGraphNodesTest( String classPrefix, final DependencyTree tree )
    {
        out.println( "   public void testResolvedDepsToNodes()" );
        out.println( "        throws GraphTaskException" );
        out.println( "   {" );
        writeTestProlog( classPrefix );

        String projectKey = toKey( project.getModel() );
        out.println( "      String expectedRootRef = \"" + projectKey + "\";" );
        out.println( "      List expectedNodes = new ArrayList();" );
        out.println( "" );
        out.println( "      // Check for all nodes, regardless of scope." );
        out.println( "      expectedNodes.clear();" );

        // Add all deps.
        List deps = new ArrayList();
        Predicate notRootNode = NotPredicate.getInstance( new RootArtifactPredicate( tree ) );
        CollectionUtils.select( tree.getArtifacts(), notRootNode, deps );
        CollectionUtils.transform( deps, new DependencyNodeToArtifactTransformer() );
        Collections.sort( deps );

        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            String depKey = toKey( artifact );
            out.println( "      expectedNodes.add( \"" + depKey + "\" );" );
        }

        out.println( "" );
        out.println( "      assertGraph( graph, expectedRootRef, expectedNodes );" );

        out.println( "   }" );
        out.println( "" );

    }

    private void writeTestProlog( String classPrefix )
    {
        out.println( "      MemoryRepositoryDependencyGraphBuilder graphBuilder = " );
        out.println( "                     new MemoryRepositoryDependencyGraphBuilder();" );
        out.println( "      MemoryRepository repository = new " + classPrefix + "MemoryRepository();" );
        out.println( "      graphBuilder.setMemoryRepository( repository );" );
        out.println( "" );
        out.println( "      // Create the factory, and add the test resolver." );
        out.println( "      DependencyGraphFactory factory = new DependencyGraphFactory();" );
        out.println( "      factory.setGraphBuilder( graphBuilder );" );
        out.println( "      factory.setDesiredScope( DependencyScope.TEST );" );
        out.println( "" );
        out.println( "      // Get the model to resolve from" );
        out.println( "      VersionedReference rootRef = toVersionedReference( \"" + project.getGroupId() + ":"
            + project.getArtifactId() + ":" + project.getVersion() + "\"); " );
        out.println( "" );
        out.println( "      // Perform the resolution." );
        out.println( "      DependencyGraph graph = factory.getGraph( rootRef );" );
        out.println( "" );
        out.println( "      // Test the results." );
        out.println( "      assertNotNull( \"Graph shouldn't be null.\", graph );" );
        out.println( "" );
    }

    private void writeTransientDependenciesCheck( DependencyTree dependencyTree, String scope )
    {
        out.println( "      // Check for transient dependencies on scope " + scope );
        out.println( "      expectedNodes.clear();" );

        // Add all deps.
        List deps = new ArrayList( dependencyTree.getArtifacts() );
        // Remove the direct deps.
        List directDeps = new ArrayList();
        directDeps.addAll( dependencyTree.getRootNode().getChildren() );
        CollectionUtils.transform( directDeps, new DependencyNodeToArtifactTransformer() );
        deps.removeAll( directDeps );

        Collections.sort( deps );

        writeExpectedNodesAdd( deps, scope );

        out.println( "      assertTransientNodes( graph, expectedNodes, \"" + scope + "\" );" );
    }

    private void writeTransitiveCompileDepsTest( String classPrefix, DependencyTree tree )
    {
        out.println( "   public void testResolveOfTransitiveCompileDeps()" );
        out.println( "        throws GraphTaskException" );
        out.println( "   {" );
        writeTestProlog( classPrefix );
        writeTransientDependenciesCheck( tree, "compile" );
        out.println( "   }" );
        out.println( "" );
    }

    private void writeTransitiveTestDepsTest( String classPrefix, DependencyTree tree )
    {
        out.println( "   public void testResolveOfTransitiveTestDeps()" );
        out.println( "        throws GraphTaskException" );
        out.println( "   {" );
        writeTestProlog( classPrefix );
        writeTransientDependenciesCheck( tree, "test" );
        out.println( "   }" );
        out.println( "" );
    }
}
