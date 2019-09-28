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

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlUtil {


    public static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    public static Element addChild(Document doc, Element parent, String name) {
        Element el = doc.createElement(name);
        parent.appendChild(el);
        return el;
    }

    public static Element addChild(Element parent, String name) {
        Document doc = parent.getOwnerDocument();
        Element el = doc.createElement(name);
        parent.appendChild(el);
        return el;
    }

    public static String getText(Node element) {
        if (element!=null) {
            element.normalize();
            try {
                String txt = element.getTextContent();
                if (txt!=null) {
                    return txt.trim();
                }
            } catch (DOMException e) {
                return "";
            }
        }
        return "";
    }

    public static Node getChild(Element parent, String name) {
        NodeList elList = parent.getElementsByTagName(name);
        if (elList.getLength()>0) {
            return elList.item(0);
        } else {
            return null;
        }
    }

    public static String getChildText(Element parent, String name) {
        return getText(getChild(parent, name));
    }

}
