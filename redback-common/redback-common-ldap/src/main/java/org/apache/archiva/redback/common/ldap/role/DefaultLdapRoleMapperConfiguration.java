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
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service("ldapRoleMapperConfiguration#default")
public class DefaultLdapRoleMapperConfiguration
    implements LdapRoleMapperConfiguration
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "userConfiguration#default")
    private UserConfiguration userConf;


    public void addLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException
    {
        log.warn( "addLdapMapping not implemented" );
    }

    public void removeLdapMapping( String group )
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
}
