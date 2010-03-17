package org.apache.archiva.metadata.model;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for metadata that is contains facets for storing extensions by various plugins.
 */
public abstract class FacetedMetadata
{
    /**
     * The facets to store, keyed by the {@linkplain MetadataFacet#getFacetId() Facet ID} of the metadata.
     */
    private Map<String, MetadataFacet> facets = new HashMap<String, MetadataFacet>();

    /**
     * Add a new facet to the metadata. If it already exists, it will be replaced.
     *
     * @param metadataFacet the facet to add
     */
    public void addFacet( MetadataFacet metadataFacet )
    {
        this.facets.put( metadataFacet.getFacetId(), metadataFacet );
    }

    /**
     * Get a particular facet of metadata.
     *
     * @param facetId the facet ID
     * @return the facet of the metadata.
     */
    public MetadataFacet getFacet( String facetId )
    {
        return this.facets.get( facetId );
    }

    /**
     * Get all the facets available on this metadata.
     *
     * @return the facets of the metadata
     */
    public Collection<MetadataFacet> getFacetList()
    {
        return this.facets.values();
    }

    /**
     * Get all the keys of the facets available on this metadata.
     *
     * @return the collection of facet IDs.
     */
    public Collection<String> getFacetIds()
    {
        return this.facets.keySet();
    }

    /**
     * Get all available facets as a Map (typically used by bean rendering, such as in Archiva's JSPs).

     * @return the map of facets
     * @see #facets
     */
    public Map<String, MetadataFacet> getFacets()
    {
        return Collections.unmodifiableMap( facets );
    }
}
