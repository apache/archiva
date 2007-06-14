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

/**
 * GraphPhaseEvent 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GraphPhaseEvent
{
    /**
     * Graph Phase Event Type - New Graph has been created.  No tasks have been run yet.
     * NOTE: {{@link #getTask()} will be null for this type.
     */
    public static final int GRAPH_NEW = 0;

    /**
     * Graph Phase Event Type - Graph Task is about to run.
     */
    public static final int GRAPH_TASK_PRE = 1;

    /**
     * Graph Phase Event Type - Graph Task has finished.
     */
    public static final int GRAPH_TASK_POST = 2;

    /**
     * Graph Phase Event Type - All Graph Tasks are done.
     * NOTE: {{@link #getTask()} will be null for this type.
     */
    public static final int GRAPH_DONE = 10;

    private int type;
    
    private GraphTask task;

    private DependencyGraph graph;

    public GraphPhaseEvent( int type, GraphTask task, DependencyGraph graph )
    {
        this.type = type;
        this.task = task;
        this.graph = graph;
    }

    public DependencyGraph getGraph()
    {
        return graph;
    }

    public GraphTask getTask()
    {
        return task;
    }

    public int getType()
    {
        return type;
    }
}
