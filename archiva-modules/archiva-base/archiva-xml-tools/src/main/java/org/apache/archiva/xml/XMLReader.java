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

import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * XMLReader - a set of common xml utility methods for reading content out of an xml file.
 */
public class XMLReader
{
    private URL xmlUrl;

    private String documentType;

    private Document document;

    private Map<String, String> namespaceMap = new HashMap<>();
    private Map<String, String> reverseNamespaceMap = new HashMap<>();

    private class NamespaceCtx implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            return namespaceMap.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return reverseNamespaceMap.get(namespaceURI);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return namespaceMap.keySet().iterator();
        }
    }

    public XMLReader( String type, Path file )
        throws XMLException
    {
        initWithFile( type, file );
    }

    private void initWithFile( String type, Path file) throws XMLException {
        if ( !Files.exists(file) )
        {
            throw new XMLException( "file does not exist: " + file.toAbsolutePath() );
        }

        if ( !Files.isRegularFile(file) )
        {
            throw new XMLException( "path is not a file: " + file.toAbsolutePath() );
        }

        if ( !Files.isReadable(file) )
        {
            throw new XMLException( "Cannot read xml file due to permissions: " + file.toAbsolutePath() );
        }

        try
        {
            initWithUrl( type, file.toUri().toURL() );
        }
        catch ( MalformedURLException e )
        {
            throw new XMLException( "Unable to translate file " + file + " to URL: " + e.getMessage(), e );
        }
    }

    public XMLReader( String type, StorageAsset asset) throws XMLException
    {
        if (asset.isFileBased()) {
            initWithFile( type, asset.getFilePath( ) );
        } else {
            URI uri = asset.getStorage( ).getLocation( ).resolve( asset.getPath( ) );
            try(InputStream in = asset.getReadStream()) {
                initWithStream( type, uri.toURL( ), in );
            }
            catch ( IOException e )
            {
                throw new XMLException( "Could not open asset stream of " + uri + ": " + e.getMessage( ), e );
            }
        }


    }


    public XMLReader( String type, URL url )
        throws XMLException
    {
        initWithUrl( type, url );
    }


    private void initWithUrl( String type, URL url ) throws XMLException {
        try(InputStream in = url.openStream()) {
            initWithStream( type, url, in );
        }
        catch ( IOException e )
        {
            throw new XMLException( "Could not open url " + url + ": " + e.getMessage( ), e );
        }
    }

    private void initWithStream( String type, URL url, InputStream in  )
        throws XMLException
    {
        this.documentType = type;
        this.xmlUrl = url;
        // SAXReader reader = new SAXReader();



        try (Reader reader = new LatinEntityResolutionReader(new BufferedReader(new InputStreamReader(in, "UTF-8"))))
        {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setExpandEntityReferences(false);
            dbf.setValidating(false);
            // dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"false");
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            // dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            // To suppress error output at System.err
            db.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {

                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            this.document = db.parse(new InputSource(reader));

        }
        catch ( IOException e )
        {
            throw new XMLException( "Unable to open stream to " + url + ": " + e.getMessage(), e );
        } catch (ParserConfigurationException e) {
            throw new XMLException("Unable to start parser "+e.getMessage());
        } catch (SAXException e) {
            throw new XMLException("Unable to parse file "+e.getMessage());
        }

        Element root = this.document.getDocumentElement();
        if ( root == null )
        {
            throw new XMLException( "Invalid " + documentType + " xml: root element is null." );
        }

        if ( !StringUtils.equals( root.getLocalName(), documentType ) )
        {
            throw new XMLException(
                "Invalid " + documentType + " xml: Unexpected root element <" + root.getLocalName() + ">, expected <"
                    + documentType + ">" + root.getNodeName() );
        }
    }

    public String getDefaultNamespaceURI()
    {
        String namespace = this.document.getNamespaceURI();
        return namespace;
    }

    public void addNamespaceMapping( String elementName, String uri )
    {
        this.namespaceMap.put( elementName, uri );
    }

    public Element getElement( String xpathExpr )
        throws XMLException
    {
        XPathExpression xpath = null;
        try {
            xpath = createXPath( xpathExpr );
            Object evaluated = xpath.evaluate( document, XPathConstants.NODE);

            if ( evaluated == null )
            {
                return null;
            }

            if ( evaluated instanceof Element )
            {
                return (Element) evaluated;
            }
            else
            {
                // Unknown evaluated type.
                throw new XMLException( ".getElement( Expr: " + xpathExpr + " ) resulted in non-Element type -> ("
                        + evaluated.getClass().getName() + ") " + evaluated );
            }
        } catch (XPathExpressionException e) {
            throw new XMLException("Could not parse xpath expression");
        }
    }

    private XPathExpression createXPath(String xpathExpr ) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        if ( !this.namespaceMap.isEmpty() )
        {
            xpath.setNamespaceContext(new NamespaceCtx());
        }
        return xpath.compile(xpathExpr);
    }

    public boolean hasElement( String xpathExpr )
        throws XMLException
    {
        XPathExpression xpath = null;
        try {
            xpath = createXPath( xpathExpr );
            Object evaluated = xpath.evaluate( document, XPathConstants.NODE );
            if ( evaluated == null )
            {
                return false;
            }
            return true;
        } catch (XPathExpressionException e) {
            throw new XMLException("Could not create xpath expression");
        }
    }

    /**
     * Remove namespaces from entire document.
     */
    public void removeNamespaces()
    {
        removeNamespaces( this.document.getDocumentElement() );
    }

    /**
     * Remove namespaces from element recursively.
     */
    @SuppressWarnings("unchecked")
    public void removeNamespaces( Node elem )
    {
        if (elem.getNodeType() == Node.ELEMENT_NODE || elem.getNodeType() == Node.ATTRIBUTE_NODE) {
            document.renameNode(elem, null, elem.getLocalName());

            Node n;

            NodeList nodeList = elem.getChildNodes();


            for (int i = 0; i < nodeList.getLength(); i++) {
                n = nodeList.item(i);
                removeNamespaces(n);
            }
        }
    }

    public String getElementText( Node context, String xpathExpr )
        throws XMLException
    {
        XPathExpression xpath = null;
        try {
            xpath = createXPath( xpathExpr );
            Object evaluated = xpath.evaluate( context, XPathConstants.NODE );

            if ( evaluated == null )
            {
                return null;
            }

            if ( evaluated instanceof Element )
            {
                Element evalElem = (Element) evaluated;
                return XmlUtil.getText(evalElem);
            }
            else
            {
                // Unknown evaluated type.
                throw new XMLException( ".getElementText( Node, Expr: " + xpathExpr + " ) resulted in non-Element type -> ("
                        + evaluated.getClass().getName() + ") " + evaluated );
            }
        } catch (XPathExpressionException e) {
            throw new XMLException("Could not parse xpath expression");
        }
    }

    public String getElementText( String xpathExpr )
        throws XMLException
    {
        return getElementText(document, xpathExpr);
    }

    @SuppressWarnings("unchecked")
    public List<Node> getElementList( String xpathExpr )
        throws XMLException
    {
        XPathExpression xpath = null;
        try {
            xpath = createXPath( xpathExpr );
            Object evaluated = xpath.evaluate( document, XPathConstants.NODESET);

            if ( evaluated == null )
            {
                return Collections.emptyList();
            }

            NodeList nl = (NodeList) evaluated;
            List<Node> nodeList = new ArrayList<>();
            for (int i = 0 ; i<nl.getLength(); i++) {
                nodeList.add(nl.item(i));
            }
            return nodeList;

        } catch (XPathExpressionException e) {
            throw new XMLException("Could not parse xpath expression");
        }
    }

    public List<String> getElementListText( String xpathExpr )
        throws XMLException
    {
        List<Node> elemList = getElementList( xpathExpr );
        if ( elemList == null )
        {
            return null;
        }

        return elemList.stream().filter(n -> n instanceof Element).map(n -> XmlUtil.getText(n)).collect(Collectors.toList());
    }

}
