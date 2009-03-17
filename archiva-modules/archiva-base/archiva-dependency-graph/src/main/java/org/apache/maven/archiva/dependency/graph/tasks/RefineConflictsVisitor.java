package org.apache.maven.archiva.dependency.graph.tasks;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.collections.list.TypedList;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.DependencyGraphUtils;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.model.ArtifactReference;

/**
 * RefineConflictsVisitor 
 *
 * @version $Id$
 */
public class RefineConflictsVisitor
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    class DepthComparator
        implements Comparator<NodeLocation>
    {
        public int compare( NodeLocation obj0, NodeLocation obj1 )
        {
            return obj0.depth - obj1.depth;
        }
    }

    class NodeLocation
    {
        public ArtifactReference artifact;

        public DependencyGraphEdge edge;

        public int depth;

        public NodeLocation( ArtifactReference artifact, DependencyGraphEdge edge, int depth )
        {
            this.artifact = artifact;
            this.edge = edge;
            this.depth = depth;
        }
    }

    class NodeLocationPredicate
        implements Predicate
    {
        private ArtifactReference artifact;

        public NodeLocationPredicate( ArtifactReference artifact )
        {
            this.artifact = artifact;
        }

        public NodeLocationPredicate( DependencyGraphNode node )
        {
            this( node.getArtifact() );
        }

        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof NodeLocation )
            {
                NodeLocation nodeloc = (NodeLocation) object;
                satisfies = nodeloc.artifact.equals( artifact );
            }

            return satisfies;
        }

    }

    class NodeLocationVersionComparator
        implements Comparator<NodeLocation>
    {
        public int compare( NodeLocation o1, NodeLocation o2 )
        {
            if ( o1 == null && o2 == null )
            {
                return 0;
            }

            if ( o1 == null && o2 != null )
            {
                return 1;
            }

            if ( o1 != null && o2 == null )
            {
                return -1;
            }

            String version1 = o1.artifact.getVersion();
            String version2 = o2.artifact.getVersion();

            return VersionComparator.getInstance().compare( version1, version2 );
        }
    }

    class DistantNodeLocationPredicate
        implements Predicate
    {
        private int cutoff;

        public DistantNodeLocationPredicate( int distantCutoff )
        {
            this.cutoff = distantCutoff;
        }

        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof NodeLocation )
            {
                NodeLocation nodeloc = (NodeLocation) object;
                satisfies = ( nodeloc.depth >= this.cutoff );
            }

            return satisfies;
        }
    }

    private List<DependencyGraphNode> conflictingArtifacts;

    private Map<String,NodeLocation> foundNodesMap = new HashMap<String, NodeLocation>();

    private int currentDepth = 0;

    private DependencyGraph currentGraph;

    @SuppressWarnings("unchecked")
    public RefineConflictsVisitor()
    {
        conflictingArtifacts = TypedList.decorate( new ArrayList<ArtifactReference>(), ArtifactReference.class );
    }

    public void discoverGraph( DependencyGraph graph )
    {
        super.discoverGraph( graph );
        this.currentGraph = graph;
        this.foundNodesMap.clear();
    }

    public void discoverNode( DependencyGraphNode node )
    {
        super.discoverNode( node );

        currentDepth++;

        List<DependencyGraphEdge> edgesFrom = currentGraph.getEdgesFrom( node );
        for ( DependencyGraphEdge edge : edgesFrom )
        {
            if ( this.conflictingArtifacts.contains( edge.getNodeTo() ) )
            {
                String nodeKey = DependencyGraphKeys.toKey( edge.getNodeTo() );
                // Check for existing NodeLocation with same key
                NodeLocation nodeloc = this.foundNodesMap.get( nodeKey );

                if ( ( nodeloc == null ) || ( currentDepth < nodeloc.depth ) )
                {
                    nodeloc = new NodeLocation( edge.getNodeTo(), edge, currentDepth );
                    this.foundNodesMap.put( nodeKey, nodeloc );
                }
            }
        }
    }

    public void finishGraph( DependencyGraph graph )
    {
        super.finishGraph( graph );

        if ( MapUtils.isEmpty( this.foundNodesMap ) )
        {
            return;
        }

        // Find winning node.
        ArtifactReference winningArtifact = findWinningArtifact( this.foundNodesMap.values() );
        DependencyGraphNode winningNode = graph.getNode( winningArtifact );

        // Gather up Losing Nodes.
        Set<NodeLocation> losingNodes = new HashSet<NodeLocation>();
        Predicate losersPredicate = NotPredicate.getInstance( new NodeLocationPredicate( winningArtifact ) );
        CollectionUtils.select( this.foundNodesMap.values(), losersPredicate, losingNodes );

        // Swing losing nodes to winning node.
        for ( NodeLocation losingNodeLoc : losingNodes )
        {
            DependencyGraphNode losingNode = graph.getNode( losingNodeLoc.artifact );
            DependencyGraphUtils.collapseNodes( graph, losingNode, winningNode );
        }
    }

    @SuppressWarnings("unchecked")
    private ArtifactReference findWinningArtifact( Collection<NodeLocation> nodes )
    {
        List<NodeLocation> remainingNodes = new ArrayList<NodeLocation>();
        remainingNodes.addAll( nodes );

        /* .\ Filter by Depth \.____________________________________________________ */

        // Sort by depth.
        Collections.sort( remainingNodes, new DepthComparator() );

        // Determine 'closest' node depth.
        NodeLocation nearestNode = remainingNodes.get( 0 );
        int nearest = nearestNode.depth;

        // Filter out distant nodes. 
        Predicate distantLocations = new DistantNodeLocationPredicate( nearest );
        CollectionUtils.filter( remainingNodes, distantLocations );

        // Do we have 1 node left?
        if ( remainingNodes.size() == 1 )
        {
            // A winner!
            NodeLocation nodeloc = remainingNodes.get( 0 );
            return nodeloc.artifact;
        }

        /* .\ Filter by Newest Version \.___________________________________________ */

        // We have 2 or more nodes that are equal distance from the root.
        // Determine which one is 'newest' based on version id.
        Collections.sort( remainingNodes, new ReverseComparator( new NodeLocationVersionComparator() ) );

        NodeLocation nodeloc = remainingNodes.get( 0 );
        return nodeloc.artifact;
    }

    public void finishNode( DependencyGraphNode node )
    {
        super.finishNode( node );
        currentDepth--;
    }

    public List<DependencyGraphNode> getConflictingArtifacts()
    {
        return conflictingArtifacts;
    }

    public void addAllConflictingArtifacts( Collection<DependencyGraphNode> nodes )
    {
        this.conflictingArtifacts.addAll( nodes );
    }

    public void resetConflictingArtifacts()
    {
        this.conflictingArtifacts.clear();
    }
}
