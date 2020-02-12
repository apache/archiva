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

import org.apache.archiva.repository.UnsupportedConversionException;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.Project;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract implementation of ContentItem interface.
 *
 * The attribute map is created, when the first values are put to the map.
 *
 */
public abstract class ArchivaContentItem implements ContentItem
{

    private Map<String, String> attributes;

    @Override
    public abstract  <T extends Project> T adapt( Class<T> clazz ) throws UnsupportedConversionException;

    @Override
    public abstract <T extends Project> boolean supports( Class<T> clazz );


    /**
     * Does lazy initialization of the attributes map.
     * Returns a unmodifiable map.
     *
     * @return unmodifiable map of attributes
     */
    @Override
    public Map<String, String> getAttributes( )
    {
        if (this.attributes==null) {
            return Collections.emptyMap( );
        } else
        {
            return Collections.unmodifiableMap( this.attributes );
        }
    }

    /**
     * Adds a attribute value. The key must not be <code>null</code>.
     *
     * @param key the attribute key
     * @param value the attribute value
     * @throws IllegalArgumentException if the key is <code>null</code> or empty
     */
    public void putAttribute(String key, String value) throws IllegalArgumentException {
        if (this.attributes==null) {
            this.attributes = new HashMap<>( );
        }
        if ( StringUtils.isEmpty( key ) ) {
            throw new IllegalArgumentException( "Key value must not be empty or null" );
        }
        this.attributes.put( key, value );
    }

    @Override
    public String getAttribute( String key )
    {
        if (this.attributes==null) {
            return null;
        } else
        {
            return this.attributes.get( key );
        }
    }
}
