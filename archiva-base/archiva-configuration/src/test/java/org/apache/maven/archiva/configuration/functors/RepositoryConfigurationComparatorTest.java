package org.apache.maven.archiva.configuration.functors;

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
import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import java.util.Comparator;

/**
 * Test the repositry comparator.
 */
public class RepositoryConfigurationComparatorTest
    extends TestCase
{
    public void testComparator()
    {
        Comparator<AbstractRepositoryConfiguration> comparator = new RepositoryConfigurationComparator();

        assertEquals( 0, comparator.compare( null, null ) );
        assertEquals( 1, comparator.compare( createRepository( "id" ), null ) );
        assertEquals( -1, comparator.compare( null, createRepository( "id" ) ) );
        assertEquals( 0, comparator.compare( createRepository( "id1" ), createRepository( "id1" ) ) );
        assertEquals( -1, comparator.compare( createRepository( "id1" ), createRepository( "id2" ) ) );
        assertEquals( 1, comparator.compare( createRepository( "id2" ), createRepository( "id1" ) ) );
    }

    private ManagedRepositoryConfiguration createRepository( String id )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        return repo;
    }
}
