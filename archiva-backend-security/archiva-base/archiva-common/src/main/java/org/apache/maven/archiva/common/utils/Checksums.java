package org.apache.maven.archiva.common.utils;

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

import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Checksums utility component to validate or update checksums on Files. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.common.utils.Checksums"
 */
public class Checksums
    extends AbstractLogEnabled
{
    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    /**
     * @plexus.requirement role-hint="md5"
     */
    private Digester digestMd5;

    /**
     * @plexus.requirement
     */
    private ChecksumFile checksumFile;

    public boolean check( File file )
    {
        boolean checksPass = true;

        File sha1File = getSha1File( file );
        File md5File = getMd5File( file );

        // Both files missing is a failure.
        if ( !sha1File.exists() && !md5File.exists() )
        {
            getLogger().error( "File " + file.getPath() + " has no checksum files (sha1 or md5)." );
            checksPass = false;
        }

        if ( sha1File.exists() )
        {
            // Bad sha1 checksum is a failure.
            if ( !validateChecksum( sha1File, "sha1" ) )
            {
                getLogger().warn( "SHA1 is incorrect for " + file.getPath() );
                checksPass = false;
            }
        }

        if ( md5File.exists() )
        {
            // Bad md5 checksum is a failure.
            if ( !validateChecksum( md5File, "md5" ) )
            {
                getLogger().warn( "MD5 is incorrect for " + file.getPath() );
                checksPass = false;
            }
        }

        // TODO: eek!
        if ( !checksPass )
        {
            // On failure. delete files.
            if ( sha1File.exists() )
            {
                sha1File.delete();
            }

            if ( md5File.exists() )
            {
                md5File.delete();
            }

            file.delete();
        }

        return checksPass;
    }

    public boolean update( File file )
    {
        boolean checksPass = true;

        File sha1File = getSha1File( file );
        File md5File = getMd5File( file );

        if ( !fixChecksum( file, sha1File, digestSha1 ) )
        {
            checksPass = false;
        }

        if ( !fixChecksum( file, md5File, digestMd5 ) )
        {
            checksPass = false;
        }

        return checksPass;
    }

    private boolean createChecksum( File localFile, Digester digester )
    {
        try
        {
            checksumFile.createChecksum( localFile, digester );
            return true;
        }
        catch ( DigesterException e )
        {
            getLogger().warn( "Unable to create " + digester.getFilenameExtension() + " file: " + e.getMessage(), e );
            return false;
        }
        catch ( IOException e )
        {
            getLogger().warn( "Unable to create " + digester.getFilenameExtension() + " file: " + e.getMessage(), e );
            return false;
        }
    }

    private boolean fixChecksum( File localFile, File hashFile, Digester digester )
    {
        String ext = digester.getFilenameExtension();

        if ( !hashFile.getPath().endsWith( ext ) )
        {
            throw new IllegalArgumentException( "Cannot fix " + hashFile.getPath() + " using " + ext + " digester." );
        }

        // If hashfile doesn't exist, create it.
        if ( !hashFile.exists() )
        {
            return createChecksum( localFile, digester );
        }

        // Validate checksum, if bad, recreate it.
        try
        {
            if ( checksumFile.isValidChecksum( hashFile ) )
            {
                getLogger().debug( "Valid checksum: " + hashFile.getPath() );
                return true;
            }
            else
            {
                getLogger().debug( "Not valid checksum: " + hashFile.getPath() );
                return createChecksum( localFile, digester );
            }
        }
        catch ( FileNotFoundException e )
        {
            getLogger().warn( "Unable to find " + ext + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
        catch ( DigesterException e )
        {
            getLogger().warn( "Unable to process " + ext + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
        catch ( IOException e )
        {
            getLogger().warn( "Unable to process " + ext + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
    }

    private File getMd5File( File file )
    {
        return new File( file.getAbsolutePath() + ".md5" );
    }

    private File getSha1File( File file )
    {
        return new File( file.getAbsolutePath() + ".sha1" );

    }

    private boolean validateChecksum( File hashFile, String type )
    {
        try
        {
            boolean validity = checksumFile.isValidChecksum( hashFile );
            if ( validity )
            {
                getLogger().debug( "Valid checksum: " + hashFile.getPath() );
            }
            else
            {
                getLogger().debug( "Not valid checksum: " + hashFile.getPath() );
            }
            return validity;
        }
        catch ( FileNotFoundException e )
        {
            getLogger().warn( "Unable to find " + type + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
        catch ( DigesterException e )
        {
            getLogger().warn( "Unable to process " + type + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
        catch ( IOException e )
        {
            getLogger().warn( "Unable to process " + type + " file: " + hashFile.getAbsolutePath(), e );
            return false;
        }
    }
}
