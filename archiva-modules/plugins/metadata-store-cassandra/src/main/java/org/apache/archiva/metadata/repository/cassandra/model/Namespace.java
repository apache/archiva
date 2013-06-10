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
import java.io.Serializable;


/**
 * @author Olivier Lamy
 */
@Entity
//@Table( name = "namespace", schema = "ArchivaKeySpace@archiva")
public class Namespace
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "repository")
    private Repository repository;

    //@ManyToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    //@JoinColumn(name = "repository_id")
    //private transient Repository repository;


    public Namespace()
    {
        // no op
    }


    public Namespace( String id, Repository repository )
    {
        this.id = new KeyBuilder().withNamespace( id ).withRepositoryId( repository.getId() ).build();
        this.name = id;
        this.repository = repository;
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

    public Repository getRepository()
    {
        return repository;
    }

    public void setRepository( Repository repository )
    {
        this.repository = repository;
    }

    /*
    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }*/

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

        Namespace namespace = (Namespace) o;

        if ( !id.equals( namespace.id ) )
        {
            return false;
        }
        if ( !repository.getId().equals( namespace.repository.getId() ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + repository.getId().hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Namespace{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", repository='" ).append( repository ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    public static class KeyBuilder
    {

        private String namespace;

        private String repositoryId;

        public KeyBuilder()
        {

        }

        public KeyBuilder withNamespace( Namespace namespace )
        {
            this.namespace = namespace.getName();
            this.repositoryId = namespace.getRepository().getId();
            return this;
        }

        public KeyBuilder withNamespace( String namespace )
        {
            this.namespace = namespace;
            return this;
        }

        public KeyBuilder withRepositoryId( String repositoryId )
        {
            this.repositoryId = repositoryId;
            return this;
        }

        public String build()
        {
            // FIXME add some controls
            return this.repositoryId + "-" + this.namespace;
        }
    }
}
