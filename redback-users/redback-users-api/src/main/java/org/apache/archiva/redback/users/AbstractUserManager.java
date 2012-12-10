package org.apache.archiva.redback.users;

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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * AbstractUserManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public abstract class AbstractUserManager
    implements UserManager
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private List<UserManagerListener> listeners = new ArrayList<UserManagerListener>();

    public void addUserManagerListener( UserManagerListener listener )
    {
        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }
    }

    public void removeUserManagerListener( UserManagerListener listener )
    {
        listeners.remove( listener );
    }

    protected void fireUserManagerInit( boolean freshDatabase )
    {
        for ( UserManagerListener listener : listeners )
        {
            try
            {
                listener.userManagerInit( freshDatabase );
            }
            catch ( Exception e )
            {
                // Ignore
            }
        }
    }

    protected void fireUserManagerUserAdded( User addedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            try
            {
                listener.userManagerUserAdded( addedUser );
            }
            catch ( Exception e )
            {
                // Ignore
            }
        }
    }

    protected void fireUserManagerUserRemoved( User removedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            try
            {
                listener.userManagerUserRemoved( removedUser );
            }
            catch ( Exception e )
            {
                // Ignore
            }
        }
    }

    protected void fireUserManagerUserUpdated( User updatedUser )
    {
        for ( UserManagerListener listener : listeners )
        {
            try
            {
                listener.userManagerUserUpdated( updatedUser );
            }
            catch ( Exception e )
            {
                // Ignore
            }
        }
    }

    public User getGuestUser()
        throws UserNotFoundException, UserManagerException
    {
        return findUser( GUEST_USERNAME );
    }

    public User createGuestUser()
        throws UserManagerException
    {
        try
        {
            User u = getGuestUser();
            if ( u != null )
            {
                return u;
            }
        }
        catch ( UserNotFoundException e )
        {
            //Nothing to do
        }

        User user = createUser( GUEST_USERNAME, "Guest", "" );
        user.setPermanent( true );
        user.setPasswordChangeRequired( false );

        user = addUser( user );
        return user;
    }

    public void initialize()
    {
        // no op prevent sub classes to need implement this method
        // sub classes can implement their own
    }

    public boolean isFinalImplementation()
    {
        return false;
    }

    protected List<UserManagerListener> getListeners()
    {
        return listeners;
    }
}
