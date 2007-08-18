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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * {@link PreDownloadPolicy} to check if the requested url has failed before. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.policies.PreDownloadPolicy"
 *                   role-hint="cache-failures"
 */
public class CachedFailuresPolicy
    extends AbstractLogEnabled
    implements PreDownloadPolicy
{
    /**
     * The CACHED policy indicates that if the URL provided exists in the
     * cached failures pool, then the policy fails, and the download isn't even 
     * attempted.
     */
    public static final String CACHED = "cached";

    /**
     * @plexus.requirement role-hint="default"
     */
    private UrlFailureCache urlFailureCache;

    private List options = new ArrayList();

    public CachedFailuresPolicy()
    {
        options.add( IGNORED );
        options.add( CACHED );
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
            getLogger().debug( "OK to fetch, check-failures policy set to IGNORED." );
            return true;
        }

        String url = request.getProperty( "url" );

        if ( StringUtils.isNotBlank( url ) )
        {
            if ( urlFailureCache.hasFailedBefore( url ) )
            {
                getLogger().debug( "NO to fetch, check-failures detected previous failure on url: " + url );
                return false;
            }
        }
        
        getLogger().debug( "OK to fetch, check-failures detected no issues." );

        return true;
    }

    public String getDefaultOption()
    {
        return IGNORED;
    }

    public String getId()
    {
        return "cache-failures";
    }

    public List getOptions()
    {
        return options;
    }
}
