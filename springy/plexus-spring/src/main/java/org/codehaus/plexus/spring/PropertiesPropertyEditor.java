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
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PropertiesPropertyEditor
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
        registry.registerCustomEditor( Properties.class, this );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Support for plexus properties injection
     * <pre>
     * &lt;fieldName>
     *     &lt;property&gt;
     *       &lt;name&gt;key&lt;/name&gt;
     *       &lt;value&gt;true&lt;/value&gt;
     *     &lt;/property&gt;
     * ...
     * </pre>
     *
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    public void setAsText( String text )
        throws IllegalArgumentException
    {
        if ( StringUtils.isBlank( text ) )
        {
            setValue( null );
            return;
        }
        Properties properties = new Properties();
        try
        {
            SAXReader reader = new SAXReader();
            Document doc = reader.read( new StringReader( text ) );
            Element root = doc.getRootElement();
            for ( Iterator i = root.elementIterator(); i.hasNext(); )
            {
                Element element = (Element) i.next();
                properties.setProperty( element.element( "name" ).getText(), element.element( "value" ).getText() );
            }
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Failed to convert to Properties", e );
        }
        setValue( properties );
    }

}
