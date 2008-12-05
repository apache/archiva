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
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkBreadthFirstSearch;

/**
 * ReduceTransitiveEdgesTask 
 *
 * @version $Id$
 */
public class ReduceTransitiveEdgesTask
    implements GraphTask
{

    public void executeTask( DependencyGraph graph )
    {
        DependencyGraphWalker walker = new WalkBreadthFirstSearch();
        ReduceTransitiveEdgesVisitor reduceTransitiveEdgesResolver = new ReduceTransitiveEdgesVisitor();
        walker.visit( graph, reduceTransitiveEdgesResolver );
    }

    public String getTaskId()
    {
        return "reduce-transitive-edges";
    }
}
