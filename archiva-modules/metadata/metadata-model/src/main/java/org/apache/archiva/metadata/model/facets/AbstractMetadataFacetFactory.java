package org.apache.archiva.metadata.model.facets;

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

import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractMetadataFacetFactory<T extends MetadataFacet> implements MetadataFacetFactory<T>
{
    private final String facetId;
    private final Class<T> facetClazz;

    protected AbstractMetadataFacetFactory( Class<T> facetClazz, String facetId) {
        this.facetId = facetId;
        this.facetClazz = facetClazz;
    }

    protected AbstractMetadataFacetFactory(Class<T> facetClazz ) {
        this.facetClazz = facetClazz;
        try
        {
            this.facetId = (String) this.facetClazz.getField( "FACET_ID" ).get(null);
        }
        catch ( Throwable e)
        {
            throw new RuntimeException( "There is no FACET_ID static public field on the class " + facetClazz );
        }
    }

    @Override
    public abstract T createMetadataFacet( );

    @Override
    public abstract T createMetadataFacet( String repositoryId, String name );

    @Override
    public Class<T> getFacetClass( )
    {
        return facetClazz;
    }

    @Override
    public String getFacetId( )
    {
        return facetId;
    }
}
