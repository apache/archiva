package org.apache.archiva.rest.services.v2;/*
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.rest.api.model.v2.BeanInformation;
import org.apache.archiva.rest.api.model.v2.CacheConfiguration;
import org.apache.archiva.rest.api.model.v2.LdapConfiguration;
import org.apache.archiva.rest.api.model.v2.SecurityConfiguration;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.v2.ErrorMessage;
import org.apache.archiva.rest.api.services.v2.SecurityConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static org.apache.archiva.rest.services.v2.ErrorKeys.REPOSITORY_ADMIN_ERROR;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("v2.defaultSecurityConfigurationService")
public class DefaultSecurityConfigurationService implements SecurityConfigurationService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultSecurityConfigurationService.class );

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Override
    public SecurityConfiguration getConfiguration( ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            return SecurityConfiguration.ofRedbackConfiguration( redbackRuntimeConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }
    }

    @Override
    public PagedResult<PropertyEntry> getConfigurationProperties( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public LdapConfiguration getLdapConfiguration( ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public CacheConfiguration getCacheConfiguration( ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public List<BeanInformation> getAvailableUserManagers( ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public List<BeanInformation> getAvailableRbacManagers( ) throws ArchivaRestServiceException
    {
        return null;
    }
}
