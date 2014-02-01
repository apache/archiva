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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test( groups = { "proxyConnector" } )
public class ProxyConnectorTest
	extends AbstractRepositoryTest
{
    @BeforeTest
    public void setUp()
    {
        loginAsAdmin();
    }

    @AfterSuite
    public void tearDown()
    {
        deleteProxyConnector( "snapshots", "central", false );
        enableProxyConnector( "internal", "central", false );
    }

    public void add()
    {
        addProxyConnector( "snapshots", "central" );
    }

    @Test(dependsOnMethods = "add")
    public void edit()
    {
        editProxyConnector( "snapshots", "central" );
    }

    @Test(dependsOnMethods = "edit")
    public void delete()
    {
        deleteProxyConnector( "snapshots", "central", true );
    }

    public void disable()
    {
        disableProxyConnector( "internal", "central", true );
    }

    @Test(dependsOnMethods = "disable")
    public void enable()
    {
        enableProxyConnector( "internal", "central", true );
    }
}
