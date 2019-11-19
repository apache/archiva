package org.apache.archiva.web.security;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.redback.users.AbstractUserManager;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("userManager#archiva")
public class ArchivaConfigurableUsersManager
    extends AbstractUserManager
{

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Inject
    private ApplicationContext applicationContext;

    private Map<String, UserManager> userManagerPerId;

    @Inject
    @Named(value = "cache#users")
    private Cache<String, User> usersCache;

    private boolean useUsersCache;

    @PostConstruct
    @Override
    public void initialize()
    {
        try
        {
            List<String> userManagerImpls =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getUserManagerImpls();
            log.info( "use userManagerImpls: '{}'", userManagerImpls );

            userManagerPerId = new LinkedHashMap<>( userManagerImpls.size() );
            for ( String id : userManagerImpls )
            {
                UserManager userManagerImpl = applicationContext.getBean( "userManager#" + id, UserManager.class );
                setUserManagerImpl( userManagerImpl );
                userManagerPerId.put( id, userManagerImpl );
            }
            this.usersCache.clear();
            this.useUsersCache = redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().isUseUsersCache();
        }
        catch ( RepositoryAdminException e )
        {
            // revert to a default one ?
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    protected boolean useUsersCache()
    {
        return this.useUsersCache;
    }

    @Override
    public User addUser( User user )
        throws UserManagerException
    {
        user = userManagerPerId.get( user.getUserManagerId() ).addUser( user );

        if ( useUsersCache() )
        {
            usersCache.put( user.getUsername(), user );
        }

        return user;
    }

    @Override
    public void addUserUnchecked( User user )
        throws UserManagerException
    {
        userManagerPerId.get( user.getUserManagerId() ).addUserUnchecked( user );

        if ( useUsersCache() )
        {
            usersCache.put( user.getUsername(), user );
        }
    }

    @Override
    public User createUser( String username, String fullName, String emailAddress )
        throws UserManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        User user = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                if ( !userManager.isReadOnly() )
                {
                    user = userManager.createUser( username, fullName, emailAddress );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new UserManagerException( lastException.getMessage(), lastException );
        }
        return user;
    }

    @Override
    public UserQuery createUserQuery()
    {
        return userManagerPerId.values().iterator().next().createUserQuery();
    }


    @Override
    public void deleteUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        User user = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                if ( !userManager.isReadOnly() )
                {
                    userManager.deleteUser( username );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new UserManagerException( lastException.getMessage(), lastException );
        }
    }

    @Override
    public void eraseDatabase()
    {
        for ( UserManager userManager : userManagerPerId.values() )
        {
            userManager.eraseDatabase();
        }
    }

    @Override
    public User findUser( String username, boolean useCache )
        throws UserNotFoundException, UserManagerException
    {
        User user = null;
        if ( useUsersCache() && useCache )
        {
            user = usersCache.get( username );
            if ( user != null )
            {
                return user;
            }

        }
        Exception lastException = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                user = userManager.findUser( username );
                if ( user != null )
                {
                    if ( useUsersCache() )
                    {
                        usersCache.put( username, user );
                    }
                    return user;
                }
            }
            catch ( UserNotFoundException e )
            {
                lastException = e;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( user == null )
        {
            if ( lastException != null )
            {
                if ( lastException instanceof UserNotFoundException )
                {
                    throw (UserNotFoundException) lastException;
                }
                throw new UserManagerException( lastException.getMessage(), lastException );
            }
        }

        return user;
    }

    @Override
    public User findUser( String username )
        throws UserManagerException
    {
        return findUser( username, useUsersCache() );
    }


    @Override
    public User getGuestUser()
     throws UserNotFoundException, UserManagerException
    {
        return findUser( GUEST_USERNAME );
    }

    @Override
    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.findUsersByEmailKey( emailKey, orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.findUsersByFullNameKey( fullNameKey, orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByQuery( UserQuery query )
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.findUsersByQuery( query );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.findUsersByUsernameKey( usernameKey, orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public String getId()
    {
        return "archiva-configurable";
    }

    @Override
    public List<User> getUsers()
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.getUsers();
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> getUsers( boolean orderAscending )
        throws UserManagerException
    {
        List<User> users = new ArrayList<>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<? extends User> found = userManager.getUsers( orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public boolean isReadOnly()
    {
        boolean readOnly = false;

        for ( UserManager userManager : userManagerPerId.values() )
        {
            readOnly = readOnly || userManager.isReadOnly();
        }
        return readOnly;
    }

    @Override
    public User updateUser( User user )
        throws UserNotFoundException, UserManagerException
    {

        UserManager userManager = userManagerPerId.get( user.getUserManagerId() );

        user = userManager.updateUser( user );

        if ( useUsersCache() )
        {
            usersCache.put( user.getUsername(), user );
        }

        return user;
    }

    @Override
    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException, UserManagerException
    {
        user = userManagerPerId.get( user.getUserManagerId() ).updateUser( user, passwordChangeRequired );

        if ( useUsersCache() )
        {
            usersCache.put( user.getUsername(), user );
        }

        return user;
    }

    public void setUserManagerImpl( UserManager userManagerImpl )
    {
        // not possible here but we know so no need of log.error
        log.debug( "setUserManagerImpl cannot be used in this implementation" );
    }

    @Override
    public User createGuestUser()
        throws UserManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        User user = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                if ( !userManager.isReadOnly() )
                {
                    user = userManager.createGuestUser();
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new UserManagerException( lastException.getMessage(), lastException );
        }
        return user;
    }


    @Override
    public boolean userExists( String userName )
        throws UserManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        boolean exists = false;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {

                if ( userManager.userExists( userName ) )
                {
                    exists = true;
                }
                allFailed = false;

            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new UserManagerException( lastException.getMessage(), lastException );
        }
        return exists;
    }


    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    @Override
    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.configurable.archiva";
    }


}
