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

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Perform a transitive reduction of the graph. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
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
        implements Comparator
    {
        public int compare( Object obj0, Object obj1 )
        {
            EdgeInfo edgeInfo0 = (EdgeInfo) obj0;
            EdgeInfo edgeInfo1 = (EdgeInfo) obj1;

            return edgeInfo0.depth - edgeInfo1.depth;
        }
    }

    /**
     * A Map of &lt;(Node To) ArtifactReference, Map of &lt;(Node From) ArtifactReference, EdgeInfo&gt;&gt;
     */
    private Map /*<ArtifactReference,<ArtifactReference,EdgeInfo>>*/nodeDistanceMap = new HashMap();

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
        Map edgeInfoMap = (Map) nodeDistanceMap.get( nodeTo );

        // Create sub-map if not present (yet)
        if ( edgeInfoMap == null )
        {
            edgeInfoMap = new HashMap();
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

        Comparator edgeInfoDepthComparator = new EdgeInfoDepthComparator();

        Iterator it = nodeDistanceMap.values().iterator();
        while ( it.hasNext() )
        {
            Map edgeInfoMap = (Map) it.next();

            if ( edgeInfoMap.size() > 1 )
            {
                List edgeInfos = new ArrayList();
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
