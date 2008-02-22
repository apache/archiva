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
        assertEquals( '-', parser.seperator() );
        assertEquals( "sources", parser.remaining() );

        assertNull( parser.expect( "jar" ) );
    }

    public void testExpectWithRemainingDualExtensions()
    {
        FilenameParser parser = new FilenameParser( "example-presentation-3.2.xml.zip" );

        assertEquals( "example-presentation-3.2.xml", parser.getName() );
        assertEquals( "zip", parser.getExtension() );

        assertEquals( "example-presentation", parser.expect( "example-presentation" ) );
        assertEquals( "3.2", parser.expect( "3.2" ) );
        assertEquals( '.', parser.seperator() );
        assertEquals( "xml", parser.remaining() );

    }

    public void testNextNonVersion()
    {
        FilenameParser parser = new FilenameParser( "maven-test-plugin-1.8.3.jar" );

        assertEquals( "maven-test-plugin", parser.nextNonVersion() );
        assertEquals( "1.8.3", parser.remaining() );
    }

    public void testNextArbitraryNonVersion()
    {
        FilenameParser parser = new FilenameParser( "maven-jdk-1.4-plugin-1.0-20070828.123456-42.jar" );

        assertEquals( "maven-jdk-1.4-plugin", parser.nextNonVersion() );
        assertEquals( "1.0-20070828.123456-42", parser.remaining() );
    }

    public void testNextJython()
    {
        FilenameParser parser = new FilenameParser( "jython-20020827-no-oro.jar" );

        assertEquals( "jython", parser.nextNonVersion() );
        assertEquals( "20020827", parser.nextVersion() );
        assertEquals( "no-oro", parser.remaining() );
    }

    public void testLongExtension()
    {
        FilenameParser parser = new FilenameParser( "libfobs4jmf-0.4.1.4-20080217.211715-4.jnilib" );

        assertEquals( "libfobs4jmf-0.4.1.4-20080217.211715-4", parser.getName() );
        assertEquals( "jnilib", parser.getExtension() );
    }

    public void testInterveningVersion()
    {
        FilenameParser parser = new FilenameParser( "artifact-id-1.0-abc-1.1-20080221.062205-9.pom" );

        assertEquals( "artifact-id", parser.nextNonVersion() );
        assertEquals( "1.0-abc-1.1-20080221.062205-9", parser.expect( "1.0-abc-1.1-SNAPSHOT" ) );
        assertNull( null, parser.remaining() );
        assertEquals( "artifact-id-1.0-abc-1.1-20080221.062205-9", parser.getName() );
        assertEquals( "pom", parser.getExtension() );
    }

    public void testExpectWrongSnapshot()
    {
        FilenameParser parser = new FilenameParser( "artifact-id-1.0-20080221.062205-9.pom" );

        assertEquals( "artifact-id", parser.nextNonVersion() );
        assertNull( parser.expect( "2.0-SNAPSHOT" ) );
    }

    public void testClassifier()
    {
        FilenameParser parser = new FilenameParser( "artifact-id-1.0-20070219.171202-34-test-sources.jar" );

        assertEquals( "artifact-id", parser.nextNonVersion() );
        assertEquals( "1.0-20070219.171202-34", parser.nextVersion() );
        assertEquals( "test-sources", parser.remaining() );
        assertEquals( "artifact-id-1.0-20070219.171202-34-test-sources", parser.getName() );
        assertEquals( "jar", parser.getExtension() );
    }
}
