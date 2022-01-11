package org.apache.archiva.rest.api.v2.model.map;
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

import org.apache.archiva.common.ModelMapperFactory;
import org.apache.archiva.common.MultiModelMapper;
import org.apache.archiva.configuration.model.ConfigurationModel;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.rest.api.v2.model.RestModel;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
@SuppressWarnings( "unchecked" )
@Service("modelMapperFactory#rest")
public class ServiceMapperFactory implements ModelMapperFactory<RestModel, ConfigurationModel, Repository>
{
    List<MultiModelMapper> modelMapperList;

    @Inject
    public ServiceMapperFactory( List<MultiModelMapper> modelMapperList )
    {
        this.modelMapperList = modelMapperList;
        initMapper();
    }

    Map<Integer, MultiModelMapper<? extends RestModel, ? extends ConfigurationModel, ? extends Repository>> modelMap = new HashMap<>(  );

    void initMapper() {
        modelMap = new HashMap<>( );
        for ( MultiModelMapper<?, ?, ?> mapper : modelMapperList )
        {
            if (!mapper.supports( RestModel.class, ConfigurationModel.class, Repository.class )) {
                continue;
            }
            Integer key = mapper.hashCode( );
            modelMap.put( key, (MultiModelMapper<? extends RestModel, ? extends ConfigurationModel, ? extends Repository>) mapper );
        }
    }

    @Override
    public <S extends RestModel, T extends ConfigurationModel, R extends Repository> MultiModelMapper<S, T, R> getMapper( Class<S> baseType, Class<T> destinationType, Class<R> reverseSourceType ) throws IllegalArgumentException
    {
        Integer key = MultiModelMapper.getHash( baseType, destinationType, reverseSourceType );
        if (!modelMap.containsKey( key )) {
            throw new IllegalArgumentException( "No mapper defined for the given source type "+baseType );
        }
        return (MultiModelMapper<S, T, R>) modelMap.get( key );
    }
}
