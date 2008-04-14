package org.apache.maven.archiva.dependency.graph;

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

import junit.framework.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.DependencyGraphFactory;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.VersionedReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * GraphvizDotTool - testing utility to help understand the graph. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GraphvizDotTool
    implements GraphListener
{
    private int phaseNumber = 0;

    protected VersionedReference toVersionedReference( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );
        Assert.assertEquals( "Versioned Reference [" + key + "] part count.", 3, parts.length );

        VersionedReference ref = new VersionedReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );
        return ref;
    }

    private DependencyGraph getDependencyGraph( MemoryRepository repository, String rootRefKey )
        throws GraphTaskException
    {
        MemoryRepositoryDependencyGraphBuilder graphBuilder = new MemoryRepositoryDependencyGraphBuilder();
        graphBuilder.setMemoryRepository( repository );

        // Create the factory, and add the test resolver.
        DependencyGraphFactory factory = new DependencyGraphFactory();
        factory.setGraphBuilder( graphBuilder );
        factory.setDesiredScope( DependencyScope.TEST );
        factory.addGraphListener( this );

        // Get the model to resolve from
        VersionedReference rootRef = toVersionedReference( rootRefKey );

        // Perform the resolution.
        phaseNumber = 0;
        DependencyGraph graph = factory.getGraph( rootRef );

        // Test the results.
        Assert.assertNotNull( "Graph shouldn't be null.", graph );

        return graph;
    }

    public void testGenerateDots()
        throws GraphTaskException
    {
        getDependencyGraph( new ArchivaWebappMemoryRepository(),
                            "org.apache.maven.archiva:archiva-webapp:1.0-alpha-2-SNAPSHOT" );

        //        getDependencyGraph( new ArchivaCommonMemoryRepository(),
        //                            "org.apache.maven.archiva:archiva-common:1.0-alpha-2-SNAPSHOT" );
        //
        //        getDependencyGraph( new ArchivaXmlToolsMemoryRepository(),
        //                            "org.apache.maven.archiva:archiva-xml-tools:1.0-alpha-2-SNAPSHOT" );
        //
        //        getDependencyGraph( new ContinuumStoreMemoryRepository(),
        //                            "org.apache.maven.continuum:continuum-store:1.1-SNAPSHOT" );
        //
        //        getDependencyGraph( new MavenProjectInfoReportsPluginMemoryRepository(),
        //                            "org.apache.maven.plugins:maven-project-info-reports-plugin:2.1-SNAPSHOT" );
        //
        //        getDependencyGraph( new WagonManagerMemoryRepository(), "org.apache.maven.wagon:wagon-manager:2.0-SNAPSHOT" );

        getDependencyGraph( new DepManDeepVersionMemoryRepository(), "net.example.depman.deepversion:A:1.0" );
    }

    public void dependencyResolutionEvent( DependencyResolutionEvent event )
    {
        /* do nothing */
    }

    public void graphError( GraphTaskException e, DependencyGraph currentGraph )
    {
        /* do nothing */
    }

    public void graphPhaseEvent( GraphPhaseEvent event )
    {
        String graphId = event.getGraph().getRootNode().getArtifact().getArtifactId();
        String title = "Graph: " + graphId;

        switch ( event.getType() )
        {
            case GraphPhaseEvent.GRAPH_TASK_POST:
                phaseNumber++;
                title += " - Phase: " + phaseNumber + " - Task: " + event.getTask().getTaskId();
                writeDot( "target/graph_" + graphId + "_" + phaseNumber + "_" + event.getTask().getTaskId() + ".dot",
                          event.getGraph(), title );
                break;
            case GraphPhaseEvent.GRAPH_DONE:
                title += " FINISHED";
                writeDot( "target/graph_" + graphId + ".dot", event.getGraph(), title );
                break;
        }
    }

    private void writeDot( String outputFilename, DependencyGraph graph, String title )
    {
        System.out.println( "Writing Graphviz output: " + outputFilename );
        try
        {
            File outputFile = new File( outputFilename );
            FileWriter writer = new FileWriter( outputFile );
            PrintWriter dot = new PrintWriter( writer );

            dot.println( "// Auto generated dot file from plexus-graph-visualizer-graphviz." );

            dot.println( "digraph example {" );

            dot.println( "" );

            dot.println( "  // Graph Defaults" );
            dot.println( "  graph [" );
            dot.println( "    bgcolor=\"#ffffff\"," );
            dot.println( "    fontname=\"Helvetica\"," );
            dot.println( "    fontsize=\"11\"," );
            dot.println( "    label=\"" + title + "\"," );
            dot.println( "    labeljust=\"l\"" );
            dot.println( "    rankdir=\"LR\"" );
            dot.println( "  ];" );

            // Node Defaults.

            dot.println( "" );
            dot.println( "  // Node Defaults." );
            dot.println( "  node [" );
            dot.println( "    fontname=\"Helvetica\"," );
            dot.println( "    fontsize=\"11\"," );
            dot.println( "    shape=\"box\"" );
            dot.println( "  ];" );

            // Edge Defaults.

            dot.println( "" );
            dot.println( "  // Edge Defaults." );
            dot.println( "  edge [" );
            dot.println( "    arrowsize=\"0.8\"" );
            dot.println( "    fontsize=\"11\"," );
            dot.println( "  ];" );

            Iterator it;

            it = graph.getNodes().iterator();
            while ( it.hasNext() )
            {
                DependencyGraphNode node = (DependencyGraphNode) it.next();

                writeNode( dot, graph, node );
            }

            it = graph.getEdges().iterator();
            while ( it.hasNext() )
            {
                DependencyGraphEdge edge = (DependencyGraphEdge) it.next();

                DependencyGraphNode from = graph.getNode( edge.getNodeFrom() );
                DependencyGraphNode to = graph.getNode( edge.getNodeTo() );

                writeEdge( dot, edge, from, to );
            }

            dot.println( "}" );
            dot.flush();
            dot.close();
        }
        catch ( IOException e )
        {
            System.err.println( "Unable to write GraphViz file " + outputFilename + " : " + e.getMessage() );
            e.printStackTrace( System.err );
        }
    }

    private String toLabel( DependencyGraphNode node )
    {
        StringBuffer lbl = new StringBuffer();

        lbl.append( node.getArtifact().getGroupId() ).append( "\n" );
        lbl.append( node.getArtifact().getArtifactId() ).append( "\n" );
        lbl.append( node.getArtifact().getVersion() );

        return StringEscapeUtils.escapeJava( lbl.toString() );
    }

    private String toId( DependencyGraphNode node )
    {
        StringBuffer id = new StringBuffer();

        String raw = DependencyGraphKeys.toKey( node.getArtifact() );

        for ( int i = 0; i < raw.length(); i++ )
        {
            char c = raw.charAt( i );
            if ( Character.isLetterOrDigit( c ) )
            {
                id.append( Character.toUpperCase( c ) );
            }
            else if ( ( c == '-' ) || ( c == '_' ) )
            {
                id.append( "_" );
            }
        }

        return id.toString();
    }

    private void writeNode( PrintWriter dot, DependencyGraph graph, DependencyGraphNode node )
    {
        dot.println( "" );
        dot.println( "  // Node" );
        dot.println( "  \"" + toId( node ) + "\" [" );
        dot.println( "    label=\"" + toLabel( node ) + "\"," );

        List edgesTo = graph.getEdgesTo( node );
        boolean orphan = CollectionUtils.isEmpty( edgesTo );

        if ( node.isFromParent() )
        {
            dot.println( "    color=\"#FF0000\"," );
            dot.println( "    shape=ellipse," );
        }
        else
        {
            dot.println( "    shape=box," );
        }

        if ( node.isConflicted() )
        {
            // dot.println( "    fontcolor=\"#FF88FF\"," );
            dot.println( "    style=filled," );
            dot.println( "    fillcolor=\"#88FF88\"," );
        }
        else if ( orphan )
        {
            dot.println( "    style=filled," );
            dot.println( "    fillcolor=\"#8888FF\"," );
        }

        dot.println( "  ];" );
    }

    private void writeEdge( PrintWriter dot, DependencyGraphEdge edge, DependencyGraphNode from, DependencyGraphNode to )
    {
        dot.println( "" );
        dot.println( "  // Edge" );

        dot.println( "  \"" + toId( from ) + "\" -> \"" + toId( to ) + "\" [" );

        if ( edge.isDisabled() )
        {
            switch ( edge.getDisabledType() )
            {
                case DependencyGraph.DISABLED_CYCLIC:
                    dot.println( "    color=\"#FF0000\"," );
                    break;
                case DependencyGraph.DISABLED_OPTIONAL:
                    dot.println( "    color=\"#FF00FF\"," );
                    break;
                case DependencyGraph.DISABLED_NEARER_DEP:
                    dot.println( "    color=\"#00FF00\"," );
                    break;
                case DependencyGraph.DISABLED_NEARER_EDGE:
                    dot.println( "    color=\"#88FF88\"," );
                    break;
                default:
                case DependencyGraph.DISABLED_EXCLUDED:
                    dot.println( "    color=\"#0000FF\"," );
                    break;
            }

            dot.println( "    label=\"" + edge.getDisabledReason() + "\"," );
            dot.println( "    fontsize=\"8\"," );
        }
        else if ( DependencyScope.TEST.equals( edge.getScope() ) )
        {
            dot.println( "    style=\"dashed\"," );
            dot.println( "    color=\"#DDDDDD\"," );
        }
        else if ( DependencyScope.RUNTIME.equals( edge.getScope() ) )
        {
            dot.println( "    style=\"dashed\"," );
            dot.println( "    color=\"#DDFFDD\"," );
            dot.println( "    label=\"runtime\"," );
            dot.println( "    fontsize=\"8\"," );
        }
        else if ( DependencyScope.PROVIDED.equals( edge.getScope() ) )
        {
            dot.println( "    style=\"dashed\"," );
            dot.println( "    color=\"#DDDDFF\"," );
            dot.println( "    label=\"provided\"," );
            dot.println( "    fontsize=\"8\"," );
        }
        else if ( DependencyScope.SYSTEM.equals( edge.getScope() ) )
        {
            dot.println( "    style=\"dashed\"," );
            dot.println( "    color=\"#FFDDDD\"," );
            dot.println( "    label=\"system\"," );
            dot.println( "    fontsize=\"8\"," );
        }

        dot.println( "    arrowtail=none," );
        dot.println( "    arrowhead=normal" );

        dot.println( "  ];" );
    }

}
