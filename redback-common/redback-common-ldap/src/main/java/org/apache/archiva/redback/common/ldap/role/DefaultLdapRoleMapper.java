package org.apache.archiva.redback.common.ldap.role;
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

import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service( "ldapRoleMapper#default" )
public class DefaultLdapRoleMapper
    implements LdapRoleMapper
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration userConf;

    //---------------------------
    // fields
    //---------------------------

    private String ldapGroupClass = "groupOfUniqueNames";

    private String groupsDn;

    private String baseDn;

    @PostConstruct
    public void initialize()
    {
        this.ldapGroupClass = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_CLASS, this.ldapGroupClass );

        this.baseDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_BASEDN, this.baseDn );

        this.groupsDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_GROUPS_BASEDN, this.baseDn );
    }

    public String getLdapGroup( String role )
    {
        return userConf.getString( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY + role );
    }

    public List<String> getAllGroups()
        throws MappingException
    {
        LdapConnection ldapConnection = null;

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();

            DirContext context = ldapConnection.getDirContext();

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = context.search( getGroupsDn(), filter, searchControls );

            List<String> allGroups = new ArrayList<String>();

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();

                String groupName = searchResult.getName();
                // cn=blabla we only want bla bla
                groupName = StringUtils.substringAfter( groupName, "=" );

                log.debug( "found groupName: '{}", groupName );

                allGroups.add( groupName );

            }

            return allGroups;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

        finally
        {
            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
            if ( namingEnumeration != null )
            {
                try
                {
                    namingEnumeration.close();
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    public List<String> getGroupsMember( String group )
        throws MappingException
    {
        LdapConnection ldapConnection = null;

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();

            DirContext context = ldapConnection.getDirContext();

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = context.search( "cn=" + group + "," + getGroupsDn(), filter, searchControls );

            List<String> allMembers = new ArrayList<String>();

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();

                Attribute uniqueMemberAttr = searchResult.getAttributes().get( "uniquemember" );

                if ( uniqueMemberAttr != null )
                {
                    NamingEnumeration<String> allMembersEnum = (NamingEnumeration<String>) uniqueMemberAttr.getAll();
                    while ( allMembersEnum.hasMore() )
                    {
                        String userName = allMembersEnum.next();
                        // uid=blabla we only want bla bla
                        userName = StringUtils.substringAfter( userName, "=" );
                        userName = StringUtils.substringBefore( userName, "," );
                        log.debug( "found userName for group {}: '{}", group, userName );

                        allMembers.add( userName );
                    }
                    close( allMembersEnum );
                }


            }

            return allMembers;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

        finally
        {
            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
            close( namingEnumeration );
        }
    }

    public List<String> getGroups( String username )
        throws MappingException
    {

        List<String> userGroups = new ArrayList<String>();

        LdapConnection ldapConnection = null;

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();

            DirContext context = ldapConnection.getDirContext();

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter =
                new StringBuilder().append( "(&" ).append( "(objectClass=" + getLdapGroupClass() + ")" ).append(
                    "(uniquemember=" ).append( "uid=" + username + "," + this.getBaseDn() ).append( ")" ).append(
                    ")" ).toString();

            log.debug( "filter: {}", filter );

            namingEnumeration = context.search( getGroupsDn(), filter, searchControls );

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();

                List<String> allMembers = new ArrayList<String>();

                Attribute uniqueMemberAttr = searchResult.getAttributes().get( "uniquemember" );

                if ( uniqueMemberAttr != null )
                {
                    NamingEnumeration<String> allMembersEnum = (NamingEnumeration<String>) uniqueMemberAttr.getAll();
                    while ( allMembersEnum.hasMore() )
                    {
                        String userName = allMembersEnum.next();
                        // uid=blabla we only want bla bla
                        userName = StringUtils.substringAfter( userName, "=" );
                        userName = StringUtils.substringBefore( userName, "," );
                        allMembers.add( userName );
                    }
                    close( allMembersEnum );
                }

                if ( allMembers.contains( username ) )
                {
                    String groupName = searchResult.getName();
                    // cn=blabla we only want bla bla
                    groupName = StringUtils.substringAfter( groupName, "=" );
                    userGroups.add( groupName );

                }


            }

            return userGroups;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

        finally
        {
            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
            close( namingEnumeration );
        }

    }

    private void close( NamingEnumeration namingEnumeration )
    {
        if ( namingEnumeration != null )
        {
            try
            {
                namingEnumeration.close();
            }
            catch ( NamingException e )
            {
                log.warn( "fail to close namingEnumeration: {}", e.getMessage() );
            }
        }
    }

    public String getGroupsDn()
    {
        return this.groupsDn;
    }

    public String getLdapGroupClass()
    {
        return this.ldapGroupClass;
    }

    public void addLdapMapping( String role, String ldapGroup )
    {
        log.warn( "addLdapMapping not implemented" );
    }

    public void removeLdapMapping( String role )
    {
        log.warn( "removeLdapMapping not implemented" );
    }

    public Map<String, String> getLdapGroupMappings()
    {
        Map<String, String> map = new HashMap<String, String>();

        Collection<String> keys = userConf.getKeys();

        for ( String key : keys )
        {
            if ( key.startsWith( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ) )
            {
                map.put( StringUtils.substringAfter( key, UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ),
                         userConf.getString( key ) );
            }
        }

        return map;
    }

    //---------------------------------
    // setters for unit tests
    //---------------------------------


    public void setGroupsDn( String groupsDn )
    {
        this.groupsDn = groupsDn;
    }

    public void setLdapGroupClass( String ldapGroupClass )
    {
        this.ldapGroupClass = ldapGroupClass;
    }

    public void setUserConf( UserConfiguration userConf )
    {
        this.userConf = userConf;
    }

    public void setLdapConnectionFactory( LdapConnectionFactory ldapConnectionFactory )
    {
        this.ldapConnectionFactory = ldapConnectionFactory;
    }

    public String getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }
}
