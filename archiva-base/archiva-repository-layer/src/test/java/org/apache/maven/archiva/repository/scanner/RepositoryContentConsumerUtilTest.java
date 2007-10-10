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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.MockControl;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumerUtilTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryContentConsumerUtilTest
    extends AbstractRepositoryLayerTestCase
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
            fail( msg + ": Object [" + o.getClass().getName() + "] should have been an instanceof [" + clazz.getName() +
                "]" );
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

    public void testExecution()
        throws Exception
    {
        MockControl knownControl = MockControl.createNiceControl( KnownRepositoryContentConsumer.class );
        RepositoryContentConsumers consumers = lookupRepositoryConsumerUtil();
        KnownRepositoryContentConsumer knownConsumer = (KnownRepositoryContentConsumer) knownControl.getMock();
        consumers.setAvailableKnownConsumers( Collections.singletonList( knownConsumer ) );

        MockControl invalidControl = MockControl.createControl( InvalidRepositoryContentConsumer.class );
        InvalidRepositoryContentConsumer invalidConsumer = (InvalidRepositoryContentConsumer) invalidControl.getMock();
        consumers.setAvailableInvalidConsumers( Collections.singletonList( invalidConsumer ) );

        ManagedRepositoryConfiguration repo = createRepository( "id", "name", getTestFile( "target/test-repo" ) );
        File testFile = getTestFile( "target/test-repo/path/to/test-file.txt" );
        
        knownConsumer.beginScan( repo );
        knownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        knownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
        knownConsumer.processFile( _OS("path/to/test-file.txt") );
//        knownConsumer.completeScan();
        knownControl.replay();

        invalidConsumer.beginScan( repo );
//        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, testFile );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File notIncludedTestFile = getTestFile( "target/test-repo/path/to/test-file.xml" );

        knownConsumer.beginScan( repo );
        knownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        knownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
//        knownConsumer.completeScan();
        knownControl.replay();

        invalidConsumer.beginScan( repo );
        invalidConsumer.processFile( _OS("path/to/test-file.xml") );
        invalidConsumer.getId();
        invalidControl.setReturnValue( "invalid" );
//        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, notIncludedTestFile );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File excludedTestFile = getTestFile( "target/test-repo/path/to/test-file.txt" );

        knownConsumer.beginScan( repo );
        knownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.singletonList( "**/test-file.txt" ) );
//        knownConsumer.completeScan();
        knownControl.replay();

        invalidConsumer.beginScan( repo );
        invalidConsumer.processFile( _OS("path/to/test-file.txt") );
        invalidConsumer.getId();
        invalidControl.setReturnValue( "invalid" );
//        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, excludedTestFile );

        knownControl.verify();
        invalidControl.verify();
    }

    /**
     * Create an OS specific version of the filepath.
     * Provide path in unix "/" format.
     */
    private String _OS( String path )
    {
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            return path.replace( '/', '\\' );
        }
        return path;
    }
}
