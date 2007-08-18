package org.apache.maven.archiva.policies;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ChecksumPolicy 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.policies.PostDownloadPolicy"
 *                   role-hint="checksum"
 */
public class ChecksumPolicy
    extends AbstractLogEnabled
    implements PostDownloadPolicy
{
    /**
     * The FAIL policy indicates that if the checksum does not match the
     * downloaded file, then remove the downloaded artifact, and checksum
     * files, and fail the transfer to the client side.
     */
    public static final String FAIL = "fail";

    /**
     * The FIX policy indicates that if the checksum does not match the
     * downloaded file, then fix the checksum file locally, and return
     * to the client side the corrected checksum.
     */
    public static final String FIX = "fix";

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

    private List options = new ArrayList();

    public ChecksumPolicy()
    {
        options.add( FAIL );
        options.add( FIX );
        options.add( IGNORED );
    }

    public boolean applyPolicy( String policySetting, Properties request, File localFile )
    {
        if ( !options.contains( policySetting ) )
        {
            // No valid code? false it is then.
            getLogger().error( "Unknown checksum policyCode [" + policySetting + "]" );
            return false;
        }

        if ( IGNORED.equals( policySetting ) )
        {
            // Ignore.
            return true;
        }

        if ( !localFile.exists() )
        {
            // Local File does not exist.
            getLogger().debug( "Local file " + localFile.getAbsolutePath() + " does not exist." );
            return false;
        }

        File sha1File = new File( localFile.getAbsolutePath() + ".sha1" );
        File md5File = new File( localFile.getAbsolutePath() + ".md5" );

        if ( FAIL.equals( policySetting ) )
        {
            boolean checksPass = true;

            // Both files missing is a failure.
            if ( !sha1File.exists() && !md5File.exists() )
            {
                getLogger().error( "File " + localFile.getPath() + " has no checksum files (sha1 or md5)." );
                checksPass = false;
            }

            if ( sha1File.exists() )
            {
                // Bad sha1 checksum is a failure.
                if ( !validateChecksum( sha1File, "sha1" ) )
                {
                    getLogger().warn( "SHA1 is incorrect for " + localFile.getPath() );
                    checksPass = false;
                }
            }

            if ( md5File.exists() )
            {
                // Bad md5 checksum is a failure.
                if ( !validateChecksum( md5File, "md5" ) )
                {
                    getLogger().warn( "MD5 is incorrect for " + localFile.getPath() );
                    checksPass = false;
                }
            }

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

                localFile.delete();
            }

            return checksPass;
        }

        if ( FIX.equals( policySetting ) )
        {
            boolean checksPass = true;

            if ( !fixChecksum( localFile, sha1File, digestSha1 ) )
            {
                checksPass = false;
            }

            if ( !fixChecksum( localFile, md5File, digestMd5 ) )
            {
                checksPass = false;
            }

            return checksPass;
        }

        getLogger().error( "Unhandled policyCode [" + policySetting + "]" );
        return false;
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

    public String getDefaultOption()
    {
        return FIX;
    }

    public String getId()
    {
        return "checksum";
    }

    public List getOptions()
    {
        return options;
    }

}
