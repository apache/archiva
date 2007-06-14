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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.PotentialCyclicEdgeProducer;
import org.apache.maven.archiva.dependency.graph.functors.UnresolvedGraphNodePredicate;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * Loop through the unresolved nodes and resolve them, until there
 * are no more unresolved nodes.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="resolve-graph"
 *      instantiation-strategy="per-lookup"
 */
public class ResolveGraphTask
    implements GraphTask, PotentialCyclicEdgeProducer
{
    private DependencyGraphBuilder builder;
    
    private int resolvedCount = 0;

    private VersionedReference toVersionedReference( DependencyGraphNode node )
    {
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( node.getArtifact().getGroupId() );
        ref.setArtifactId( node.getArtifact().getArtifactId() );
        ref.setVersion( node.getArtifact().getVersion() );

        return ref;
    }

    public void executeTask( DependencyGraph graph )
    {
        resolvedCount = 0;
        VersionedReference rootRef = toVersionedReference( graph.getRootNode() );

        if ( !graph.getRootNode().isResolved() )
        {
            builder.resolveNode( graph, graph.getRootNode(), rootRef );
            resolvedCount++;
        }

        boolean done = false;

        while ( !done )
        {
            DependencyGraphNode node = findUnresolvedNode( graph );
            if ( node == null )
            {
                done = true;
                break;
            }

            VersionedReference otherRef = toVersionedReference( node );

            builder.resolveNode( graph, node, otherRef );
            resolvedCount++;
        }
    }

    private DependencyGraphNode findUnresolvedNode( DependencyGraph graph )
    {
        return (DependencyGraphNode) CollectionUtils
            .find( graph.getNodes(), UnresolvedGraphNodePredicate.getInstance() );
    }

    public DependencyGraphBuilder getBuilder()
    {
        return builder;
    }

    public void setBuilder( DependencyGraphBuilder graphBuilder )
    {
        this.builder = graphBuilder;
    }

    public String getTaskId()
    {
        return "resolve-graph";
    }

    public int getResolvedCount()
    {
        return resolvedCount;
    }
}
