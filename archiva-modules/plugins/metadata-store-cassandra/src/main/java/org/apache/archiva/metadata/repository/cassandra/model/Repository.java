package org.apache.archiva.metadata.repository.cassandra.model;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Olivier Lamy
 */
@Entity
@Table(name = "repository", schema = "ArchivaKeySpace@archiva")
public class Repository
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    @Id
    @Column( name = "id" )
    private String id;

    @Column(name = "name")
    private String name;

    private transient List<Namespace> namespaces = new ArrayList<Namespace>();

    public Repository()
    {
        // no op
    }

    public Repository( String id )
    {
        this.id = id;
        this.name = id;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }


    public List<Namespace> getNamespaces()
    {
        if ( this.namespaces == null )
        {
            this.namespaces = new ArrayList<Namespace>();
        }
        return namespaces;
    }

    public void setNamespaces( List<Namespace> namespaces )
    {
        this.namespaces = namespaces;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Repository that = (Repository) o;

        if ( !id.equals( that.id ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Repository{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", namespaces=" ).append( namespaces );
        sb.append( '}' );
        return sb.toString();
    }
}
