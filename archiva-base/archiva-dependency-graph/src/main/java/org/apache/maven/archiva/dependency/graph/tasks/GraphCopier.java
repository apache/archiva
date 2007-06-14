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
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;

/**
 * GraphCopier 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GraphCopier
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    protected DependencyGraph copiedGraph;

    public DependencyGraph getGraph()
    {
        return copiedGraph;
    }

    public void setGraph( DependencyGraph graph )
    {
        this.copiedGraph = graph;
    }

    public void discoverNode( DependencyGraphNode node )
    {
        if ( copiedGraph == null )
        {
            copiedGraph = new DependencyGraph( node );
        }
    }

    /**
     * Be sure to override and NOT call this method in your sub class,
     * if you want to copy edges based on some kind of criteria.
     */
    public void discoverEdge( DependencyGraphEdge edge )
    {
        copyEdge( edge );
    }

    public void copyEdge( DependencyGraphEdge edge )
    {
        DependencyGraphNode nodeFrom = graph.getNode( edge.getNodeFrom() );
        DependencyGraphNode nodeTo = graph.getNode( edge.getNodeTo() );

        this.copiedGraph.addNode( nodeFrom );
        this.copiedGraph.addNode( nodeTo );
        this.copiedGraph.addEdge( edge );
    }

    public void reset()
    {
        this.copiedGraph = null;
    }
}
