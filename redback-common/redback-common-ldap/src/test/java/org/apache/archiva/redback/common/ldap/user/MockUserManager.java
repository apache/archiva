package org.apache.archiva.redback.common.ldap.user;
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
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserManagerListener;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "userManager#mock" )
public class MockUserManager
    implements UserManager
{
    public boolean isReadOnly()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getId()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addUserManagerListener( UserManagerListener listener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeUserManagerListener( UserManagerListener listener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public User createUser( String username, String fullName, String emailAddress )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public User createGuestUser()
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UserQuery createUserQuery()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> getUsers()
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> getUsers( boolean orderAscending )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public User addUser( User user )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public User updateUser( User user )
        throws UserNotFoundException, UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public User findUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public User getGuestUser()
        throws UserNotFoundException, UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<User> findUsersByQuery( UserQuery query )
        throws UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean userExists( String principal )
        throws UserManagerException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addUserUnchecked( User user )
        throws UserManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void eraseDatabase()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException, UserManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void initialize()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isFinalImplementation()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescriptionKey()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
