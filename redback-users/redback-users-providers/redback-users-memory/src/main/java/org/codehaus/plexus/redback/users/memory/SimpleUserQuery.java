package org.codehaus.plexus.redback.users.memory;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.redback.users.AbstractUserQuery;
import org.codehaus.plexus.redback.users.User;

import java.util.Comparator;

public class SimpleUserQuery
    extends AbstractUserQuery
{

    /**
     * Returns true if this user should be considered a match of the current query
     *
     * @param user
     * @return
     */
    public boolean matches( User user )
    {
        if ( getUsername() != null && user.getUsername() != null &&
            user.getUsername().toLowerCase().indexOf( getUsername().toLowerCase() ) == -1 )
        {
            return false;
        }
        else if ( getFullName() != null && user.getFullName() != null &&
            user.getFullName().toLowerCase().indexOf( getFullName().toLowerCase() ) == -1 )
        {
            return false;
        }
        else if ( getEmail() != null && user.getEmail() != null &&
            user.getEmail().toLowerCase().indexOf( getEmail().toLowerCase() ) == -1 )
        {
            return false;
        }
        else
        {
            return true;
        }

    }

    /**
     * Returns a comparator used for sorting a collection of User objects based on the ordering set
     * on this UserQuery's {@link #setOrderBy(String)} and {@link #setAscending(boolean)}. 
     * @return
     */
    public Comparator<User> getComparator()
    {
        return new Comparator<User>()
        {
            public int compare( User user1, User user2 )
            {
                return ( isAscending() ? 1 : -1 ) * compareUsers( user1, user2 );
            }
        };
    }

    private int compareUsers( User user, User user1 )
    {
        if ( ORDER_BY_EMAIL.equals( getOrderBy() ) )
        {
            return user.getEmail() == null ? -1
                : user1.getEmail() == null ? 1 : user.getEmail().compareTo( user1.getEmail() );
        }
        else if ( ORDER_BY_FULLNAME.equals( getOrderBy() ) )
        {
            return user.getFullName() == null ? -1
                : user1.getFullName() == null ? 1 : user.getFullName().compareTo( user1.getFullName() );
        }
        else
        {
            return user.getUsername().compareTo( user1.getUsername() );
        }
    }
}
