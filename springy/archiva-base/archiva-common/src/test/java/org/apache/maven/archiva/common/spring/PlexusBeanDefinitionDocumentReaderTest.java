package org.apache.maven.archiva.common.spring;

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

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusBeanDefinitionDocumentReaderTest
    extends TestCase
{

    public void testXslt()
        throws Exception
    {
        URL plexus = getClass().getResource( "components.xml" );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse( plexus.openStream() );

        PlexusBeanDefinitionDocumentReader reader = new PlexusBeanDefinitionDocumentReader();
        doc = reader.convertPlexusDescriptorToSpringBeans( doc );

        new XMLWriter( System.out, OutputFormat.createPrettyPrint() ).write( new DOMReader().read( doc ) );
    }

    /**
     * Test conversion from a typical Plexus components descriptor to a spring beanFactory
     * @throws Exception
     */
    public void testConvertPlexusToSpring()
        throws Exception
    {
        URL plexus = getClass().getResource( "components.xml" );
        PlexusBeanFactory factory = new PlexusBeanFactory( new UrlResource( plexus ) );
        assertEquals( 2, factory.getBeanDefinitionCount() );

        BeanDefinition bd = factory.getBeanDefinition( "org.apache.maven.archiva.configuration.ArchivaConfiguration" );
        assertEquals( "org.apache.maven.archiva.configuration.DefaultArchivaConfiguration", bd.getBeanClassName() );
        assertEquals( "prototype", bd.getScope() );
        assertEquals( 5, bd.getPropertyValues().size() );
    }
}
