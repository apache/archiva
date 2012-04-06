package org.codehaus.plexus.redback.keys;

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

/**
 * KeyManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface KeyManager
{


    /**
     * String identifying the key manager implementation.
     *
     * @return the key manager implementation id.
     */
    String getId();

    /**
     * Attempt to find a specific key in the store.
     * <p/>
     * NOTE: Implementations of this interface should never return an expired key.
     *
     * @param key the key to find.
     * @return the actual key found.
     * @throws KeyNotFoundException when the requested, unexpired, key cannot be found.
     * @throws KeyManagerException  when there is a fundamental problem with the KeyManager implementation.
     */
    AuthenticationKey findKey( String key )
        throws KeyNotFoundException, KeyManagerException;

    /**
     * Create a key (and save it to the store) for the specified principal.
     *
     * @param principal         the principal to generate the key for.
     * @param purpose           the purpose of the key. (Example: "selfservice password reset", "new user validation",
     *                          "remember me")  This is a purely informational field .
     * @param expirationMinutes the amount in minutes until this key expires. (-1 means no expiration)
     * @return the key created
     * @throws KeyManagerException if there is a fundamental problem with the KeyManager implementation.
     */
    AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
        throws KeyManagerException;

    /**
     * Delete a key from the underlying store.
     *
     * @param key the key to delete.
     */
    void deleteKey( AuthenticationKey key )
        throws KeyManagerException;

    /**
     * Delete a key from the underlying store.
     *
     * @param key the key to delete.
     */
    void deleteKey( String key )
        throws KeyManagerException;

    List<AuthenticationKey> getAllKeys();

    AuthenticationKey addKey( AuthenticationKey key );

    void eraseDatabase();

    /**
     * Remove all keys that are expired.
     */
    void removeExpiredKeys()
        throws KeyManagerException;
}
