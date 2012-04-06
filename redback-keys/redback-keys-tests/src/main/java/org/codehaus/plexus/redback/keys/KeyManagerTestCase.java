package org.codehaus.plexus.redback.keys;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * KeyManagerTestCase
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath*:/spring-context.xml"} )
public class KeyManagerTestCase
    extends TestCase
{
    private KeyManager manager;

    public KeyManager getKeyManager()
    {
        return manager;
    }

    public void setKeyManager( KeyManager manager )
    {
        this.manager = manager;
    }

    private void assertSameDates( Date expected, Date actual )
    {
        if ( ( expected == null ) && ( actual != null ) )
        {
            fail( "Expected date is null, actual date [" + actual + "]." );
        }

        if ( ( expected != null ) && ( actual == null ) )
        {
            fail( "Expected date [" + expected + "], actual date is null." );
        }

        SimpleDateFormat format = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z" );
        assertEquals( format.format( expected ), format.format( actual ) );
    }

    @Test
    public void testNormal()
        throws KeyNotFoundException, KeyManagerException
    {
        String principal = "foo";
        String purpose = "Testing";

        AuthenticationKey created = getKeyManager().createKey( principal, purpose, 15 );

        assertNotNull( created );
        assertNotNull( created.getKey() );
        assertNotNull( created.getDateCreated() );
        assertNotNull( created.getDateExpires() );

        assertEquals( principal, created.getForPrincipal() );
        assertEquals( purpose, created.getPurpose() );

        Date expectedCreated = created.getDateCreated();
        Date expectedExpires = created.getDateExpires();

        String expectedKey = created.getKey();

        AuthenticationKey found = getKeyManager().findKey( expectedKey );

        assertEquals( expectedKey, found.getKey() );
        assertEquals( principal, found.getForPrincipal() );
        assertEquals( purpose, found.getPurpose() );
        assertSameDates( expectedCreated, found.getDateCreated() );
        assertSameDates( expectedExpires, found.getDateExpires() );
    }

    @Test
    public void testGetAllKeys()
        throws KeyManagerException
    {
        getKeyManager().eraseDatabase();
        AuthenticationKey created1 = getKeyManager().createKey( "foo", "Testing", 15 );
        AuthenticationKey created2 = getKeyManager().createKey( "bar", "Something", 23 );

        assertNotNull( created1 );
        assertNotNull( created2 );

        assertEquals( "foo", created1.getForPrincipal() );
        assertEquals( "Testing", created1.getPurpose() );

        assertEquals( "bar", created2.getForPrincipal() );
        assertEquals( "Something", created2.getPurpose() );

        List<AuthenticationKey> keys = getKeyManager().getAllKeys();
        Collections.sort( keys, new Comparator<AuthenticationKey>()
        {
            public int compare( AuthenticationKey key1, AuthenticationKey key2 )
            {
                return key2.getForPrincipal().compareTo( key1.getForPrincipal() );
            }
        } );

        AuthenticationKey found = (AuthenticationKey) keys.get( 0 );
        assertEquals( created1.getKey(), found.getKey() );
        assertEquals( "foo", found.getForPrincipal() );
        assertEquals( "Testing", found.getPurpose() );
        assertSameDates( created1.getDateCreated(), found.getDateCreated() );
        assertSameDates( created1.getDateExpires(), found.getDateExpires() );

        found = (AuthenticationKey) keys.get( 1 );
        assertEquals( created2.getKey(), found.getKey() );
        assertEquals( "bar", found.getForPrincipal() );
        assertEquals( "Something", found.getPurpose() );
        assertSameDates( created2.getDateCreated(), found.getDateCreated() );
        assertSameDates( created2.getDateExpires(), found.getDateExpires() );
    }

    @Test
    public void testNotThere()
        throws KeyManagerException
    {
        String principal = "foo";
        String purpose = "Testing";

        AuthenticationKey created = getKeyManager().createKey( principal, purpose, 15 );

        assertNotNull( created );
        assertNotNull( created.getKey() );
        assertNotNull( created.getDateCreated() );
        assertNotNull( created.getDateExpires() );

        assertEquals( principal, created.getForPrincipal() );
        assertEquals( purpose, created.getPurpose() );

        try
        {
            getKeyManager().findKey( "deadbeefkey" );
            fail( "Invalid Key should not have been found." );
        }
        catch ( KeyNotFoundException e )
        {
            // Expected path for this test.
        }
    }

    @Test
    public void testExpired()
        throws KeyManagerException, InterruptedException
    {
        String principal = "foo";
        String purpose = "Testing";

        AuthenticationKey created = getKeyManager().createKey( principal, purpose, 0 );

        assertNotNull( created );
        assertNotNull( created.getKey() );
        assertNotNull( created.getDateCreated() );
        assertNotNull( created.getDateExpires() );

        assertEquals( principal, created.getForPrincipal() );
        assertEquals( purpose, created.getPurpose() );

        String expectedKey = created.getKey();

        try
        {
            Thread.sleep( 500 ); // Sleep to let it expire
            getKeyManager().findKey( expectedKey );
            fail( "Expired Key should not have been found." );
        }
        catch ( KeyNotFoundException e )
        {
            // Expected path for this test.
        }
    }

    @Test
    public void testPermanent()
        throws KeyManagerException
    {
        String principal = "foo";
        String purpose = "Testing";

        AuthenticationKey created = getKeyManager().createKey( principal, purpose, -1 );

        assertNotNull( created );
        assertNotNull( created.getKey() );
        assertNotNull( created.getDateCreated() );
        assertNull( created.getDateExpires() );

        assertEquals( principal, created.getForPrincipal() );
        assertEquals( purpose, created.getPurpose() );

        Date expectedCreated = created.getDateCreated();

        String expectedKey = created.getKey();

        AuthenticationKey found = getKeyManager().findKey( expectedKey );

        assertEquals( expectedKey, found.getKey() );
        assertEquals( principal, found.getForPrincipal() );
        assertEquals( purpose, found.getPurpose() );
        assertSameDates( expectedCreated, found.getDateCreated() );
        assertNull( found.getDateExpires() );
    }
}
