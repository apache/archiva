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
import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.policies.urlcache.UrlFailureCache;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.MissingResourceException;
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

    private FilesystemStorage filesystemStorage;

    @Inject
    @Named( value = "preDownloadPolicy#cache-failures" )
    DownloadPolicy downloadPolicy;

    private DownloadPolicy lookupPolicy()
        throws Exception
    {
        return downloadPolicy;
    }

    private StorageAsset getFile() throws IOException {
        if (filesystemStorage==null) {
            filesystemStorage = new FilesystemStorage(Paths.get("target/cache-failures"), new DefaultFileLockManager());
        }
        return filesystemStorage.getAsset( getName() + ".txt" );
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
        StorageAsset localFile = getFile();
        Properties request = createRequest();

        request.setProperty( "url", "http://a.bad.hostname.maven.org/path/to/resource.txt" );

        policy.applyPolicy( CachedFailuresPolicy.NO, request, localFile );
    }

    @Test( expected = PolicyViolationException.class )
    public void testPolicyYes()
        throws Exception
    {

        DownloadPolicy policy = lookupPolicy();
        StorageAsset localFile = getFile();
        Properties request = createRequest();
        // make unique name
        String url = "http://a.bad.hostname.maven.org/path/to/resource"+ System.currentTimeMillis() +".txt";
        
        request.setProperty( "url", url );

        // should not fail
        try {
            policy.applyPolicy(CachedFailuresPolicy.YES, request, localFile);
        } catch (PolicyViolationException e) {
            // Converting to runtime exception, because it should be thrown later
            throw new RuntimeException(e);
        }
        // status Yes Not In cache

        // Yes in Cache
        
        urlFailureCache.cacheFailure( url );

        request.setProperty( "url", url );

        policy.applyPolicy( CachedFailuresPolicy.YES, request, localFile );       
    }

    @Test
    public void testNamesAndDescriptions() throws Exception {
        DownloadPolicy policy = lookupPolicy();
        assertEquals("Cache Failures Policy", policy.getName());
        assertTrue(policy.getDescription(Locale.US).contains("if download failures will be cached"));
        assertEquals("Yes", policy.getOptionName(Locale.US, StandardOption.YES));
        assertEquals("No", policy.getOptionName(Locale.US, StandardOption.NO));
        assertTrue(policy.getOptionDescription(Locale.US, StandardOption.YES).contains("failures are cached and download is not attempted"));
        assertTrue(policy.getOptionDescription(Locale.US, StandardOption.NO).contains("failures are not cached"));
        try {
            policy.getOptionName(Locale.US, StandardOption.NOOP);
            // Exception should be thrown
            assertTrue(false);
        } catch (MissingResourceException e) {
            //
        }

    }
}
