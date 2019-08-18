package org.apache.archiva.metadata.repository;

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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */

@SuppressWarnings( "SpringJavaInjectionPointsAutowiringInspection" )
@Service("metadataService")
public class MetadataService
{

    private Map<String, MetadataFacetFactory<? extends MetadataFacet>> facetFactories = new HashMap<>( );
    private Map<Class<? extends MetadataFacet>, MetadataFacetFactory<? extends MetadataFacet>> facetFactoriesByClass = new HashMap<>( );
    private Map<String, Class<? extends MetadataFacet>> reverseFactoryMap = new HashMap<>( );

    private MetadataResolver metadataResolver = null;

    @Inject
    ApplicationContext applicationContext;


    @Inject
    public void setMetadataFacetFactories( List<MetadataFacetFactory> factoryList ) {
        Map<String, MetadataFacetFactory<? extends MetadataFacet>> facetFactories = new HashMap<>( );
        Map<Class<? extends MetadataFacet>, MetadataFacetFactory<? extends MetadataFacet>> facetFactoriesByClass = new HashMap<>( );
        Map<String, Class<? extends MetadataFacet>> reverseFactoryMap = new HashMap<>( );
        for (MetadataFacetFactory factory : factoryList) {
            facetFactories.put( factory.getFacetId( ), factory );
            facetFactoriesByClass.put( factory.getFacetClass( ), factory );
            reverseFactoryMap.put( factory.getFacetId( ), factory.getFacetClass( ) );
        }
        this.facetFactories = facetFactories;
        this.facetFactoriesByClass = facetFactoriesByClass;
        this.reverseFactoryMap = reverseFactoryMap;
    }

    public <T extends MetadataFacet> MetadataFacetFactory<T> getFactory(Class<T> facetClazz) {
        return (MetadataFacetFactory<T>) facetFactoriesByClass.get( facetClazz );
    }

    public MetadataFacetFactory<?> getFactory(String facetId) {
        return facetFactories.get( facetId );
    }

    public Set<String> getSupportedFacets() {
        return facetFactories.keySet( );
    }

    public boolean supportsFacet(Class<? extends MetadataFacet> facetClazz) {
        return facetFactoriesByClass.containsKey( facetClazz );
    }

    public boolean supportsFacet(String facetId) {
        return facetFactories.containsKey( facetId );
    }

    public Class<? extends MetadataFacet> getFactoryClassForId( String facetId ) {
        return reverseFactoryMap.get( facetId );
    }

    // Lazy evaluation to avoid problems with circular dependencies during initialization
    public MetadataResolver getMetadataResolver()
    {
        if ( this.metadataResolver == null && applicationContext!=null)
        {
            this.metadataResolver = applicationContext.getBean( MetadataResolver.class );
        }
        return this.metadataResolver;
    }
}
