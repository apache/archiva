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
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * CachedFailuresPolicyTest
 *
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class CachedFailuresPolicyTest
    extends TestCase
{


    @Inject
    private UrlFailureCache urlFailureCache;

    @Inject
    @Named( value = "preDownloadPolicy#cache-failures" )
    DownloadPolicy downloadPolicy;

    private DownloadPolicy lookupPolicy()
        throws Exception
    {
        return downloadPolicy;
    }

    private Path getFile()
    {
        return Paths.get( "target/cache-failures/" + getName() + ".txt" );
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
        Path localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        policy.applyPolicy( CachedFailuresPolicy.NO, request, localFile );
    }

    @Test( expected = PolicyViolationException.class )
    public void testPolicyYes()
        throws Exception
    {

        DownloadPolicy policy = lookupPolicy();
        Path localFile = getFile();
        Properties request = createRequest();
        // make unique name
        String url = "http://a.bad.hostname.maven.org/path/to/resource"+ System.currentTimeMillis() +".txt";
        
        request.setProperty( "url", url );

        // should not fail
        policy.applyPolicy( CachedFailuresPolicy.YES, request, localFile );
        // status Yes Not In cache

        // Yes in Cache
        
        urlFailureCache.cacheFailure( url );

        request.setProperty( "url", url );

        policy.applyPolicy( CachedFailuresPolicy.YES, request, localFile );       
    }
}
