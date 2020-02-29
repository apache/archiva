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
import java.util.*;

/**
 * CachedFailuresPolicyTest
 *
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class PropagateErrorsDownloadPolicyTest
    extends TestCase
{


    @Inject
    private UrlFailureCache urlFailureCache;

    private FilesystemStorage filesystemStorage;

    @Inject
    @Named( value = "downloadErrorPolicy#propagate-errors" )
    DownloadErrorPolicy downloadPolicy;

    private DownloadErrorPolicy lookupPolicy()
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
    public void testPolicyStop()
        throws Exception
    {
        DownloadErrorPolicy policy = lookupPolicy();
        StorageAsset localFile = getFile();
        Properties request = createRequest();

        Exception ex = new RuntimeException();
        Map<String, Exception> exMap = new HashMap<>();

        assertTrue(policy.applyPolicy( PropagateErrorsDownloadPolicy.STOP, request, localFile, ex, exMap ));
    }

    @Test
    public void testPolicyQueue()
            throws Exception
    {
        DownloadErrorPolicy policy = lookupPolicy();
        StorageAsset localFile = getFile();
        Properties request = createRequest();

        Exception ex = new RuntimeException();
        Map<String, Exception> exMap = new HashMap<>();

        assertTrue(policy.applyPolicy( PropagateErrorsDownloadPolicy.QUEUE, request, localFile, ex, exMap ));
    }

    @Test
    public void testPolicyIgnore()
            throws Exception
    {
        DownloadErrorPolicy policy = lookupPolicy();
        StorageAsset localFile = getFile();
        Properties request = createRequest();

        Exception ex = new RuntimeException();
        Map<String, Exception> exMap = new HashMap<>();

        assertFalse(policy.applyPolicy( PropagateErrorsDownloadPolicy.IGNORE, request, localFile, ex, exMap ));
    }

    @Test
    public void testNamesAndDescriptions() throws Exception {

        DownloadErrorPolicy policy = lookupPolicy();
        assertEquals("Propagate Download Errors Policy", policy.getName());
        assertTrue(policy.getDescription(Locale.US).contains("error occurs during download"));
        assertEquals("Stop on error", policy.getOptionName(Locale.US, DownloadErrorOption.STOP));
        assertEquals("Continue on error", policy.getOptionName(Locale.US, DownloadErrorOption.QUEUE));
        assertEquals("Ignore errors", policy.getOptionName(Locale.US, DownloadErrorOption.IGNORE));
        assertTrue(policy.getOptionDescription(Locale.US, DownloadErrorOption.STOP).contains("Stops the artifact download"));
        assertTrue(policy.getOptionDescription(Locale.US, DownloadErrorOption.QUEUE).contains("Checks further"));
        assertTrue(policy.getOptionDescription(Locale.US, DownloadErrorOption.IGNORE).contains("not found"));
        try {
            policy.getOptionName(Locale.US, StandardOption.NOOP);
            // Exception should be thrown
            assertTrue(false);
        } catch (MissingResourceException e) {
            //
        }

    }
}
