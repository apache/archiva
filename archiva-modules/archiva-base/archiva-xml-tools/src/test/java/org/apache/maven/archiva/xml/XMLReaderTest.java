package org.apache.maven.archiva.xml;

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

import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * XMLReaderTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class XMLReaderTest
    extends AbstractArchivaXmlTestCase
{
    private void assertElementTexts( List elementList, String[] expectedTexts )
    {
        assertEquals( "Element List Size", expectedTexts.length, elementList.size() );

        List texts = new ArrayList();
        for ( Iterator iter = elementList.iterator(); iter.hasNext(); )
        {
            Element element = (Element) iter.next();
            texts.add( element.getTextTrim() );
        }

        for ( int i = 0; i < expectedTexts.length; i++ )
        {
            String expectedText = expectedTexts[i];
            assertTrue( "Contains [" + expectedText + "]", texts.contains( expectedText ) );
        }
    }

    public void testNoPrologBasicRead()
        throws XMLException
    {
        File xmlFile = getExampleXml( "no-prolog-basic.xml" );
        XMLReader reader = new XMLReader( "basic", xmlFile );

        List fruits = reader.getElementList( "//basic/fruits/fruit" );
        assertElementTexts( fruits, new String[] { "apple", "cherry", "pear", "peach" } );
    }

    public void testNoPrologEntitiesRead()
        throws XMLException
    {
        File xmlFile = getExampleXml( "no-prolog-with-entities.xml" );
        XMLReader reader = new XMLReader( "basic", xmlFile );

        List names = reader.getElementList( "//basic/names/name" );
        assertElementTexts( names, new String[] { TRYGVIS, INFINITE_ARCHIVA } );
    }

    public void testNoPrologUtf8Read()
        throws XMLException
    {
        File xmlFile = getExampleXml( "no-prolog-with-utf8.xml" );
        XMLReader reader = new XMLReader( "basic", xmlFile );

        List names = reader.getElementList( "//basic/names/name" );
        assertElementTexts( names, new String[] { TRYGVIS, INFINITE_ARCHIVA } );
    }

    public void testPrologUtf8Read()
        throws XMLException
    {
        File xmlFile = getExampleXml( "prolog-with-utf8.xml" );
        XMLReader reader = new XMLReader( "basic", xmlFile );

        List names = reader.getElementList( "//basic/names/name" );
        assertElementTexts( names, new String[] { TRYGVIS, INFINITE_ARCHIVA } );
    }
    
    // MRM-1136 or MRM-1152
    public void testProxiedMetadataRead()
        throws XMLException
    {
        File xmlFile = getExampleXml( "maven-metadata-codehaus-snapshots.xml" );
        XMLReader reader = new XMLReader( "metadata", xmlFile );        
        reader.removeNamespaces();
        
        Element groupId = reader.getElement( "//metadata/groupId" );        
        assertNotNull( groupId );
        assertEquals( "org.codehaus.mojo", groupId.getTextTrim() );   
    }

}
