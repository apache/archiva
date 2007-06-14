package org.apache.maven.archiva.dependency.graph.walk;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.tasks.FlagCyclicEdgesTask;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.List;

import junit.framework.TestCase;

/**
 * DependencyGraphWalkerTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphWalkerTest
    extends TestCase
{
    /**
     * <pre>
     *  [foo-util] ---&gt; [foo-common]
     *      \
     *       ---------&gt; [foo-xml] ---&gt; [xercesImpl] ---&gt; [xmlParserAPIs]
     *                        \  \
     *                         \  ---&gt; [jdom] ----+
     *                          \                 |
     *                           ----&gt; [jaxen] &lt;--+
     * </pre>
     */
    public void testModerateWalk()
    {
        DependencyGraph graph = new DependencyGraph( "org.foo", "foo-util", "1.0" );
        String rootKey = DependencyGraphKeys.toKey( graph.getRootNode().getArtifact() );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-common:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-xml:1.0::jar" ) );

        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "xerces:xercesImpl:2.2.1::jar" ) );
        addEdgeAndNodes( graph, toEdge( "xerces:xercesImpl:2.2.1::jar", "xerces:xmlParserAPIs:2.2.1::jar" ) );
        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "jdom:jdom:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "jaxen:jaxen:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( "jdom:jdom:1.0::jar", "jaxen:jaxen:1.0::jar" ) );

        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        WalkCollector walkCollector = new WalkCollector();
        walker.visit( graph, walkCollector );

        String expectedPath[] = new String[] {
            rootKey,
            "org.foo:foo-common:1.0::jar",
            "org.foo:foo-xml:1.0::jar",
            "jaxen:jaxen:1.0::jar",
            "xerces:xercesImpl:2.2.1::jar",
            "xerces:xmlParserAPIs:2.2.1::jar",
            "jdom:jdom:1.0::jar" };

        assertVisitor( walkCollector, 1, 7, 7 );
        assertPath( expectedPath, walkCollector.getCollectedPath() );
    }

    /**
     * <pre>
     *  [foo-util] ---&gt; [foo-common]
     *      \
     *       ---------&gt; [foo-xml] ---&gt; [xercesImpl] ---&gt; [xmlParserAPIs]
     * </pre>
     */
    public void testSimpleWalk()
    {
        DependencyGraph graph = new DependencyGraph( "org.foo", "foo-util", "1.0" );
        String rootKey = DependencyGraphKeys.toKey( graph.getRootNode().getArtifact() );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-common:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-xml:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "xerces:xercesImpl:2.2.1::jar" ) );
        addEdgeAndNodes( graph, toEdge( "xerces:xercesImpl:2.2.1::jar", "xerces:xmlParserAPIs:2.2.1::jar" ) );

        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        WalkCollector walkCollector = new WalkCollector();
        walker.visit( graph, walkCollector );

        String expectedPath[] = new String[] {
            rootKey,
            "org.foo:foo-common:1.0::jar",
            "org.foo:foo-xml:1.0::jar",
            "xerces:xercesImpl:2.2.1::jar",
            "xerces:xmlParserAPIs:2.2.1::jar" };

        assertVisitor( walkCollector, 1, 5, 4 );
        assertPath( expectedPath, walkCollector.getCollectedPath() );
    }

    /**
     * <pre>
     *  [foo-util] ---&gt; [foo-common]
     *      \
     *       \              +----------------------------------------+
     *        \             v                                        |
     *         -------&gt; [foo-xml] ---&gt; [xercesImpl] ---&gt; [xmlParserAPIs]
     *                        \  \
     *                         \  ---&gt; [jdom] ----+
     *                          \                 |
     *                           ----&gt; [jaxen] &lt;--+
     * </pre>
     */
    public void testDeepNodeWalk()
    {
        DependencyGraph graph = new DependencyGraph( "org.foo", "foo-util", "1.0" );
        String rootKey = DependencyGraphKeys.toKey( graph.getRootNode().getArtifact() );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-common:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( rootKey, "org.foo:foo-xml:1.0::jar" ) );

        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "xerces:xercesImpl:2.2.1::jar" ) );
        addEdgeAndNodes( graph, toEdge( "xerces:xercesImpl:2.2.1::jar", "xerces:xmlParserAPIs:2.2.1::jar" ) );
        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "jdom:jdom:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( "org.foo:foo-xml:1.0::jar", "jaxen:jaxen:1.0::jar" ) );
        addEdgeAndNodes( graph, toEdge( "jdom:jdom:1.0::jar", "jaxen:jaxen:1.0::jar" ) );
        // introduce cyclic dep. intentional. should only result in walking to foo-xml once. 
        addEdgeAndNodes( graph, toEdge( "xerces:xmlParserAPIs:2.2.1::jar", "org.foo:foo-xml:1.0::jar" ) );

        new FlagCyclicEdgesTask().executeTask( graph );

        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        WalkCollector walkCollector = new WalkCollector();
        ArtifactReference startRef = toArtifactReference( "org.foo:foo-xml:1.0::jar" );
        DependencyGraphNode startNode = new DependencyGraphNode( startRef );
        walker.visit( graph, startNode, walkCollector );

        String expectedPath[] = new String[] {
            "org.foo:foo-xml:1.0::jar",
            "jaxen:jaxen:1.0::jar",
            "xerces:xercesImpl:2.2.1::jar",
            "xerces:xmlParserAPIs:2.2.1::jar",
            "jdom:jdom:1.0::jar" };

        assertVisitor( walkCollector, 1, 5, 6 );
        assertPath( expectedPath, walkCollector.getCollectedPath() );
    }

    private void addEdgeAndNodes( DependencyGraph graph, DependencyGraphEdge edge )
    {
        ensureNodeExists( graph, edge.getNodeFrom() );
        ensureNodeExists( graph, edge.getNodeTo() );
        graph.addEdge( edge );
    }

    private void ensureNodeExists( DependencyGraph graph, ArtifactReference artifact )
    {
        DependencyGraphNode node = graph.getNode( artifact );
        if ( node == null )
        {
            node = new DependencyGraphNode( artifact );
            graph.addNode( node );
        }
    }

    private void assertPath( String[] expectedPath, List collectedPath )
    {
        assertEquals( "Path.length", expectedPath.length, collectedPath.size() );

        for ( int i = 0; i < expectedPath.length; i++ )
        {
            assertEquals( "Walk path[" + i + "]", expectedPath[i], (String) collectedPath.get( i ) );
        }
    }

    private void assertVisitor( WalkCollector walkCollector, int countGraphs, int countNodes, int countEdges )
    {
        assertEquals( "Count of graph discovery.", countGraphs, walkCollector.getCountDiscoverGraph() );
        assertEquals( "Count of graph finished.", countGraphs, walkCollector.getCountFinishGraph() );
        assertEquals( "Discover - Finish = 0 (on graph counts)", 0,
                      ( walkCollector.getCountDiscoverGraph() - walkCollector.getCountFinishGraph() ) );

        assertEquals( "Count of node discovery.", countNodes, walkCollector.getCountDiscoverNode() );
        assertEquals( "Count of node finished.", countNodes, walkCollector.getCountFinishNode() );
        assertEquals( "Discover - Finish = 0 (on node counts)", 0,
                      ( walkCollector.getCountDiscoverNode() - walkCollector.getCountFinishNode() ) );

        assertEquals( "Count of edge discovery.", countEdges, walkCollector.getCountDiscoverEdge() );
        assertEquals( "Count of edge finished.", countEdges, walkCollector.getCountFinishEdge() );
        assertEquals( "Discover - Finish = 0 (on edge counts)", 0,
                      ( walkCollector.getCountDiscoverEdge() - walkCollector.getCountFinishEdge() ) );
    }

    private DependencyGraphEdge toEdge( String fromKey, String toKey )
    {
        ArtifactReference nodeFrom = toArtifactReference( fromKey );
        ArtifactReference nodeTo = toArtifactReference( toKey );

        return new DependencyGraphEdge( nodeFrom, nodeTo );
    }

    private ArtifactReference toArtifactReference( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );
        assertEquals( "ArtifactReference [" + key + "] parts should equal 5", 5, parts.length );

        ArtifactReference artifact = new ArtifactReference();
        artifact.setGroupId( parts[0] );
        artifact.setArtifactId( parts[1] );
        artifact.setVersion( parts[2] );
        artifact.setClassifier( parts[3] );
        artifact.setType( parts[4] );

        return artifact;
    }
}
