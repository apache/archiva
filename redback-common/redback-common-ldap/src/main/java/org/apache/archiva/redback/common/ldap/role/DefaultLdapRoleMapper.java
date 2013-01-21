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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.archiva.redback.common.ldap.MappingException;
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
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private boolean useDefaultRoleName = false;

    /**
     * possible to user cn=beer or uid=beer or sn=beer etc
     * so make it configurable
     */
    private String userIdAttribute = "uid";

    @PostConstruct
    public void initialize()
    {
        this.ldapGroupClass = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_CLASS, this.ldapGroupClass );

        this.baseDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_BASEDN, this.baseDn );

        this.groupsDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_GROUPS_BASEDN, this.groupsDn );

        if ( StringUtils.isEmpty( this.groupsDn ) )
        {
            this.groupsDn = this.baseDn;
        }

        this.useDefaultRoleName =
            userConf.getBoolean( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME, this.useDefaultRoleName );

        this.userIdAttribute = userConf.getString( UserConfigurationKeys.LDAP_USER_ID_ATTRIBUTE, this.userIdAttribute );
    }

    public String getLdapGroup( String role )
    {
        return userConf.getString( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY + role );
    }

    public List<String> getAllGroups( DirContext context )
        throws MappingException
    {

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

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
            close( namingEnumeration );
        }
    }

    protected void closeNamingEnumeration( NamingEnumeration namingEnumeration )
    {
        if ( namingEnumeration != null )
        {
            try
            {
                namingEnumeration.close();
            }
            catch ( NamingException e )
            {
                log.warn( "failed to close NamingEnumeration", e );
            }
        }
    }

    public boolean hasRole( DirContext context, String roleName )
        throws MappingException
    {
        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            if ( this.useDefaultRoleName )
            {
                groupName = roleName;
            }
            else
            {
                log.warn( "skip group creation as no mapping for roleName:'{}'", roleName );
                return false;
            }
        }
        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = context.search( "cn=" + groupName + "," + getGroupsDn(), filter, searchControls );

            return namingEnumeration.hasMore();
        }
        catch ( NameNotFoundException e )
        {
            log.debug( "group {} for role {} not found", groupName, roleName );
            return false;
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
            close( namingEnumeration );
        }
    }

    public List<String> getAllRoles( DirContext context )
        throws MappingException
    {
        List<String> groups = getAllGroups( context );

        if ( groups.isEmpty() )
        {
            return Collections.emptyList();
        }

        Set<String> roles = new HashSet<String>( groups.size() );

        Map<String, Collection<String>> mapping = getLdapGroupMappings();

        for ( String group : groups )
        {
            Collection<String> rolesPerGroup = mapping.get( group );
            if ( rolesPerGroup != null )
            {
                for ( String role : rolesPerGroup )
                {
                    roles.add( role );
                }
            }
        }

        return new ArrayList<String>( roles );
    }

    public List<String> getGroupsMember( String group, DirContext context )
        throws MappingException
    {

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

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
            close( namingEnumeration );
        }
    }

    public List<String> getGroups( String username, DirContext context )
        throws MappingException
    {

        List<String> userGroups = new ArrayList<String>();

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter =
                new StringBuilder().append( "(&" ).append( "(objectClass=" + getLdapGroupClass() + ")" ).append(
                    "(uniquemember=" ).append( this.userIdAttribute + "=" + username + "," + this.getBaseDn() ).append(
                    ")" ).append( ")" ).toString();

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
            close( namingEnumeration );
        }
    }

    public List<String> getRoles( String username, DirContext context )
        throws MappingException
    {
        List<String> groups = getGroups( username, context );

        Map<String, Collection<String>> rolesMapping = getLdapGroupMappings();

        Set<String> roles = new HashSet<String>( groups.size() );

        for ( String group : groups )
        {
            Collection<String> rolesPerGroup = rolesMapping.get( group );
            if ( rolesPerGroup != null )
            {
                for ( String role : rolesPerGroup )
                {
                    roles.add( role );
                }
            }
        }

        return new ArrayList<String>( roles );
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

    public void setLdapGroupMappings( Map<String, Collection<String>> mappings )
        throws MappingException
    {
        log.warn( "setLdapGroupMappings not implemented" );
    }

    public Map<String, Collection<String>> getLdapGroupMappings()
    {
        Multimap<String, String> map = ArrayListMultimap.create();

        Collection<String> keys = userConf.getKeys();

        for ( String key : keys )
        {
            if ( key.startsWith( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ) )
            {
                String val = userConf.getString( key );
                String[] roles = StringUtils.split( val, ',' );
                for ( String role : roles )
                {
                    map.put( StringUtils.substringAfter( key, UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ),
                             role );
                }
            }
        }

        return map.asMap();
    }

    public boolean saveRole( String roleName, DirContext context )
        throws MappingException
    {

        if ( hasRole( context, roleName ) )
        {
            return true;
        }

        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            if ( this.useDefaultRoleName )
            {
                groupName = roleName;
            }
            else
            {
                log.warn( "skip group creation as no mapping fro roleName:'{}'", roleName );
                return false;
            }
        }

        List<String> allGroups = getAllGroups( context );
        if ( allGroups.contains( groupName ) )
        {
            log.info( "group {} already exists for role.", groupName, roleName );
            return false;
        }

        Attributes attributes = new BasicAttributes( true );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        attributes.put( objectClass );
        attributes.put( "cn", groupName );

        // attribute mandatory when created a group so add admin as default member
        // TODO make this default configurable
        BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );
        basicAttribute.add( this.userIdAttribute + "=admin," + getBaseDn() );
        attributes.put( basicAttribute );

        try
        {
            String dn = "cn=" + groupName + "," + this.groupsDn;

            context.createSubcontext( dn, attributes );

            log.info( "created group with dn:'{}", dn );

            return true;
        }
        catch ( NameAlreadyBoundException e )
        {
            log.info( "skip group '{}' creation as already exists", groupName );
            return true;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
    }

    public boolean saveUserRole( String roleName, String username, DirContext context )
        throws MappingException
    {

        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            log.warn( "no group found for role '{}", roleName );
            groupName = roleName;
        }

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = context.search( "cn=" + groupName + "," + getGroupsDn(), filter, searchControls );

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();
                Attribute attribute = searchResult.getAttributes().get( "uniquemember" );
                if ( attribute == null )
                {
                    BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );
                    basicAttribute.add( this.userIdAttribute + "=" + username + "," + getGroupsDn() );
                    context.modifyAttributes( "cn=" + groupName + "," + getGroupsDn(), new ModificationItem[]{
                        new ModificationItem( DirContext.ADD_ATTRIBUTE, basicAttribute ) } );
                }
                else
                {
                    attribute.add( this.userIdAttribute + "=" + username + "," + getGroupsDn() );
                    context.modifyAttributes( "cn=" + groupName + "," + getGroupsDn(), new ModificationItem[]{
                        new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attribute ) } );
                }
                return true;
            }

            return false;
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

    public boolean removeUserRole( String roleName, String username, DirContext context )
        throws MappingException
    {
        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            log.warn( "no group found for role '{}", roleName );
            return false;
        }

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls();

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass();

            namingEnumeration = context.search( "cn=" + groupName + "," + getGroupsDn(), filter, searchControls );

            while ( namingEnumeration.hasMore() )
            {
                SearchResult searchResult = namingEnumeration.next();
                Attribute attribute = searchResult.getAttributes().get( "uniquemember" );
                if ( attribute != null )
                {
                    BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );
                    basicAttribute.add( this.userIdAttribute + "=" + username + "," + getGroupsDn() );
                    context.modifyAttributes( "cn=" + groupName + "," + getGroupsDn(), new ModificationItem[]{
                        new ModificationItem( DirContext.REMOVE_ATTRIBUTE, basicAttribute ) } );
                }
                return true;
            }

            return false;
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

    public void removeAllRoles( DirContext context )
        throws MappingException
    {
        //all mapped roles
        Collection<String> groups = getLdapGroupMappings().keySet();

        try
        {
            for ( String groupName : groups )
            {

                String dn = "cn=" + groupName + "," + this.groupsDn;

                context.unbind( dn );

                log.debug( "deleted group with dn:'{}", dn );
            }

        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
    }

    public void removeRole( String roleName, DirContext context )
        throws MappingException
    {

        String groupName = findGroupName( roleName );

        try
        {

            String dn = "cn=" + groupName + "," + this.groupsDn;

            context.unbind( dn );

            log.info( "deleted group with dn:'{}", dn );

        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage(), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
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

    //-------------------
    // utils methods
    //-------------------

    protected String findGroupName( String role )
    {
        Map<String, Collection<String>> mapping = getLdapGroupMappings();

        for ( Map.Entry<String, Collection<String>> entry : mapping.entrySet() )
        {
            if ( entry.getValue().contains( role ) )
            {
                return entry.getKey();
            }
        }
        return null;
    }



    public String getUserIdAttribute()
    {
        return userIdAttribute;
    }

    public void setUserIdAttribute( String userIdAttribute )
    {
        this.userIdAttribute = userIdAttribute;
    }
}
