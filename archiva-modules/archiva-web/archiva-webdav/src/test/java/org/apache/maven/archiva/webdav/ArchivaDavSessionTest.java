package org.apache.maven.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

public class ArchivaDavSessionTest extends TestCase
{
    public void testTokens()
    {
        ArchivaDavSession session = new ArchivaDavSession();
        final String myToken = "thisisadavtoken";
        
        session.addLockToken(myToken);
        assertEquals(1, session.getLockTokens().length);
        assertEquals(myToken, session.getLockTokens()[0]);
        
        session.removeLockToken(myToken);
        assertEquals(0, session.getLockTokens().length);
    }
    
    public void testAddReferencesThrowsUnsupportedOperationException()
    {
        ArchivaDavSession session = new ArchivaDavSession();
        try
        {
            session.addReference(new Object());
            fail("Did not throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e)
        {
            assertTrue(true);
        } 
    }
    
    public void testRemoveReferencesThrowsUnsupportedOperationException()
    {
        ArchivaDavSession session = new ArchivaDavSession();
        try
        {
            session.removeReference(new Object());
            fail("Did not throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e)
        {
            assertTrue(true);
        }
    }
}
