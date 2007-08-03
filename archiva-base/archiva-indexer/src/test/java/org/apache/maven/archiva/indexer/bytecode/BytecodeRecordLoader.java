package org.apache.maven.archiva.indexer.bytecode;

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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactJavaDetails;
import org.apache.maven.archiva.model.platform.JavaArtifactHelper;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;

/**
 * BytecodeRecordLoader - Utility method for loading dump files into BytecordRecords. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BytecodeRecordLoader
{
    private static Map cache = new HashMap();

    public static BytecodeRecord loadRecord( File dumpFile, ArchivaArtifact artifact )
    {
        BytecodeRecord record = (BytecodeRecord) cache.get( artifact );
        if ( record != null )
        {
            return record;
        }

        record = new BytecodeRecord();
        record.setArtifact( artifact );

        record.setClasses( new ArrayList() );
        record.setMethods( new ArrayList() );
        record.setFiles( new ArrayList() );

        FileReader freader = null;
        BufferedReader reader = null;

        try
        {
            freader = new FileReader( dumpFile );
            reader = new BufferedReader( freader );

            String line = reader.readLine();
            while ( line != null )
            {
                if ( line.startsWith( "FILENAME|" ) )
                {
                    String filename = line.substring( "FILENAME|".length() );
                    record.setFilename( filename );
                }
                else if ( line.startsWith( "SIZE|" ) )
                {
                    String size = line.substring( "SIZE|".length() );
                    record.getArtifact().getModel().setSize( Long.parseLong( size ) );
                }
                else if ( line.startsWith( "HASH_MD5|" ) )
                {
                    String md5 = line.substring( "HASH_MD5|".length() );
                    record.getArtifact().getModel().setChecksumMD5( md5 );
                }
                else if ( line.startsWith( "HASH_SHA1|" ) )
                {
                    String sha1 = line.substring( "HASH_SHA1|".length() );
                    record.getArtifact().getModel().setChecksumSHA1( sha1 );
                }
                else if ( line.startsWith( "HASH_BYTECODE|" ) )
                {
                    String hash = line.substring( "HASH_BYTECODE|".length() );
                    ArchivaArtifactJavaDetails javaDetails = JavaArtifactHelper.getJavaDetails( record.getArtifact() );
                    javaDetails.setChecksumBytecode( hash );
                }
                else if ( line.startsWith( "JDK|" ) )
                {
                    String jdk = line.substring( "JDK|".length() );
                    ArchivaArtifactJavaDetails javaDetails = JavaArtifactHelper.getJavaDetails( record.getArtifact() );
                    javaDetails.setJdk( jdk );
                }
                else if ( line.startsWith( "CLASS|" ) )
                {
                    String classname = line.substring( "CLASS|".length() );
                    record.getClasses().add( classname );
                }
                else if ( line.startsWith( "METHOD|" ) )
                {
                    String methodName = line.substring( "METHOD|".length() );
                    record.getMethods().add( methodName );
                }
                else if ( line.startsWith( "FILE|" ) )
                {
                    String fileentry = line.substring( "FILE|".length() );
                    record.getFiles().add( fileentry );
                }

                line = reader.readLine();
            }
        }
        catch ( IOException e )
        {
            throw new AssertionFailedError( "Unable to load record " + dumpFile + " from disk: " + e.getMessage() );
        }
        finally
        {
            IOUtil.close( reader );
            IOUtil.close( freader );
        }

        cache.put( artifact, record );

        return record;
    }
}
