package org.apache.archiva.redback.users.ldap.service;

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

import org.apache.archiva.redback.common.ldap.user.LdapUser;

/**
 * LdapCacheService
 *
 * Service that manages the LDAP caches: LDAP connections and LDAP users
 *
 * @author: Maria Odea Ching <oching@apache.org>
 * @version
 */
public interface LdapCacheService
{
    /**
     * Retrieve LDAP user with the given username from the cache.
     * Returns null if user is not found.
     *
     * @param username
     * @return
     */
    LdapUser getUser( String username );

    /**
     * Remove LDAP user with the given username from the cache.
     * Returns the removed object if it was in the cache. Otherwise, returns null.
     * 
     * @param username
     * @return
     */
    boolean removeUser( String username );

    /**
     * Remove all LDAP users in the cache. In short, it flushes the cache.
     *
     */
    void removeAllUsers();

    /**
     * Adds the user to the LDAP users cache.
     *
     * @param user
     */
    void addUser( LdapUser user );

    /**
     * Retrieve the cached LDAP userDn for the given user.
     *
     * @param username
     * @return
     */
    String getLdapUserDn( String username );

    /**
     * Remove the cached LDAP userDn for the given user.
     *
     * @param username
     * @return
     */
    boolean removeLdapUserDn( String username );

    /**
     * Remove all cached LDAP userDn
     */
    void removeAllLdapUserDn();

    /**
     * All the LDAP userDn for the given user to the cache
     *
     * @param username
     * @param userDn
     */
    void addLdapUserDn( String username, String userDn );
}
