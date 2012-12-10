package org.apache.archiva.redback.authorization.rbac;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authorization.AuthorizationDataSource;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.authorization.Authorizer;
import org.apache.archiva.redback.authorization.NotAuthorizedException;
import org.apache.archiva.redback.authorization.rbac.evaluator.PermissionEvaluationException;
import org.apache.archiva.redback.authorization.rbac.evaluator.PermissionEvaluator;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * RbacAuthorizer:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 */
@Service("authorizer#rbac")
public class RbacAuthorizer
    implements Authorizer
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "rBACManager#cached")
    private RBACManager manager;

    @Inject
    @Named(value = "userManager#configurable")
    private UserManager userManager;

    @Inject
    private PermissionEvaluator evaluator;

    public String getId()
    {
        return "RBAC Authorizer - " + this.getClass().getName();
    }

    /**
     * @param source
     * @return
     * @throws AuthorizationException
     */
    public AuthorizationResult isAuthorized( AuthorizationDataSource source )
        throws AuthorizationException
    {
        Object principal = source.getPrincipal();
        Object operation = source.getPermission();
        Object resource = source.getResource();

        try
        {
            if ( principal != null )
            {
                // Set permissions = manager.getAssignedPermissions( principal.toString(), operation );
                Map<String, List<Permission>> permissionMap = manager.getAssignedPermissionMap( principal.toString() );

                if ( permissionMap.keySet().contains( operation.toString() ) )
                {
                    for ( Permission permission : permissionMap.get( operation.toString() ) )
                    {

                        log.debug( "checking permission {} for operation {} resource {}",
                                   ( permission != null ? permission.getName() : "null" ), operation, resource );

                        if ( evaluator.evaluate( permission, operation, resource, principal ) )
                        {
                            return new AuthorizationResult( true, permission, null );
                        }
                    }

                    log.debug( "no permission found for operation {} resource {}", operation.toString(), resource );
                }
                else
                {
                    log.debug( "permission map does not contain operation: {}", operation.toString() );
                }
            }
            // check if guest user is enabled, if so check the global permissions
            User guest = userManager.getGuestUser();

            if ( !guest.isLocked() )
            {
                // Set permissions = manager.getAssignedPermissions( principal.toString(), operation );
                Map<String, List<Permission>> permissionMap = manager.getAssignedPermissionMap( guest.getUsername() );

                if ( permissionMap.keySet().contains( operation.toString() ) )
                {
                    for ( Permission permission : permissionMap.get( operation.toString() ) )
                    {
                        log.debug( "checking permission {}", permission.getName() );

                        if ( evaluator.evaluate( permission, operation, resource, guest.getUsername() ) )
                        {
                            return new AuthorizationResult( true, permission, null );
                        }
                    }
                }
            }

            return new AuthorizationResult( false, null, new NotAuthorizedException( "no matching permissions" ) );
        }
        catch ( PermissionEvaluationException pe )
        {
            return new AuthorizationResult( false, null, pe );
        }
        catch ( RbacObjectNotFoundException nfe )
        {
            return new AuthorizationResult( false, null, nfe );
        }
        catch ( UserNotFoundException ne )
        {
            return new AuthorizationResult( false, null,
                                            new NotAuthorizedException( "no matching permissions, guest not found" ) );
        }
        catch ( RbacManagerException rme )
        {
            return new AuthorizationResult( false, null, rme );
        }
        catch ( UserManagerException e )
        {
            return new AuthorizationResult( false, null, e );
        }
    }

    public RBACManager getManager()
    {
        return manager;
    }

    public void setManager( RBACManager manager )
    {
        this.manager = manager;
    }

    public UserManager getUserManager()
    {
        return userManager;
    }

    public void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }

    public PermissionEvaluator getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator( PermissionEvaluator evaluator )
    {
        this.evaluator = evaluator;
    }
}
