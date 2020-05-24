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

import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.builder.ArchivaContentItemOptBuilder;
import org.apache.archiva.repository.content.base.builder.WithAssetBuilder;
import org.apache.archiva.repository.storage.StorageAsset;

/**
 * Abstract implementation of ContentItem interface.
 * <p>
 * The attribute map is created, when the first values are put to the map.
 */
public class ArchivaContentItem extends BaseContentItem implements ContentItem
{


    /**
     * Creates the builder for creating new archiva project instances.
     * You have to set all required attributes before you can call the build() method.
     *
     * @param storageAsset the asset
     * @return a builder instance
     */
    public static ArchivaContentItemOptBuilder withAsset( StorageAsset storageAsset )
    {
        return new ArchivaContentItemBuilder().withAsset( storageAsset );
    }

    public static WithAssetBuilder<ArchivaContentItemOptBuilder> withRepository( ManagedRepositoryContent repository )
    {
        return new ArchivaContentItemBuilder().withRepository( repository );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        ArchivaContentItem that = (ArchivaContentItem) o;

        if ( !repository.equals( that.repository ) ) return false;
        return asset.equals( that.asset );
    }

    @Override
    public int hashCode( )
    {
        int result = repository.hashCode( );
        result = 31 * result + asset.hashCode( );
        return result;
    }


    /// Builder section

    /*
     * Builder implementation for each content item.
     * Should be extended by the subclasses.
     */


    public static final class ArchivaContentItemBuilder extends ContentItemBuilder<ArchivaContentItem, ArchivaContentItemOptBuilder, ArchivaContentItemOptBuilder>
        implements ArchivaContentItemOptBuilder
    {

        private ArchivaContentItemBuilder(  )
        {
            super( new ArchivaContentItem() );
        }

        @Override
        public ArchivaContentItemOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        public ArchivaContentItemOptBuilder getNextBuilder( )
        {
            return this;
        }


        @Override
        public ArchivaContentItemOptBuilder withNamespace( Namespace namespace )
        {
            item.setCharacteristic( Namespace.class, namespace );
            return this;
        }

        @Override
        public ArchivaContentItemOptBuilder withProject( Project project )
        {
            item.setCharacteristic( Project.class, project );
            return this;
        }

        @Override
        public ArchivaContentItemOptBuilder withVersion( Version version )
        {
            item.setCharacteristic( Version.class, version );
            return this;
        }

        @Override
        public ArchivaContentItem build( )
        {
            super.build( );
            return item;
        }
    }


}
