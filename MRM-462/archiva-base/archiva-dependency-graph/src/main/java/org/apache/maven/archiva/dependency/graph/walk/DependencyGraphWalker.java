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
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.model.ArtifactReference;

/**
 * Walk nodes of the {@link DependencyGraph}. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DependencyGraphWalker
{
    /**
     * A {@link #getNodeVisitState(ArtifactReference)} for a node not yet seen in the walker.
     */
    public static final Integer UNSEEN = new Integer( 0 );

    /**
     * A {@link #getNodeVisitState(ArtifactReference)} for a node that is actively being processed, 
     * but not yet finished processing.
     */
    public static final Integer PROCESSING = new Integer( 1 );

    /**
     * A {@link #getNodeVisitState(ArtifactReference)} for a node that has been seen, and fully processed.
     */
    public static final Integer SEEN = new Integer( 2 );

    /**
     * For a provided node, get the current node visit state.
     *  
     * @param node the node that you are interested in.
     * @return the state of that node. (Can be {@link #UNSEEN}, {@link #PROCESSING}, or {@link #SEEN} )
     */
    public Integer getNodeVisitState( ArtifactReference artifact );

    /**
     * Get the predicate used to determine if the walker should traverse an edge (or not).
     * 
     * @return the Predicate that returns true for edges that should be traversed.
     */
    public Predicate getEdgePredicate();

    /**
     * Set the predicate used for edge traversal
     * 
     * @param edgePredicate the Predicate that returns true for edges that should be traversed.
     */
    public void setEdgePredicate( Predicate edgePredicate );

    /**
     * Visit every node and edge in the graph from the startNode.
     * 
     * @param graph the graph to visit.
     * @param startNode the node to start the visit on.
     * @param visitor the visitor object to use during this visit. 
     */
    public void visit( DependencyGraph graph, DependencyGraphNode startNode, DependencyGraphVisitor visitor );

    /**
     * Visit every node and edge in the entire graph.
     * 
     * @param graph the graph to visit.
     * @param visitor the visitor object to use during this visit. 
     */
    public void visit( DependencyGraph graph, DependencyGraphVisitor visitor );
}
