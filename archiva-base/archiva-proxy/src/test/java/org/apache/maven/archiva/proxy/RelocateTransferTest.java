package org.apache.maven.archiva.proxy;

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

/**
 * RelocateTransferTest
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RelocateTransferTest
    extends AbstractProxyTestCase
{

    public void testRelocateMaven1Request()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-relocated-artefact-1.0.jar";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }

    public void testDoublyRelocateMaven1Request()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-doubly-relocated-artefact-1.0.jar";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }

    public void testRelocateMaven1PomRequest()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/poms/get-relocated-artefact-with-pom-1.0.pom";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present-with-pom/1.0/get-default-layout-present-with-pom-1.0.pom";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //
        //        assertTrue( expectedFile.exists() );
    }

    public void testRelocateMaven1PomRequestMissingTarget()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/poms/get-relocated-artefact-1.0.pom";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.pom";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        try
        //        {
        //            requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //            fail( "Should have failed to find target POM" );
        //        }
        //        catch ( ResourceDoesNotExistException e )
        //        {
        //            assertTrue( true );
        //        }
    }

    public void testRelocateMaven1ChecksumRequest()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-relocated-artefact-1.0.jar.md5";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar.md5";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        path = "org.apache.maven.test/jars/get-relocated-artefact-1.0.jar.sha1";
        //        relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar.sha1";
        //        expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        try
        //        {
        //            requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //            fail( "Checksum was not present, should not be found" );
        //        }
        //        catch ( ResourceDoesNotExistException e )
        //        {
        //            assertTrue( true );
        //        }
    }

    public void testRelocateMaven2Request()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-relocated-artefact/1.0/get-relocated-artefact-1.0.jar";
        //        String relocatedPath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }

    public void testRelocateMaven2RequestInLegacyManagedRepo()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-relocated-artefact/1.0/get-relocated-artefact-1.0.jar";
        //        String relocatedPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( legacyManagedRepository.getBasedir(), relocatedPath );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, legacyManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }

}
