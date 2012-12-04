package org.apache.archiva.redback.keys.memory;

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

import org.apache.archiva.redback.keys.AbstractKeyManager;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KeyManager backed by an in-memory only store.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Service("keyManager#memory")
public class MemoryKeyManager
    extends AbstractKeyManager
{
    private Map<String, AuthenticationKey> keys = new HashMap<String, AuthenticationKey>();

    public AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
        throws KeyManagerException
    {
        AuthenticationKey key = new MemoryAuthenticationKey();
        key.setKey( super.generateUUID() );
        key.setForPrincipal( principal );
        key.setPurpose( purpose );
        key.setDateCreated( new Date() );

        if ( expirationMinutes >= 0 )
        {
            Calendar expiration = Calendar.getInstance();
            expiration.add( Calendar.MINUTE, expirationMinutes );
            key.setDateExpires( expiration.getTime() );
        }

        keys.put( key.getKey(), key );

        return key;
    }

    public AuthenticationKey findKey( String key )
        throws KeyNotFoundException, KeyManagerException
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new KeyNotFoundException( "Empty key not found." );
        }

        AuthenticationKey authkey = keys.get( key );

        if ( authkey == null )
        {
            throw new KeyNotFoundException( "Key [" + key + "] not found." );
        }

        assertNotExpired( authkey );

        return authkey;
    }

    public void deleteKey( AuthenticationKey authkey )
        throws KeyManagerException
    {
        keys.remove( authkey );
    }

    public void deleteKey( String key )
        throws KeyManagerException
    {
        AuthenticationKey authkey = keys.get( key );
        if ( authkey != null )
        {
            keys.remove( authkey );
        }
    }

    public List<AuthenticationKey> getAllKeys()
    {
        return new ArrayList<AuthenticationKey>( keys.values() );
    }

    public AuthenticationKey addKey( AuthenticationKey key )
    {
        keys.put( key.getKey(), key );
        return key;
    }

    public void eraseDatabase()
    {
        keys.clear();
    }

    public String getId()
    {
        return "Memory Key Manager";
    }
}
