package org.apache.archiva.redback.authorization;

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

import org.apache.archiva.redback.users.User;

/**
 * @author Jason van Zyl
 */
public class AuthorizationDataSource
{
    private String principal;

    private User user;

    private String permission;

    private String resource;

    public AuthorizationDataSource( String principal, User user, String permission )
    {
        this.principal = principal;
        this.user = user;
        this.permission = permission;
    }

    public AuthorizationDataSource( String principal, User user, String permission, String resource )
    {
        this.principal = principal;
        this.user = user;
        this.permission = permission;
        this.resource = resource;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public String getPermission()
    {
        return permission;
    }

    public void setPermission( String permission )
    {
        this.permission = permission;
    }

    public String getResource()
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "AuthorizationDataSource" );
        sb.append( "{principal='" ).append( principal ).append( '\'' );
        sb.append( ", user=" ).append( user );
        sb.append( ", permission='" ).append( permission ).append( '\'' );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
