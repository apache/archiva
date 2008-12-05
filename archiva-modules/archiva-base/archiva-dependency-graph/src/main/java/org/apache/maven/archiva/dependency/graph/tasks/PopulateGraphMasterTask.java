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
import org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.archiva.dependency.graph.DependencyGraphUtils;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.GraphTaskException;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

/**
 * PopulateGraphMasterTask - will perform a resolve / depman apply loop until the graph is fully populated. 
 *
 * @version $Id$
 */
public class PopulateGraphMasterTask
    implements GraphTask
{
    private DependencyGraphBuilder builder;

    private ResolveGraphTask resolveGraphTask = new ResolveGraphTask();

    private DependencyManagementApplier depManApplier = new DependencyManagementApplier();

    public void executeTask( DependencyGraph graph )
        throws GraphTaskException
    {
        DependencyGraphWalker walker = new WalkDepthFirstSearch();

        boolean done = false;
        int maxiters = 5;

        while ( !done )
        {
            resolveGraphTask.executeTask( graph );
            walker.visit( graph, depManApplier );

            if ( !depManApplier.hasCreatedNodes() || ( maxiters < 0 ) )
            {
                done = true;
                break;
            }

            maxiters--;
        }

        DependencyGraphUtils.cleanupOrphanedNodes( graph );
    }

    public String getTaskId()
    {
        return "populate-graph";
    }

    public DependencyGraphBuilder getBuilder()
    {
        return builder;
    }

    public void setBuilder( DependencyGraphBuilder builder )
    {
        this.builder = builder;
        this.resolveGraphTask.setBuilder( builder );
        this.depManApplier.setBuilder( builder );
    }

}
