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

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;

/**
 * Interface for progress during search.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DependencyGraphVisitor
{
    /**
     * Called once, for when the graph itself is discovered.
     * 
     * @param graph the graph that was discovered.
     */
    public void discoverGraph( DependencyGraph graph );

    /**
     * Called for each node, when that node is visited.
     * 
     * @param node the node that is being visited.
     */
    public void discoverNode( DependencyGraphNode node );

    /**
     * Called for each edge, when that edge is visited.
     * 
     * @param edge the edge that is being visited.
     */
    public void discoverEdge( DependencyGraphEdge edge );

    /**
     * Called for each edge, when that edge has been fully visited.
     * 
     * @param edge the edge that was finished being visited.
     */
    public void finishEdge( DependencyGraphEdge edge );

    /**
     * Called for each node, when the node has been fully visited.
     * 
     * @param node the node that was finished being visited.
     */
    public void finishNode( DependencyGraphNode node );

    /**
     * Called once, for when the graph is finished being visited.
     * 
     * @param graph the graph that finished being visited.
     */
    public void finishGraph( DependencyGraph graph );

}
