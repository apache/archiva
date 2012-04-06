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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;


public interface UserQuery
{
    final static String ORDER_BY_USERNAME = "username";

    final static String ORDER_BY_FULLNAME = "fullname";

    final static String ORDER_BY_EMAIL = "email";

    final static Set<String> ALLOWED_ORDER_FIELDS =
        new HashSet<String>( Arrays.asList( ORDER_BY_USERNAME, ORDER_BY_FULLNAME, ORDER_BY_EMAIL ) );

    /**
     * Returns the case insensitive substring user name criteria.
     *
     * @return the username criteria.
     */
    String getUsername();

    /**
     * Sets the case insensitive substring user name criteria.
     *
     * @param userName the username criteria
     */
    void setUsername( String userName );

    /**
     * Returns the case insensitive substring full name criteria.
     *
     * @return the username criteria.
     */
    String getFullName();

    /**
     * Sets the case insensitive substring full name criteria.
     *
     * @param fullName the full name criteria
     */
    void setFullName( String fullName );

    /**
     * Returns the case insensitive substring email criteria.
     *
     * @return the email criteria.
     */
    String getEmail();

    /**
     * Sets the case insensitive substring email criteria.
     *
     * @param email the email criteria
     */
    void setEmail( String email );

    /**
     * Returns the index (zero based) of the first result to include. Useful for paging.
     *
     * @return the first index
     */
    long getFirstResult();

    /**
     * Sets the index (zero based) of the first result to include. Useful for paging.
     *
     * @param firstResult the first index
     */
    void setFirstResult( int firstResult );

    /**
     * Returns the maximum number of users to return.
     *
     * @return the maximum number of users to return.
     */
    long getMaxResults();

    /**
     * Sets the maximum number of users to return.
     *
     * @param maxResults the maximum number of users to return.
     */
    void setMaxResults( int maxResults );

    /**
     * Returns the property used to order the results of this query.
     * This is one of {@link #ORDER_BY_USERNAME}, {@link #ORDER_BY_FULLNAME} or {@link #ORDER_BY_EMAIL}.
     *
     * @return the order property.
     */
    String getOrderBy();

    /**
     * Sets the property used to order the results of this query.
     * This is one of {@link #ORDER_BY_USERNAME}, {@link #ORDER_BY_FULLNAME} or {@link #ORDER_BY_EMAIL}.
     *
     * @param orderBy the order property.
     */
    void setOrderBy( String orderBy );

    /**
     * Returns true if the results should be returned in ascending order.
     *
     * @return ascending
     */
    boolean isAscending();

    /**
     * Set this to true if the results should be returned in ascending order.
     *
     * @param ascending true if the results should be returned in ascending
     */
    void setAscending( boolean ascending );
}
