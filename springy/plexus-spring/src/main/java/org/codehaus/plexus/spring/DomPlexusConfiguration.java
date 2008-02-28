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

import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.ATR;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class DomPlexusConfiguration
    implements PlexusConfiguration
{
    private Element root;

    /**
     *
     */
    public DomPlexusConfiguration( Element root )
    {
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
        return root.getAttribute( paramName );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getAttribute(java.lang.String, java.lang.String)
     */
    public String getAttribute( String name, String defaultValue )
    {
        String attribute = root.getAttribute( name );
        if ( attribute == null )
        {
            attribute = defaultValue;
        }
        return attribute;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getAttributeNames()
     */
    public String[] getAttributeNames()
    {
        NamedNodeMap attributes = root.getAttributes();
        String[] names = new String[attributes.getLength()];
        for ( int i = 0; i < names.length; i++ )
        {
            names[i] = attributes.item( i ).getLocalName();
        }
        return names;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(java.lang.String)
     */
    public PlexusConfiguration getChild( String child )
    {
        Element e = DomUtils.getChildElementByTagName( root, child );
        if (e != null)
        {
            return new DomPlexusConfiguration( e );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(int)
     */
    public PlexusConfiguration getChild( int i )
    {
        NodeList childs = root.getChildNodes();
        for ( int j = 0; j < childs.getLength(); j++ )
        {
            Node child = childs.item( j );
            if ( child.getNodeType() == Node.ELEMENT_NODE )
            {
                if (i-- == 0)
                {
                    return new DomPlexusConfiguration( (Element) child );
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChild(java.lang.String, boolean)
     */
    public PlexusConfiguration getChild( String child, boolean createChild )
    {
        PlexusConfiguration config = getChild( child );
        if (config == null && createChild )
        {
            // Creating a new child requires a Document
            throw new UnsupportedOperationException( "Not implemented" );
        }
        return config;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildCount()
     */
    public int getChildCount()
    {
        int count = 0;
        NodeList childs = root.getChildNodes();
        for ( int i = 0; i < childs.getLength(); i++ )
        {
            Node child = childs.item( i );
            if ( child.getNodeType() == Node.ELEMENT_NODE )
            {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildren()
     */
    public PlexusConfiguration[] getChildren()
    {
        int count = getChildCount();
        PlexusConfiguration[] children = new PlexusConfiguration[count];
        for ( int i = 0; i < children.length; i++ )
        {
            children[i] = getChild( i );
        }
        return children;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getChildren(java.lang.String)
     */
    public PlexusConfiguration[] getChildren( String name )
    {
        throw new UnsupportedOperationException( "Not implemented" );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getName()
     */
    public String getName()
    {
        return root.getNodeName();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getValue()
     */
    public String getValue()
        throws PlexusConfigurationException
    {
        return root.getNodeValue();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.configuration.PlexusConfiguration#getValue(java.lang.String)
     */
    public String getValue( String defaultValue )
    {
        String value = root.getTextContent();
        if ( value == null )
        {
            value = defaultValue;
        }
        return value;
    }
}
