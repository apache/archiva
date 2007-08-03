package org.apache.maven.archiva.repository.scanner;

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

import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumerUtilTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryContentConsumerUtilTest
    extends PlexusTestCase
{
    private RepositoryContentConsumers lookupRepositoryConsumerUtil()
        throws Exception
    {
        RepositoryContentConsumers consumerUtil = (RepositoryContentConsumers) lookup( RepositoryContentConsumers.class
            .getName() );
        assertNotNull( "RepositoryContentConsumerUtil should not be null.", consumerUtil );
        return consumerUtil;
    }

    public void testGetSelectedIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumerUtil();

        List knownConsumers = consumerutil.getSelectedKnownConsumerIds();
        assertNotNull( "Known Consumer IDs should not be null", knownConsumers );
        assertEquals( "Known Consumer IDs.size", 9, knownConsumers.size() );

        List invalidConsumers = consumerutil.getSelectedInvalidConsumerIds();
        assertNotNull( "Invalid Consumer IDs should not be null", invalidConsumers );
        assertEquals( "Invalid Consumer IDs.size", 1, invalidConsumers.size() );
    }

    public void testGetSelectedConsumersMaps()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumerUtil();

        Map knownConsumerMap = consumerutil.getSelectedKnownConsumersMap();
        assertNotNull( "Known Consumer Map should not be null", knownConsumerMap );
        assertEquals( "Known Consumer Map.size", 1, knownConsumerMap.size() );

        Object o = knownConsumerMap.get( "sample-known" );
        assertNotNull( "Known[sample-known] should not be null.", o );
        assertInstanceof( "Known[sample-known]", RepositoryContentConsumer.class, o );
        assertInstanceof( "Known[sample-known]", KnownRepositoryContentConsumer.class, o );
        
        Map invalidConsumerMap = consumerutil.getSelectedInvalidConsumersMap();
        assertNotNull( "Invalid Consumer Map should not be null", invalidConsumerMap );
        assertEquals( "Invalid Consumer Map.size", 0, invalidConsumerMap.size() );
    }

    private void assertInstanceof( String msg, Class clazz, Object o )
    {
        if ( clazz.isInstance( o ) == false )
        {
            fail( msg + ": Object [" + o.getClass().getName() + "] should have been an instanceof [" + clazz.getName()
                + "]" );
        }
    }

    public void testGetAvailableLists()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumerUtil();

        List knownConsumers = consumerutil.getAvailableKnownConsumers();
        assertNotNull( "known consumers should not be null.", knownConsumers );
        assertEquals( "known consumers", 1, knownConsumers.size() );
        assertInstanceof( "Available Known Consumers", RepositoryContentConsumer.class, knownConsumers.get( 0 ) );

        List invalidConsumers = consumerutil.getAvailableInvalidConsumers();
        assertNotNull( "invalid consumers should not be null.", invalidConsumers );
        assertEquals( "invalid consumers", 0, invalidConsumers.size() );
    }
}
