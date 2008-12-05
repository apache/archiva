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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XMLReader - a set of common xml utility methods for reading content out of an xml file. 
 *
 * @version $Id$
 */
public class XMLReader
{
    private URL xmlUrl;

    private String documentType;

    private Document document;

    private Map<String, String> namespaceMap = new HashMap<String, String>();

    public XMLReader( String type, File file )
        throws XMLException
    {
        if ( !file.exists() )
        {
            throw new XMLException( "file does not exist: " + file.getAbsolutePath() );
        }

        if ( !file.isFile() )
        {
            throw new XMLException( "path is not a file: " + file.getAbsolutePath() );
        }

        if ( !file.canRead() )
        {
            throw new XMLException( "Cannot read xml file due to permissions: " + file.getAbsolutePath() );
        }

        try
        {
            init( type, file.toURL() );
        }
        catch ( MalformedURLException e )
        {
            throw new XMLException( "Unable to translate file " + file + " to URL: " + e.getMessage(), e );
        }
    }

    public XMLReader( String type, URL url )
        throws XMLException
    {
        init( type, url );
    }

    private void init( String type, URL url )
        throws XMLException
    {
        this.documentType = type;
        this.xmlUrl = url;

        InputStream in = null;
        SAXReader reader = new SAXReader();
        
        try
        {
            in = url.openStream();
            InputStreamReader inReader = new InputStreamReader( in, "UTF-8" );
            LatinEntityResolutionReader latinReader = new LatinEntityResolutionReader( inReader );
            this.document = reader.read( latinReader );
        }
        catch ( DocumentException e )
        {
            throw new XMLException( "Unable to parse " + documentType + " xml " + xmlUrl + ": " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new XMLException( "Unable to open stream to " + url + ": " + e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }

        Element root = this.document.getRootElement();
        if ( root == null )
        {
            throw new XMLException( "Invalid " + documentType + " xml: root element is null." );
        }

        if ( !StringUtils.equals( root.getName(), documentType ) )
        {
            throw new XMLException( "Invalid " + documentType + " xml: Unexpected root element <" + root.getName()
                + ">, expected <" + documentType + ">" );
        }
    }

    public String getDefaultNamespaceURI()
    {
        Namespace namespace = this.document.getRootElement().getNamespace();
        return namespace.getURI();
    }

    public void addNamespaceMapping( String elementName, String uri )
    {
        this.namespaceMap.put( elementName, uri );
    }

    public Element getElement( String xpathExpr )
        throws XMLException
    {
        XPath xpath = createXPath( xpathExpr );
        Object evaluated = xpath.selectSingleNode( document );

        if ( evaluated == null )
        {
            return null;
        }

        if ( evaluated instanceof Element )
        {
            Element evalElem = (Element) evaluated;
            return evalElem;
        }
        else
        {
            // Unknown evaluated type.
            throw new XMLException( ".getElement( Expr: " + xpathExpr + " ) resulted in non-Element type -> ("
                + evaluated.getClass().getName() + ") " + evaluated );
        }
    }

    private XPath createXPath( String xpathExpr )
    {
        XPath xpath = document.createXPath( xpathExpr );
        if ( !this.namespaceMap.isEmpty() )
        {
            xpath.setNamespaceURIs( this.namespaceMap );
        }
        return xpath;
    }

    public boolean hasElement( String xpathExpr )
        throws XMLException
    {
        XPath xpath = createXPath( xpathExpr );
        Object evaluated = xpath.selectSingleNode( document );

        if ( evaluated == null )
        {
            return false;
        }

        return true;
    }

    /**
     * Remove namespaces from entire document.
     */
    public void removeNamespaces()
    {
        removeNamespaces( this.document.getRootElement() );
    }

    /**
     * Remove namespaces from element recursively.
     */
    public void removeNamespaces( Element elem )
    {
        elem.setQName( QName.get( elem.getName(), Namespace.NO_NAMESPACE, elem.getQualifiedName() ) );

        Node n;

        Iterator<Node> it = elem.elementIterator();
        while ( it.hasNext() )
        {
            n = it.next();

            switch ( n.getNodeType() )
            {
                case Node.ATTRIBUTE_NODE:
                    ( (Attribute) n ).setNamespace( Namespace.NO_NAMESPACE );
                    break;
                case Node.ELEMENT_NODE:
                    removeNamespaces( (Element) n );
                    break;
            }
        }
    }

    public String getElementText( Node context, String xpathExpr )
        throws XMLException
    {
        XPath xpath = createXPath( xpathExpr );
        Object evaluated = xpath.selectSingleNode( context );

        if ( evaluated == null )
        {
            return null;
        }

        if ( evaluated instanceof Element )
        {
            Element evalElem = (Element) evaluated;
            return evalElem.getTextTrim();
        }
        else
        {
            // Unknown evaluated type.
            throw new XMLException( ".getElementText( Node, Expr: " + xpathExpr
                + " ) resulted in non-Element type -> (" + evaluated.getClass().getName() + ") " + evaluated );
        }
    }

    public String getElementText( String xpathExpr )
        throws XMLException
    {
        XPath xpath = createXPath( xpathExpr );
        Object evaluated = xpath.selectSingleNode( document );

        if ( evaluated == null )
        {
            return null;
        }

        if ( evaluated instanceof Element )
        {
            Element evalElem = (Element) evaluated;
            return evalElem.getTextTrim();
        }
        else
        {
            // Unknown evaluated type.
            throw new XMLException( ".getElementText( Expr: " + xpathExpr + " ) resulted in non-Element type -> ("
                + evaluated.getClass().getName() + ") " + evaluated );
        }
    }

    public List<Element> getElementList( String xpathExpr )
        throws XMLException
    {
        XPath xpath = createXPath( xpathExpr );
        Object evaluated = xpath.evaluate( document );

        if ( evaluated == null )
        {
            return null;
        }

        /* The xpath.evaluate(Context) method can return:
         *   1) A Collection or List of dom4j Nodes. 
         *   2) A single dom4j Node.
         */

        if ( evaluated instanceof List )
        {
            return (List<Element>) evaluated;
        }
        else if ( evaluated instanceof Node )
        {
            List<Element> ret = new ArrayList<Element>();
            ret.add( (Element) evaluated );
            return ret;
        }
        else
        {
            // Unknown evaluated type.
            throw new XMLException( ".getElementList( Expr: " + xpathExpr + " ) resulted in non-List type -> ("
                + evaluated.getClass().getName() + ") " + evaluated );
        }
    }

    public List<String> getElementListText( String xpathExpr )
        throws XMLException
    {
        List<Element> elemList = getElementList( xpathExpr );
        if ( elemList == null )
        {
            return null;
        }

        List<String> ret = new ArrayList<String>();
        for ( Iterator<Element> iter = elemList.iterator(); iter.hasNext(); )
        {
            Element listelem = iter.next();
            ret.add( listelem.getTextTrim() );
        }
        return ret;
    }

}
