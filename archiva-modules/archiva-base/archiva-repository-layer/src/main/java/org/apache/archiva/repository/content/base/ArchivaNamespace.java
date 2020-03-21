/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.archiva.repository.content.base;

import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.base.builder.NamespaceOptBuilder;
import org.apache.archiva.repository.content.base.builder.WithAssetBuilder;
import org.apache.archiva.repository.content.base.builder.WithNamespaceBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Namespace representation.
 * Two namespace instances are equal, if the namespace string and the base attributes, like repository and
 * asset are equal. The separator expression does not influence equality.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class ArchivaNamespace extends ArchivaContentItem implements Namespace
{
    private String namespace;
    private List<String> namespacePath;
    private String separatorExpression = "\\.";

    private ArchivaNamespace( )
    {

    }

    @Override
    public String getNamespace( )
    {
        return namespace;
    }

    @Override
    public List<String> getNamespacePath( )
    {
        return this.namespacePath;
    }

    public static WithAssetBuilder<WithNamespaceBuilder> withRepository( ManagedRepositoryContent repository )
    {
        return new Builder( ).withRepository( repository );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        ArchivaNamespace that = (ArchivaNamespace) o;

        return namespace.equals( that.namespace );
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + namespace.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        return namespace;
    }

    private static class Builder extends ContentItemBuilder<ArchivaNamespace, NamespaceOptBuilder, WithNamespaceBuilder>
        implements WithNamespaceBuilder, NamespaceOptBuilder
    {

        private Builder( )
        {
            super( new ArchivaNamespace( ) );
        }

        @Override
        protected NamespaceOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        protected WithNamespaceBuilder getNextBuilder( )
        {
            return this;
        }

        @Override
        public NamespaceOptBuilder withNamespace( String namespace )
        {
            if ( namespace == null )
            {
                throw new IllegalArgumentException( "Namespace may not be null" );
            }
            this.item.namespace = namespace;
            setNamespacePath( namespace );
            return this;
        }

        private void setNamespacePath( String namespace )
        {
            if ( StringUtils.isEmpty( namespace ) )
            {
                this.item.namespacePath = Collections.emptyList( );
            }
            else
            {
                this.item.namespacePath = Arrays.asList( namespace.split( this.item.separatorExpression ) );
            }
        }

        @Override
        public NamespaceOptBuilder withSeparatorExpression( String expression )
        {
            if ( StringUtils.isEmpty( expression ) )
            {
                throw new IllegalArgumentException( "Separator expression may not be null or empty" );
            }
            this.item.separatorExpression = expression;
            try
            {
                setNamespacePath( this.item.namespace );
            }
            catch ( PatternSyntaxException e )
            {
                throw new IllegalArgumentException( "Bad pattern syntax separator expression " + expression + ": " + e.getMessage( ), e );
            }
            return this;
        }

        @Override
        public ArchivaNamespace build( )
        {
            super.build( );
            return this.item;
        }
    }


}
