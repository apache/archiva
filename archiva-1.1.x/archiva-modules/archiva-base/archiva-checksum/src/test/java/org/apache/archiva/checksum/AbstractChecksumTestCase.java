package org.apache.archiva.checksum;

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

import java.io.File;

import junit.framework.TestCase;

/**
 * AbstractChecksumTestCase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractChecksumTestCase
    extends TestCase
{
    private File basedir;

    public File getBasedir()
    {
        if ( basedir == null )
        {
            String sysprop = System.getProperty( "basedir" );
            if ( sysprop != null )
            {
                basedir = new File( sysprop );
            }
            else
            {
                basedir = new File( System.getProperty( "user.dir" ) );
            }
        }
        return basedir;
    }

    public File getTestOutputDir()
    {
        File dir = new File( getBasedir(), "target/test-output/" + getName() );
        if ( dir.exists() == false )
        {
            if ( dir.mkdirs() == false )
            {
                fail( "Unable to create test output directory: " + dir.getAbsolutePath() );
            }
        }
        return dir;
    }

    public File getTestResource( String filename )
    {
        File dir = new File( getBasedir(), "src/test/resources" );
        File file = new File( dir, filename );
        if ( file.exists() == false )
        {
            fail( "Test Resource does not exist: " + file.getAbsolutePath() );
        }
        return file;
    }
}
