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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.archiva.redback.common.ldap.user.LdapUser;
import org.apache.archiva.redback.common.ldap.user.LdapUserMapper;
import org.apache.archiva.redback.common.ldap.user.UserMapper;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.users.ldap.LdapUserQuery;
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
    @Named( value = "userMapper#ldap" )
    private UserMapper mapper;

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
            //We don't store guest
            return;
        }

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

}
