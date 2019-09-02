package org.apache.archiva;
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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.apache.archiva.remotedownload.DownloadArtifactsTest;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.archiva.web.api.RuntimeInfoService;
import org.apache.archiva.web.model.ApplicationRuntimeInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class RuntimeInfoServiceTest
    extends AbstractRestServicesTest
{

    private static Path appServerBase;
    private static String previousAppServerBase;

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        appServerBase = Files.createTempDirectory( "archiva-common-web_appsrvrt_" );
        System.setProperty( "appserver.base", appServerBase.toString( ) );
    }

    @AfterClass
    public static void resetAppServerBase()
    {
        if (Files.exists(appServerBase)) {
            FileUtils.deleteQuietly( appServerBase.toFile() );
        }
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    @Before
    public void startServer()
        throws Exception
    {
        Path jcrDirectory =  appServerBase.resolve( "jcr" );

        if ( Files.exists(jcrDirectory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( jcrDirectory );
        }

        super.startServer();
    }

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml,classpath:/spring-context-with-jcr.xml";
    }

    @Override
    protected String getRestServicesPath()
    {
        return "restServices";
    }

    protected String getBaseUrl()
    {
        String baseUrlSysProps = System.getProperty( "archiva.baseRestUrl" );
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + getServerPort() : baseUrlSysProps;
    }

    @Test
    public void runtimeInfoService()
        throws Exception
    {
        RuntimeInfoService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaUiServices/",
                                       RuntimeInfoService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client(service).header("Referer","http://localhost");
        ApplicationRuntimeInfo applicationRuntimeInfo = service.getApplicationRuntimeInfo( "en" );

        assertEquals( System.getProperty( "expectedVersion" ), applicationRuntimeInfo.getVersion() );
        assertFalse( applicationRuntimeInfo.isJavascriptLog() );
        assertTrue( applicationRuntimeInfo.isLogMissingI18n() );

    }
}
