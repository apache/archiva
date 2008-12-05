package org.apache.maven.archiva.transaction;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.codehaus.plexus.digest.Digester;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 
 * @version $Id$
 */
public abstract class AbstractFileEventTest
    extends PlexusInSpringTestCase
{
    protected List digesters;

    public void setUp()
        throws Exception
    {
        super.setUp();

        digesters = PlexusToSpringUtils.lookupList( PlexusToSpringUtils.buildSpringId( Digester.class.getName() ), getApplicationContext() );
    }

    protected void assertChecksumExists( File file, String algorithm )
    {
        assertChecksum( file, algorithm, true );
    }

    protected void assertChecksumDoesNotExist( File file, String algorithm )
    {
        assertChecksum( file, algorithm, false );
    }

    private void assertChecksum( File file, String algorithm, boolean exist )
    {
        String msg = exist ? "exists" : "does not exist";
        File checksumFile = new File( file.getPath() + "." + algorithm );
        assertEquals( "Test file " + algorithm + " checksum " + msg, exist, checksumFile.exists() );
    }

    protected void assertChecksumCommit( File file )
        throws IOException
    {
        assertChecksumExists( file, "md5" );
        assertChecksumExists( file, "sha1" );
    }

    protected void assertChecksumRollback( File file )
        throws IOException
    {
        assertChecksumDoesNotExist( file, "md5" );
        assertChecksumDoesNotExist( file, "sha1" );
    }

    protected String readFile( File file )
        throws IOException
    {
        return FileUtils.readFileToString( file );
    }

    protected void writeFile( File file, String content )
        throws IOException
    {
        FileUtils.writeStringToFile( file, content );
    }
}
