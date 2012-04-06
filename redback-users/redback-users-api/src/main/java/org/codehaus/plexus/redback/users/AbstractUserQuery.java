package org.codehaus.plexus.redback.users;

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

/**
 * Abstract Implementation of UserQuery.
 * Intended to be subclassed by UserManager providers.
 */
public abstract class AbstractUserQuery
    implements UserQuery
{

    private String username;

    private String fullName;

    private String email;

    private long maxResults = -1;

    private long firstResult;

    private String orderBy = ORDER_BY_USERNAME;

    private boolean ascending = true;

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String userName )
    {
        this.username = userName;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public long getFirstResult()
    {
        return firstResult;
    }

    public void setFirstResult( int firstResult )
    {
        this.firstResult = firstResult;
    }

    public long getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults( int maxResults )
    {
        this.maxResults = maxResults;
    }

    public String getOrderBy()
    {
        return orderBy;
    }

    public void setOrderBy( String orderBy )
    {
        if ( orderBy == null )
        {
            throw new IllegalArgumentException( "orderBy cannot be set to null" );
        }
        else if ( !ALLOWED_ORDER_FIELDS.contains( orderBy ) )
        {
            throw new IllegalArgumentException( orderBy + " is not an allowed orderBy field: " + orderBy );
        }
        this.orderBy = orderBy;
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