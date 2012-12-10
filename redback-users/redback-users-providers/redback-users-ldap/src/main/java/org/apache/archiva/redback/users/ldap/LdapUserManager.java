package org.apache.archiva.redback.users.ldap;

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


import org.apache.archiva.redback.common.ldap.LdapUser;
import org.apache.archiva.redback.common.ldap.UserMapper;
import org.apache.archiva.redback.users.AbstractUserManager;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.ldap.ctl.LdapController;
import org.apache.archiva.redback.users.ldap.ctl.LdapControllerException;
import org.apache.archiva.redback.users.ldap.service.LdapCacheService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.directory.DirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="jesse@codehaus.org"> jesse
 */
@Service("userManager#ldap")
public class LdapUserManager
    extends AbstractUserManager
    implements UserManager
{
    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory connectionFactory;

    @Inject
    private LdapController controller;

    @Inject
    @Named(value = "userMapper#ldap")
    private UserMapper mapper;

    @Inject
    private LdapCacheService ldapCacheService;

    private User guestUser;

    public boolean isReadOnly()
    {
        return true;
    }

    public User addUser( User user )
    {
        try
        {
            return addUser( user, true );
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
    }

    public void addUserUnchecked( User user )
    {
        try
        {
            addUser( user, false );
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
    }

    private User addUser( User user, boolean checked )
        throws LdapException
    {
        if ( user == null )
        {
            return null;
        }

        if ( GUEST_USERNAME.equals( user.getUsername() ) )
        {
            guestUser = user;
            return guestUser;
        }

        LdapConnection ldapConnection = getLdapConnection();
        try
        {
            DirContext context = ldapConnection.getDirContext();
            controller.createUser( user, context, checked );
        }
        catch ( LdapControllerException e )
        {
            log.error( "Error mapping user: " + user.getUsername() + " to LDAP attributes.", e );
        }
        catch ( MappingException e )
        {
            log.error( "Error mapping user: " + user.getUsername() + " to LDAP attributes.", e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
        return user;
    }

    public User createUser( String username, String fullName, String emailAddress )
    {
        return mapper.newUserInstance( username, fullName, emailAddress );
    }

    public UserQuery createUserQuery()
    {
        return new LdapUserQuery();
    }


    public void deleteUser( String username )
        throws UserNotFoundException
    {
        if ( username != null )
        {
            clearFromCache( username );
        }
        LdapConnection ldapConnection = null;
        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            controller.removeUser( username, context );
        }
        catch ( LdapControllerException e )
        {
            log.error( "Failed to delete user: " + username, e );
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
    }

    public void eraseDatabase()
    {
        // TODO Implement erase!
    }

    public User findUser( String username )
        throws UserNotFoundException
    {
        if ( username == null )
        {
            throw new UserNotFoundException( "Unable to find user based on null username." );
        }

        if ( GUEST_USERNAME.equals( username ) )
        {
            return getGuestUser();
        }

        // REDBACK-289/MRM-1488
        // look for the user in the cache first
        LdapUser ldapUser = ldapCacheService.getUser( username );
        if ( ldapUser != null )
        {
            log.debug( "User {} found in cache.", username );
            return ldapUser;
        }

        LdapConnection ldapConnection = null;

        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            User user = controller.getUser( username, context );
            if ( user == null )
            {
                throw new UserNotFoundException( "user with name " + username + " not found " );
            }

            // REDBACK-289/MRM-1488
            log.debug( "Adding user {} to cache..", username );

            ldapCacheService.addUser( (LdapUser) user );

            return user;
        }
        catch ( LdapControllerException e )
        {
            log.error( "Failed to find user: {}", username, e );
            return null;
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
        catch ( MappingException e )
        {
            log.error( "Failed to map user: {}", username, e );
            return null;
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
    }

    public User getGuestUser()
        throws UserNotFoundException
    {
        if ( guestUser == null )
        {
            throw new UserNotFoundException( "Guest user doesn't exist." );
        }
        return guestUser;
    }

    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
    {
        LdapUserQuery query = new LdapUserQuery();
        query.setEmail( emailKey );
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        query.setAscending( orderAscending );
        return findUsersByQuery( query );
    }

    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
    {
        LdapUserQuery query = new LdapUserQuery();
        query.setFullName( fullNameKey );
        query.setOrderBy( UserQuery.ORDER_BY_FULLNAME );
        query.setAscending( orderAscending );
        return findUsersByQuery( query );
    }

    public List<User> findUsersByQuery( UserQuery query )
    {
        if ( query == null )
        {
            return Collections.emptyList();
        }

        LdapConnection ldapConnection = null;

        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            return controller.getUsersByQuery( (LdapUserQuery) query, context );
        }
        catch ( LdapControllerException e )
        {
            log.error( "Failed to find user", e );
            return null;
        }
        catch ( MappingException e )
        {
            log.error( "Failed to map user", e );
            return null;
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
    }

    /**
     * @see org.apache.archiva.redback.users.UserManager#findUsersByUsernameKey(java.lang.String, boolean)
     */
    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
    {
        LdapUserQuery query = new LdapUserQuery();
        query.setUsername( usernameKey );
        query.setOrderBy( UserQuery.ORDER_BY_USERNAME );
        query.setAscending( orderAscending );
        return findUsersByQuery( query );
    }

    public String getId()
    {
        return "ldap";
    }

    /**
     * @see org.apache.archiva.redback.users.UserManager#getUsers()
     */
    public List<User> getUsers()
    {
        LdapConnection ldapConnection = null;

        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            List<User> users = new ArrayList<User>( controller.getUsers( context ) );
            //We add the guest user because it isn't in LDAP
            try
            {
                User u = getGuestUser();
                if ( u != null )
                {
                    users.add( u );
                }
            }
            catch ( UserNotFoundException e )
            {
                //Nothing to do
            }
            return users;
        }
        /*catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }*/
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
        return Collections.emptyList();
    }

    public List<User> getUsers( boolean orderAscending )
    {
        return getUsers();
    }

    public User updateUser( User user )
        throws UserNotFoundException
    {
        return updateUser( user, false );
    }

    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException
    {
        if ( user != null )
        {
            clearFromCache( user.getUsername() );
        }

        LdapConnection ldapConnection = null;

        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            controller.updateUser( user, context );
        }
        catch ( LdapControllerException e )
        {
            log.error( "Failed to update user: " + user.getUsername(), e );
        }
        catch ( MappingException e )
        {
            log.error( "Failed to update user: " + user.getUsername(), e );
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
        return user;
    }

    public boolean userExists( String principal )
    {
        if ( principal == null )
        {
            return false;
        }

        // REDBACK-289/MRM-1488
        // look for the user in the cache first
        LdapUser ldapUser = ldapCacheService.getUser( principal.toString() );
        if ( ldapUser != null )
        {
            log.debug( "User {} found in cache.", principal );
            return true;
        }

        LdapConnection ldapConnection = null;

        try
        {
            ldapConnection = getLdapConnection();
            DirContext context = ldapConnection.getDirContext();
            return controller.userExists( principal, context );
        }
        catch ( LdapControllerException e )
        {
            log.warn( "Failed to search for user: " + principal, e );
            return false;
        }
        catch ( LdapException e )
        {
            throw new UserManagerException( e.getMessage(), e );
        }
        finally
        {
            closeLdapConnection( ldapConnection );
        }
    }

    private LdapConnection getLdapConnection()
        throws LdapException
    {
        try
        {
            return connectionFactory.getConnection();
        }
        catch ( LdapException e )
        {
            log.warn( "failed to get a ldap connection " + e.getMessage(), e );
            throw new LdapException( "failed to get a ldap connection " + e.getMessage(), e );
        }
    }

    private void closeLdapConnection( LdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            ldapConnection.close();
        }
    }

    // REDBACK-289/MRM-1488
    private void clearFromCache( String username )
    {
        log.debug( "Removing user {} from cache..", username );
        ldapCacheService.removeUser( username );

        log.debug( "Removing userDn for user {} from cache..", username );
        ldapCacheService.removeLdapUserDn( username );
    }

    public boolean isFinalImplementation()
    {
        return true;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.ldap";
    }
}
