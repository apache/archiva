package org.codehaus.plexus.spring;

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

import junit.framework.TestCase;

import org.springframework.context.ConfigurableApplicationContext;

public class FieldInjectionTest
    extends TestCase
{
    public void testFieldInjectionInSpringContext()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext = new PlexusClassPathXmlApplicationContext( new String[] { "components.xml", "applicationContext.xml" } );
        PlexusBean plexusBean = (PlexusBean) applicationContext.getBean( "plexusBean" );
        assertEquals( PlexusBean.INITIALIZED, plexusBean.getState() );
        assertEquals( "field injection failed", "expected SpringBean", plexusBean.toString() );
        applicationContext.close();
        assertEquals( PlexusBean.DISPOSED, plexusBean.getState() );

    }

    public void testInjectMapForRole()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext = new PlexusClassPathXmlApplicationContext( new String[] { "components.xml", "applicationContext.xml" } );
        ComplexPlexusBean plexusBean = (ComplexPlexusBean) applicationContext.getBean( "complexPlexusBean" );
        assertEquals( "2 components for role org.codehaus.plexus.spring.PlexusBean", plexusBean.toString() );
    }

}
