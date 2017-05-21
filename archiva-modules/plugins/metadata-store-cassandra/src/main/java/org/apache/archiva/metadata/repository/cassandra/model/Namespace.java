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

import org.apache.archiva.metadata.repository.cassandra.CassandraUtils;

import java.io.Serializable;


/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class Namespace
    implements Serializable
{

    private String name;

    private String repositoryName;

    public Namespace()
    {
        // no op
    }


    public Namespace( String id, Repository repository )
    {
        this.name = id;
        this.repositoryName = repository.getName();
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
        return new Repository( this.repositoryName );
    }

    public void setRepository( Repository repository )
    {
        this.repositoryName = repository.getName();
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

        Namespace namespace = (Namespace) o;

        if ( !name.equals( namespace.name ) )
        {
            return false;
        }
        if ( !repositoryName.equals( namespace.repositoryName ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + repositoryName.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Namespace{" );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", repository='" ).append( repositoryName ).append( '\'' );
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
            this.repositoryId = namespace.getRepository().getName();
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
            return CassandraUtils.generateKey( this.repositoryId, this.namespace );
        }
    }
}
