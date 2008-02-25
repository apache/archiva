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

import java.io.File;

import junit.framework.TestCase;

import org.springframework.context.ConfigurableApplicationContext;

;

/**
 * Mimic org.codehaus.plexus.PlexusTestCase as simple replacement for test
 * cases.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusInSpringTestCase
    extends TestCase
{
    private static String basedir;

    private ConfigurableApplicationContext applicationContext;

    protected void setUp()
        throws Exception
    {
        basedir = getBasedir();
        applicationContext =
            new PlexusClassPathXmlApplicationContext( new String[] {
                "classpath*:META-INF/plexus/components.xml",
                "classpath*:" + getPlexusConfigLocation(),
                "classpath*:" + getSpringConfigLocation()} );
    }

    protected String getSpringConfigLocation()
        throws Exception
    {
        return getClass().getName().replace( '.', '/' ) + "-context.xml";
    }

    protected String getPlexusConfigLocation()
        throws Exception
    {
        return getClass().getName().replace( '.', '/' ) + ".xml";
    }

    /**
     * {@inheritDoc}
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        if ( applicationContext != null )
        {
            applicationContext.close();
        }
    }

    public static String getBasedir()
    {
        if ( basedir != null )
        {
            return basedir;
        }

        basedir = System.getProperty( "basedir" );
        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }

    public String getTestConfiguration()
    {
        return getTestConfiguration( getClass() );
    }

    public static String getTestConfiguration( Class clazz )
    {
        String s = clazz.getName().replace( '.', '/' );

        return s.substring( 0, s.indexOf( "$" ) ) + ".xml";
    }

    public Object lookup( Class role )
    {
        return lookup( role, null );
    }

    public Object lookup( Class role, String roleHint )
    {
        return lookup( role.getName(), roleHint );

    }

    public Object lookup( String role )
    {
        return lookup( role, null );
    }

    public Object lookup( String role, String roleHint )
    {
        return applicationContext.getBean( PlexusToSpringUtils.buildSpringId( role, roleHint ) );
    }

    public static File getTestFile( String path )
    {
        return new File( getBasedir(), path );
    }

    public static File getTestFile( String basedir,
                                    String path )
    {
        File basedirFile = new File( basedir );

        if ( !basedirFile.isAbsolute() )
        {
            basedirFile = getTestFile( basedir );
        }

        return new File( basedirFile, path );
    }

    public static String getTestPath( String path )
    {
        return getTestFile( path ).getAbsolutePath();
    }

    public static String getTestPath( String basedir,
                                      String path )
    {
        return getTestFile( basedir, path ).getAbsolutePath();
    }

    protected ConfigurableApplicationContext getApplicationContext()
    {
        return applicationContext;
    }
}
