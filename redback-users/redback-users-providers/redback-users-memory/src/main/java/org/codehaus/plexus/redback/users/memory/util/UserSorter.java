package org.codehaus.plexus.redback.users.memory.util;

import org.codehaus.plexus.redback.users.User;

import java.util.Comparator;

/**
 * UserSorter
 */
public class UserSorter
    implements Comparator<User>
{
    private boolean ascending;

    public UserSorter()
    {
        this.ascending = true;
    }

    public UserSorter( boolean ascending )
    {
        this.ascending = ascending;
    }

    public int compare( User o1, User o2 )
    {
        if ( ( o1 == null ) && ( o2 == null ) )
        {
            return 0;
        }

        if ( ( o1 == null ) && ( o2 != null ) )
        {
            return -1;
        }

        if ( ( o1 != null ) && ( o2 != null ) )
        {
            return 1;
        }

        User u1 = null;
        User u2 = null;

        if ( isAscending() )
        {
            u1 = o1;
            u2 = o2;
        }
        else
        {
            u1 = o2;
            u2 = o1;
        }

        return u1.getUsername().compareTo( u2.getUsername() );
    }

    public boolean isAscending()
    {
        return ascending;
    }

    public void setAscending( boolean ascending )
    {
        this.ascending = ascending;
    }

}
