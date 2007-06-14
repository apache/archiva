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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.AndPredicate;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.functors.NodePredicate;
import org.apache.maven.archiva.dependency.graph.functors.OrphanedNodePredicate;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.VersionedReference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utilities for manipulating the DependencyGraph. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphUtils
{
    /**
     * Standard way to add a model to the graph.
     * 
     * NOTE: Used by archiva-repository-layer runtime and archiva-dependency-graph tests.
     * 
     * @param model the model to add
     * @param graph the graph to add it to
     * @param fromNode the node to add it from.
     */
    public static void addNodeFromModel( ArchivaProjectModel model, DependencyGraph graph, DependencyGraphNode fromNode )
    {
        if ( model == null )
        {
            throw new IllegalStateException( "Unable to add null model for "
                + DependencyGraphKeys.toKey( fromNode.getArtifact() ) );
        }

        if ( model.getRelocation() != null )
        {
            // We need to CHANGE this node.
            ArtifactReference refTO = new ArtifactReference();

            refTO.setGroupId( fromNode.getArtifact().getGroupId() );
            refTO.setArtifactId( fromNode.getArtifact().getArtifactId() );
            refTO.setVersion( fromNode.getArtifact().getVersion() );
            refTO.setClassifier( fromNode.getArtifact().getClassifier() );
            refTO.setType( fromNode.getArtifact().getType() );

            VersionedReference relocation = model.getRelocation();

            if ( StringUtils.isNotBlank( relocation.getGroupId() ) )
            {
                refTO.setGroupId( relocation.getGroupId() );
            }

            if ( StringUtils.isNotBlank( relocation.getArtifactId() ) )
            {
                refTO.setArtifactId( relocation.getArtifactId() );
            }

            if ( StringUtils.isNotBlank( relocation.getVersion() ) )
            {
                refTO.setVersion( relocation.getVersion() );
            }

            DependencyGraphNode nodeTO = new DependencyGraphNode( refTO );

            graph.addNode( nodeTO );
            collapseNodes( graph, fromNode, nodeTO );
            return;
        }

        boolean isRootNode = graph.getRootNode().equals( fromNode );

        Iterator it;

        if ( CollectionUtils.isNotEmpty( model.getDependencyManagement() ) )
        {
            it = model.getDependencyManagement().iterator();
            while ( it.hasNext() )
            {
                Dependency dependency = (Dependency) it.next();
                fromNode.addDependencyManagement( dependency );
            }
        }

        if ( CollectionUtils.isNotEmpty( model.getDependencies() ) )
        {
            it = model.getDependencies().iterator();
            while ( it.hasNext() )
            {
                Dependency dependency = (Dependency) it.next();

                String scope = dependency.getScope();

                // Test scopes *NOT* from root node can be skipped.
                if ( DependencyScope.TEST.equals( scope ) && !isRootNode )
                {
                    // skip add of test scope
                    continue;
                }

                ArtifactReference artifactRef = new ArtifactReference();
                artifactRef.setGroupId( dependency.getGroupId() );
                artifactRef.setArtifactId( dependency.getArtifactId() );
                artifactRef.setVersion( dependency.getVersion() );
                artifactRef.setClassifier( dependency.getClassifier() );
                artifactRef.setType( dependency.getType() );

                DependencyGraphNode toNode = new DependencyGraphNode( artifactRef );

                if ( CollectionUtils.isNotEmpty( dependency.getExclusions() ) )
                {
                    Iterator itexclusion = dependency.getExclusions().iterator();
                    while ( itexclusion.hasNext() )
                    {
                        Exclusion exclusion = (Exclusion) itexclusion.next();
                        toNode.addExclude( exclusion );
                    }
                }

                if ( dependency.isFromParent() )
                {
                    toNode.setFromParent( true );
                }

                // Add node (to)
                graph.addNode( toNode );

                DependencyGraphEdge edge = new DependencyGraphEdge( fromNode.getArtifact(), toNode.getArtifact() );
                edge.setScope( StringUtils.defaultIfEmpty( dependency.getScope(), DependencyScope.COMPILE ) );

                if ( dependency.isOptional() )
                {
                    edge.setDisabled( true );
                    edge.setDisabledType( DependencyGraph.DISABLED_OPTIONAL );
                    edge.setDisabledReason( "Optional Dependency" );
                }

                graph.addEdge( edge );
            }
        }

        fromNode.setResolved( true );
        graph.addNode( fromNode );
    }

    /**
     * Clean out any nodes that may have become orphaned in the graph.
     * 
     * @param graph the graph to check.
     */
    public static void cleanupOrphanedNodes( DependencyGraph graph )
    {
        boolean done = false;

        Predicate orphanedNodePredicate = new OrphanedNodePredicate( graph );
        Predicate notRootNode = NotPredicate.getInstance( new NodePredicate( graph.getRootNode().getArtifact() ) );
        Predicate orphanedChildNodePredicate = AndPredicate.getInstance( notRootNode, orphanedNodePredicate );

        while ( !done )
        {
            // Find orphaned node.
            DependencyGraphNode orphanedNode = (DependencyGraphNode) CollectionUtils.find( graph.getNodes(),
                                                                                           orphanedChildNodePredicate );

            if ( orphanedNode == null )
            {
                done = true;
                break;
            }

            // Remove edges FROM orphaned node.
            List edgesFrom = graph.getEdgesFrom( orphanedNode );

            Iterator it = edgesFrom.iterator();
            while ( it.hasNext() )
            {
                DependencyGraphEdge edge = (DependencyGraphEdge) it.next();
                graph.removeEdge( edge );
            }

            // Remove orphaned node.
            graph.removeNode( orphanedNode );
        }
    }

    /**
     * Functionaly similar to {@link #collapseVersions(DependencyGraph, ArtifactReference, String, String)}, but 
     * in a new, easier to use, format.
     * 
     * 1) Removes the FROM edges connected to the FROM node
     * 2) Moves the TO edges connected to the FROM node to the TO node.
     * 3) Removes the FROM node (which is now orphaned)  
     *  
     * @param graph the graph to perform operation on
     * @param nodeFrom the node to collapse from
     * @param nodeTo the node to collapse to
     */
    public static void collapseNodes( DependencyGraph graph, DependencyGraphNode nodeFROM, DependencyGraphNode nodeTO )
    {
        Iterator it;

        Set edgesToRemove = new HashSet();

        // 1) Remove all of the edge.from references from nodeFROM
        List fromEdges = graph.getEdgesFrom( nodeFROM );
        if ( CollectionUtils.isNotEmpty( fromEdges ) )
        {
            edgesToRemove.addAll( fromEdges );
        }

        // 2) Swing all of the edge.to references from nodeFROM to nodeTO.
        //        System.out.println( "Swinging incoming edges from " + nodeFROM );
        //        System.out.println( "                          to " + nodeTO );
        List toEdges = graph.getEdgesTo( nodeFROM );
        it = toEdges.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphEdge edge = (DependencyGraphEdge) it.next();

            // Identify old edge to remove.
            edgesToRemove.add( edge );

            // Clone edge, set edge.to and add to graph.
            DependencyGraphEdge newedge = clone( edge );
            newedge.setNodeTo( nodeTO );
            //            System.out.println( "   edge from: " + edge );
            //            System.out.println( "          to: " + newedge );
            graph.addEdge( newedge );
        }

        // Actually remove the old edges.
        it = edgesToRemove.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphEdge edge = (DependencyGraphEdge) it.next();
            graph.removeEdge( edge );
        }

        // 3) Remove the nodeFROM
        graph.removeNode( nodeFROM );
    }

    /**
     * Create a clone of an edge.
     * 
     * @param edge the edge to clone.
     * @return the cloned edge.
     */
    public static DependencyGraphEdge clone( DependencyGraphEdge edge )
    {
        DependencyGraphEdge cloned = new DependencyGraphEdge( edge.getNodeFrom(), edge.getNodeTo() );
        cloned.setDisabled( edge.isDisabled() );
        cloned.setDisabledReason( edge.getDisabledReason() );
        cloned.setDisabledType( edge.getDisabledType() );
        cloned.setScope( edge.getScope() );

        return cloned;
    }
}
