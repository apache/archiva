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
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.Iterator;
import java.util.Stack;

/**
 * FlagExcludedEdgesVisitor 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FlagExcludedEdgesVisitor
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    private Stack nodePath = new Stack();

    public void discoverEdge( DependencyGraphEdge edge )
    {
        ArtifactReference artifact = edge.getNodeTo(); 
        
        // Process for excluded edges.
        String toKey = DependencyGraphKeys.toManagementKey( artifact );
        Iterator it = this.nodePath.iterator();
        while ( it.hasNext() )
        {
            DependencyGraphNode pathNode = (DependencyGraphNode) it.next();
        
            // Process dependency declared exclusions.
            if ( pathNode.getExcludes().contains( toKey ) )
            {
                edge.setDisabled( true );
                edge.setDisabledType( DependencyGraph.DISABLED_EXCLUDED );
                String whoExcluded = DependencyGraphKeys.toKey( pathNode );
                edge.setDisabledReason( "Specifically Excluded by " + whoExcluded );
                break;
            }
        }
    }

    public void discoverNode( DependencyGraphNode node )
    {
        super.discoverNode( node );
        nodePath.push( node );
    }

    public void finishNode( DependencyGraphNode node )
    {
        super.finishNode( node );
        DependencyGraphNode pathNode = (DependencyGraphNode) nodePath.pop();
        if ( !node.equals( pathNode ) )
        {
            String pathNodeKey = ArtifactReference.toKey( pathNode.getArtifact() );
            String finishNodeKey = ArtifactReference.toKey( node.getArtifact() );
            throw new IllegalStateException( "Encountered bad visitor state.  Expected finish on node " + pathNodeKey
                + ", but instead got notified of node " + finishNodeKey );
        }
    }
}
