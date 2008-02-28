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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A spring namespace handler to support plexus components creation and direct
 * field-injection in a spring XmlApplicationContext.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusNamespaceHandler
    extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser( "component", new PlexusComponentBeanDefinitionParser() );
        registerBeanDefinitionParser( "requirement", new NopBeanDefinitionParser() );
        registerBeanDefinitionParser( "configuration", new NopBeanDefinitionParser() );
    }

    private class NopBeanDefinitionParser
        extends AbstractBeanDefinitionParser
    {
        protected AbstractBeanDefinition parseInternal( Element element, ParserContext parserContext )
        {
            return null;
        }

    }

    /**
     * BeanDefinitionParser for &lt;plexus:component&gt;. Register a bean
     * definition for a PlexusComponentFactoryBean with all nested requirement /
     * configuration injected using direct field injection.
     * <p>
     * Also register an alias for the Plexus component using spring conventions
     * (interface class simple name + "#" role-hint)
     */
    private class PlexusComponentBeanDefinitionParser
        extends AbstractSingleBeanDefinitionParser
    {
        private int count;

        protected void doParse( Element element, BeanDefinitionBuilder builder )
        {
            builder.addPropertyValue( "role", element.getAttribute( "role" ) );
            String implementation = element.getAttribute( "implementation" );
            builder.addPropertyValue( "implementation", implementation );
            builder.addPropertyValue( "instanciationStrategy", element.getAttribute( "instantiation-strategy" ) );

            Map dependencies = new HashMap();

            List requirements = DomUtils.getChildElementsByTagName( element, "requirement" );
            for ( Iterator iterator = requirements.iterator(); iterator.hasNext(); )
            {
                Element child = (Element) iterator.next();
                String name = child.getAttribute( "field-name" );
                if ( name.length() == 0 )
                {
                    // Plexus doesn't require to specify the field-name if only
                    // one field matches the injected type
                    name = "#" + count++;
                }
                String role = child.getAttribute( "role" );
                String roleHint = child.getAttribute( "role-hint" );
                String ref = PlexusToSpringUtils.buildSpringId( role, roleHint );
                dependencies.put( name, new RuntimeBeanReference( ref ) );

            }

            List configurations = DomUtils.getChildElementsByTagName( element, "configuration" );
            for ( Iterator iterator = configurations.iterator(); iterator.hasNext(); )
            {
                Element child = (Element) iterator.next();
                String name = child.getAttribute( "name" );
                if ( child.getChildNodes().getLength() == 1 )
                {
                    dependencies.put( name, child.getTextContent() );
                }
                else
                {
                    StringWriter xml = new StringWriter();
                    flatten( child, new PrintWriter( xml ) );
                    dependencies.put( name, xml.toString() );
                }
            }

            builder.addPropertyValue( "requirements", dependencies );
        }

        protected String resolveId( Element element, AbstractBeanDefinition definition, ParserContext parserContext )
            throws BeanDefinitionStoreException
        {
            String role = element.getAttribute( "role" );
            String roleHint = element.getAttribute( "role-hint" );
            return PlexusToSpringUtils.buildSpringId( role, roleHint );
        }

        protected Class getBeanClass( Element element )
        {
            return PlexusComponentFactoryBean.class;
        }

    }
    /**
     * @param childNodes
     * @return
     */
    private void flatten( NodeList childNodes, PrintWriter out )
    {
        for ( int i = 0; i < childNodes.getLength(); i++ )
        {
            Node node = childNodes.item( i );
            if (node.getNodeType() == Node.ELEMENT_NODE )
            {
                flatten( (Element) node, out );
            }
        }
    }
    /**
     * @param item
     * @param out
     */
    private void flatten( Element el, PrintWriter out )
    {
        out.print( '<' );
        out.print( el.getTagName() );
        NamedNodeMap attributes = el.getAttributes();
        for ( int i = 0; i < attributes.getLength(); i++ )
        {
            Node attribute = attributes.item( i );
            out.print( " ");
            out.print( attribute.getLocalName() );
            out.print( "=\"" );
            out.print( attribute.getTextContent() );
            out.print( "\"" );
        }
        out.print( '>' );
        flatten( el.getChildNodes(), out );
        out.print( "</" );
        out.print( el.getTagName() );
        out.print( '>' );
    }
}
