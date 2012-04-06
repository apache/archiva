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

import junit.framework.TestCase;

public class KeyManagerTest
    extends TestCase
{
    private final class AbstractKeyManagerExtension
        extends AbstractKeyManager
    {
        public AuthenticationKey addKey( AuthenticationKey key )
        {
            // TODO Auto-generated method stub
            return null;
        }

        public AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
            throws KeyManagerException
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void deleteKey( AuthenticationKey key )
            throws KeyManagerException
        {
            // TODO Auto-generated method stub

        }

        public void deleteKey( String key )
            throws KeyManagerException
        {
            // TODO Auto-generated method stub

        }

        public void eraseDatabase()
        {
            // TODO Auto-generated method stub

        }

        public AuthenticationKey findKey( String key )
            throws KeyNotFoundException, KeyManagerException
        {
            // TODO Auto-generated method stub
            return null;
        }

        public List<AuthenticationKey> getAllKeys()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public String getId()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public void testUUID()
        throws KeyManagerException
    {
        AbstractKeyManager manager = new AbstractKeyManagerExtension();

        // verifies we can get the provider after change not to require Sun one
        assertNotNull( manager.generateUUID() );
        assertTrue( manager.isRandomMode() );
    }
}
