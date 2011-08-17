package org.apache.archiva.rest.services;

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

import org.apache.archiva.rest.api.services.PingService;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.codehaus.redback.rest.services.AbstractRestServicesTest;
import org.codehaus.redback.rest.services.FakeCreateAdminService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class PingServiceTest
    extends AbstractRestServicesTest
{

    @Before
    public void setUp()
        throws Exception
    {
        super.startServer();

        FakeCreateAdminService fakeCreateAdminService =
            JAXRSClientFactory.create( "http://localhost:" + port + "/services/fakeCreateAdminService/",
                                       FakeCreateAdminService.class );

        Boolean res = fakeCreateAdminService.createAdminIfNeeded();
        assertTrue( res.booleanValue() );
    }

    PingService getPingService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          PingService.class );

    }


    @Test
    public void ping()
        throws Exception
    {
        // 1000000L
        //WebClient.getConfig( userService ).getHttpConduit().getClient().setReceiveTimeout(3000);

        String res = getPingService().ping();
        assertEquals( "Yeah Baby It rocks!", res );
    }
}
