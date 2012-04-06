package org.codehaus.redback.integration.interceptor;

/*
 * Copyright 2005-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.redback.rbac.Resource;

/**
 * SecureActionBundle:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 * @version: $Id$
 */
public class SecureActionBundle
{
    private boolean requiresAuthentication = false;

    private List<AuthorizationTuple> authorizationTuples = new ArrayList<AuthorizationTuple>();

    public static final SecureActionBundle OPEN;

    public static final SecureActionBundle AUTHONLY;

    static
    {
        OPEN = new SecureActionBundle();
        AUTHONLY = new SecureActionBundle();
        AUTHONLY.setRequiresAuthentication( true );
    }

    /**
     * Add an authorization tuple
     *
     * @param operation
     * @param resource
     * @throws SecureActionException
     */
    public void addRequiredAuthorization( String operation, String resource )
        throws SecureActionException
    {
        if ( operation != null && resource != null )
        {
            authorizationTuples.add( new AuthorizationTuple( operation, resource ) );
        }
        else
        {
            throw new SecureActionException( "operation and resource are required to be non-null" );
        }
    }

    /**
     * add an authorization tuple, assuming the resource part of it is Resource.GLOBAL
     *
     * @param operation
     * @throws SecureActionException
     */
    public void addRequiredAuthorization( String operation )
        throws SecureActionException
    {
        if ( operation != null )
        {
            authorizationTuples.add( new AuthorizationTuple( operation, Resource.GLOBAL ) );
        }
        else
        {
            throw new SecureActionException( "operation is required to be non-null" );
        }
    }

    public List<AuthorizationTuple> getAuthorizationTuples()
    {
        return authorizationTuples;
    }

    public boolean requiresAuthentication()
    {
        return requiresAuthentication;
    }

    public void setRequiresAuthentication( boolean requiresAuthentication )
    {
        this.requiresAuthentication = requiresAuthentication;
    }

    public static class AuthorizationTuple
    {
        private String operation;

        private String resource;

        public AuthorizationTuple( String operation, String resource )
        {
            this.operation = operation;
            this.resource = resource;
        }

        public String getOperation()
        {
            return operation;
        }

        public String getResource()
        {
            return resource;
        }


        public String toString()
        {
            return "AuthorizationTuple[" + operation + "," + resource + "]";
        }
    }
}
