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
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * {@link PreDownloadPolicy} to check if the requested url has failed before.
 *
 * @version $Id$
 */
@Service("preDownloadPolicy#cache-failures")
public class CachedFailuresPolicy
    implements PreDownloadPolicy
{
    private Logger log = LoggerFactory.getLogger( CachedFailuresPolicy.class );
    
    /**
     * The NO policy setting means that the the existence of old failures is <strong>not</strong> checked.
     * All resource requests are allowed thru to the remote repo.
     */
    public static final String NO = "no";

    /**
     * The YES policy setting means that the existence of old failures is checked, and will
     * prevent the request from being performed against the remote repo.
     */
    public static final String YES = "yes";

    /**
     * plexus.requirement
     */
    @Inject
    private UrlFailureCache urlFailureCache;

    private List<String> options = new ArrayList<String>();

    public CachedFailuresPolicy()
    {
        options.add( NO );
        options.add( YES );
    }

    public void applyPolicy( String policySetting, Properties request, File localFile )
        throws PolicyViolationException, PolicyConfigurationException
    {
        if ( !options.contains( policySetting ) )
        {
            // Not a valid code.
            throw new PolicyConfigurationException( "Unknown cache-failues policy setting [" + policySetting +
                "], valid settings are [" + StringUtils.join( options.iterator(), "," ) + "]" );
        }

        if ( NO.equals( policySetting ) )
        {
            // Skip.
            log.debug( "OK to fetch, check-failures policy set to NO." );
            return;
        }

        String url = request.getProperty( "url" );

        if ( StringUtils.isNotBlank( url ) )
        {
            if ( urlFailureCache.hasFailedBefore( url ) )
            {
                throw new PolicyViolationException(
                    "NO to fetch, check-failures detected previous failure on url: " + url );
            }
        }

        log.debug( "OK to fetch, check-failures detected no issues." );
    }

    public String getDefaultOption()
    {
        return NO;
    }

    public String getId()
    {
        return "cache-failures";
    }

    public String getName()
    {
        return "Cache failures";
    }

    public List<String> getOptions()
    {
        return options;
    }
}
