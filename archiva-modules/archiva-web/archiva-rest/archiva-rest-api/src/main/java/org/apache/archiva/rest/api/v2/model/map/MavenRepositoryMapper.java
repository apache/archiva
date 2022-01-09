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
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
public class MavenRepositoryMapper implements RestMapper<MavenManagedRepository, ManagedRepositoryConfiguration>
{
    @Override
    public ManagedRepositoryConfiguration map( MavenManagedRepository source )
    {
        return null;
    }

    @Override
    public void update( MavenManagedRepository source, ManagedRepositoryConfiguration target )
    {

    }

    @Override
    public MavenManagedRepository reverseMap( ManagedRepositoryConfiguration target )
    {
        return null;
    }

    @Override
    public void reverseUpdate( ManagedRepositoryConfiguration target, MavenManagedRepository source )
    {

    }

    @Override
    public Class<MavenManagedRepository> getSourceType( )
    {
        return MavenManagedRepository.class;
    }

    @Override
    public Class<ManagedRepositoryConfiguration> getTargetType( )
    {
        return ManagedRepositoryConfiguration.class;
    }

    @Override
    public <S, T> boolean supports( Class<S> sourceType, Class<T> targetType )
    {
        return (
            sourceType.isAssignableFrom( getSourceType() ) &&
                targetType.isAssignableFrom( getTargetType() ) );
    }
}
