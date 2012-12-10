package org.apache.archiva.redback.users.memory;

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
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.AbstractUserManager;
import org.apache.archiva.redback.users.PermanentUserException;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.memory.util.UserSorter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

/**
 *
 */
@Service("userManager#memory")
public class MemoryUserManager
    extends AbstractUserManager
    implements UserManager
{
    @Resource
    private UserSecurityPolicy userSecurityPolicy;

    public String getId()
    {
        return "memory";
    }
    
    public boolean isReadOnly()
    {
        return false;
    }

    public UserQuery createUserQuery()
    {
        return new SimpleUserQuery();
    }

    public List<User> findUsersByQuery( UserQuery query )
    {
        SimpleUserQuery uq = (SimpleUserQuery) query;

        List<User> list = new ArrayList<User>();

        for ( Iterator<User> i = users.values().iterator(); i.hasNext(); )
        {
            SimpleUser user = (SimpleUser) i.next();
            boolean matches = uq.matches( user );
            if ( matches )
            {
                list.add( user );
            }
        }

        Collections.sort( list, uq.getComparator() );

        List<User> cutList = new ArrayList<User>();

        for ( long i = query.getFirstResult();
              i < list.size() && ( query.getMaxResults() == -1 || i < query.getFirstResult() + uq.getMaxResults() );
              i++ )
        {
            cutList.add( list.get( (int) i ) );
        }
        return cutList;
    }

    private Map<Object, User> users = new HashMap<Object, User>();

    public User addUser( User user )
    {
        saveUser( user );
        fireUserManagerUserAdded( user );

        // If there exists no encoded password, then this is a new user setup 
        if ( StringUtils.isEmpty( user.getEncodedPassword() ) )
        {
            userSecurityPolicy.extensionChangePassword( user );
        }

        return user;
    }

    private void saveUser( User user )
    {
        triggerInit();
        users.put( user.getUsername(), user );
    }

    public User updateUser( User user )
    {
        return updateUser( user, false );
    }

    public User updateUser( User user, boolean passwordChangeRequired )
    {
        if ( StringUtils.isNotEmpty( user.getPassword() ) )
        {
            userSecurityPolicy.extensionChangePassword( user, passwordChangeRequired );
        }

        saveUser( user );

        fireUserManagerUserUpdated( user );

        return user;
    }

    public boolean userExists( String principal )
    {
        try
        {
            findUser( principal );
            return true;
        }
        catch ( UserNotFoundException ne )
        {
            return false;
        }
    }


    public User createUser( String username, String fullName, String emailAddress )
    {
        User user = new SimpleUser();
        user.setUsername( username );
        user.setFullName( fullName );
        user.setEmail( emailAddress );

        return user;
    }

    public void deleteUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        User user = findUser( username );

        if ( user.isPermanent() )
        {
            throw new PermanentUserException( "Cannot delete permanent user." );
        }

        users.remove( user.getUsername() );

        fireUserManagerUserRemoved( user );
    }

    public void addUserUnchecked( User user )
    {
        addUser( user );
    }

    public void eraseDatabase()
    {
        users.clear();
    }

    public User findUser( String username )
        throws UserNotFoundException
    {
        triggerInit();
        User user = null;

        Iterator<User> it = users.values().iterator();
        while ( it.hasNext() )
        {
            User u = it.next();
            if ( u.getUsername().equals( username ) )
            {
                user = u;
            }
        }

        if ( user == null )
        {
            throw new UserNotFoundException( "Unable to find user '" + username + "'" );
        }

        return user;
    }

    public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
    {
        triggerInit();

        List<User> userList = new ArrayList<User>();

        Iterator<User> it = users.values().iterator();
        while ( it.hasNext() )
        {
            User u = it.next();
            if ( u.getUsername().indexOf( usernameKey ) > -1 )
            {
                userList.add( u );
            }
        }

        Collections.sort( userList, new UserSorter( orderAscending ) );

        return userList;
    }

    public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
    {
        triggerInit();

        List<User> userList = new ArrayList<User>();

        Iterator<User> it = users.values().iterator();
        while ( it.hasNext() )
        {
            User u = it.next();
            if ( u.getFullName().indexOf( fullNameKey ) > -1 )
            {
                userList.add( u );
            }
        }

        Collections.sort( userList, new UserSorter( orderAscending ) );

        return userList;
    }

    public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
    {
        triggerInit();

        List<User> userList = new ArrayList<User>();

        Iterator<User> it = users.values().iterator();
        while ( it.hasNext() )
        {
            User u = it.next();
            if ( u.getEmail().indexOf( emailKey ) > -1 )
            {
                userList.add( u );
            }
        }

        Collections.sort( userList, new UserSorter( orderAscending ) );

        return userList;
    }

    public List<User> getUsers()
    {
        triggerInit();
        return new ArrayList<User>( users.values() );
    }

    public List<User> getUsers( boolean ascendingUsername )
    {
        return getUsers();
    }

    private boolean hasTriggeredInit = false;

    public void triggerInit()
    {
        if ( !hasTriggeredInit )
        {
            fireUserManagerInit( users.isEmpty() );
            hasTriggeredInit = true;
        }
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.memory";
    }
}
