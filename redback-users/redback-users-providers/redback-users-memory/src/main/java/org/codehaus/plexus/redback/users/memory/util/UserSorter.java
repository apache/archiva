package org.codehaus.plexus.redback.users.memory.util;

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
