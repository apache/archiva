package org.apache.archiva.xml;

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

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringWriter;

/**
 * XMLWriterTest 
 *
 *
 */
public class XMLWriterTest
    extends AbstractArchivaXmlTestCase
{
    @Test
    public void testWrite()
        throws Exception
    {
        StringBuilder expected = new StringBuilder();

        expected.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
        expected.append( "<basic>\n" );
        expected.append( "  <names>\n" );
        expected.append( "    <name>" ).append( TRYGVIS ).append( "</name>\n" );
        expected.append( "    <name>" ).append( INFINITE_ARCHIVA ).append( "</name>\n" );
        expected.append( "  </names>\n" );
        expected.append( "</basic>\n" );

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element basic = doc.createElement("basic");
        doc.appendChild(basic);
        Element names = doc.createElement( "names" );
        basic.appendChild(names);
        Element name = doc.createElement("name");
        name.setTextContent(TRYGVIS);
        names.appendChild(name);
        name = doc.createElement("name");
        name.setTextContent(INFINITE_ARCHIVA);

        names.appendChild(name);

        StringWriter actual = new StringWriter();
        XMLWriter.write( doc, actual );

        assertEquals( "Comparision of contents:", expected.toString(), actual.toString() );
    }
}
