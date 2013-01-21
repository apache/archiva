package org.apache.archiva.redback.users.ldap.ctl;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.user.LdapUser;
import org.apache.archiva.redback.common.ldap.user.LdapUserMapper;
import org.apache.archiva.redback.common.ldap.user.UserMapper;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.encoders.SHA1PasswordEncoder;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.users.ldap.LdapUserQuery;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author <a href="jesse@codehaus.org"> jesse
 */
@Service
public class DefaultLdapController
    implements LdapController
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "userMapper#ldap")
    private UserMapper mapper;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration userConf;

    private boolean writableLdap = false;

    private PasswordEncoder passwordEncoder;

    private String baseDn;

    private String groupsDn;

    private String ldapGroupClass = "groupOfUniqueNames";

    @PostConstruct
    public void initialize()
    {
        this.writableLdap = userConf.getBoolean( UserConfigurationKeys.LDAP_WRITABLE, this.writableLdap );
        this.baseDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_BASEDN, null );
        this.passwordEncoder = new SHA1PasswordEncoder();
        this.groupsDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_GROUPS_BASEDN, this.groupsDn );
        this.ldapGroupClass = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_CLASS, this.ldapGroupClass );
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#removeUser(String, javax.naming.directory.DirContext)
     */
    public void removeUser( String principal, DirContext context )
        throws LdapControllerException
    {
        // no op
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#updateUser(org.apache.archiva.redback.users.User, javax.naming.directory.DirContext)
     */
    public void updateUser( User user, DirContext context )
        throws LdapControllerException, MappingException
    {
        // no op
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#userExists(String, javax.naming.directory.DirContext)
     */
    public boolean userExists( String key, DirContext context )
        throws LdapControllerException
    {
        NamingEnumeration<SearchResult> results = null;
        try
        {
            results = searchUsers( key, context );
            return results.hasMoreElements();
        }
        catch ( NamingException e )
        {
            throw new LdapControllerException( "Error searching for the existence of user: " + key, e );
        }
        finally
        {
            if ( results != null )
            {
                try
                {
                    results.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "Error closing search results", e );
                }
            }
        }
    }

    protected NamingEnumeration<SearchResult> searchUsers( String key, DirContext context )
        throws NamingException
    {
        LdapUserQuery query = new LdapUserQuery();
        query.setUsername( key );
        return searchUsers( context, null, query );
    }

    protected NamingEnumeration<SearchResult> searchUsers( DirContext context )
        throws NamingException
    {
        return searchUsers( context, null, null );
    }

    protected NamingEnumeration<SearchResult> searchUsers( DirContext context, String[] returnAttributes )
        throws NamingException
    {
        return searchUsers( context, returnAttributes, null );
    }

    protected NamingEnumeration<SearchResult> searchUsers( DirContext context, String[] returnAttributes,
                                                           LdapUserQuery query )
        throws NamingException
    {
        if ( query == null )
        {
            query = new LdapUserQuery();
        }
        SearchControls ctls = new SearchControls();

        ctls.setDerefLinkFlag( true );
        ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        ctls.setReturningAttributes( mapper.getReturningAttributes() );
        ctls.setCountLimit( ( (LdapUserMapper) mapper ).getMaxResultCount() );

        String finalFilter = new StringBuilder( "(&(objectClass=" + mapper.getUserObjectClass() + ")" ).append(
            ( mapper.getUserFilter() != null ? mapper.getUserFilter() : "" ) ).append(
            query.getLdapFilter( mapper ) + ")" ).toString();

        log.debug( "Searching for users with filter: '{}' from base dn: {}", finalFilter, mapper.getUserBaseDn() );

        return context.search( mapper.getUserBaseDn(), finalFilter, ctls );
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#getUsers(javax.naming.directory.DirContext)
     */
    public Collection<User> getUsers( DirContext context )
        throws LdapControllerException, MappingException
    {
        NamingEnumeration<SearchResult> results = null;
        try
        {
            results = searchUsers( context, null, null );
            Set<User> users = new LinkedHashSet<User>();

            while ( results.hasMoreElements() )
            {
                SearchResult result = results.nextElement();

                users.add( mapper.getUser( result.getAttributes() ) );
            }

            return users;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";

            throw new LdapControllerException( message, e );
        }
        finally
        {
            if ( results != null )
            {
                try
                {
                    results.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#getUsersByQuery(org.apache.archiva.redback.users.ldap.LdapUserQuery, javax.naming.directory.DirContext)
     */
    public List<User> getUsersByQuery( LdapUserQuery query, DirContext context )
        throws LdapControllerException, MappingException
    {
        NamingEnumeration<SearchResult> results = null;
        try
        {
            results = searchUsers( context, null, query );
            List<User> users = new LinkedList<User>();

            while ( results.hasMoreElements() )
            {
                SearchResult result = results.nextElement();

                users.add( mapper.getUser( result.getAttributes() ) );
            }

            return users;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";

            throw new LdapControllerException( message, e );
        }
        finally
        {
            if ( results != null )
            {
                try
                {
                    results.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#createUser(org.apache.archiva.redback.users.User, javax.naming.directory.DirContext, boolean)
     */
    public void createUser( User user, DirContext context, boolean encodePasswordIfChanged )
        throws LdapControllerException, MappingException
    {
        if ( user == null )
        {
            return;
        }
        if ( user.getUsername().equals( UserManager.GUEST_USERNAME ) )
        {
            log.warn( "skip user '{}' creation" );
            //We don't store guest
            return;
        }
        boolean userExists = userExists( user.getUsername(), context );
        if ( userExists )
        {
            log.debug( "user '{}' exists skip creation", user.getUsername() );
            return;
        }
        if ( writableLdap )
        {
            try
            {
                bindUserObject( context, user );
                log.info( "user {} created in ldap", user.getUsername() );
            }
            catch ( NamingException e )
            {
                throw new LdapControllerException( e.getMessage(), e );
            }
        }
    }


    private void bindUserObject( DirContext context, User user )
        throws NamingException
    {
        Attributes attributes = new BasicAttributes( true );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "inetOrgPerson" );
        objectClass.add( "person" );
        objectClass.add( "organizationalperson" );
        attributes.put( objectClass );
        attributes.put( "cn", user.getUsername() );
        attributes.put( "sn", "foo" );
        if ( StringUtils.isNotEmpty( user.getEmail() ) )
        {
            attributes.put( "mail", user.getEmail() );
        }

        if ( userConf.getBoolean( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ALLOW_EMPTY_PASSWORDS, false )
            && StringUtils.isNotEmpty( user.getPassword() ) )
        {
            attributes.put( "userPassword", passwordEncoder.encodePassword( user.getPassword() ) );
        }
        attributes.put( "givenName", "foo" );
        context.createSubcontext( "cn=" + user.getUsername() + "," + this.getBaseDn(), attributes );
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.ctl.LdapController#getUser(String, javax.naming.directory.DirContext)
     */
    public LdapUser getUser( String username, DirContext context )
        throws LdapControllerException, MappingException
    {

        log.debug( "Searching for user: {}", username );

        LdapUserQuery query = new LdapUserQuery();
        query.setUsername( username );

        NamingEnumeration<SearchResult> result = null;
        try
        {
            result = searchUsers( context, null, query );

            if ( result.hasMoreElements() )
            {
                SearchResult next = result.nextElement();

                log.info( "Found user: {}", username );

                return mapper.getUser( next.getAttributes() );
            }
            else
            {
                return null;
            }
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve information for user: " + username;

            throw new LdapControllerException( message, e );
        }
        finally
        {
            if ( result != null )
            {
                try
                {
                    result.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    public Map<String, Collection<String>> findUsersWithRoles( DirContext dirContext )
        throws LdapControllerException
    {
        Map<String, Collection<String>> usersWithRoles = new HashMap<String, Collection<String>>();

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = dirContext.search( getGroupsDn(), filter, searchControls );

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();

                String groupName = searchResult.getName();
                // cn=blabla we only want bla bla
                groupName = StringUtils.substringAfter( groupName, "=" );

                Attribute uniqueMemberAttr = searchResult.getAttributes().get( "uniquemember" );

                if ( uniqueMemberAttr != null )
                {
                    NamingEnumeration<String> allMembersEnum = (NamingEnumeration<String>) uniqueMemberAttr.getAll();
                    while ( allMembersEnum.hasMore() )
                    {
                        String userName = allMembersEnum.next();
                        // uid=blabla we only want bla bla
                        userName = StringUtils.substringAfter( userName, "=" );
                        userName = StringUtils.substringBefore( userName, "," );
                        Collection<String> roles = usersWithRoles.get( userName );
                        if ( roles == null )
                        {
                            roles = new HashSet<String>();
                        }

                        roles.add( groupName );

                        usersWithRoles.put( userName, roles );

                    }
                }

                log.debug( "found groupName: '{}' with users: {}", groupName );

            }

            return usersWithRoles;
        }
        catch ( NamingException e )
        {
            throw new LdapControllerException( e.getMessage(), e );
        }

        finally
        {

            if ( namingEnumeration != null )
            {
                try
                {
                    namingEnumeration.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    //-----------------------------
    // setters/getters
    //-----------------------------
    public UserMapper getMapper()
    {
        return mapper;
    }

    public void setMapper( UserMapper mapper )
    {
        this.mapper = mapper;
    }

    public UserConfiguration getUserConf()
    {
        return userConf;
    }

    public void setUserConf( UserConfiguration userConf )
    {
        this.userConf = userConf;
    }

    public boolean isWritableLdap()
    {
        return writableLdap;
    }

    public void setWritableLdap( boolean writableLdap )
    {
        this.writableLdap = writableLdap;
    }

    public PasswordEncoder getPasswordEncoder()
    {
        return passwordEncoder;
    }

    public void setPasswordEncoder( PasswordEncoder passwordEncoder )
    {
        this.passwordEncoder = passwordEncoder;
    }

    public String getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    public String getGroupsDn()
    {
        return groupsDn;
    }

    public void setGroupsDn( String groupsDn )
    {
        this.groupsDn = groupsDn;
    }

    public String getLdapGroupClass()
    {
        return ldapGroupClass;
    }

    public void setLdapGroupClass( String ldapGroupClass )
    {
        this.ldapGroupClass = ldapGroupClass;
    }
}
