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

import org.apache.maven.archiva.model.VersionedReference;

/**
 * DependencyGraphBuilder 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DependencyGraphBuilder
{
    /**
     * Given a node and a versioned project rexpandeference, resolve the details of the node, creating
     * any dependencies and edges as needed.
     * 
     * @param graph the graph to add nodes and edges to.
     * @param node the node where the resolution should occur.
     * @param versionedProjectReference the versioned project reference for the node
     *                                  that needs to be resolved.
     */
    public void resolveNode( DependencyGraph graph, DependencyGraphNode node,
                             VersionedReference versionedProjectReference );

    /**
     * Create a new graph, with the root of the graph for the node specified. 
     * 
     * @param versionedProjectReference the root node for the graph.
     * @return the new DependencyGraph, complete with root node and direct dependencies. 
     */
    public DependencyGraph createGraph( VersionedReference versionedProjectReference );
}
