package org.apache.maven.archiva.dependency.graph;

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.DependencyScope;

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
 * DependencyGraphEdge 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphEdge
{
    private ArtifactReference nodeFrom;

    private ArtifactReference nodeTo;

    private String scope;
    
    private boolean disabled = false;

    private int disabledType;

    private String disabledReason;

    public DependencyGraphEdge( ArtifactReference fromNode, ArtifactReference toNode )
    {
        super();
        this.nodeFrom = fromNode;
        this.nodeTo = toNode;
        this.scope = DependencyScope.COMPILE;
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
        final DependencyGraphEdge other = (DependencyGraphEdge) obj;
        if ( nodeFrom == null )
        {
            if ( other.nodeFrom != null )
            {
                return false;
            }
        }
        else if ( !nodeFrom.equals( other.nodeFrom ) )
        {
            return false;
        }
        if ( nodeTo == null )
        {
            if ( other.nodeTo != null )
            {
                return false;
            }
        }
        else if ( !nodeTo.equals( other.nodeTo ) )
        {
            return false;
        }
        return true;
    }

    public String getDisabledReason()
    {
        return disabledReason;
    }

    public int getDisabledType()
    {
        return disabledType;
    }

    public ArtifactReference getNodeFrom()
    {
        return nodeFrom;
    }

    public ArtifactReference getNodeTo()
    {
        return nodeTo;
    }

    public String getScope()
    {
        return scope;
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( nodeFrom == null ) ? 0 : nodeFrom.hashCode() );
        result = PRIME * result + ( ( nodeTo == null ) ? 0 : nodeTo.hashCode() );
        return result;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
        if( this.disabled == false )
        {
            this.disabledReason = null;
            this.disabledType = -1;
        }
    }

    public void setDisabledReason( String disabledReason )
    {
        this.disabledReason = disabledReason;
    }

    public void setDisabledType( int disabledType )
    {
        this.disabledType = disabledType;
    }
    
    public void setNodeFrom( ArtifactReference ref )
    {
        this.nodeFrom = ref;
    }

    public void setNodeFrom( DependencyGraphNode node )
    {
        this.nodeFrom = node.getArtifact();
    }
    
    public void setNodeTo( ArtifactReference ref )
    {
        this.nodeTo = ref;
    }

    public void setNodeTo( DependencyGraphNode node )
    {
        this.nodeTo = node.getArtifact();
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "GraphEdge[" );
        sb.append( "from=" ).append( DependencyGraphKeys.toKey( nodeFrom ) );
        sb.append( ",to=" ).append( DependencyGraphKeys.toKey( nodeTo ) );
        sb.append( "]" );

        return sb.toString();
    }
}
