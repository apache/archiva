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

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusConfigurationPropertyEditor
    extends PropertyEditorSupport
    implements PropertyEditorRegistrar
{

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
     */
    public void registerCustomEditors( PropertyEditorRegistry registry )
    {
        registry.registerCustomEditor( PlexusConfiguration.class, this );
    }

    /**
     * {@inheritDoc}
     *
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    public void setAsText( String text )
        throws IllegalArgumentException
    {
        if (StringUtils.isBlank( text ))
        {
            setValue( null );
            return;
        }
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( new StringReader( text ) );
            XmlPlexusConfiguration configuration = new XmlPlexusConfiguration( dom );
            setValue( configuration );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Failed to convert to Plexus XML configuration", e );
        }
    }

}
