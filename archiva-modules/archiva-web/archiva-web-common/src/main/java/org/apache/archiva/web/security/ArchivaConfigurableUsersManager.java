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
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("userManager#archiva")
public class ArchivaConfigurableUsersManager
    extends ConfigurableUserManager
{

    @Inject
    private ArchivaRuntimeConfigurationAdmin archivaRuntimeConfigurationAdmin;

    @Inject
    private ApplicationContext applicationContext;

    private List<UserManager> userManagers;

    @Override
    public void initialize()
    {
        try
        {
            List<String> userManagerImpls =
                archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfiguration().getUserManagerImpls();
            log.info( "use userManagerImpls: '{}'", userManagerImpls );

            userManagers = new ArrayList<UserManager>( userManagerImpls.size() );
            for ( String id : userManagerImpls )
            {
                UserManager userManagerImpl = applicationContext.getBean( "userManager#" + id, UserManager.class );
                setUserManagerImpl( userManagerImpl );
                userManagers.add( userManagerImpl );
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
        return super.addUser( user );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void addUserUnchecked( User user )
    {
        super.addUserUnchecked( user );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User createUser( String username, String fullName, String emailAddress )
    {
        return super.createUser( username, fullName,
                                 emailAddress );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public UserQuery createUserQuery()
    {
        return super.createUserQuery();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void deleteUser( Object principal )
        throws UserNotFoundException
    {
        super.deleteUser( principal );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void deleteUser( String username )
        throws UserNotFoundException
    {
        super.deleteUser( username );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void eraseDatabase()
    {
        super.eraseDatabase();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User findUser( String username )
        throws UserNotFoundException
    {
        return super.findUser(
            username );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User findUser( Object principal )
        throws UserNotFoundException
    {
        return super.findUser(
            principal );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User getGuestUser()
        throws UserNotFoundException
    {
        return super.getGuestUser();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
    {
        return super.findUsersByEmailKey( emailKey,
                                          orderAscending );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
    {
        return super.findUsersByFullNameKey( fullNameKey,
                                             orderAscending );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> findUsersByQuery( UserQuery query )
    {
        return super.findUsersByQuery(
            query );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
    {
        return super.findUsersByUsernameKey( usernameKey,
                                             orderAscending );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getId()
    {
        return super.getId();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> getUsers()
    {
        return super.getUsers();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<User> getUsers( boolean orderAscending )
    {
        return super.getUsers(
            orderAscending );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isReadOnly()
    {
        return super.isReadOnly();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User updateUser( User user )
        throws UserNotFoundException
    {
        return super.updateUser( user );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException
    {
        return super.updateUser( user,
                                 passwordChangeRequired );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean userExists( Object principal )
    {
        return super.userExists(
            principal );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setUserManagerImpl( UserManager userManagerImpl )
    {
        super.setUserManagerImpl(
            userManagerImpl );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void addUserManagerListener( UserManagerListener listener )
    {
        super.addUserManagerListener(
            listener );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserManagerListener( UserManagerListener listener )
    {
        super.removeUserManagerListener(
            listener );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void fireUserManagerInit( boolean freshDatabase )
    {
        super.fireUserManagerInit(
            freshDatabase );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void fireUserManagerUserAdded( User addedUser )
    {
        super.fireUserManagerUserAdded(
            addedUser );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void fireUserManagerUserRemoved( User removedUser )
    {
        super.fireUserManagerUserRemoved(
            removedUser );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void fireUserManagerUserUpdated( User updatedUser )
    {
        super.fireUserManagerUserUpdated(
            updatedUser );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public User createGuestUser()
    {
        return super.createGuestUser();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isFinalImplementation()
    {
        return super.isFinalImplementation();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.configurable.archiva";
    }


}
