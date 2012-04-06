package org.codehaus.plexus.redback.authentication;

/*
 * Copyright 2001-2006 The Codehaus.
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

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * PasswordBasedAuthenticationDataSource: the username is considered the principal with this data source
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 */
@Service("authenticationDataSource#password")
@Scope("prototype")
public class PasswordBasedAuthenticationDataSource
    implements AuthenticationDataSource
{
    private String password;

    private String principal;

    public PasswordBasedAuthenticationDataSource()
    {

    }

    public PasswordBasedAuthenticationDataSource( String principal, String password )
    {
        this.principal = principal;
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "PasswordBasedAuthenticationDataSource[" );
        sb.append( "principal=" ).append( principal );
        sb.append( ",password=" );
        if ( StringUtils.isNotEmpty( password ) )
        {
            // Intentionally not showing real password 
            sb.append( "***" );
        }
        sb.append( ']' );
        return sb.toString();
    }

    public boolean isEnforcePasswordChange()
    {
        return true;
    }
}
