package org.codehaus.redback.integration.util;

/*
 * Copyright 2005-2006 The Codehaus.
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

import java.util.Comparator;

import org.codehaus.plexus.redback.users.User;

/**
 * UserComparator
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class UserComparator
    implements Comparator<User>
{
    private static final int UNKNOWN = -1;

    private static final int USERNAME = 1;

    private static final int FULLNAME = 2;

    private static final int EMAIL = 3;

    private static final int VALIDATED = 4;

    private static final int LOCKED = 5;

    private static final int PERMANENT = 6;

    private int propKey = UNKNOWN;

    private boolean ascending;

    public UserComparator( String property, boolean ascending )
    {
        this.ascending = ascending;

        if ( "username".equals( property ) )
        {
            propKey = USERNAME;
        }
        else if ( "fullName".equals( property ) )
        {
            propKey = FULLNAME;
        }
        else if ( "email".equals( property ) )
        {
            propKey = EMAIL;
        }
        else if ( "validated".equals( property ) )
        {
            propKey = VALIDATED;
        }
        else if ( "locked".equals( property ) )
        {
            propKey = LOCKED;
        }
        else if ( "permanent".equals( property ) )
        {
            propKey = PERMANENT;
        }
    }

    public int compare( User user1, User user2 )
    {
        if ( ( user1 == null ) && ( user2 == null ) )
        {
            return 0;
        }

        if ( ( user1 == null ) && ( user2 != null ) )
        {
            return -1;
        }

        if ( ( user1 != null ) && ( user2 == null ) )
        {
            return 1;
        }

        return compareUsers( user1, user2 ) * ( ascending ? 1 : -1 );
    }

    private int compareUsers( User u1, User u2 )
    {
        switch ( propKey )
        {
            case USERNAME:
                return compareStrings( u1.getUsername(), u2.getUsername() );
            case FULLNAME:
                return compareStrings( u1.getFullName(), u2.getFullName() );
            case EMAIL:
                return compareStrings( u1.getEmail(), u2.getEmail() );
            case VALIDATED:
                return compareBooleans( u1.isValidated(), u2.isValidated() );
            case LOCKED:
                return compareBooleans( u1.isLocked(), u2.isLocked() );
            case PERMANENT:
                return compareBooleans( u1.isPermanent(), u2.isPermanent() );
            default:
                return 0;

        }
    }

    private int compareStrings( String s1, String s2 )
    {
        if ( ( s1 == null ) && ( s2 == null ) )
        {
            return 0;
        }

        if ( ( s1 == null ) && ( s2 != null ) )
        {
            return -1;
        }

        if ( ( s1 != null ) && ( s2 == null ) )
        {
            return 1;
        }

        return s1.toLowerCase().compareTo( s2.toLowerCase() );
    }

    private int compareBooleans( boolean b1, boolean b2 )
    {
        if ( b1 == b2 )
        {
            return 0;
        }

        return ( b1 ) ? 1 : -1;
    }
}
