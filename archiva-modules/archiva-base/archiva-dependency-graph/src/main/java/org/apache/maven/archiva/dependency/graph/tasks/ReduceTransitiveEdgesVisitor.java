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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;

/**
 * Perform a transitive reduction of the graph. 
 *
 * @version $Id$
 */
public class ReduceTransitiveEdgesVisitor
    extends AbstractReduceEdgeVisitor
    implements DependencyGraphVisitor
{
    class EdgeInfo
    {
        public DependencyGraphEdge edge;

        public int depth = Integer.MAX_VALUE;
    }

    class EdgeInfoDepthComparator
        implements Comparator<EdgeInfo>
    {
        public int compare( EdgeInfo obj0, EdgeInfo obj1 )
        {
            return obj0.depth - obj1.depth;
        }
    }

    /**
     * A Map of &lt;(Node To) ArtifactReference, Map of &lt;(Node From) ArtifactReference, EdgeInfo&gt;&gt;
     */
    private Map<String, Map<String, EdgeInfo>> nodeDistanceMap = new HashMap<String, Map<String, EdgeInfo>>();

    private int currentDepth;

    public void discoverGraph( DependencyGraph graph )
    {
        super.discoverGraph( graph );
        nodeDistanceMap.clear();
        currentDepth = 0;
    }

    public void discoverEdge( DependencyGraphEdge edge )
    {
        /* WARNING: it is unwise to remove the edge at this point.
         *          as modifying the graph as it's being walked is dangerous.
         *          
         * Just record the edge's current depth.
         */

        String nodeTo = DependencyGraphKeys.toKey( edge.getNodeTo() );
        String nodeFrom = DependencyGraphKeys.toKey( edge.getNodeFrom() );

        // Get sub-map
        Map<String,EdgeInfo> edgeInfoMap = nodeDistanceMap.get( nodeTo );

        // Create sub-map if not present (yet)
        if ( edgeInfoMap == null )
        {
            edgeInfoMap = new HashMap<String,EdgeInfo>();
            nodeDistanceMap.put( nodeTo, edgeInfoMap );
        }

        // Get sub-map-value.
        EdgeInfo edgeInfo = (EdgeInfo) edgeInfoMap.get( nodeFrom );

        if ( edgeInfo == null )
        {
            // Create a new edgeinfo.
            edgeInfo = new EdgeInfo();
            edgeInfo.edge = edge;
            edgeInfo.depth = currentDepth;
            edgeInfoMap.put( nodeFrom, edgeInfo );
        }
        // test the current depth, if it is less than previous depth, save it
        else if ( currentDepth < edgeInfo.depth )
        {
            edgeInfo.depth = currentDepth;
            edgeInfoMap.put( nodeFrom, edgeInfo );
        }

        nodeDistanceMap.put( nodeTo, edgeInfoMap );
    }

    public void discoverNode( DependencyGraphNode node )
    {
        super.discoverNode( node );
        currentDepth++;

    }

    public void finishNode( DependencyGraphNode node )
    {
        super.finishNode( node );
        currentDepth--;
    }

    public void finishGraph( DependencyGraph graph )
    {
        super.finishGraph( graph );

        // Now we prune/remove the edges that are transitive in nature.

        Comparator<EdgeInfo> edgeInfoDepthComparator = new EdgeInfoDepthComparator();

        for ( Map<String, EdgeInfo> edgeInfoMap : nodeDistanceMap.values() )
        {
            if ( edgeInfoMap.size() > 1 )
            {
                List<EdgeInfo> edgeInfos = new ArrayList<EdgeInfo>();
                edgeInfos.addAll( edgeInfoMap.values() );
                Collections.sort( edgeInfos, edgeInfoDepthComparator );

                for ( int i = 1; i < edgeInfos.size(); i++ )
                {
                    EdgeInfo edgeInfo = (EdgeInfo) edgeInfos.get( i );
                    graph.removeEdge( edgeInfo.edge );
                }
            }
        }
    }
}
