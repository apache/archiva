package org.codehaus.plexus.redback.users.ldap.service;

/*
 * Copyright 2011 The Codehaus.
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

import org.codehaus.plexus.cache.builder.CacheBuilder;
import org.codehaus.plexus.redback.common.ldap.LdapUser;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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
    private CacheBuilder cacheBuilder;

    // LDAP Users

    /**
     * @see LdapCacheService#getUser(String)
     */
    public LdapUser getUser( String username )
    {
        return (LdapUser) cacheBuilder.getCache( "ldapUser" ).get( username );
    }

    /**
     * @see LdapCacheService#removeUser(String)
     */
    public boolean removeUser( String username )
    {
        return ( cacheBuilder.getCache( "ldapUser" ).remove( username ) == null ? false : true );
    }

    /**
     * @see LdapCacheService#removeAllUsers()
     */
    public void removeAllUsers()
    {
        cacheBuilder.getCache( "ldapUser" ).clear();
    }

    /**
     * @see LdapCacheService#addUser(org.codehaus.plexus.redback.common.ldap.LdapUser)
     */
    public void addUser( LdapUser user )
    {
        LdapUser existingUser = (LdapUser) cacheBuilder.getCache( "ldapUser" ).get( user.getUsername() );
        if( existingUser != null )
        {
            removeUser( user.getUsername() );
        }

        cacheBuilder.getCache( "ldapUser" ).put( user.getUsername(), user );
    }

    // LDAP UserDn

    /**
     * @see LdapCacheService#getLdapUserDn(String)
     */
    public String getLdapUserDn( String username )
    {
        return (String) cacheBuilder.getCache( "ldapUserDn" ).get( username );
    }

    /**
     * @see LdapCacheService#removeLdapUserDn(String)
     */
    public boolean removeLdapUserDn( String username )
    {
        return ( cacheBuilder.getCache( "ldapUserDn" ).remove( username ) == null ? false : true );
    }

    /**
     * @see org.codehaus.plexus.redback.users.ldap.service.LdapCacheService#removeAllLdapUserDn()
     */
    public void removeAllLdapUserDn()
    {
        cacheBuilder.getCache( "ldapUserDn" ).clear();
    }

    /**
     * @see LdapCacheService#addLdapUserDn(String, String) 
     */
    public void addLdapUserDn( String username, String userDn )
    {
        String existingUserDn = (String) cacheBuilder.getCache( "ldapUserDn" ).get( username );
        if( existingUserDn != null )
        {
            removeUser( username );
        }

        cacheBuilder.getCache( "ldapUserDn" ).put( username, userDn );    
    }
    
}
