/* ========================================================================== *
 *         Copyright (C) 2004-2006, Pier Fumagalli <http://could.it/>         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package it.could.webdav;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * <p>A {@link DAVRepository} instance enforcing all {@link DAVResource}s to
 * be XML files.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class XMLRepository extends DAVRepository {

    /**
     * <p>Create a new {@link XMLRepository} instance.</p>
     */
    public XMLRepository(File root)
    throws IOException {
        super(root);
    }

    /**
     * <p>Return the {@link DAVResource} associated with a {@link URI}.</p>
     */
    public DAVResource getResource(URI uri)
    throws IOException {
        return new XMLResource(this, super.getResource(uri));
    }
    
    /**
     * <p>A simple {@link DAVResource} extension enforcing XML writes.</p>
     */
    private static final class XMLResource extends DAVResource {

        /**
         * <p>Create a new {@link XMLResource} instance.</p>
         */
        public XMLResource(XMLRepository repository, DAVResource resource) {
            super(repository, resource.getFile());
        }

        /**
         * <p>Override the MIME Content-Type to <code>text/xml</code> for
         * normal resources.</p>
         */
        public String getContentType() {
            if (this.isResource()) return "text/xml";
            return super.getContentType();
        }
        
        /**
         * <p>Return a {@link DAVOutputStream} enforcing XML formatted data.</p>
         */
        public DAVOutputStream write() {
            return new XMLOutputStream(this);
        }
    }

    /**
     * <p>A simple {@link DAVOutputStream} enforcing XML formatted data.</p>
     */
    private static final class XMLOutputStream extends DAVOutputStream {

        /**
         * <p>Create a new {@link XMLOutputStream} instance.</p>
         */
        protected XMLOutputStream(XMLResource resource) {
            super(resource);
        }

        /**
         * <p>Ensure that whatever is in the temporary file is XML.</p>
         */
        protected void rename(File temporary, File original)
        throws IOException {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(false);
                SAXParser parser = factory.newSAXParser();
                parser.parse(temporary, new DefaultHandler());
                super.rename(temporary, original);
            } catch (ParserConfigurationException exception) {
                throw new DAVException(500, "JAXP parser error", exception);
            } catch (SAXException exception) {
                throw new DAVException(415, "Error parsing data", exception);
            }
        }
    }
}
