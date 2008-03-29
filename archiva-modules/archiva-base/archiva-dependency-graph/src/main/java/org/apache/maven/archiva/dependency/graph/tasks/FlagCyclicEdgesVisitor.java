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
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.HashSet;
import java.util.Set;

/**
 * FlagCyclicEdgesVisitor 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FlagCyclicEdgesVisitor
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    private DependencyGraphWalker walker;

    private Set cyclicEdges = new HashSet();

    public FlagCyclicEdgesVisitor( DependencyGraphWalker walker )
    {
        this.walker = walker;
    }

    public void discoverEdge( DependencyGraphEdge edge )
    {
        ArtifactReference artifact = edge.getNodeTo();

        // Process for cyclic edges.
        if ( walker.getNodeVisitState( artifact ) == DependencyGraphWalker.PROCESSING )
        {
            edge.setDisabled( true );
            edge.setDisabledType( DependencyGraph.DISABLED_CYCLIC );
            edge.setDisabledReason( "Cycle detected" );
            // TODO: insert into reason the path for the cycle that was detected.
            cyclicEdges.add( edge );
        }
    }

    public Set getCyclicEdges()
    {
        return cyclicEdges;
    }
}
