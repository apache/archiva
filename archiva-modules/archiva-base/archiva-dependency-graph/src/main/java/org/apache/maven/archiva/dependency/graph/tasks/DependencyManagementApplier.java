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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.DependencyGraphUtils;
import org.apache.maven.archiva.dependency.graph.tasks.DependencyManagementStack.Rules;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * Takes a stack of DependencyManagement objects and applies them to the node in question.
 * This merely sets the version / scope / and exclusions on the nodes, as defined by DependencyManagement.
 * 
 * @version $Id$
 */
public class DependencyManagementApplier
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    private DependencyManagementStack depStack = new DependencyManagementStack();

    private DependencyGraphBuilder builder;

    /**
     * Map of changes to node versions (that will likely cause a reorganization of
     * the graph), this is tracked until the walk is complete, at which point the
     * changes are applied to the graph.
     * 
     * Performing graph changes of this scope during a walk of graph is hazardous,
     * as you will be moving nodes around, mergeing nodes, dropping edges, etc.
     */
    private Map<ArtifactReference, String> nodeVersionChanges = new HashMap<ArtifactReference, String>();

    private int nodesAdded = 0;

    public void discoverGraph( DependencyGraph graph )
    {
        super.discoverGraph( graph );
        nodeVersionChanges.clear();
        depStack.reset();
        nodesAdded = 0;
    }

    public void discoverNode( DependencyGraphNode node )
    {
        super.discoverNode( node );

        depStack.push( node );

        for ( DependencyGraphEdge edge : graph.getEdgesFrom( node ) )
        {
            Rules rules = depStack.getRules( edge );

            if ( rules == null )
            {
                // No rules for edge, skip it.
                continue;
            }

            DependencyGraphNode subnode = graph.getNode( edge.getNodeTo() );

            /* There are 3 steps to processing the DependencyManagement. */

            /* 1) Add exclusions to node ________________________________________________ */
            node.getExcludes().addAll( rules.exclusions );

            /* 2) Track version changes to node _________________________________________ */

            // This is the version as specified by the rules.
            String specifiedVersion = rules.artifact.getVersion();

            // This is the version as being tracked by the nodeVersionChanges map.
            String trackedVersion = (String) nodeVersionChanges.get( edge.getNodeTo() );

            // This is the version of the subnode. 
            String nodeVersion = subnode.getArtifact().getVersion();

            // This is the actual version as determined by tracked and subnode
            String actualVersion = StringUtils.defaultString( trackedVersion, nodeVersion );

            // If the specified version changes the actual version ...
            if ( !StringUtils.equals( specifiedVersion, actualVersion ) )
            {
                // ... save this new value to be track ( for processing in #finishedGraph )
                nodeVersionChanges.put( edge.getNodeTo(), specifiedVersion );
            }

            /* 3) Update scope to edge __________________________________________________ */

            if ( StringUtils.isNotBlank( rules.scope ) )
            {
                edge.setScope( rules.scope );
            }
        }
    }

    public void finishNode( DependencyGraphNode node )
    {
        super.finishNode( node );

        depStack.pop();
    }

    public void finishGraph( DependencyGraph graph )
    {
        super.finishGraph( graph );

        for ( ArtifactReference ref : this.nodeVersionChanges.keySet() )
        {
            String toVersion = this.nodeVersionChanges.get( ref );

            collapseVersions( graph, ref, ref.getVersion(), toVersion );
        }
    }

    /**
     * Collapses Versions of nodes.
     * 
     * Takes two nodes, with differing versions.
     * 
     * 1) Removes the FROM edges connected to the FROM node
     * 2) Moves the TO edges connected to the FROM node to the TO node.
     * 3) Removes the FROM node (which is now orphaned)  
     *  
     * @param graph the graph to perform operation on
     * @param fromRef the artifact reference of the FROM node.
     * @param fromVersion the version of the FROM node
     * @param toVersion the version of the TO node
     */
    private void collapseVersions( DependencyGraph graph, ArtifactReference fromRef, String fromVersion,
                                   String toVersion )
    {
        if ( StringUtils.equals( fromVersion, toVersion ) )
        {
            // No point in doing anything.  nothing has changed.
            return;
        }

        ArtifactReference toRef = new ArtifactReference();
        toRef.setGroupId( fromRef.getGroupId() );
        toRef.setArtifactId( fromRef.getArtifactId() );
        toRef.setVersion( toVersion );
        toRef.setClassifier( fromRef.getClassifier() );
        toRef.setType( fromRef.getType() );

        DependencyGraphNode nodeFROM = graph.getNode( fromRef );
        DependencyGraphNode nodeTO = graph.getNode( toRef );

        if ( nodeTO == null )
        {
            // new node doesn't exist in graph (yet)
            nodeTO = new DependencyGraphNode( toRef );
            nodeTO.setResolved( false );

            graph.addNode( nodeTO );

            VersionedReference projectRef = new VersionedReference();
            projectRef.setGroupId( toRef.getGroupId() );
            projectRef.setArtifactId( toRef.getArtifactId() );
            projectRef.setVersion( toRef.getVersion() );

            builder.resolveNode( graph, nodeTO, projectRef );
            nodesAdded++;
        }

        DependencyGraphUtils.collapseNodes( graph, nodeFROM, nodeTO );
    }

    public DependencyGraphBuilder getBuilder()
    {
        return builder;
    }

    public void setBuilder( DependencyGraphBuilder builder )
    {
        this.builder = builder;
    }

    public boolean hasCreatedNodes()
    {
        return ( nodesAdded > 0 );
    }
}
