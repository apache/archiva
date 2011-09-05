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


import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.PingService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.codehaus.redback.rest.services.AbstractRestServicesTest;

/**
 * @author Olivier Lamy
 */
public abstract class AbstractArchivaRestTest
    extends AbstractRestServicesTest
{
    public String guestAuthzHeader =
        "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "guest" + ":" ).getBytes() );

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml,classpath:META-INF/spring-context-test.xml";
    }

    protected RepositoriesService getRepositoriesService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          RepositoriesService.class );

    }

    protected ManagedRepositoriesService getManagedRepositoriesService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          ManagedRepositoriesService.class );

    }

    protected PingService getPingService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          PingService.class );
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          RemoteRepositoriesService.class );


    }
}
