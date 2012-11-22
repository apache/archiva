package org.apache.archiva.redback.common.ldap;

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

import javax.naming.directory.Attributes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// TODO this class should be able to be replaced with a model
public class LdapUser
    implements User, Serializable
{

    private String username;

    private String fullName;

    private String email;

    private String encodedPassword;

    private List<String> previousEncodedPasswords;

    private boolean locked = false;

    private boolean requiresPasswordChange = false;

    private boolean permanent = true;

    private boolean valid = true;

    private Date creationDate = null;

    private int failedLoginAttempts;

    private Date lastLoginDate = null;

    private Date lastPasswordChange = null;

    // DO NOT STORE AS SUCH!!!
    private String newPassword;

    private Attributes originalAttributes;

    public LdapUser( String username )
    {
        this.username = username;
        this.previousEncodedPasswords = new ArrayList<String>( 0 );
        this.failedLoginAttempts = 0;
    }

    public LdapUser( String username, String fullName, String email )
    {
        this( username );
        this.fullName = fullName;
        this.email = email;
    }

    public LdapUser()
    {
        previousEncodedPasswords = new ArrayList<String>( 0 );
        failedLoginAttempts = Integer.MIN_VALUE;
    }

    public void addPreviousEncodedPassword( String encodedPassword )
    {
        previousEncodedPasswords.add( encodedPassword );
    }

    public Date getAccountCreationDate()
    {
        return creationDate;
    }

    public int getCountFailedLoginAttempts()
    {
        return failedLoginAttempts;
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
        return newPassword;
    }

    public List<String> getPreviousEncodedPasswords()
    {
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

    public boolean isPasswordChangeRequired()
    {
        return requiresPasswordChange;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public boolean isValidated()
    {
        return valid;
    }

    public void setCountFailedLoginAttempts( int count )
    {
        failedLoginAttempts = count;
    }

    public void setEmail( String address )
    {
        email = address;
    }

    public void setEncodedPassword( String encodedPassword )
    {
        this.encodedPassword = encodedPassword;
    }

    public void setFullName( String name )
    {
        fullName = name;
    }

    public void setAccountCreationDate( Date date )
    {
        creationDate = date;
    }

    public void setLastLoginDate( Date date )
    {
        lastLoginDate = date;
    }

    public void setLastPasswordChange( Date passwordChangeDate )
    {
        lastPasswordChange = passwordChangeDate;
    }

    public void setLocked( boolean locked )
    {
        this.locked = locked;
    }

    public void setPassword( String rawPassword )
    {
        newPassword = rawPassword;
    }

    public void setPasswordChangeRequired( boolean changeRequired )
    {
        requiresPasswordChange = changeRequired;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
    {
        previousEncodedPasswords = new ArrayList<String>( encodedPasswordList );
    }

    public void setUsername( String name )
    {
        username = name;
    }

    public void setValidated( boolean valid )
    {
        this.valid = valid;
    }

    public Attributes getOriginalAttributes()
    {
        return originalAttributes;
    }

    public void setOriginalAttributes( Attributes originalAttributes )
    {
        this.originalAttributes = originalAttributes;
    }

}
