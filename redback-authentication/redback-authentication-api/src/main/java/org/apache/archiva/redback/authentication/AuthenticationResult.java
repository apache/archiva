package org.apache.archiva.redback.authentication;

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

import java.io.Serializable;
import java.util.Map;

/**
 * AuthenticationResult: wrapper object for information that comes back from the authentication system
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 *
 */
public class AuthenticationResult
    implements Serializable
{
    private boolean isAuthenticated;

    /**
     * FIXME olamy this Object is a pain !!!
     */
    private String principal;

    // TODO: why aren't these just thrown from the authenticate() method?
    private Exception exception;

    private Map<String,String> exceptionsMap;

    public AuthenticationResult()
    {
        this.isAuthenticated = false;
        this.principal = null;
        this.exception = null;
    }

    public AuthenticationResult( boolean authenticated, String principal, Exception exception )
    {
        isAuthenticated = authenticated;
        this.principal = principal;
        this.exception = exception;
    }

    public AuthenticationResult( boolean authenticated, String principal, Exception exception, Map<String,String> exceptionsMap )
    {
        isAuthenticated = authenticated;
        this.principal = principal;
        this.exception = exception;
        this.exceptionsMap = exceptionsMap;
    }

    public boolean isAuthenticated()
    {
        return isAuthenticated;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public Exception getException()
    {
        return exception;
    }

    public Map<String,String> getExceptionsMap()
    {
        return exceptionsMap;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "AuthenticationResult[" );
        sb.append( "principal=" ).append( principal );
        sb.append( ",isAuthenticated=" ).append( Boolean.toString( isAuthenticated ) );
        sb.append( ",exception=" );
        if ( exception != null )
        {
            sb.append( exception.getClass().getName() );
            sb.append( " : " ).append( exception.getMessage() );
        }
        else
        {
            sb.append( "<null>" );
        }
        sb.append( ']' );
        return sb.toString();
    }
}
