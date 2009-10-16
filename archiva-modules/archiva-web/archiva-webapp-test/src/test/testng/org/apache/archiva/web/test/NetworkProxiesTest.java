package org.apache.archiva.web.test;

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

import org.apache.archiva.web.test.parent.AbstractRepositoryTest;
import org.testng.annotations.Test;

@Test( groups = { "networkproxies" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class NetworkProxiesTest 
	extends AbstractRepositoryTest
{
	@Test (dependsOnMethods = { "testDeleteRepositoryGroup" } )
	public void testAddNetworkProxyNullValues()
	{
		goToNetworkProxiesPage();
		addNetworkProxy( "", "", "", "", "", "");
		assertTextPresent( "You must enter an identifier." );
		assertTextPresent( "You must enter a protocol." );
		assertTextPresent( "You must enter a host." );
	}
	
	@Test (dependsOnMethods = { "testAddNetworkProxyNullValues" } )
	public void testAddNetworkProxyNullIdentifier()
	{
		goToNetworkProxiesPage();
		addNetworkProxy( "", "http", "localhost", "8080", "", "");
		assertTextPresent( "You must enter an identifier." );
	}
	
	@Test (dependsOnMethods = { "testAddNetworkProxyNullIdentifier" } )
	public void testAddNetworkProxyNullProtocol()
	{
		goToNetworkProxiesPage();
		addNetworkProxy( "testing123", "", "localhost", "8080", "", "");
		assertTextPresent( "You must enter a protocol." );
	}
	
	@Test (dependsOnMethods = { "testAddNetworkProxyNullProtocol" } )
	public void testAddNetworkProxiesNullHostname()
	{
		goToNetworkProxiesPage();
		addNetworkProxy( "testing123", "http", "", "8080", "", "");
		assertTextPresent( "You must enter a host." );
	}
	
	@Test (dependsOnMethods = { "testAddNetworkProxiesNullHostname" } )
	public void testAddNetworkProxiesValidValues()
	{
		goToNetworkProxiesPage();
		addNetworkProxy( "testing123", "http", "localhost", "8080", "", "");
		assertPage( "Apache Archiva \\ Administration - Network Proxies" );
		assertTextPresent( "testing123" );
	}
	
	@Test (dependsOnMethods = { "testAddNetworkProxiesValidValues" } )
	public void testEditNetworkProxy()
	{
		editNetworkProxies( "proxy.host", "localhost" );
		assertPage( "Apache Archiva \\ Administration - Network Proxies" );
		assertTextPresent( "localhost" );
	}
	
	@Test (dependsOnMethods = { "testEditNetworkProxy" } )
	public void testDeleteNetworkProxy()
	{
		deleteNetworkProxy();
		assertPage( "Apache Archiva \\ Administration - Network Proxies" );
		assertTextPresent( "There are no network proxies configured yet." );
	}
	
	@Test (dependsOnMethods = { "testDeleteNetworkProxy" } )
	public void testAddNetworkProxyAfterDelete()
	{
		addNetworkProxy( "testing123", "http", "localhost", "8080", "", "");
		assertPage( "Apache Archiva \\ Administration - Network Proxies" );
		assertTextPresent( "testing123" );
	}
	
}
