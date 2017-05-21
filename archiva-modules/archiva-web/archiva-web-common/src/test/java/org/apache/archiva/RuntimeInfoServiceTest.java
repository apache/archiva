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
import org.apache.archiva.web.api.RuntimeInfoService;
import org.apache.archiva.web.model.ApplicationRuntimeInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.runner.RunWith;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class RuntimeInfoServiceTest
    extends AbstractRestServicesTest
{
    @Override
    @Before
    public void startServer()
        throws Exception
    {
        File appServerBase = new File( System.getProperty( "appserver.base" ) );

        File jcrDirectory = new File( appServerBase, "jcr" );

        if ( jcrDirectory.exists() )
        {
            FileUtils.deleteDirectory( jcrDirectory );
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
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + port : baseUrlSysProps;
    }

    @Test
    public void runtimeInfoService()
        throws Exception
    {
        RuntimeInfoService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaUiServices/",
                                       RuntimeInfoService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        ApplicationRuntimeInfo applicationRuntimeInfo = service.getApplicationRuntimeInfo( "en" );

        assertEquals( System.getProperty( "expectedVersion" ), applicationRuntimeInfo.getVersion() );
        assertFalse( applicationRuntimeInfo.isJavascriptLog() );
        assertTrue( applicationRuntimeInfo.isLogMissingI18n() );

    }
}
