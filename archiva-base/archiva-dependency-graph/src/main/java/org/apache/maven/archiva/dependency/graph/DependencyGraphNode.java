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

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DependencyGraphNode 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphNode
{
    /**
     * The artifact reference for this node.
     */
    private ArtifactReference artifact;

    /**
     * The project level dependency management section for this artifact.
     */
    private List dependencyManagement = new ArrayList();

    /**
     * The list of excluded groupId:artifactId for this node's sub-nodes. 
     */
    private Set excludes = new HashSet();

    /**
     * Flag indicating that this node has been resolved from disk.
     * Initially this is set to false, when the node is added due to a dependency entry in the
     * project's pom.
     * When the resolver comes through and reads the model for this node, it sets this to true.
     */
    private boolean resolved = false;
    
    /**
     * Flag indicating that this dependency exists because of a parent dependency.
     * TODO: move this to DependencyGraphEdge (where it really belongs)
     */
    private boolean fromParent = false;

    /**
     * Booleaning indicating that this node is in conflict with another node in the graph.
     * If this is true, that means this node is flagged for removal.
     */
    private boolean conflicted = false;

    public DependencyGraphNode( ArtifactReference artifact )
    {
        super();
        this.artifact = artifact;
    }

    public void addExclude( Exclusion exclusion )
    {
        this.excludes.add( DependencyGraphKeys.toManagementKey( exclusion ) );
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final DependencyGraphNode other = (DependencyGraphNode) obj;
        if ( artifact == null )
        {
            if ( other.artifact != null )
            {
                return false;
            }
        }
        else if ( !artifact.equals( other.artifact ) )
        {
            return false;
        }
        return true;
    }

    public ArtifactReference getArtifact()
    {
        return artifact;
    }

    public List getDependencyManagement()
    {
        return dependencyManagement;
    }

    public Set getExcludes()
    {
        return excludes;
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( artifact == null ) ? 0 : artifact.hashCode() );
        return result;
    }

    public boolean isConflicted()
    {
        return conflicted;
    }

    public boolean isResolved()
    {
        return resolved;
    }

    public void addDependencyManagement( Dependency dep )
    {
        this.dependencyManagement.add( dep );
    }

    public void setArtifact( ArtifactReference artifact )
    {
        this.artifact = artifact;
    }

    public void setConflicted( boolean conflicted )
    {
        this.conflicted = conflicted;
    }

    public void setDependencyManagement( List dependencyManagement )
    {
        this.dependencyManagement = dependencyManagement;
    }

    public void setExcludes( Set excludes )
    {
        this.excludes = excludes;
    }

    public void setResolved( boolean resolved )
    {
        this.resolved = resolved;
    }

    public String toString()
    {
        return DependencyGraphKeys.toKey( artifact );
    }

    public boolean isFromParent()
    {
        return fromParent;
    }

    public void setFromParent( boolean fromParent )
    {
        this.fromParent = fromParent;
    }
}
