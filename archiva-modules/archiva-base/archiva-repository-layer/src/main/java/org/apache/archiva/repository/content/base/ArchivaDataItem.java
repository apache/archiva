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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.content.BaseDataItemTypes;
import org.apache.archiva.repository.content.DataItem;
import org.apache.archiva.repository.content.DataItemType;
import org.apache.archiva.repository.content.base.builder.DataItemOptBuilder;
import org.apache.archiva.repository.content.base.builder.DataItemWithIdBuilder;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

/**
 * Base implementation of artifact.
 * <p>
 * You have to use the builder method {@link #withAsset(StorageAsset)} to create a instance.
 * The build() method can be called after the required attributes are set.
 * <p>
 * Artifacts are equal if the following coordinates match:
 * <ul>
 *     <li>repository</li>
 *     <li>asset</li>
 *     <li>version</li>
 *     <li>artifactId</li>
 *     <li>artifactVersion</li>
 *     <li>type</li>
 *     <li>classifier</li>
 *     <li>artifactType</li>
 * </ul>
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ArchivaDataItem extends BaseContentItem implements DataItem
{
    private String id;
    private String contentType;
    private DataItemType dataItemType;

    private ArchivaDataItem( )
    {

    }

    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public String getContentType( )
    {
        return contentType;
    }

    @Override
    public DataItemType getDataType( )
    {
        return dataItemType;
    }


    /**
     * Returns the builder for creating a new artifact instance. You have to fill the
     * required attributes before the build() method is available.
     *
     * @param asset the storage asset representing the artifact
     * @return a builder for creating new artifact instance
     */
    public static DataItemWithIdBuilder withAsset( StorageAsset asset )
    {
        return new Builder( ).withAsset( asset );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        ArchivaDataItem that = (ArchivaDataItem) o;

        if ( !id.equals( that.id ) ) return false;
        return dataItemType.equals(that.dataItemType );
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + id.hashCode( );
        result = 31 * result + dataItemType.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "ArchivaDataItem{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", contentType='" ).append( contentType ).append( '\'' );
        sb.append( ", artifactType=" ).append( dataItemType );
        sb.append( '}' );
        return sb.toString( );
    }

    public static String defaultString( String value )
    {
        if ( value == null )
        {
            return "";
        }

        return value.trim();
    }

    private static class Builder
        extends ContentItemBuilder<ArchivaDataItem, DataItemOptBuilder, DataItemWithIdBuilder >
        implements DataItemOptBuilder,DataItemWithIdBuilder
    {

        Builder( )
        {
            super( new ArchivaDataItem( ) );
        }

        @Override
        protected DataItemOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        protected DataItemWithIdBuilder getNextBuilder( )
        {
            return this;
        }

        @Override
        public DataItemOptBuilder withId( String id )
        {
            if ( StringUtils.isEmpty( id ) )
            {
                throw new IllegalArgumentException( "Artifact id may not be null or empty" );
            }
            item.id = id;
            return this;
        }


        @Override
        public DataItemOptBuilder withContentType( String contentType )
        {
            item.contentType = contentType;
            return this;
        }

        @Override
        public DataItemOptBuilder withDataType( DataItemType type )
        {
            item.dataItemType = type;
            return this;
        }

        @Override
        public ArchivaDataItem build( )
        {
            super.build( );
            if ( item.contentType == null )
            {
                item.contentType = "";
            }
            if (item.dataItemType ==null) {
                item.dataItemType = BaseDataItemTypes.UNKNOWN;
            }

            return item;
        }
    }
}
