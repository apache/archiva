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

import java.util.Collection;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
public interface LdapRoleMapperConfiguration
{
    /**
     * add mapping redback role <-> ldap group
     *
     * @param role      redback role
     * @param ldapGroup ldap group
     */
    void addLdapMapping( String role, String ldapGroup )
        throws MappingException;

    /**
     * remove a mapping
     *
     * @param role redback role
     */
    void removeLdapMapping( String role )
        throws MappingException;

    /**
     * @return Map of corresponding LDAP group (key) and Redback roles (value)
     */
    Map<String, Collection<String>> getLdapGroupMappings()
        throws MappingException;

    void setLdapGroupMappings( Map<String, Collection<String>> mappings )
        throws MappingException;
}
