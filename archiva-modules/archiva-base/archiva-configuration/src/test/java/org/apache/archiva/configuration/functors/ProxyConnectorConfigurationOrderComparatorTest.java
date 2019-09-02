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

import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ProxyConnectorConfigurationOrderComparatorTest
 *
 *
 */
@SuppressWarnings( "deprecation" )
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class ProxyConnectorConfigurationOrderComparatorTest
{
    @Test
    public void testSortOfAllZeros()
    {
        List<ProxyConnectorConfiguration> proxies = new ArrayList<>();

        proxies.add( createConnector( "corporate", 0 ) );
        proxies.add( createConnector( "snapshots", 0 ) );
        proxies.add( createConnector( "3rdparty", 0 ) );
        proxies.add( createConnector( "sandbox", 0 ) );

        Collections.sort( proxies, ProxyConnectorConfigurationOrderComparator.getInstance() );

        assertProxyOrder( new String[]{ "corporate", "snapshots", "3rdparty", "sandbox" }, proxies );
    }

    @Test
    public void testSortNormal()
    {
        List<ProxyConnectorConfiguration> proxies = new ArrayList<>();

        proxies.add( createConnector( "corporate", 3 ) );
        proxies.add( createConnector( "snapshots", 1 ) );
        proxies.add( createConnector( "3rdparty", 2 ) );
        proxies.add( createConnector( "sandbox", 4 ) );

        Collections.sort( proxies, new ProxyConnectorConfigurationOrderComparator() );

        assertProxyOrder( new String[]{ "snapshots", "3rdparty", "corporate", "sandbox" }, proxies );
    }

    @Test
    public void testSortPartial()
    {
        List<ProxyConnectorConfiguration> proxies = new ArrayList<>();

        proxies.add( createConnector( "corporate", 3 ) );
        proxies.add( createConnector( "snapshots", 0 ) );
        proxies.add( createConnector( "3rdparty", 2 ) );
        proxies.add( createConnector( "sandbox", 0 ) );

        Collections.sort( proxies, new ProxyConnectorConfigurationOrderComparator() );

        assertProxyOrder( new String[]{ "3rdparty", "corporate", "snapshots", "sandbox" }, proxies );
    }

    private void assertProxyOrder( String[] ids, List<ProxyConnectorConfiguration> proxies )
    {
        assertEquals( "Proxies.size() == ids.length", ids.length, proxies.size() );

        int orderFailedAt = -1;

        for ( int i = 0; i < ids.length; i++ )
        {
            if ( !StringUtils.equals( ids[i], proxies.get( i ).getProxyId() ) )
            {
                orderFailedAt = i;
                break;
            }
        }

        if ( orderFailedAt >= 0 )
        {
            StringBuilder msg = new StringBuilder();

            msg.append( "Failed expected order of the proxies <" );
            msg.append( StringUtils.join( ids, ", " ) );
            msg.append( ">, actual <" );

            boolean needsComma = false;
            for ( ProxyConnectorConfiguration proxy : proxies )
            {
                if ( needsComma )
                {
                    msg.append( ", " );
                }
                msg.append( proxy.getProxyId() );
                needsComma = true;
            }
            msg.append( "> failure at index <" ).append( orderFailedAt ).append( ">." );

            fail( msg.toString() );
        }
    }

    private ProxyConnectorConfiguration createConnector( String id, int order )
    {
        ProxyConnectorConfiguration proxy = new ProxyConnectorConfiguration();
        proxy.setProxyId( id );
        proxy.setOrder( order );
        proxy.setSourceRepoId( id + "_m" );
        proxy.setTargetRepoId( id + "_r" );

        return proxy;
    }
}
