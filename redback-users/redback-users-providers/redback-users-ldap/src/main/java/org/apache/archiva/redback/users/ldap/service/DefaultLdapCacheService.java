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
import org.apache.archiva.redback.components.cache.Cache;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * DefaultLdapCacheService
 *
 * @author: Maria Odea Ching <oching@apache.org>
 * @version
 */
@Service
public class DefaultLdapCacheService
    implements LdapCacheService
{
    @Inject
    @Named(value = "cache#ldapUser")
    private Cache<String, LdapUser> usersCache;

    @Inject
    @Named(value = "cache#ldapUserDn")
    private Cache<String, String> ldapCacheDn;



    // LDAP Users

    /**
     * @see LdapCacheService#getUser(String)
     */
    public LdapUser getUser( String username )
    {
        return usersCache.get( username );
    }

    /**
     * @see LdapCacheService#removeUser(String)
     */
    public boolean removeUser( String username )
    {
        return ( usersCache.remove( username ) == null ? false : true );
    }

    /**
     * @see LdapCacheService#removeAllUsers()
     */
    public void removeAllUsers()
    {
        usersCache.clear();
    }

    /**
     * @see LdapCacheService#addUser(org.apache.archiva.redback.common.ldap.user.LdapUser)
     */
    public void addUser( LdapUser user )
    {
        LdapUser existingUser = usersCache.get( user.getUsername() );
        if( existingUser != null )
        {
            removeUser( user.getUsername() );
        }

        usersCache.put( user.getUsername(), user );
    }

    // LDAP UserDn

    /**
     * @see LdapCacheService#getLdapUserDn(String)
     */
    public String getLdapUserDn( String username )
    {
        return ldapCacheDn.get( username );
    }

    /**
     * @see LdapCacheService#removeLdapUserDn(String)
     */
    public boolean removeLdapUserDn( String username )
    {
        return ( ldapCacheDn.remove( username ) == null ? false : true );
    }

    /**
     * @see org.apache.archiva.redback.users.ldap.service.LdapCacheService#removeAllLdapUserDn()
     */
    public void removeAllLdapUserDn()
    {
        ldapCacheDn.clear();
    }

    /**
     * @see LdapCacheService#addLdapUserDn(String, String) 
     */
    public void addLdapUserDn( String username, String userDn )
    {
        String existingUserDn = ldapCacheDn.get( username );
        if( existingUserDn != null )
        {
            removeUser( username );
        }

        ldapCacheDn.put( username, userDn );
    }
    
}
