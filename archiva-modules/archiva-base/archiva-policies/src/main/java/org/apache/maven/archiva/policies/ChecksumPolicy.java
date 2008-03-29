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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.Checksums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChecksumPolicy - a policy applied after the download to see if the file has been downloaded
 * successfully and completely (or not).
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.policies.PostDownloadPolicy"
 *                   role-hint="checksum"
 */
public class ChecksumPolicy
    implements PostDownloadPolicy
{
    private Logger log = LoggerFactory.getLogger( ChecksumPolicy.class );
    
    /**
     * The IGNORE policy indicates that if the checksum policy is ignored, and
     * the state of, contents of, or validity of the checksum files are not
     * checked.
     */
    public static final String IGNORE = "ignore";
    
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
        options.add( IGNORE );
    }

    public void applyPolicy( String policySetting, Properties request, File localFile )
        throws PolicyViolationException, PolicyConfigurationException
    {
        if ( !options.contains( policySetting ) )
        {
            // Not a valid code. 
            throw new PolicyConfigurationException( "Unknown checksum policy setting [" + policySetting
                + "], valid settings are [" + StringUtils.join( options.iterator(), "," ) + "]" );
        }

        if ( IGNORE.equals( policySetting ) )
        {
            // Ignore.
            log.debug( "Checksum policy set to IGNORE." );
            return;
        }

        if ( !localFile.exists() )
        {
            // Local File does not exist.
            throw new PolicyViolationException( "Checksum policy failure, local file " + localFile.getAbsolutePath()
                + " does not exist to check." );
        }

        if ( FAIL.equals( policySetting ) )
        {
            if( checksums.check( localFile ) )
            {
                return;
            }
            
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
            throw new PolicyViolationException( "Checksums do not match, policy set to FAIL, "
                + "deleting checksum files and local file " + localFile.getAbsolutePath() + "." );
        }

        if ( FIX.equals( policySetting ) )
        {
            if( checksums.update( localFile ) )
            {
                log.debug( "Checksum policy set to FIX, checksum files have been updated." );
                return;
            }
            else
            {
                throw new PolicyViolationException( "Checksum policy set to FIX, "
                    + "yet unable to update checksums for local file " + localFile.getAbsolutePath() + "." );
            }
        }

        throw new PolicyConfigurationException( "Unable to process checksum policy of [" + policySetting
            + "], please file a bug report." );
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
