package org.apache.archiva.repository.content;

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

/**
 *
 * A data item is a item that is not a real artifact because it does not have
 * a version, but is normally file based.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface DataItem extends ContentItem
{

    /**
     * Returns the identifier of the data item.
     * @return the identifier string
     */
    String getId( );

    /**
     * Returns the extension of the file. This method should always return the extension string after the last
     * '.'-character.
     *
     * @return the file name extension
     */
    default String getExtension( )
    {
        final String name = getAsset( ).getName( );
        final int idx = name.lastIndexOf( '.' )+1;
        if ( idx > 0 )
        {
            return name.substring( idx );
        }
        else
        {
            return "";
        }
    }

    /**
     * Should return the mime type of the artifact.
     *
     * @return the mime type of the artifact.
     */
    String getContentType( );


    /**
     * Short cut for the file name. Should always return the same value as the artifact name.
     *
     * @return the name of the file
     */
    default String getFileName( )
    {
        return getAsset( ).getName( );
    }

    /**
     * Returns the
     * @return the type of the item
     */
    DataItemType getDataType();

}
