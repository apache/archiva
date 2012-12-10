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
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerListener;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.configurable.ConfigurableUserManager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "userManager#archiva" )
public class ArchivaConfigurableUsersManager
    extends ConfigurableUserManager
{

    @Inject
    private ArchivaRuntimeConfigurationAdmin archivaRuntimeConfigurationAdmin;

    @Inject
    private ApplicationContext applicationContext;

    private Map<String, UserManager> userManagerPerId;

    private List<UserManagerListener> listeners = new ArrayList<UserManagerListener>();

    @Override
    public void initialize()
    {
        try
        {
            List<String> userManagerImpls =
                archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfiguration().getUserManagerImpls();
            log.info( "use userManagerImpls: '{}'", userManagerImpls );

            userManagerPerId = new LinkedHashMap<String, UserManager>( userManagerImpls.size() );
            for ( String id : userManagerImpls )
            {
                UserManager userManagerImpl = applicationContext.getBean( "userManager#" + id, UserManager.class );
                setUserManagerImpl( userManagerImpl );
                userManagerPerId.put( id, userManagerImpl );
            }
        }
        catch ( RepositoryAdminException e )
        {
            // revert to a default one ?
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    @Override
    public User addUser( User user )
    {
        return userManagerPerId.get( user.getUserManagerId() ).addUser( user );
    }

    @Override
    public void addUserUnchecked( User user )
    {
        userManagerPerId.get( user.getUserManagerId() ).addUserUnchecked( user );
    }

    protected UserManager findFirstWritable()
    {
        for ( UserManager userManager : userManagerPerId.values() )
        {
            if ( !userManager.isReadOnly() )
            {
                return userManager;
            }
        }
        return null;
    }

    @Override
    public User createUser( String username, String fullName, String emailAddress )
    {
        UserManager userManager = findFirstWritable();
        if ( userManager == null )
        {
            throw new RuntimeException( "impossible to find a writable userManager" );
        }
        return userManager.createUser( username, fullName, emailAddress );
    }

    @Override
    public UserQuery createUserQuery()
    {
        return super.createUserQuery();    //To change body of overridden methods use File | Settings | File Templates.
    }


    @Override
    public void deleteUser( String username )
        throws UserNotFoundException
    {
        UserManager userManager = findFirstWritable();
        if ( userManager == null )
        {
            throw new RuntimeException( "impossible to find a writable userManager" );
        }
        userManager.deleteUser( username );
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
    public User findUser( String username )
        throws UserNotFoundException
    {
        User user = null;
        UserNotFoundException lastException = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                user = userManager.findUser( username );
                if ( user != null )
                {
                    return user;
                }
            }
            catch ( UserNotFoundException e )
            {
                lastException = e;
            }
        }

        if ( user == null )
        {
            throw lastException;
        }

        return user;
    }


    @Override
    public User getGuestUser()
        throws UserNotFoundException
    {
        User user = null;
        UserNotFoundException lastException = null;
        for ( UserManager userManager : userManagerPerId.values() )
        {
            try
            {
                user = userManager.getGuestUser();
                if ( user != null )
                {
                    return user;
                }
            }
            catch ( UserNotFoundException e )
            {
                lastException = e;
            }
        }

        if ( user == null )
        {
            throw lastException;
        }

        return user;
    }

    @Override
    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.findUsersByEmailKey( emailKey, orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.findUsersByFullNameKey( fullNameKey, orderAscending );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByQuery( UserQuery query )
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.findUsersByQuery( query );
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.findUsersByUsernameKey( usernameKey, orderAscending );
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
        return null;
    }

    @Override
    public List<User> getUsers()
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.getUsers();
            if ( found != null )
            {
                users.addAll( found );
            }
        }
        return users;
    }

    @Override
    public List<User> getUsers( boolean orderAscending )
    {
        List<User> users = new ArrayList<User>();

        for ( UserManager userManager : userManagerPerId.values() )
        {
            List<User> found = userManager.getUsers( orderAscending );
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
        //olamy: must be it depends :-)
        return true;
    }

    @Override
    public User updateUser( User user )
        throws UserNotFoundException
    {
        return userManagerPerId.get( user.getUserManagerId() ).updateUser( user );
    }

    @Override
    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException
    {
        return userManagerPerId.get( user.getUserManagerId() ).updateUser( user, passwordChangeRequired );
    }

    @Override
    public void setUserManagerImpl( UserManager userManagerImpl )
    {
        log.error( "setUserManagerImpl cannot be used in this implementation" );
    }

    @Override
    public void addUserManagerListener( UserManagerListener listener )
    {
        this.listeners.add( listener );
    }

    @Override
    public void removeUserManagerListener( UserManagerListener listener )
    {
        this.listeners.remove( listener );
    }

    @Override
    protected void fireUserManagerInit( boolean freshDatabase )
    {
        for ( UserManagerListener listener : listeners )
        {
            listener.userManagerInit( freshDatabase );
        }
    }

    @Override
    protected void fireUserManagerUserAdded( User addedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            listener.userManagerUserAdded( addedUser );
        }
    }

    @Override
    protected void fireUserManagerUserRemoved( User removedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            listener.userManagerUserRemoved( removedUser );
        }
    }

    @Override
    protected void fireUserManagerUserUpdated( User updatedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            listener.userManagerUserUpdated( updatedUser );
        }
    }

    @Override
    public User createGuestUser()
    {
        return findFirstWritable().createGuestUser();
    }

    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.configurable.archiva";
    }


}
