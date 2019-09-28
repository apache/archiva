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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.Writer;

/**
 * XMLWriter - Making writing XML files easier.
 */
public class XMLWriter {
    /**
     * Write the Document to the provided Writer, leaving the Writer open.
     *
     * @param doc    the document to write.
     * @param writer the writer to write to.
     * @throws XMLException if there was a problem writing the xml to the writer.
     */
    public static void write(Document doc, Writer writer)
            throws XMLException {
        write(doc, writer, false);
    }

    /**
     * Write the Document to the provided Writer, with an option to close the writer upon completion.
     *
     * @param doc    the document to write.
     * @param writer the writer to write to.
     * @param close  true to close the writer on completion.
     * @throws XMLException if there was a problem writing the xml to the writer.
     */
    public static void write(Document doc, Writer writer, boolean close)
            throws XMLException {

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            try {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            } catch (IllegalArgumentException ex) {
                // Indent not supported
            }
            // Writing the XML declaration, because the JDK implementation does not create a newline
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            StreamResult result = new StreamResult(writer);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);


        } catch (TransformerException e) {
            throw new XMLException("Could not create the xml transformer: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new XMLException("Could not write to xml output: " + e.getMessage(), e);
        } finally {
            if (writer!=null) {
                if (close) {
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        /* quietly ignore */
                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        /* quietly ignore */
                    }
                }
            }
        }
    }
}
