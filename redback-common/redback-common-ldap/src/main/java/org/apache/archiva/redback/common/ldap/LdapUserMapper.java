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

import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.users.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.util.Date;

/**
 * @author <a href="jesse@codehaus.org"> jesse
 */
@Service("userMapper#ldap")
public class LdapUserMapper
    implements UserMapper
{
    /**
     *
     */
    String emailAttribute = "mail";

    /**
     *
     */
    String fullNameAttribute = "givenName";

    /**
     *
     */
    String passwordAttribute = "userPassword";

    /**
     *
     */
    String userIdAttribute = "cn";

    /**
     *
     */
    String userBaseDn;

    /**
     *
     */
    String userObjectClass = "inetOrgPerson";

    /**
     *
     */
    String userFilter;

    /**
     *
     */
    int maxResultCount = 0;

    @Inject
    @Named(value = "userConfiguration")
    private UserConfiguration userConf;

    @PostConstruct
    public void initialize()
    {
        emailAttribute = userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_EMAIL, emailAttribute );
        fullNameAttribute =
            userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_FULLNAME, fullNameAttribute );
        passwordAttribute =
            userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_PASSWORD, passwordAttribute );
        userIdAttribute = userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_ID, userIdAttribute );
        userBaseDn = userConf.getConcatenatedList( "ldap.config.mapper.attribute.user.base.dn",
                                                   userConf.getConcatenatedList( "ldap.config.base.dn", userBaseDn ) );
        userObjectClass =
            userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_OBJECT_CLASS, userObjectClass );
        userFilter = userConf.getString( UserConfigurationKeys.LDAP_MAPPER_USER_ATTRIBUTE_FILTER, userFilter );
        maxResultCount = userConf.getInt( UserConfigurationKeys.LDAP_MAX_RESULT_COUNT, maxResultCount );
    }

    public Attributes getCreationAttributes( User user, boolean encodePasswordIfChanged )
        throws MappingException
    {
        Attributes userAttrs = new BasicAttributes();

        boolean passwordSet = false;

        if ( !passwordSet && ( user.getEncodedPassword() != null ) )
        {
            userAttrs.put( getPasswordAttribute(), user.getEncodedPassword() );
        }

        if ( !StringUtils.isEmpty( user.getFullName() ) )
        {
            userAttrs.put( getUserFullNameAttribute(), user.getFullName() );
        }

        if ( !StringUtils.isEmpty( user.getEmail() ) )
        {
            userAttrs.put( getEmailAddressAttribute(), user.getEmail() );
        }

        return userAttrs;
    }

    public String getEmailAddressAttribute()
    {
        return emailAttribute;
    }

    public String getUserFullNameAttribute()
    {
        return fullNameAttribute;
    }

    public String getPasswordAttribute()
    {
        return passwordAttribute;
    }

    public String[] getUserAttributeNames()
    {
        return new String[]{ emailAttribute, fullNameAttribute, passwordAttribute, userIdAttribute };
    }

    public int getMaxResultCount()
    {
        return maxResultCount;
    }

    public UserUpdate getUpdate( LdapUser user )
        throws MappingException
    {

        Attributes addAttrs = new BasicAttributes();

        Attributes modAttrs = new BasicAttributes();

        if ( !StringUtils.isEmpty( user.getFullName() ) )
        {
            if ( user.getFullName() == null )
            {
                addAttrs.put( getUserFullNameAttribute(), user.getFullName() );
            }
            else if ( !user.getFullName().equals( user.getFullName() ) )
            {
                modAttrs.put( getUserFullNameAttribute(), user.getFullName() );
            }
        }

        if ( !StringUtils.isEmpty( user.getEmail() ) )
        {
            if ( user.getEmail() == null )
            {
                addAttrs.put( getEmailAddressAttribute(), user.getEmail() );
            }
            else if ( !user.getEmail().equals( user.getEmail() ) )
            {
                modAttrs.put( getEmailAddressAttribute(), user.getEmail() );
            }
        }

        return null;
    }

    public LdapUser getUser( Attributes attributes )
        throws MappingException
    {
        String userIdAttribute = getUserIdAttribute();
        String emailAddressAttribute = getEmailAddressAttribute();
        String nameAttribute = getUserFullNameAttribute();
        String passwordAttribute = getPasswordAttribute();

        String userId = ( LdapUtils.getAttributeValue( attributes, userIdAttribute, "username" ) );

        LdapUser user = new LdapUser( userId );
        user.setOriginalAttributes( attributes );

        user.setEmail( LdapUtils.getAttributeValue( attributes, emailAddressAttribute, "email address" ) );
        user.setFullName( LdapUtils.getAttributeValue( attributes, nameAttribute, "name" ) );

        String encodedPassword = LdapUtils.getAttributeValueFromByteArray( attributes, passwordAttribute, "password" );

        // it seems to be a common convention for the password to come back prepended with the encoding type..
        // however we deal with that via configuration right now so just smoke it.
        if ( encodedPassword != null && encodedPassword.startsWith( "{" ) )
        {
            encodedPassword = encodedPassword.substring( encodedPassword.indexOf( "}" ) + 1 );
        }

        user.setEncodedPassword( encodedPassword );

        // REDBACK-215: skip NPE
        user.setLastPasswordChange( new Date() );

        return user;
    }

    public String getUserIdAttribute()
    {
        return userIdAttribute;
    }

    public String getEmailAttribute()
    {
        return emailAttribute;
    }

    public void setEmailAttribute( String emailAttribute )
    {
        this.emailAttribute = emailAttribute;
    }

    public String getFullNameAttribute()
    {
        return fullNameAttribute;
    }

    public void setFullNameAttribute( String fullNameAttribute )
    {
        this.fullNameAttribute = fullNameAttribute;
    }

    public void setMaxResultCount( int maxResultCount )
    {
        this.maxResultCount = maxResultCount;
    }

    public String getUserBaseDn()
    {
        return userBaseDn;
    }

    public void setUserBaseDn( String userBaseDn )
    {
        this.userBaseDn = userBaseDn;
    }

    public String getUserObjectClass()
    {
        return userObjectClass;
    }

    public String getUserFilter()
    {
        return userFilter;
    }

    public void setUserFilter( String userFilter )
    {
        this.userFilter = userFilter;
    }

    public void setUserObjectClass( String userObjectClass )
    {
        this.userObjectClass = userObjectClass;
    }

    public void setPasswordAttribute( String passwordAttribute )
    {
        this.passwordAttribute = passwordAttribute;
    }

    public void setUserIdAttribute( String userIdAttribute )
    {
        this.userIdAttribute = userIdAttribute;
    }

    public LdapUser newUserInstance( String username, String fullName, String email )
    {
        return new LdapUser( username, fullName, email );
    }

    public LdapUser newTemplateUserInstance()
    {
        return new LdapUser();
    }

    public String[] getReturningAttributes()
    {
        return new String[]{ getUserIdAttribute(), getEmailAttribute(), getFullNameAttribute(),
            getPasswordAttribute() };
    }

    public UserConfiguration getUserConf()
    {
        return userConf;
    }

    public void setUserConf( UserConfiguration userConf )
    {
        this.userConf = userConf;
    }
}
