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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.AndPredicate;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.functors.EdgeExactScopePredicate;
import org.apache.maven.archiva.dependency.graph.functors.EdgeFromPredicate;
import org.apache.maven.archiva.dependency.graph.functors.NodeFromParentPredicate;
import org.apache.maven.archiva.dependency.graph.functors.NodePredicate;
import org.apache.maven.archiva.dependency.graph.functors.ToKeyTransformer;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.VersionedReference;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * AbstractDependencyGraphFactoryTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractDependencyGraphFactoryTestCase
    extends PlexusInSpringTestCase
{
    public class ExpectedEdge
    {
        public String from;

        public String to;

        public ExpectedEdge( String from, String to )
        {
            this.from = from;
            this.to = to;
        }
    }

    public class GraphEdgePredicate
        implements Predicate
    {
        private String edgeFrom;

        private String edgeTo;

        public GraphEdgePredicate( String edgeFrom, String edgeTo )
        {
            this.edgeFrom = edgeFrom;
            this.edgeTo = edgeTo;
        }

        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof DependencyGraphEdge )
            {
                DependencyGraphEdge edge = (DependencyGraphEdge) object;
                String actualFrom = ArtifactReference.toKey( edge.getNodeFrom() );
                String actualTo = ArtifactReference.toKey( edge.getNodeTo() );

                satisfies = ( StringUtils.equals( edgeFrom, actualFrom ) && StringUtils.equals( edgeTo, actualTo ) );
            }

            return satisfies;
        }
    }

    protected void assertDirectNodes( DependencyGraph graph, List expectedNodes, String scope )
    {
        Iterator it;
        DependencyGraphNode rootNode = graph.getRootNode();
        List rootEdges = graph.getEdgesFrom( rootNode );
        List actualEdges = new ArrayList();

        Predicate directDep = NotPredicate.getInstance( new NodeFromParentPredicate() );
        Predicate scopedDirectDeps = AndPredicate.getInstance( new EdgeExactScopePredicate( scope ), directDep );
        CollectionUtils.select( rootEdges, scopedDirectDeps, actualEdges );
        // CollectionUtils.select( rootEdges, new EdgeExactScopePredicate( scope ), actualEdges );

        if ( expectedNodes.size() != actualEdges.size() )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "Direct node.count with <" ).append( scope ).append( "> edges from [" );
            sb.append( DependencyGraphKeys.toKey( rootNode.getArtifact() ) ).append( "]" ).append( " expected:<" );
            sb.append( expectedNodes.size() ).append( "> but was:<" );
            sb.append( actualEdges.size() ).append( ">" );

            CollectionUtils.transform( actualEdges, new ToKeyTransformer() );

            Collection missingActualKeys = CollectionUtils.subtract( actualEdges, expectedNodes );
            it = missingActualKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Actual) " ).append( (String) it.next() );
            }

            Collection missingExpectedKeys = CollectionUtils.subtract( expectedNodes, actualEdges );
            it = missingExpectedKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Expected) " ).append( (String) it.next() );
            }

            fail( sb.toString() );
        }

        it = actualEdges.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphEdge edge = (DependencyGraphEdge) it.next();
            String actualKey = DependencyGraphKeys.toKey( edge.getNodeTo() );
            assertTrue( "Direct <" + scope + "> node To [" + actualKey + "] exists in expectedNodes.", expectedNodes
                .contains( actualKey ) );
        }
    }

    protected void assertEdges( DependencyGraph graph, List expectedEdges )
    {
        assertNotNull( "Graph.edges should never be null.", graph.getEdges() );
        assertEquals( "Graph.edges.size()", expectedEdges.size(), graph.getEdges().size() );

        Iterator it = expectedEdges.iterator();
        while ( it.hasNext() )
        {
            ExpectedEdge expectedEdge = (ExpectedEdge) it.next();
            Predicate edgePredicate = new GraphEdgePredicate( expectedEdge.from, expectedEdge.to );

            DependencyGraphEdge edge = (DependencyGraphEdge) CollectionUtils.find( graph.getEdges(), edgePredicate );
            if ( edge == null )
            {
                fail( "Unable to find expected edge from:<" + expectedEdge.from + "> to:<" + expectedEdge.to + ">" );
            }
        }
    }

    protected void assertGraph( DependencyGraph graph, String rootRefKey, List expectedNodeKeys )
    {
        assertNotNull( "Graph.nodes should never be null.", graph.getNodes() );
        assertTrue( "Graph.nodes.size() should always be 1 or better.", graph.getNodes().size() >= 1 );

        ArtifactReference rootRef = graph.getRootNode().getArtifact();
        StringBuffer actualRootRef = new StringBuffer();
        actualRootRef.append( rootRef.getGroupId() ).append( ":" );
        actualRootRef.append( rootRef.getArtifactId() ).append( ":" );
        actualRootRef.append( rootRef.getVersion() );

        assertEquals( "Graph.root", rootRefKey, actualRootRef.toString() );

        Iterator it;
        List actualNodes = new ArrayList();

        Predicate notRootNode = NotPredicate.getInstance( new NodePredicate( graph.getRootNode() ) );
        CollectionUtils.select( graph.getNodes(), notRootNode, actualNodes );

        boolean fail = false;
        StringBuffer sb = new StringBuffer();
        
        if ( expectedNodeKeys.size() != actualNodes.size() )
        {
            sb.append( "node.count expected:<" );
            sb.append( expectedNodeKeys.size() ).append( "> but was:<" );
            sb.append( actualNodes.size() ).append( ">" );
            fail = true;
        }

        CollectionUtils.transform( actualNodes, new ToKeyTransformer() );

        Collection missingActualKeys = CollectionUtils.subtract( actualNodes, expectedNodeKeys );
        it = missingActualKeys.iterator();
        while ( it.hasNext() )
        {
            sb.append( "\n (Extra Actual) " ).append( (String) it.next() );
            fail = true;
        }

        Collection missingExpectedKeys = CollectionUtils.subtract( expectedNodeKeys, actualNodes );
        it = missingExpectedKeys.iterator();
        while ( it.hasNext() )
        {
            sb.append( "\n (Extra Expected) " ).append( (String) it.next() );
            fail = true;
        }

        if( fail )
        {
            fail( sb.toString() );
        }

        /*
        it = actualNodes.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphNode node = (DependencyGraphNode) it.next();
            assertNotNull( "Artifact reference in node should not be null.", node.getArtifact() );
            String key = ArtifactReference.toKey( node.getArtifact() );
            assertTrue( "Artifact reference [" + key + "] should be in expectedNodeKeys.", expectedNodeKeys
                .contains( key ) );
        }
        */
    }

    protected void assertNodes( DependencyGraph graph, List expectedNodeKeys )
    {
        assertNotNull( "Graph.nodes should never be null.", graph.getNodes() );
        assertTrue( "Graph.nodes.size() should always be 1 or better.", graph.getNodes().size() >= 1 );
        // assertEquals( "Graph.nodes.size()", expectedNodeKeys.size(), graph.getNodes().size() );

        Iterator it;
        List actualNodes = new ArrayList();
        actualNodes.addAll( graph.getNodes() );

        if ( expectedNodeKeys.size() != actualNodes.size() )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "node.count expected:<" );
            sb.append( expectedNodeKeys.size() ).append( "> but was:<" );
            sb.append( actualNodes.size() ).append( ">" );

            CollectionUtils.transform( actualNodes, new ToKeyTransformer() );

            Collection missingActualKeys = CollectionUtils.subtract( actualNodes, expectedNodeKeys );
            it = missingActualKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Actual) " ).append( (String) it.next() );
            }

            Collection missingExpectedKeys = CollectionUtils.subtract( expectedNodeKeys, actualNodes );
            it = missingExpectedKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Expected) " ).append( (String) it.next() );
            }

            fail( sb.toString() );
        }

        it = graph.getNodes().iterator();
        while ( it.hasNext() )
        {
            DependencyGraphNode node = (DependencyGraphNode) it.next();
            assertNotNull( "Artifact reference in node should not be null.", node.getArtifact() );
            String key = ArtifactReference.toKey( node.getArtifact() );
            assertTrue( "Artifact reference [" + key + "] should be in expectedNodeKeys.", expectedNodeKeys
                .contains( key ) );
        }
    }

    protected void assertRootNode( DependencyGraph graph, String expectedKey )
    {
        DependencyGraphNode node = graph.getRootNode();

        String actualKey = DependencyGraphKeys.toKey( node.getArtifact() );
        assertEquals( "Root Node", expectedKey, actualKey );
    }

    protected void assertTransientNodes( DependencyGraph graph, List expectedNodes, String scope )
    {
        Iterator it;

        // Gather up the transient nodes from the DependencyGraph.
        List actualEdges = new ArrayList();

        DependencyGraphNode rootNode = graph.getRootNode();

        Predicate transientDep = NotPredicate.getInstance( new EdgeFromPredicate( rootNode.getArtifact() ) );
        Predicate edgeByExactScope = new EdgeExactScopePredicate( scope );
        Predicate transitiveEdgesByScopePredicate = AndPredicate.getInstance( transientDep, edgeByExactScope );

        CollectionUtils.select( graph.getEdges(), transitiveEdgesByScopePredicate, actualEdges );

        if ( expectedNodes.size() != actualEdges.size() )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "Transient node.count with <" ).append( scope ).append( "> edges from [" );
            sb.append( DependencyGraphKeys.toKey( rootNode.getArtifact() ) ).append( "]" ).append( " expected:<" );
            sb.append( expectedNodes.size() ).append( "> but was:<" );
            sb.append( actualEdges.size() ).append( ">" );

            CollectionUtils.transform( actualEdges, new ToKeyTransformer() );

            Collection missingActualKeys = CollectionUtils.subtract( actualEdges, expectedNodes );
            it = missingActualKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Actual) " ).append( (String) it.next() );
            }

            Collection missingExpectedKeys = CollectionUtils.subtract( expectedNodes, actualEdges );
            it = missingExpectedKeys.iterator();
            while ( it.hasNext() )
            {
                sb.append( "\n (Extra Expected) " ).append( (String) it.next() );
            }

            fail( sb.toString() );
        }

        it = actualEdges.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphEdge edge = (DependencyGraphEdge) it.next();
            String actualKey = DependencyGraphKeys.toKey( edge.getNodeTo() );
            assertTrue( "Transient Node To [" + actualKey + "] exists in expectedNodes.", expectedNodes
                .contains( actualKey ) );
        }
    }

    protected Dependency toDependency( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        assertEquals( "Dependency key [" + key + "] should be 5 parts.", 5, parts.length );

        Dependency dep = new Dependency();

        dep.setGroupId( parts[0] );
        dep.setArtifactId( parts[1] );
        dep.setVersion( parts[2] );
        dep.setClassifier( parts[3] );
        dep.setType( parts[4] );

        return dep;
    }

    protected ArchivaProjectModel toModel( String key, Dependency deps[] )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        assertEquals( "Dependency key [" + key + "] should be 3 parts.", 3, parts.length );

        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( parts[0] );
        model.setArtifactId( parts[1] );
        model.setVersion( parts[2] );
        model.setOrigin( "testcase" );
        model.setPackaging( "jar" );

        if ( deps != null )
        {
            for ( int i = 0; i < deps.length; i++ )
            {
                Dependency dep = deps[i];
                model.addDependency( dep );
            }
        }

        return model;
    }

    protected VersionedReference toVersionedReference( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );
        assertEquals( "Versioned Reference [" + key + "] part count.", 3, parts.length );

        VersionedReference ref = new VersionedReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );
        return ref;
    }
}
