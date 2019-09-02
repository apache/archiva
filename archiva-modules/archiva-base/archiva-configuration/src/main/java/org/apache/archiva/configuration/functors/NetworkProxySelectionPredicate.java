package org.apache.archiva.configuration.functors;

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

import org.apache.archiva.configuration.NetworkProxyConfiguration;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * NetworkProxySelectionPredicate
 *
 *
 */
public class NetworkProxySelectionPredicate
    implements Predicate
{
    private String proxyId;

    public NetworkProxySelectionPredicate( String id )
    {
        this.proxyId = id;
    }

    @Override
    public boolean evaluate( Object object )
    {
        boolean satisfies = false;

        if ( object instanceof NetworkProxyConfiguration )
        {
            NetworkProxyConfiguration proxy = (NetworkProxyConfiguration) object;
            return ( StringUtils.equals( proxyId, proxy.getId() ) );
        }

        return satisfies;
    }
}
