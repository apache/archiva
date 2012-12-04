package org.apache.archiva.redback.authentication.ldap;

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

import org.apache.archiva.redback.authentication.AuthenticationConstants;
import org.apache.archiva.redback.common.ldap.UserMapper;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.users.ldap.service.LdapCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.HashMap;
import java.util.Map;

/**
 * LdapBindAuthenticator:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service( "authenticator#ldap" )
public class LdapBindAuthenticator
    implements Authenticator
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "userMapper#ldap" )
    private UserMapper mapper;

    @Inject
    @Named( value = "ldapConnectionFactory#configurable" )
    private LdapConnectionFactory connectionFactory;

    @Inject
    @Named( value = "userConfiguration" )
    private UserConfiguration config;

    @Inject
    private LdapCacheService ldapCacheService;

    public String getId()
    {
        return "LdapBindAuthenticator";
    }

    public AuthenticationResult authenticate( AuthenticationDataSource s )
        throws AuthenticationException
    {
        PasswordBasedAuthenticationDataSource source = (PasswordBasedAuthenticationDataSource) s;

        if ( !config.getBoolean( "ldap.bind.authenticator.enabled" ) || (
            !config.getBoolean( "ldap.bind.authenticator.allowEmptyPasswords", false ) && StringUtils.isEmpty(
                source.getPassword() ) ) )
        {
            return new AuthenticationResult( false, source.getPrincipal(), null );
        }

        SearchControls ctls = new SearchControls();

        ctls.setCountLimit( 1 );

        ctls.setDerefLinkFlag( true );
        ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String filter = "(&(objectClass=" + mapper.getUserObjectClass() + ")" + ( mapper.getUserFilter() != null
            ? mapper.getUserFilter()
            : "" ) + "(" + mapper.getUserIdAttribute() + "=" + source.getPrincipal() + "))";

        log.debug( "Searching for users with filter: '{}' from base dn: {}", filter, mapper.getUserBaseDn() );

        LdapConnection ldapConnection = null;
        LdapConnection authLdapConnection = null;
        NamingEnumeration<SearchResult> results = null;
        try
        {
            ldapConnection = getLdapConnection();
            // check the cache for user's userDn in the ldap server
            String userDn = ldapCacheService.getLdapUserDn( source.getPrincipal() );

            if ( userDn == null )
            {
                log.debug( "userDn for user {} not found in cache. Retrieving from ldap server..",
                           source.getPrincipal() );

                DirContext context = ldapConnection.getDirContext();

                results = context.search( mapper.getUserBaseDn(), filter, ctls );

                log.debug( "Found user '{}': {}", source.getPrincipal(), results.hasMoreElements() );

                if ( results.hasMoreElements() )
                {
                    SearchResult result = results.nextElement();

                    userDn = result.getNameInNamespace();

                    log.debug( "Adding userDn {} for user {} to the cache..", userDn, source.getPrincipal() );

                    // REDBACK-289/MRM-1488 cache the ldap user's userDn to lessen calls to ldap server
                    ldapCacheService.addLdapUserDn( source.getPrincipal(), userDn );
                }
                else
                {
                    return new AuthenticationResult( false, source.getPrincipal(), null );
                }
            }

            log.debug( "Attempting Authenication: {}", userDn );

            authLdapConnection = connectionFactory.getConnection( userDn, source.getPassword() );

            log.info( "user '{}' authenticated", source.getPrincipal() );

            return new AuthenticationResult( true, source.getPrincipal(), null );
        }
        catch ( LdapException e )
        {
            return new AuthenticationResult( false, source.getPrincipal(), e );
        }
        catch ( NamingException e )
        {
            return new AuthenticationResult( false, source.getPrincipal(), e );
        }
        finally
        {
            closeNamingEnumeration( results );
            closeLdapConnection( ldapConnection );
            if ( authLdapConnection != null )
            {
                closeLdapConnection( authLdapConnection );
            }
        }
    }

    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof PasswordBasedAuthenticationDataSource );
    }

    private LdapConnection getLdapConnection()
        throws LdapException
    {
        return connectionFactory.getConnection();
    }

    private void closeLdapConnection( LdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            ldapConnection.close();
        }
    }

    private void closeNamingEnumeration( NamingEnumeration<SearchResult> results )
    {
        try
        {
            if ( results != null )
            {
                results.close();
            }
        }
        catch ( NamingException e )
        {
            log.warn( "skip exception closing naming search result " + e.getMessage() );
        }
    }
}
