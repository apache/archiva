package org.apache.maven.repository.proxy.web.actionmapper.test;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.webwork.dispatcher.mapper.ActionMapping;
import org.apache.maven.repository.proxy.web.action.test.stub.HttpServletRequestStub;
import org.apache.maven.repository.proxy.web.actionmapper.RepositoryProxyActionMapper;
import org.codehaus.plexus.PlexusTestCase;

public class RepositoryProxyActionMapperTest
    extends PlexusTestCase
{
    RepositoryProxyActionMapper actionMapper;

    public void setUp()
        throws Exception
    {
        actionMapper = new RepositoryProxyActionMapper();
    }

    // TODO: uncomment once we know how to make the default action mapper work using stubs
    //    public void testDefaultActionMapping()
    //    throws Exception
    //    {
    //        ActionMapping mapping = actionMapper.getMapping( new DefaultActionMapperRequestStub() );
    //
    //        String expectedNamespace = "test";
    //        String expectedName = "test";
    //
    //        assertNotNull( "ActionMapping is null", mapping );
    //        assertNotNull( "namespace is null", mapping.getNamespace() );
    //        assertNotNull( "name is null", mapping.getName() );
    //        assertTrue( "invalid namespace: " + mapping.getNamespace(), mapping.getNamespace().equals( expectedNamespace ) );
    //        assertTrue( "invalid name: " + mapping.getName(), mapping.getName().equals( expectedName ) );
    //    }

    public void testRepositoryProxyActionMapping()
        throws Exception
    {
        String testDir = getBasedir() + "/target/test-classes/unit/proxy-test";

        actionMapper.setConfigfile( testDir + "/maven-proxy-complete.conf" );

        ActionMapping mapping = actionMapper.getMapping( new HttpServletRequestStub() );
        String expectedName = "proxy";
        String expectedFile = "org/sometest/artifact-0.0.jar";

        assertNotNull( "ActionMapping is null", mapping );
        assertNotNull( "name is null", mapping.getName() );

        String mappingName = mapping.getName();
        String requestedFile = (String) mapping.getParams().get( "requestedFile" );

        assertTrue( "invalid name: " + mappingName, mappingName.equals( expectedName ) );
        assertTrue( "invalid parameter: " + requestedFile, requestedFile.equals( expectedFile ) );
    }

    public void tearDown()
        throws Exception
    {
        // do nothing
    }
}
