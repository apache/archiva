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

import org.apache.archiva.common.ModelMapper;
import org.apache.archiva.common.ModelMapperFactory;
import org.apache.archiva.configuration.model.ConfigurationModel;
import org.apache.archiva.rest.api.v2.model.RestModel;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
@Service("modelMapperFactory#rest")
public class ServiceMapperFactory implements ModelMapperFactory<RestModel, ConfigurationModel>
{
    @Inject
    List<ModelMapper> modelMapperList;

    Map<Class<? extends RestModel>, Map<Class<? extends ConfigurationModel>,ModelMapper<? extends RestModel, ? extends ConfigurationModel>>> modelMap;

    @PostConstruct
    void initMapper() {
        modelMap = new HashMap<>( );
        for ( ModelMapper<?, ?> mapper : modelMapperList )
        {
            if (!mapper.supports( RestModel.class, ConfigurationModel.class )) {
                continue;
            }
            Class<? extends RestModel> sType = (Class<? extends RestModel>) mapper.getSourceType( );
            Class<? extends ConfigurationModel> tType = (Class<? extends ConfigurationModel>) mapper.getTargetType( );
            Map<Class<? extends ConfigurationModel>, ModelMapper<? extends RestModel, ? extends ConfigurationModel>> tMap;
            if (modelMap.containsKey( sType )) {
                tMap = modelMap.get( sType );
            } else {
                tMap = new HashMap<>( );
            }
            tMap.put( tType, (ModelMapper<? extends RestModel, ? extends ConfigurationModel>) mapper );
        }
    }

    @Override
    public <S extends RestModel, T extends ConfigurationModel> ModelMapper<S, T> getMapper( Class<S> sourceType, Class<T> targetType ) throws IllegalArgumentException
    {
        if (!modelMap.containsKey( sourceType )) {
            throw new IllegalArgumentException( "No mapper defined for the given source type "+sourceType );
        }
        Map<Class<? extends ConfigurationModel>, ModelMapper<? extends RestModel, ? extends ConfigurationModel>> tMap = modelMap.get( sourceType );
        if ( !tMap.containsKey( targetType ) )
        {
            throw new IllegalArgumentException( "No mapper defined for the given target type "+targetType );
        }
        return (ModelMapper<S, T>) tMap.get( targetType );
    }
}
