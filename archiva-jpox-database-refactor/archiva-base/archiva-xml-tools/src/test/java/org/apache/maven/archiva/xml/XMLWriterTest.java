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

import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.StringWriter;

/**
 * XMLWriterTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class XMLWriterTest
    extends AbstractArchivaXmlTestCase
{
    public void testWrite()
        throws Exception
    {
        String expectedResults = FileUtils.fileRead( getExampleXml( "prolog-with-utf8.xml" ) );

        Element basic = DocumentHelper.createElement( "basic" );
        Document doc = DocumentHelper.createDocument( basic );

        Element names = basic.addElement( "names" );
        names.addElement( "name" ).setText( TRYGVIS );
        names.addElement( "name" ).setText( INFINITE_ARCHIVA );

        StringWriter actual = new StringWriter();
        XMLWriter.write( doc, actual );

        assertEquals( "Comparision of contents:", expectedResults, actual.toString() );
    }
}
