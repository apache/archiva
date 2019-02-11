package org.apache.archiva.configuration;

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

/**
 * Class ProxyConnectorConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ProxyConnectorConfiguration
    extends AbstractRepositoryConnectorConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The order of the proxy connectors. (0 means no
     * order specified)
     *           .
     */
    private int order = 0;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the order of the proxy connectors. (0 means no order
     * specified).
     * 
     * @return int
     */
    public int getOrder()
    {
        return this.order;
    } //-- int getOrder()

    /**
     * Set the order of the proxy connectors. (0 means no order
     * specified).
     * 
     * @param order
     */
    public void setOrder( int order )
    {
        this.order = order;
    } //-- void setOrder( int )

    
    /**
     * The order id for UNORDERED
     */
    public static final int UNORDERED = 0;

    /**
     * The policy key {@link #getPolicies()} for error handling.
     * See {@link org.apache.archiva.policies.DownloadErrorPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_PROPAGATE_ERRORS = "propagate-errors";

    /**
     * The policy key {@link #getPolicies()} for error handling when an artifact is present.
     * See {@link org.apache.archiva.policies.DownloadErrorPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_PROPAGATE_ERRORS_ON_UPDATE = "propagate-errors-on-update";

    /**
     * The policy key {@link #getPolicies()} for snapshot handling.
     * See {@link org.apache.archiva.policies.SnapshotsPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_SNAPSHOTS = "snapshots";

    /**
     * The policy key {@link #getPolicies()} for releases handling.
     * See {@link org.apache.archiva.policies.ReleasesPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_RELEASES = "releases";

    /**
     * The policy key {@link #getPolicies()} for checksum handling.
     * See {@link org.apache.archiva.policies.ChecksumPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_CHECKSUM = "checksum";

    /**
     * The policy key {@link #getPolicies()} for cache-failures handling.
     * See {@link org.apache.archiva.policies.CachedFailuresPolicy}
     * for details on potential values to this policy key.
     */
    public static final String POLICY_CACHE_FAILURES = "cache-failures";
          
}
