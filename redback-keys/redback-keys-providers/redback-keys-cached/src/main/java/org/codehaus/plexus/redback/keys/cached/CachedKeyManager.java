package org.codehaus.plexus.redback.keys.cached;

/*
 * Copyright 2001-2006 The Codehaus.
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

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.cache.Cache;
import org.codehaus.plexus.redback.keys.AbstractKeyManager;
import org.codehaus.plexus.redback.keys.AuthenticationKey;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.keys.KeyManagerException;
import org.codehaus.plexus.redback.keys.KeyNotFoundException;
import org.springframework.stereotype.Service;

/**
 * CachedKeyManager 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("keyManager#cached")
public class CachedKeyManager
    extends AbstractKeyManager
    implements KeyManager
{
    @Inject @Named(value="keyManager#jdo")
    private KeyManager keyImpl;

    @Inject @Named(value="cache#keys")
    private Cache keysCache;

    public AuthenticationKey addKey( AuthenticationKey key )
    {
        if ( key != null )
        {
            keysCache.remove( key.getKey() );
        }
        return this.keyImpl.addKey( key );
    }

    public AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
        throws KeyManagerException
    {
        AuthenticationKey authkey = this.keyImpl.createKey( principal, purpose, expirationMinutes );
        keysCache.remove( authkey.getKey() );
        return authkey;
    }

    public void deleteKey( AuthenticationKey key )
        throws KeyManagerException
    {
        keysCache.remove( key.getKey() );
        this.keyImpl.deleteKey( key );
    }

    public void deleteKey( String key )
        throws KeyManagerException
    {
        keysCache.remove( key );
        this.keyImpl.deleteKey( key );
    }

    public void eraseDatabase()
    {
        try
        {
            this.keyImpl.eraseDatabase();
        }
        finally
        {
            this.keysCache.clear();
        }
    }

    public AuthenticationKey findKey( String key )
        throws KeyNotFoundException, KeyManagerException
    {
        try
        {
            AuthenticationKey authkey = (AuthenticationKey) keysCache.get( key );
            if ( authkey != null )
            {
                assertNotExpired( authkey );
                return authkey;
            }
            else
            {
                authkey = this.keyImpl.findKey( key );
                keysCache.put( key,authkey );
                return authkey;
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            // this is done to remove keys that have been expired.
            // TODO: need to make a listener for the key manager.
            keysCache.remove( key );
            throw knfe;
        }
    }

    public List<AuthenticationKey> getAllKeys()
    {
        log.debug( "NOT CACHED - .getAllKeys()" );
        return this.keyImpl.getAllKeys();
    }

    public String getId()
    {
        return "Cached Key Manager [" + this.keyImpl.getId() + "]";
    }

    public KeyManager getKeyImpl()
    {
        return keyImpl;
    }

    public void setKeyImpl( KeyManager keyImpl )
    {
        this.keyImpl = keyImpl;
    }

    public Cache getKeysCache()
    {
        return keysCache;
    }

    public void setKeysCache( Cache keysCache )
    {
        this.keysCache = keysCache;
    }
}
