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
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class PingServiceTest
    extends AbstractArchivaRestTest
{


    @Test
    public void ping()
        throws Exception
    {
        // 1000000L
        //WebClient.getConfig( userService ).getHttpConduit().getClient().setReceiveTimeout(3000);

        String res = getPingService().ping();
        assertEquals( "Yeah Baby It rocks!", res );
    }

    @Test( expected = ServerWebApplicationException.class )
    public void pingWithAuthzFailed()
        throws Exception
    {

        try
        {
            String res = getPingService().pingWithAuthz();
            fail( "not in exception" );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }
    }

    @Test
    public void pingWithAuthz()
        throws Exception
    {

        PingService service = getPingService();
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        WebClient.client( service ).header( "Authorization", authorizationHeader );
        String res = service.pingWithAuthz();
        assertEquals( "Yeah Baby It rocks!", res );
    }

    @Ignore( "FIXME guest failed ???" )
    public void pingWithAuthzGuest()
        throws Exception
    {

        PingService service = getPingService();
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        WebClient.client( service ).header( "Authorization", guestAuthzHeader );
        String res = service.pingWithAuthz();
        assertEquals( "Yeah Baby It rocks!", res );
    }
}
