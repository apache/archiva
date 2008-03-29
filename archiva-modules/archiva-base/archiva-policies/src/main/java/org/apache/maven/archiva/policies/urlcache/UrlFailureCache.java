package org.apache.maven.archiva.policies.urlcache;

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


/**
 * Cache for requested URLs that cannot be fetched. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface UrlFailureCache
{
    /**
     * Store a URL in the cache as failed.
     * 
     * @param url the url to store. 
     */
    public void cacheFailure( String url );
    
    /**
     * Test if a specified URL has failed before.
     * 
     * NOTE: If the provided URL has failed, then making this call 
     * should refresh the expiration time on that URL entry.
     * 
     * @param url the URL to test.
     * @return true if it has failed before, false if not.
     */
    public boolean hasFailedBefore( String url );
}
