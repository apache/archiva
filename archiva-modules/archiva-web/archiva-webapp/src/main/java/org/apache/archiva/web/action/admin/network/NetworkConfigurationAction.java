package org.apache.archiva.web.action.admin.network;
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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.NetworkConfiguration;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.web.action.AbstractActionSupport;
import org.apache.archiva.redback.integration.interceptor.SecureAction;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Controller( "networkConfigurationAction" )
@Scope( "prototype" )
public class NetworkConfigurationAction
    extends AbstractActionSupport
    implements Preparable, SecureAction
{

    @Inject
    private ArchivaAdministration archivaAdministration;

    private NetworkConfiguration networkConfiguration;

    public void prepare( )
        throws Exception
    {
        networkConfiguration = archivaAdministration.getNetworkConfiguration( );
    }

    public SecureActionBundle getSecureActionBundle( )
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle( );

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String edit( )
    {
        return INPUT;
    }

    public String save( )
    {
        try
        {
            archivaAdministration.setNetworkConfiguration( this.networkConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "Error during networkConfiguration upate:" + e.getMessage( ) );
            return ERROR;
        }
        addActionMessage( "Network Configuration Updated" );
        return SUCCESS;
    }

    public NetworkConfiguration getNetworkConfiguration( )
    {
        return networkConfiguration;
    }

    public void setNetworkConfiguration( NetworkConfiguration networkConfiguration )
    {
        this.networkConfiguration = networkConfiguration;
    }
}
