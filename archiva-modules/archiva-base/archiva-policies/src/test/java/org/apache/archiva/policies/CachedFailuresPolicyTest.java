package org.apache.archiva.policies;

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

import junit.framework.TestCase;
import org.apache.archiva.policies.urlcache.UrlFailureCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Properties;

/**
 * CachedFailuresPolicyTest
 *
 * @version $Id$
 */
@RunWith( value = SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class CachedFailuresPolicyTest
    extends TestCase
{


    @Inject
    private UrlFailureCache urlFailureCache;

    @Inject @Named(value="preDownloadPolicy#cache-failures")
    DownloadPolicy downloadPolicy;

    private DownloadPolicy lookupPolicy()
        throws Exception
    {
        return downloadPolicy;
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

    @Test
    public void testPolicyNo()
        throws Exception
    {
        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        policy.applyPolicy( CachedFailuresPolicy.NO, request, localFile );
    }

    @Test
    public void testPolicyYesNotInCache()
        throws Exception
    {

        //CacheManager.getInstance().clearAll();

        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        policy.applyPolicy( CachedFailuresPolicy.YES, request, localFile );
    }

    @Test
    public void testPolicyYesInCache()
        throws Exception
    {
        DownloadPolicy policy = lookupPolicy();
        File localFile = getFile();
        Properties request = createRequest();

        String url = "http://a.bad.hostname.maven.org/path/to/resource.txt";


        urlFailureCache.cacheFailure( url );

        request.setProperty( "url", url );

        try
        {
            policy.applyPolicy( CachedFailuresPolicy.YES, request, localFile );
            fail( "Expected a PolicyViolationException." );
        }
        catch ( PolicyViolationException e )
        {
            // expected path.
        }
    }
}
