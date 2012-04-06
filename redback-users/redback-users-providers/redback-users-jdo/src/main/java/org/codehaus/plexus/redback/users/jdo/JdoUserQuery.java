package org.codehaus.plexus.redback.users.jdo;

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
import org.codehaus.plexus.redback.users.UserQuery;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class JdoUserQuery
    extends AbstractUserQuery
{

    /**
     * Create the ordering string for use in {@link javax.jdo.Query#setOrdering(String)}
     *
     * @return the created filter
     */
    public String getOrdering()
    {
        StringBuffer ordering = new StringBuffer();

        if ( UserQuery.ORDER_BY_EMAIL.equals( getOrderBy() ) )
        {
            ordering.append( "email" );
        }
        else if ( UserQuery.ORDER_BY_FULLNAME.equals( getOrderBy() ) )
        {
            ordering.append( "fullName" );
        }
        else
        {
            ordering.append( "username" );
        }
        ordering.append( " " ).append( isAscending() ? "ascending" : "descending" );
        return ordering.toString();
    }

    /**
     * Create and return the filter string for use in {@link javax.jdo.Query#setFilter(String)}
     *
     * @return the query filter
     */
    public String getFilter()
    {
        Set<String> terms = new HashSet<String>();

        if ( getUsername() != null )
        {
            terms.add( "this.username.toLowerCase().indexOf(usernameKey.toLowerCase()) > -1" );
        }
        if ( getFullName() != null )
        {
            terms.add( "this.fullName.toLowerCase().indexOf(fullNameKey.toLowerCase()) > -1" );
        }
        if ( getEmail() != null )
        {
            terms.add( "this.email.toLowerCase().indexOf(emailKey.toLowerCase()) > -1" );
        }

        return StringUtils.join( terms.iterator(), " && " );
    }

    /**
     * Return an array of parameters for user in {@link javax.jdo.Query#executeWithArray(Object[])}
     *
     * @return the parameter array
     */
    public String[] getSearchKeys()
    {
        List<String> keys = new ArrayList<String>();

        if ( getUsername() != null )
        {
            keys.add( getUsername() );
        }
        if ( getFullName() != null )
        {
            keys.add( getFullName() );
        }
        if ( getEmail() != null )
        {
            keys.add( getEmail() );
        }

        return (String[]) keys.toArray( new String[0] );
    }

    /**
     * Returns the parameters for use in {@link javax.jdo.Query#declareParameters(String)}
     *
     * @return the parameter list
     */
    public String getParameters()
    {

        List<String> params = new ArrayList<String>();

        if ( getUsername() != null )
        {
            params.add( "String usernameKey" );
        }
        if ( getFullName() != null )
        {
            params.add( "String fullNameKey" );
        }
        if ( getEmail() != null )
        {
            params.add( "String emailKey" );
        }

        return StringUtils.join( params.iterator(), ", " );
    }
}
