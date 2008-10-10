package org.apache.archiva.web.xmlrpc.security;

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

import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.UserNotFoundException;

/**
 * XmlRpcAuthenticator
 * 
 * Custom authentication and authorization handler for xmlrpc requests.
 * 
 * @version $Id 
 */
public class XmlRpcAuthenticator
    implements AuthenticationHandler
{
    private final SecuritySystem securitySystem;

    public XmlRpcAuthenticator( SecuritySystem securitySystem )
    {
        this.securitySystem = securitySystem;
    }

    public boolean isAuthorized( XmlRpcRequest pRequest )
        throws XmlRpcException
    {   
        if ( pRequest.getConfig() instanceof XmlRpcHttpRequestConfigImpl )
        {
            XmlRpcHttpRequestConfigImpl config = (XmlRpcHttpRequestConfigImpl) pRequest.getConfig();
            SecuritySession session =
                authenticate( new PasswordBasedAuthenticationDataSource( config.getBasicUserName(),
                                                                         config.getBasicPassword() ) );
            String method = pRequest.getMethodName();            
            AuthorizationResult result = authorize( session, method );
            
            return result.isAuthorized();
        }

        throw new XmlRpcException( "Unsupported transport (must be http)" );
    }

    private SecuritySession authenticate( PasswordBasedAuthenticationDataSource authenticationDataSource )
        throws XmlRpcException
    {
        try
        {
            return securitySystem.authenticate( authenticationDataSource );
        }
        catch ( AccountLockedException e )
        {
            throw new XmlRpcException( 401, e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new XmlRpcException( 401, e.getMessage(), e );
        }
        catch ( UserNotFoundException e )
        {
            throw new XmlRpcException( 401, e.getMessage(), e );
        }
    }

    private AuthorizationResult authorize( SecuritySession session, String methodName )
        throws XmlRpcException
    {   
        try
        {     
            // sample attempt at simplifying authorization checking of requested service method
            // TODO test with a sample client to see if this would work!
            if ( ServiceMethodsPermissionsMapping.SERVICE_METHODS_FOR_OPERATION_MANAGE_CONFIGURATION.contains( methodName ) )
            {                
                return securitySystem.authorize( session, ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION );
            }
            else if ( ServiceMethodsPermissionsMapping.SERVICE_METHODS_FOR_OPERATION_RUN_INDEXER.contains( methodName ) )
            {                
                return securitySystem.authorize( session, ArchivaRoleConstants.OPERATION_RUN_INDEXER );
            }
            else
            {
                return securitySystem.authorize( session, ArchivaRoleConstants.GLOBAL_REPOSITORY_MANAGER_ROLE );
            }
        }
        catch ( AuthorizationException e )
        {
            throw new XmlRpcException( 401, e.getMessage(), e );
        }
    }
}
