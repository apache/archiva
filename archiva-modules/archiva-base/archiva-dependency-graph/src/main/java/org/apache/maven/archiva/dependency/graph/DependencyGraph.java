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
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.maven.archiva.dependency.graph.functors.EdgeFromPredicate;
import org.apache.maven.archiva.dependency.graph.functors.EdgeToPredicate;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DependencyGraph 
 *
 * @version $Id$
 */
public class DependencyGraph
{
    public static final int DISABLED_CYCLIC = 0;

    public static final int DISABLED_EXCLUDED = 1;

    public static final int DISABLED_OPTIONAL = 2;

    public static final int DISABLED_NEARER_DEP = 3;

    public static final int DISABLED_NEARER_EDGE = 4;

    private DependencyGraphNode rootNode;

    private Set edges = new HashSet();

    private ListOrderedMap nodes = new ListOrderedMap();

    public DependencyGraph( String groupId, String artifactId, String version )
    {
        ArtifactReference rootRef = new ArtifactReference();
        rootRef.setGroupId( groupId );
        rootRef.setArtifactId( artifactId );
        rootRef.setVersion( version );
        rootRef.setClassifier( "" );
        rootRef.setType( "pom" );

        this.rootNode = new DependencyGraphNode( rootRef );
    }

    public DependencyGraph( DependencyGraphNode root )
    {
        this.rootNode = root;
    }

    public Collection getEdges()
    {
        return edges;
    }

    public Collection getNodes()
    {
        return nodes.values();
    }

    public DependencyGraphNode getRootNode()
    {
        return rootNode;
    }

    public void setRootNode( DependencyGraphNode rootNode )
    {
        this.rootNode = rootNode;
    }

    /**
     * Add the edge to the {@link DependencyGraph}.
     * 
     * @param edge the edge to add.
     */
    public void addEdge( final DependencyGraphEdge edge )
    {
        if ( edge.getNodeFrom() == null )
        {
            throw new IllegalArgumentException( "edge.nodeFrom cannot be null." );
        }

        if ( edge.getNodeTo() == null )
        {
            throw new IllegalArgumentException( "edge.nodeTo cannot be null." );
        }

        this.edges.add( edge );
    }

    public DependencyGraphNode addNode( DependencyGraphNode node )
    {
        if ( node == null )
        {
            throw new IllegalArgumentException( "Unable to add a null node." );
        }

        if ( node.getArtifact() == null )
        {
            throw new IllegalArgumentException( "Unable to add a node with a null artifact reference." );
        }

        int prevNodeIdx = this.nodes.indexOf( node );

        // Found it in the node tree?
        if ( prevNodeIdx >= 0 )
        {
            // Merge new node into existing node.
            DependencyGraphNode previousNode = (DependencyGraphNode) this.nodes.get( prevNodeIdx );

            if ( CollectionUtils.isNotEmpty( node.getExcludes() ) )
            {
                previousNode.getExcludes().addAll( node.getExcludes() );
            }

            if ( CollectionUtils.isNotEmpty( node.getDependencyManagement() ) )
            {
                previousNode.getDependencyManagement().addAll( node.getDependencyManagement() );
            }

            if ( node.isFromParent() )
            {
                previousNode.setFromParent( true );
            }

            // Return newly merged node (from existing node)
            return previousNode;
        }

        // This is a new node, didn't exist before, just save it.
        this.nodes.put( node.getArtifact(), node );

        return node;
    }

    public boolean hasNode( DependencyGraphNode node )
    {
        return this.nodes.containsKey( node.getArtifact() );
    }

    public boolean hasEdge( DependencyGraphEdge edge )
    {
        return this.edges.contains( edge );
    }

    /**
     * Get the list of edges from the provided node.
     * 
     * @param node the node to use as the 'from' side of an edge.
     * @return the edges from the provided node.
     */
    public List getEdgesFrom( DependencyGraphNode node )
    {
        List ret = new ArrayList();
        CollectionUtils.select( this.edges, new EdgeFromPredicate( node.getArtifact() ), ret );
        return ret;
    }

    /**
     * Get the list of edges to the provided node.
     * 
     * @param node the node to use as the 'to' side of an edge.
     * @return the edges to the provided node.
     */
    public List getEdgesTo( DependencyGraphNode node )
    {
        List ret = new ArrayList();
        CollectionUtils.select( this.edges, new EdgeToPredicate( node.getArtifact() ), ret );
        return ret;
    }

    /**
     * Get the node for the specified artifact reference.
     * 
     * @param ref the artifact reference to use to find the node.
     * @return the node that was found. (null if not found)
     */
    public DependencyGraphNode getNode( ArtifactReference ref )
    {
        return (DependencyGraphNode) this.nodes.get( ref );
    }

    public void removeEdge( DependencyGraphEdge edge )
    {
        this.edges.remove( edge );
    }

    public void removeNode( DependencyGraphNode node )
    {
        List edges = getEdgesFrom( node );
        if ( !edges.isEmpty() )
        {
            System.out.println( "Removing node left <" + edges + "> hanging <from> edges." );
        }

        edges = getEdgesTo( node );
        if ( !edges.isEmpty() )
        {
            System.out.println( "Removing node left <" + edges + "> hanging <to> edges." );
        }

        this.nodes.remove( node.getArtifact() );
    }
}
