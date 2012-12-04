package org.apache.archiva.redback.integration.model;

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
import org.apache.archiva.redback.users.UserManager;
import org.apache.commons.lang.StringUtils;


import java.io.Serializable;

/**
 * UserCredentials
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public abstract class UserCredentials
    implements Serializable
{
    // Potentially Editable Field.
    private String username;

    // Editable Fields.
    private String password;

    private String confirmPassword;

    private String fullName;

    private String email;

    // Display Only Fields.
    private String timestampAccountCreation;

    private String timestampLastLogin;

    private String timestampLastPasswordChange;

    public User createUser( UserManager um )
    {
        User user = um.createUser( username, fullName, email );

        user.setPassword( password );

        return user;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "UserCredentials[" );
        sb.append( "username=" ).append( username );
        sb.append( ",fullName=" ).append( fullName );
        sb.append( ",email=" ).append( email );
        sb.append( ",password=" );
        if ( StringUtils.isNotEmpty( password ) )
        {
            sb.append( "<***>" );
        }
        else
        {
            sb.append( "<empty>" );
        }
        sb.append( ",confirmPassword=" );
        if ( StringUtils.isNotEmpty( confirmPassword ) )
        {
            sb.append( "<***>" );
        }
        else
        {
            sb.append( "<empty>" );
        }

        return sb.append( "]" ).toString();
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword( String confirmPassword )
    {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public abstract boolean isEdit();

    public String getTimestampAccountCreation()
    {
        return timestampAccountCreation;
    }

    public String getTimestampLastLogin()
    {
        return timestampLastLogin;
    }

    public String getTimestampLastPasswordChange()
    {
        return timestampLastPasswordChange;
    }

    public void setTimestampAccountCreation( String timestampAccountCreation )
    {
        this.timestampAccountCreation = timestampAccountCreation;
    }

    public void setTimestampLastLogin( String timestampLastLogin )
    {
        this.timestampLastLogin = timestampLastLogin;
    }

    public void setTimestampLastPasswordChange( String timestampLastPasswordChange )
    {
        this.timestampLastPasswordChange = timestampLastPasswordChange;
    }
}
