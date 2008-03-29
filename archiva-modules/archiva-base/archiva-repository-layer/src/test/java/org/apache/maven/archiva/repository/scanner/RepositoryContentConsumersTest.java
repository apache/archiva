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
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumersTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryContentConsumersTest
    extends AbstractRepositoryLayerTestCase
{
    private RepositoryContentConsumers lookupRepositoryConsumers()
        throws Exception
    {
        RepositoryContentConsumers consumerUtil = (RepositoryContentConsumers) lookup( RepositoryContentConsumers.class
            .getName() );
        assertNotNull( "RepositoryContentConsumers should not be null.", consumerUtil );
        return consumerUtil;
    }

    public void testGetSelectedKnownIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "validate-signature",
            "index-content",
            "auto-remove",
            "auto-rename" };

        List<String> knownConsumers = consumerutil.getSelectedKnownConsumerIds();
        assertNotNull( "Known Consumer IDs should not be null", knownConsumers );
        assertEquals( "Known Consumer IDs.size", expectedKnownIds.length, knownConsumers.size() );

        for ( String expectedId : expectedKnownIds )
        {
            assertTrue( "Known id [" + expectedId + "] exists.", knownConsumers.contains( expectedId ) );
        }
    }

    public void testGetSelectedInvalidIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[] { "update-db-bad-content" };

        List<String> invalidConsumers = consumerutil.getSelectedInvalidConsumerIds();
        assertNotNull( "Invalid Consumer IDs should not be null", invalidConsumers );
        assertEquals( "Invalid Consumer IDs.size", expectedInvalidIds.length, invalidConsumers.size() );

        for ( String expectedId : expectedInvalidIds )
        {
            assertTrue( "Invalid id [" + expectedId + "] exists.", invalidConsumers.contains( expectedId ) );
        }
    }

    public void testGetSelectedKnownConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "index-content",
            "auto-remove",
            "auto-rename" };

        Map<String, KnownRepositoryContentConsumer> knownConsumerMap = consumerutil.getSelectedKnownConsumersMap();
        assertNotNull( "Known Consumer Map should not be null", knownConsumerMap );
        assertEquals( "Known Consumer Map.size", expectedSelectedKnownIds.length, knownConsumerMap.size() );

        for ( String expectedId : expectedSelectedKnownIds )
        {
            KnownRepositoryContentConsumer consumer = knownConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    public void testGetSelectedInvalidConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedInvalidIds[] = new String[] { "update-db-bad-content" };

        Map<String, InvalidRepositoryContentConsumer> invalidConsumerMap = consumerutil
            .getSelectedInvalidConsumersMap();
        assertNotNull( "Invalid Consumer Map should not be null", invalidConsumerMap );
        assertEquals( "Invalid Consumer Map.size", expectedSelectedInvalidIds.length, invalidConsumerMap.size() );

        for ( String expectedId : expectedSelectedInvalidIds )
        {
            InvalidRepositoryContentConsumer consumer = invalidConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    public void testGetAvailableKnownList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "index-content",
            "auto-remove",
            "auto-rename",
            "available-but-unselected" };

        List<KnownRepositoryContentConsumer> knownConsumers = consumerutil.getAvailableKnownConsumers();
        assertNotNull( "known consumers should not be null.", knownConsumers );
        assertEquals( "known consumers", expectedKnownIds.length, knownConsumers.size() );

        List<String> expectedIds = Arrays.asList( expectedKnownIds );
        for ( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            assertTrue( "Consumer [" + consumer.getId() + "] returned by .getAvailableKnownConsumers() is unexpected.",
                        expectedIds.contains( consumer.getId() ) );
        }
    }

    public void testGetAvailableInvalidList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[] { "update-db-bad-content", "move-to-trash-then-notify" };

        List<InvalidRepositoryContentConsumer> invalidConsumers = consumerutil.getAvailableInvalidConsumers();
        assertNotNull( "invalid consumers should not be null.", invalidConsumers );
        assertEquals( "invalid consumers", expectedInvalidIds.length, invalidConsumers.size() );

        List<String> expectedIds = Arrays.asList( expectedInvalidIds );
        for ( InvalidRepositoryContentConsumer consumer : invalidConsumers )
        {
            assertTrue( "Consumer [" + consumer.getId()
                + "] returned by .getAvailableInvalidConsumers() is unexpected.", expectedIds.contains( consumer
                .getId() ) );
        }
    }

    public void testExecution()
        throws Exception
    {
        MockControl knownControl = MockControl.createNiceControl( KnownRepositoryContentConsumer.class );
        RepositoryContentConsumers consumers = lookupRepositoryConsumers();
        KnownRepositoryContentConsumer selectedKnownConsumer = (KnownRepositoryContentConsumer) knownControl.getMock();
        KnownRepositoryContentConsumer unselectedKnownConsumer =
            (KnownRepositoryContentConsumer) MockControl.createNiceControl(
                KnownRepositoryContentConsumer.class ).getMock();
        consumers.setAvailableKnownConsumers( Arrays.asList( selectedKnownConsumer, unselectedKnownConsumer ) );
        consumers.setSelectedKnownConsumers( Collections.singletonList( selectedKnownConsumer ) );

        MockControl invalidControl = MockControl.createControl( InvalidRepositoryContentConsumer.class );
        InvalidRepositoryContentConsumer selectedInvalidConsumer =
            (InvalidRepositoryContentConsumer) invalidControl.getMock();
        InvalidRepositoryContentConsumer unselectedInvalidConsumer =
            (InvalidRepositoryContentConsumer) MockControl.createControl(
                InvalidRepositoryContentConsumer.class ).getMock();
        consumers.setAvailableInvalidConsumers( Arrays.asList( selectedInvalidConsumer, unselectedInvalidConsumer ) );
        consumers.setSelectedInvalidConsumers( Collections.singletonList( selectedInvalidConsumer ) );

        ManagedRepositoryConfiguration repo = createRepository( "id", "name", getTestFile( "target/test-repo" ) );
        File testFile = getTestFile( "target/test-repo/path/to/test-file.txt" );

        selectedKnownConsumer.beginScan( repo );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        selectedKnownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
        selectedKnownConsumer.processFile( _OS( "path/to/test-file.txt" ) );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo );
        //        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, testFile );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File notIncludedTestFile = getTestFile( "target/test-repo/path/to/test-file.xml" );

        selectedKnownConsumer.beginScan( repo );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        selectedKnownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.xml" ) );
        selectedInvalidConsumer.getId();
        invalidControl.setReturnValue( "invalid" );
        //        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, notIncludedTestFile );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File excludedTestFile = getTestFile( "target/test-repo/path/to/test-file.txt" );

        selectedKnownConsumer.beginScan( repo );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.singletonList( "**/test-file.txt" ) );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.txt" ) );
        selectedInvalidConsumer.getId();
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
