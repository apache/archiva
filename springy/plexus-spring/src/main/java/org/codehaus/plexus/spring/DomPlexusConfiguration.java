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

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class DomPlexusConfiguration
    implements PlexusConfiguration
{
    private Element root;

    private String name;

    /**
     *
     */
    public DomPlexusConfiguration( String name, Element root )
    {
        this.name = name;
        this.root = root;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#addChild(org.codehaus.plexus.configuration.PlexusConfiguration)
     */
    public void addChild( PlexusConfiguration configuration )
    {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getAttribute(java.lang.String)
     */
    public String getAttribute( String paramName )
        throws PlexusConfigurationException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getAttribute(java.lang.String, java.lang.String)
     */
    public String getAttribute( String name, String defaultValue )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getAttributeNames()
     */
    public String[] getAttributeNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(java.lang.String)
     */
    public PlexusConfiguration getChild( String child )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(int)
     */
    public PlexusConfiguration getChild( int i )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(java.lang.String, boolean)
     */
    public PlexusConfiguration getChild( String child, boolean createChild )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildCount()
     */
    public int getChildCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildren()
     */
    public PlexusConfiguration[] getChildren()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildren(java.lang.String)
     */
    public PlexusConfiguration[] getChildren( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getName()
     */
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getValue()
     */
    public String getValue()
        throws PlexusConfigurationException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getValue(java.lang.String)
     */
    public String getValue( String defaultValue )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
