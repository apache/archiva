package org.apache.maven.archiva.repository.content;

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

/**
 * FilenameParserTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FilenameParserTest
    extends TestCase
{
    public void testNameExtensionJar()
    {
        FilenameParser parser = new FilenameParser( "maven-test-plugin-1.8.3.jar" );

        assertEquals( "maven-test-plugin-1.8.3", parser.getName() );
        assertEquals( "jar", parser.getExtension() );
    }

    public void testNameExtensionTarGz()
    {
        FilenameParser parser = new FilenameParser( "archiva-1.0-beta-2-bin.tar.gz" );

        assertEquals( "archiva-1.0-beta-2-bin", parser.getName() );
        assertEquals( "tar.gz", parser.getExtension() );
    }
    
    public void testNameExtensionTarBz2()
    {
        FilenameParser parser = new FilenameParser( "archiva-1.0-SNAPSHOT-src.tar.bz2" );

        assertEquals( "archiva-1.0-SNAPSHOT-src", parser.getName() );
        assertEquals( "tar.bz2", parser.getExtension() );
    }

    public void testNameExtensionCapitolizedTarGz()
    {
        FilenameParser parser = new FilenameParser( "ARCHIVA-1.0-BETA-2-BIN.TAR.GZ" );

        assertEquals( "ARCHIVA-1.0-BETA-2-BIN", parser.getName() );
        assertEquals( "TAR.GZ", parser.getExtension() );
    }

    public void testNext()
    {
        FilenameParser parser = new FilenameParser( "maven-test-plugin-1.8.3.jar" );

        assertEquals( "maven-test-plugin-1.8.3", parser.getName() );
        assertEquals( "jar", parser.getExtension() );
        
        assertEquals( "maven", parser.next() );
        assertEquals( "test", parser.next() );
        assertEquals( "plugin", parser.next() );
        assertEquals( "1.8.3", parser.next() );
        assertNull( parser.next() );
    }

    public void testExpect()
    {
        FilenameParser parser = new FilenameParser( "maven-test-plugin-1.8.3.jar" );

        assertEquals( "maven-test-plugin-1.8.3", parser.getName() );
        assertEquals( "jar", parser.getExtension() );

        assertEquals( "maven-test-plugin", parser.expect( "maven-test-plugin" ) );
        assertEquals( "1.8.3", parser.expect( "1.8.3" ) );
        assertNull( parser.expect( "jar" ) );
    }
    
    public void testExpectWithRemaining()
    {
        FilenameParser parser = new FilenameParser( "ganymede-ssh2-build250-sources.jar" );

        assertEquals( "ganymede-ssh2-build250-sources", parser.getName() );
        assertEquals( "jar", parser.getExtension() );

        assertEquals( "ganymede-ssh2", parser.expect( "ganymede-ssh2" ) );
        assertEquals( "build250", parser.expect( "build250" ) );
        assertEquals( "sources", parser.remaining() );
        
        assertNull( parser.expect( "jar" ) );
    }
    
    public void testNextNonVersion()
    {
        FilenameParser parser = new FilenameParser( "maven-test-plugin-1.8.3.jar" );
        
        assertEquals("maven-test-plugin", parser.nextNonVersion() );
        assertEquals("1.8.3", parser.remaining() );
    }
    
    public void testNextArbitraryNonVersion()
    {
        FilenameParser parser = new FilenameParser( "maven-jdk-1.4-plugin-1.0-20070828.123456-42.jar" );
        
        assertEquals("maven-jdk-1.4-plugin", parser.nextNonVersion() );
        assertEquals("1.0-20070828.123456-42", parser.remaining() );
    }

    public void testNextJython()
    {
        FilenameParser parser = new FilenameParser( "jython-20020827-no-oro.jar" );
        
        assertEquals("jython", parser.nextNonVersion() );
        assertEquals("20020827", parser.nextVersion() );
        assertEquals("no-oro", parser.remaining() );
    }
}
