package org.apache.archiva.metadata.generic;

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

import java.util.Map;
import java.util.TreeMap;

import org.apache.archiva.metadata.model.MetadataFacet;

public class GenericMetadataFacet
    implements MetadataFacet
{
    private Map<String, String> additionalProperties;

    public static final String FACET_ID = "org.apache.archiva.metadata.generic";

    public String getFacetId()
    {
        return FACET_ID;
    }

    public String getName()
    {
        return "";
    }

    public void fromProperties( Map<String, String> properties )
    {
        if ( additionalProperties == null )
        {
            additionalProperties = new TreeMap<String, String>();
        }

        additionalProperties.putAll( properties );
    }

    public Map<String, String> toProperties()
    {
        Map<String, String> properties = new TreeMap<String, String>();

        if ( additionalProperties != null )
        {
            for ( String key : additionalProperties.keySet() )
            {
                properties.put( key, additionalProperties.get( key ) );
            }
        }

        return properties;
    }

    public Map<String, String> getAdditionalProperties()
    {
        return additionalProperties;
    }

    public void setAdditionalProperties( Map<String, String> additionalProperties )
    {
        this.additionalProperties = additionalProperties;
    }

}
