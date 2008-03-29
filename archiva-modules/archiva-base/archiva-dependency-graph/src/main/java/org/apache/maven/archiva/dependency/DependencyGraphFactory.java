package org.apache.maven.archiva.dependency;

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
import org.apache.maven.archiva.dependency.graph.GraphListener;
import org.apache.maven.archiva.dependency.graph.GraphPhaseEvent;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.GraphTaskException;
import org.apache.maven.archiva.dependency.graph.PotentialCyclicEdgeProducer;
import org.apache.maven.archiva.dependency.graph.tasks.FlagCyclicEdgesTask;
import org.apache.maven.archiva.dependency.graph.tasks.FlagExcludedEdgesTask;
import org.apache.maven.archiva.dependency.graph.tasks.PopulateGraphMasterTask;
import org.apache.maven.archiva.dependency.graph.tasks.ReduceEnabledEdgesTask;
import org.apache.maven.archiva.dependency.graph.tasks.ReduceScopeTask;
import org.apache.maven.archiva.dependency.graph.tasks.ReduceTransitiveEdgesTask;
import org.apache.maven.archiva.dependency.graph.tasks.RefineConflictsTask;
import org.apache.maven.archiva.dependency.graph.tasks.UpdateScopesTask;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.VersionedReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DependencyGraphFactory 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.dependency.DependencyGraphFactory"
 */
public class DependencyGraphFactory
{
    private GraphTask taskFlagCyclicEdges;

    private PopulateGraphMasterTask taskPopulateGraph;

    private ReduceScopeTask taskReduceScope;

    private List listeners;

    private DependencyGraphBuilder graphBuilder;

    private List tasks;

    public DependencyGraphFactory()
    {
        listeners = new ArrayList();

        taskFlagCyclicEdges = new FlagCyclicEdgesTask();
        taskPopulateGraph = new PopulateGraphMasterTask();
        taskReduceScope = new ReduceScopeTask( DependencyScope.TEST );

        tasks = new ArrayList();

        /* Take the basic graph, and expand the nodes fully, including depman.
         */
        tasks.add( taskPopulateGraph );

        /* Identify, flag, and disable excluded edges.
         */
        tasks.add( new FlagExcludedEdgesTask() );

        /* Reduce the edges of the graph to only those that are enabled.
         */
        tasks.add( new ReduceEnabledEdgesTask() );

        /* Identify dependencies that conflict, resolve to single node.
         * 
         * This will ...
         * 1) filter the distant conflicts away for the nearer ones.
         * 2) same distance nodes will pick 'newest' version.
         * 
         * This can cause a collapsing of node versions.
         */
        tasks.add( new RefineConflictsTask() );

        /* Reduce the scope of the graph to those visible by the 'test' scope.
         */
        tasks.add( taskReduceScope );

        /* Reduce the edges of the graph.  Use the transitive reduction algorithm
         * to remove redundant edges.
         */
        tasks.add( new ReduceTransitiveEdgesTask() );

        /* Update the scopes of the edges to conform to the parent setting. 
         */
        tasks.add( new UpdateScopesTask() );
    }

    public void addGraphListener( GraphListener listener )
    {
        this.listeners.add( listener );
    }

    /**
     * Get the Graph for a specific Versioned Project Reference.
     * 
     * @param versionedProjectReference
     * @return
     */
    public DependencyGraph getGraph( VersionedReference versionedProjectReference )
        throws GraphTaskException
    {
        DependencyGraph graph = graphBuilder.createGraph( versionedProjectReference );

        triggerGraphPhase( GraphPhaseEvent.GRAPH_NEW, null, graph );

        Iterator it = this.tasks.iterator();
        while ( it.hasNext() )
        {
            GraphTask task = (GraphTask) it.next();
            try
            {
                triggerGraphPhase( GraphPhaseEvent.GRAPH_TASK_PRE, task, graph );
                task.executeTask( graph );
                if ( task instanceof PotentialCyclicEdgeProducer )
                {
                    taskFlagCyclicEdges.executeTask( graph );
                }
                triggerGraphPhase( GraphPhaseEvent.GRAPH_TASK_POST, task, graph );
            }
            catch ( GraphTaskException e )
            {
                triggerGraphError( e, graph );
                throw e;
            }
            catch ( Exception e )
            {
                GraphTaskException gte = new GraphTaskException( e.getMessage(), e );
                triggerGraphError( gte, graph );
                throw gte;
            }
        }

        triggerGraphPhase( GraphPhaseEvent.GRAPH_DONE, null, graph );

        return graph;
    }

    public void removeGraphListener( GraphListener listener )
    {
        this.listeners.remove( listener );
    }

    public void setDesiredScope( String scope )
    {
        taskReduceScope.setScope( scope );
    }

    public void setGraphBuilder( DependencyGraphBuilder graphBuilder )
    {
        this.graphBuilder = graphBuilder;
        taskPopulateGraph.setBuilder( graphBuilder );
    }

    private void triggerGraphError( GraphTaskException e, DependencyGraph graph )
    {
        Iterator it = listeners.iterator();
        while ( it.hasNext() )
        {
            GraphListener listener = (GraphListener) it.next();
            listener.graphError( e, graph );
        }
    }

    private void triggerGraphPhase( int type, GraphTask task, DependencyGraph graph )
    {
        GraphPhaseEvent evt = new GraphPhaseEvent( type, task, graph );

        Iterator it = listeners.iterator();
        while ( it.hasNext() )
        {
            GraphListener listener = (GraphListener) it.next();
            listener.graphPhaseEvent( evt );
        }
    }

}
