package org.apache.archiva.redback.users.memory;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A Simple User record.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class SimpleUser
    implements User, Serializable
{
    private String username;

    private String password;

    private String email;

    private String fullName;

    private String encodedPassword;

    private Date lastPasswordChange;

    private Date lastLoginDate;

    private int countFailedLoginAttempts = 0;

    private boolean locked = false;

    private boolean permanent = false;

    private boolean validated = false;

    private List<String> previousEncodedPasswords;

    private Date accountCreationDate;

    private boolean passwordChangeRequired;

    public SimpleUser()
    {
        // no op
    }

    public void addPreviousEncodedPassword( String encodedPassword )
    {
        getPreviousEncodedPasswords().add( encodedPassword );
    }

    public Date getAccountCreationDate()
    {
        return accountCreationDate;
    }

    public int getCountFailedLoginAttempts()
    {
        return countFailedLoginAttempts;
    }

    public String getEmail()
    {
        return email;
    }

    public String getEncodedPassword()
    {
        return encodedPassword;
    }

    public String getFullName()
    {
        return fullName;
    }

    public Date getLastLoginDate()
    {
        return lastLoginDate;
    }

    public Date getLastPasswordChange()
    {
        return lastPasswordChange;
    }

    public String getPassword()
    {
        return password;
    }

    public List<String> getPreviousEncodedPasswords()
    {
        if ( previousEncodedPasswords == null )
        {
            previousEncodedPasswords = new ArrayList<String>();
        }
        return previousEncodedPasswords;
    }

    public String getUsername()
    {
        return username;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setAccountCreationDate( Date accountCreationDate )
    {
        this.accountCreationDate = accountCreationDate;
    }

    public void setCountFailedLoginAttempts( int countFailedLoginAttempts )
    {
        this.countFailedLoginAttempts = countFailedLoginAttempts;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public void setEncodedPassword( String encodedPassword )
    {
        this.encodedPassword = encodedPassword;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public void setLastLoginDate( Date lastLoginDate )
    {
        this.lastLoginDate = lastLoginDate;
    }

    public void setLastPasswordChange( Date lastPasswordChange )
    {
        this.lastPasswordChange = lastPasswordChange;
    }

    public void setLocked( boolean locked )
    {
        this.locked = locked;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public void setPreviousEncodedPasswords( List<String> previousEncodedPasswords )
    {
        this.previousEncodedPasswords = previousEncodedPasswords;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public boolean isPasswordChangeRequired()
    {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired( boolean passwordChangeRequired )
    {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    public boolean isValidated()
    {
        return validated;
    }

    public void setValidated( boolean validated )
    {
        this.validated = validated;
    }
}
