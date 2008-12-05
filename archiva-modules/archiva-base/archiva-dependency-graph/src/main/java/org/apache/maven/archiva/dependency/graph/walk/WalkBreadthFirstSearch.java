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

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.functors.EdgeDisabledPredicate;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * WalkBreadthFirstSearch 
 *
 * @version $Id$
 */
public class WalkBreadthFirstSearch
    implements DependencyGraphWalker
{
    private Map nodeVisitStates = new HashMap();

    private Predicate edgePredicate;

    public WalkBreadthFirstSearch()
    {
        this.edgePredicate = NotPredicate.getInstance( new EdgeDisabledPredicate() );
    }

    public Predicate getEdgePredicate()
    {
        return this.edgePredicate;
    }

    public void setEdgePredicate( Predicate edgePredicate )
    {
        this.edgePredicate = edgePredicate;
    }

    public Integer getNodeVisitState( DependencyGraphNode node )
    {
        return (Integer) nodeVisitStates.get( node.getArtifact() );
    }

    public Integer getNodeVisitState( ArtifactReference artifact )
    {
        return (Integer) nodeVisitStates.get( artifact );
    }

    public void setNodeVisitState( DependencyGraphNode node, Integer state )
    {
        this.nodeVisitStates.put( node.getArtifact(), state );
    }

    public void setNodeVisitState( ArtifactReference artifact, Integer state )
    {
        this.nodeVisitStates.put( artifact, state );
    }

    private void visitEdge( DependencyGraph graph, DependencyGraphEdge e, DependencyGraphVisitor visitor )
    {
        visitor.discoverEdge( e );

        DependencyGraphNode node = graph.getNode( e.getNodeTo() );

        if ( getNodeVisitState( node ) == UNSEEN )
        {
            setNodeVisitState( node, PROCESSING );
        }

        visitor.finishEdge( e );
    }

    private void visitNode( DependencyGraph graph, DependencyGraphNode node, DependencyGraphVisitor visitor )
    {
        setNodeVisitState( node, PROCESSING );

        visitor.discoverNode( node );

        Iterator edges;
        // First dive down edges.
        edges = graph.getEdgesFrom( node ).iterator();
        while ( edges.hasNext() )
        {
            DependencyGraphEdge e = (DependencyGraphEdge) edges.next();
            if ( this.edgePredicate.evaluate( e ) )
            {
                visitEdge( graph, e, visitor );
            }
        }

        // Next move down edges.
        edges = graph.getEdgesFrom( node ).iterator();
        while ( edges.hasNext() )
        {
            DependencyGraphEdge e = (DependencyGraphEdge) edges.next();

            if ( this.edgePredicate.evaluate( e ) )
            {
                DependencyGraphNode nodeTo = graph.getNode( e.getNodeTo() );
                Integer state = getNodeVisitState( nodeTo );
                if ( ( state == UNSEEN ) || ( state == PROCESSING ) )
                {
                    visitNode( graph, nodeTo, visitor );
                }
            }
        }

        visitor.finishNode( node );

        setNodeVisitState( node, SEEN );
    }

    public void visit( DependencyGraph graph, DependencyGraphVisitor visitor )
    {
        visit( graph, graph.getRootNode(), visitor );
    }

    public void visit( DependencyGraph graph, DependencyGraphNode startNode, DependencyGraphVisitor visitor )
    {
        nodeVisitStates.clear();

        Iterator nodes = graph.getNodes().iterator();
        while ( nodes.hasNext() )
        {
            setNodeVisitState( (DependencyGraphNode) nodes.next(), UNSEEN );
        }

        visitor.discoverGraph( graph );

        visitNode( graph, startNode, visitor );

        visitor.finishGraph( graph );
    }
}
