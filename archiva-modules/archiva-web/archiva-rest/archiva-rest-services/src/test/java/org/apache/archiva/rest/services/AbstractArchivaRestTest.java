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


import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.apache.archiva.rest.api.services.PingService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.maven.archiva.common.utils.FileUtil;
import org.codehaus.redback.rest.services.AbstractRestServicesTest;

import java.io.File;

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

    protected String getRestServicesPath()
    {
        return "restServices";
    }

    protected RepositoriesService getRepositoriesService()
    {
        return getRepositoriesService( null );
    }


    protected RepositoriesService getRepositoriesService( String authzHeader )
    {
        RepositoriesService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       RepositoriesService.class );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        return service;

    }

    protected ManagedRepositoriesService getManagedRepositoriesService( String authzHeader )
    {
        ManagedRepositoriesService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       ManagedRepositoriesService.class );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        return service;

    }

    protected PingService getPingService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/", PingService.class );
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
            RemoteRepositoriesService.class );


    }

    protected RepositoryGroupService getRepositoryGroupService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
            RepositoryGroupService.class );
    }

    protected ProxyConnectorService getProxyConnectorService()
    {
        ProxyConnectorService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       ProxyConnectorService.class );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected NetworkProxyService getNetworkProxyService()
    {
        NetworkProxyService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       NetworkProxyService.class );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected ArchivaAdministrationService getArchivaAdministrationService()
    {
        ArchivaAdministrationService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       ArchivaAdministrationService.class );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected SearchService getSearchService( String authzHeader )
    {
        SearchService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/archivaServices/",
                                       SearchService.class );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        return service;

    }

    protected ManagedRepository getTestManagedRepository()
    {
        String location = new File( FileUtil.getBasedir(), "target/test-repo" ).getAbsolutePath();
        return new ManagedRepository( "TEST", "test", location, "default", true, true, false, "2 * * * * ?", null,
                                      false, 2, 3, true, false );

    }
}
