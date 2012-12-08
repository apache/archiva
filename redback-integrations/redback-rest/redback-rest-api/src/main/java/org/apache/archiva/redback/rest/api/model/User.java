package org.apache.archiva.redback.rest.api.model;

import org.apache.archiva.redback.integration.util.DateUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

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

@XmlRootElement( name = "user" )
public class User
    implements Serializable
{
    private String username;

    private String fullName;

    private String email;

    private boolean validated;

    private boolean locked;

    private String password;

    private boolean passwordChangeRequired;

    private boolean permanent;

    private String confirmPassword;

    // Display Only Fields.
    private String timestampAccountCreation;

    private String timestampLastLogin;

    private String timestampLastPasswordChange;

    /**
     * for password change only
     *
     * @since 1.4
     */
    private String previousPassword;

    /**
     * for roles update only <b>not return on user read</b>
     *
     * @since 2.0
     */
    private List<String> assignedRoles;

    /**
     * with some userManagerImpl it's not possible to edit users;
     * @since 2.1
     */
    private boolean readOnly;

    /**
     * as we can user multiple userManagers implementation we must track from which one this one comes.
     * @since 2.1
     * @return userManager id
     */
    private String userManagerId;

    public User()
    {
        // no op
    }

    public User( String username, String fullName, String email, boolean validated, boolean locked )
    {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.validated = validated;
        this.locked = locked;
    }

    public User( org.apache.archiva.redback.users.User user )
    {
        setUsername( user.getUsername() );
        this.setEmail( user.getEmail() );
        this.setFullName( user.getFullName() );
        this.setLocked( user.isLocked() );
        this.setPassword( user.getPassword() );
        this.setValidated( user.isValidated() );
        this.setPasswordChangeRequired( user.isPasswordChangeRequired() );
        this.setPermanent( user.isPermanent() );

        setTimestampAccountCreation( DateUtils.formatWithAge( user.getAccountCreationDate(), "ago" ) );
        setTimestampLastLogin( DateUtils.formatWithAge( user.getLastLoginDate(), "ago" ) );
        setTimestampLastPasswordChange( DateUtils.formatWithAge( user.getLastPasswordChange(), "ago" ) );
    }


    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public boolean isValidated()
    {
        return validated;
    }

    public void setValidated( boolean validated )
    {
        this.validated = validated;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked( boolean isLocked )
    {
        this.locked = isLocked;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
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

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword( String confirmPassword )
    {
        this.confirmPassword = confirmPassword;
    }

    public String getTimestampAccountCreation()
    {
        return timestampAccountCreation;
    }

    public void setTimestampAccountCreation( String timestampAccountCreation )
    {
        this.timestampAccountCreation = timestampAccountCreation;
    }

    public String getTimestampLastLogin()
    {
        return timestampLastLogin;
    }

    public void setTimestampLastLogin( String timestampLastLogin )
    {
        this.timestampLastLogin = timestampLastLogin;
    }

    public String getTimestampLastPasswordChange()
    {
        return timestampLastPasswordChange;
    }

    public void setTimestampLastPasswordChange( String timestampLastPasswordChange )
    {
        this.timestampLastPasswordChange = timestampLastPasswordChange;
    }

    public String getPreviousPassword()
    {
        return previousPassword;
    }

    public void setPreviousPassword( String previousPassword )
    {
        this.previousPassword = previousPassword;
    }

    public List<String> getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List<String> assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
    }

    public String getUserManagerId()
    {
        return userManagerId;
    }

    public void setUserManagerId( String userManagerId )
    {
        this.userManagerId = userManagerId;
    }

    @Override
    public String toString()
    {
        return "User{" +
            "username='" + username + '\'' +
            ", fullName='" + fullName + '\'' +
            ", email='" + email + '\'' +
            ", validated=" + validated +
            ", locked=" + locked +
            //", password='" + password + '\'' +
            ", passwordChangeRequired=" + passwordChangeRequired +
            ", permanent=" + permanent +
            ", confirmPassword='" + confirmPassword + '\'' +
            ", timestampAccountCreation='" + timestampAccountCreation + '\'' +
            ", timestampLastLogin='" + timestampLastLogin + '\'' +
            ", timestampLastPasswordChange='" + timestampLastPasswordChange + '\'' +
            ", previousPassword='" + previousPassword + '\'' +
            ", assignedRoles=" + assignedRoles +
            ", readOnly=" + readOnly +
            ", userManagerId='" + userManagerId + '\'' +
            '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof User ) )
        {
            return false;
        }

        User user = (User) o;

        if ( !username.equals( user.username ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return username.hashCode();
    }
}
