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

import org.apache.maven.archiva.common.utils.Checksums;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
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
     * @plexus.requirement
     */
    private Checksums checksums;

    private List<String> options = new ArrayList<String>();

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

        if ( FAIL.equals( policySetting ) )
        {
            boolean checksPass = checksums.check( localFile ); 
            if( ! checksPass )
            {
                File sha1File = new File( localFile.getAbsolutePath() + ".sha1" );
                File md5File = new File( localFile.getAbsolutePath() + ".md5" );

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
            return checksums.update( localFile );
        }

        getLogger().error( "Unhandled policyCode [" + policySetting + "]" );
        return false;
    }

    public String getDefaultOption()
    {
        return FIX;
    }

    public String getId()
    {
        return "checksum";
    }

    public List<String> getOptions()
    {
        return options;
    }
}
