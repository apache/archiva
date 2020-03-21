package org.apache.archiva.repository.content.base;

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

import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.base.builder.ProjectOptBuilder;
import org.apache.archiva.repository.content.base.builder.ProjectWithIdBuilder;
import org.apache.archiva.repository.content.base.builder.WithNamespaceObjectBuilder;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

/**
 * Immutable class, that represents a project.
 * <p>
 * The namespace and id are required attributes for each instance.
 * <p>
 * Two project instances are equal if the id, and the namespace are equal and if the base attributes
 * repository and asset match.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class ArchivaProject extends ArchivaContentItem implements Project
{
    private Namespace namespace;
    private String id;

    // Setting all setters to private. Builder is the way to go.
    private ArchivaProject( )
    {

    }


    /**
     * Creates the builder for creating new archiva project instances.
     * You have to set all required attributes before you can call the build() method.
     *
     * @param storageAsset the asset
     * @return a builder instance
     */
    public static WithNamespaceObjectBuilder withAsset( StorageAsset storageAsset )
    {
        return new Builder( ).withAsset( storageAsset );
    }

    @Override
    public Namespace getNamespace( )
    {
        return this.namespace;
    }

    @Override
    public String getId( )
    {
        return this.id;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        ArchivaProject that = (ArchivaProject) o;

        if ( !namespace.equals( that.namespace ) ) return false;
        return id.equals( that.id );
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + namespace.hashCode( );
        result = 31 * result + id.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        return id + ", namespace="+namespace.toString();
    }

    /*
     * Builder class
     */
    public static final class Builder
        extends ContentItemBuilder<ArchivaProject, ProjectOptBuilder, WithNamespaceObjectBuilder>
        implements ProjectOptBuilder, ProjectWithIdBuilder, WithNamespaceObjectBuilder
    {
        private Builder( )
        {
            super( new ArchivaProject( ) );
        }


        @Override
        protected ProjectOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        protected WithNamespaceObjectBuilder getNextBuilder( )
        {
            return this;
        }

        @Override
        public ProjectOptBuilder withId( String id )
        {
            if ( StringUtils.isEmpty( id ) )
            {
                throw new IllegalArgumentException( "Null or empty value not allowed for id" );
            }
            item.id = id;
            return this;
        }

        @Override
        public ProjectWithIdBuilder withNamespace( Namespace namespace )
        {
            if ( namespace == null )
            {
                throw new IllegalArgumentException( "Null value not allowed for namespace" );
            }
            item.namespace = namespace;
            super.setRepository( namespace.getRepository( ) );
            return this;
        }

        @Override
        public ArchivaProject build( )
        {
            super.build( );
            if ( item.namespace == null )
            {
                throw new IllegalArgumentException( "Namespace may not be null" );
            }
            return item;
        }
    }


}
