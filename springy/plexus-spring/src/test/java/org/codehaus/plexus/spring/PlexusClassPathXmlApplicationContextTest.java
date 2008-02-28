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

public class PlexusClassPathXmlApplicationContextTest
    extends TestCase
{
    /**
     * {@inheritDoc}
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        System.setProperty( "plexus-spring.debug", "true" );
    }

    public void testInjectSpringBeansInPlexusComponent()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "testInjectSpringBeansInPlexusComponent.xml",
                "testInjectSpringBeansInPlexusComponent-context.xml" } );
        PlexusBean plexusBean = (PlexusBean) applicationContext.getBean( "plexusBean" );
        assertEquals( "field injection failed", "expected SpringBean", plexusBean.describe() );
        applicationContext.close();
    }

    public void testPlexusLifecycleSupport()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "testPlexusLifecycleSupport.xml" } );
        PlexusBean plexusBean = (PlexusBean) applicationContext.getBean( "plexusBean" );
        assertEquals( PlexusBean.INITIALIZED, plexusBean.getState() );
        assertNotNull( plexusBean.getContext() );
        assertNotNull( plexusBean.getLogger() );
        applicationContext.close();
        assertEquals( PlexusBean.DISPOSED, plexusBean.getState() );

    }

    public void testInjectMapForRole()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "testInjectMapForRole.xml",
                "testInjectMapForRole-context.xml" } );
        ComplexPlexusBean plexusBean = (ComplexPlexusBean) applicationContext.getBean( "complexPlexusBean" );
        assertTrue( plexusBean.getBeans().containsKey( "spring" ) );
        assertTrue( plexusBean.getBeans().containsKey( "plexus" ) );
        assertEquals( "2 components for role org.codehaus.plexus.spring.PlexusBean", plexusBean.toString() );
    }

    public void testInjectListForRole()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "testInjectListForRole.xml",
                "testInjectListForRole-context.xml" } );
        ComplexPlexusBean plexusBean = (ComplexPlexusBean) applicationContext.getBean( "complexPlexusBean" );
        assertEquals( 2, plexusBean.getBeansList().size() );
    }

    public void testInjectPlexusConfiguration()
        throws Exception
    {
        ConfigurableApplicationContext applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "testInjectPlexusConfiguration.xml" } );
        ConfigPlexusBean plexusBean = (ConfigPlexusBean) applicationContext.getBean( "plexusBean" );
        assertEquals( "expected", plexusBean.getConfig().getChild( "xml" ).getAttribute( "test" ) );
    }

}
