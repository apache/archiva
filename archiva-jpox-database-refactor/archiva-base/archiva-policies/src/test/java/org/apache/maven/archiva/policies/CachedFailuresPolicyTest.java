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

import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Properties;

/**
 * CachedFailuresPolicyTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CachedFailuresPolicyTest
    extends PlexusTestCase
{
    private DownloadPolicy lookupPolicy()
        throws Exception
    {
        return (DownloadPolicy) lookup( PreDownloadPolicy.class.getName(), "cache-failures" );
    }

    private UrlFailureCache lookupUrlFailureCache()
        throws Exception
    {
        return (UrlFailureCache) lookup( UrlFailureCache.class.getName(), "default" );
    }

    private File getFile()
    {
        return new File( "target/cache-failures/" + getName() + ".txt" );
    }

    private Properties createRequest()
    {
        Properties request = new Properties();

        return request;
    }

    public void testIgnored()
        throws Exception
    {
        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        assertTrue( policy.applyPolicy( CachedFailuresPolicy.IGNORED, request, localFile ) );
    }

    public void testCachedNotInCache()
        throws Exception
    {
        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        assertTrue( policy.applyPolicy( CachedFailuresPolicy.CACHED, request, localFile ) );
    }

    public void testCachedInCache()
        throws Exception
    {
        UrlFailureCache urlFailureCache = lookupUrlFailureCache();

        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        String url = "http://a.bad.hostname.maven.org/path/to/resource.txt";

        urlFailureCache.cacheFailure( url );

        request.setProperty( "url", url );

        assertFalse( policy.applyPolicy( CachedFailuresPolicy.CACHED, request, localFile ) );
    }
}
