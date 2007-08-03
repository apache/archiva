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
 * The Baseline Visitor.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BaseVisitor
    implements DependencyGraphVisitor
{
    private static DependencyGraphVisitor INSTANCE = new BaseVisitor();
    
    protected DependencyGraph graph;

    public static DependencyGraphVisitor getInstance()
    {
        return INSTANCE;
    }

    public void discoverEdge( DependencyGraphEdge edge )
    {
        /* do nothing */
    }

    public void discoverGraph( DependencyGraph graph )
    {
        this.graph = graph;
    }

    public void discoverNode( DependencyGraphNode node )
    {
        /* do nothing */
    }

    public void finishEdge( DependencyGraphEdge edge )
    {
        /* do nothing */
    }

    public void finishGraph( DependencyGraph graph )
    {
        /* do nothing */
    }

    public void finishNode( DependencyGraphNode node )
    {
        /* do nothing */
    }
}
