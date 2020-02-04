package org.apache.archiva.maven2.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@XmlRootElement( name = "treeEntry" )
public class TreeEntry
    implements Serializable
{

    private List<TreeEntry> childs = new ArrayList<>();

    private Artifact artifact;

    private TreeEntry parent;

    public TreeEntry()
    {
        // no op
    }

    public TreeEntry( Artifact artifact )
    {
        this.artifact = artifact;
    }


    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public List<TreeEntry> getChilds()
    {
        return childs;
    }

    public void setChilds( List<TreeEntry> childs )
    {
        this.childs = childs;
    }

    @XmlTransient
    public TreeEntry getParent()
    {
        return parent;
    }

    public void setParent( TreeEntry parent )
    {
        this.parent = parent;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof TreeEntry ) )
        {
            return false;
        }

        TreeEntry treeEntry = (TreeEntry) o;

        if ( artifact != null ? !artifact.equals( treeEntry.artifact ) : treeEntry.artifact != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return artifact != null ? artifact.hashCode() : 0;
    }
}
