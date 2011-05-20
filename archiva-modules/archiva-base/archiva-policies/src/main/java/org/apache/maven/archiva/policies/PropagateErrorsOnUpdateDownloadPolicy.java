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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * PropagateErrorsPolicy - a policy applied on error to determine how to treat the error.
 *
 * @plexus.component role="org.apache.maven.archiva.policies.DownloadErrorPolicy"
 *                   role-hint="propagate-errors-on-update"
 */
@Service("downloadErrorPolicy#propagate-errors-on-update")
public class PropagateErrorsOnUpdateDownloadPolicy
    implements DownloadErrorPolicy
{
    /**
     * Signifies any error should cause a failure whether the artifact is already present or not.
     */
    public static final String ALWAYS = "always";

    /**
     * Signifies any error should cause a failure only if the artifact is not already present.
     */
    public static final String NOT_PRESENT = "artifact not already present";

    private List<String> options = new ArrayList<String>();

    public PropagateErrorsOnUpdateDownloadPolicy()
    {
        options.add( ALWAYS );
        options.add( NOT_PRESENT );
    }

    public boolean applyPolicy( String policySetting, Properties request, File localFile, Exception exception,
                             Map<String,Exception> previousExceptions )
        throws PolicyConfigurationException
    {
        if ( !options.contains( policySetting ) )
        {
            // Not a valid code.
            throw new PolicyConfigurationException( "Unknown error policy setting [" + policySetting
                + "], valid settings are [" + StringUtils.join( options.iterator(), "," ) + "]" );
        }

        if ( ALWAYS.equals( policySetting ) )
        {
            // throw ther exception regardless
            return true;
        }

        if ( NOT_PRESENT.equals( policySetting ) )
        {
            // cancel the exception if the file exists
            return !localFile.exists();
        }

        throw new PolicyConfigurationException( "Unable to process checksum policy of [" + policySetting
            + "], please file a bug report." );
    }

    public String getDefaultOption()
    {
        return NOT_PRESENT;
    }

    public String getId()
    {
        return "propagate-errors-on-update";
    }

    public String getName()
    {
        return "Return error when";
    }

    public List<String> getOptions()
    {
        return options;
    }
}