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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * A spring namespace handler to support plexus components creation and direct field-injection in a spring
 * XmlApplicationContext.
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
        registerBeanDefinitionParser( "requirement", new PlexusPropertyBeanDefinitionParser() );
    }

    private class PlexusPropertyBeanDefinitionParser
        extends AbstractBeanDefinitionParser
    {
        protected AbstractBeanDefinition parseInternal( Element element, ParserContext parserContext )
        {
            return null;
        }

    }

    /**
     * BeanDefinitionParser for &lt;plexus:component&gt;. Register a bean definition for a PlexusComponentFactoryBean
     * with all nested requirement / configuration injected using direct field injection.
     * <p>
     * Also register an alias for the Plexus component using spring conventions (interface class simple name + "#"
     * role-hint)
     */
    private class PlexusComponentBeanDefinitionParser
        extends AbstractSingleBeanDefinitionParser
    {

        protected void doParse( Element element, BeanDefinitionBuilder builder )
        {
            builder.addPropertyValue( "role", element.getAttribute( "role" ) );
            String implementation = element.getAttribute( "implementation" );
            builder.addPropertyValue( "implementation", implementation );

            Map dependencies = new HashMap();

            List requirements = DomUtils.getChildElementsByTagName( element, "requirement" );
            for ( Iterator iterator = requirements.iterator(); iterator.hasNext(); )
            {
                Element child = (Element) iterator.next();
                String name = child.getAttribute( "name" );
                String role = child.getAttribute( "role" );
                String roleHint = child.getAttribute( "role-hint" );
                String ref = PlexusToSpringUtils.buildSpringId( role, roleHint );
                if ( roleHint == null )
                {
//                    Field f = ClassUtils.forName( implementation ).getField( name );
//                    if ( Map.class.isAssignableFrom( f.getType() ) )
//                    {
//                        // TODO add add support for plexus role --> Map<role-hint, component>
//                    }
                }
                dependencies.put( name, new RuntimeBeanReference( ref ) );

            }

            List configurations = DomUtils.getChildElementsByTagName( element, "configuration" );
            for ( Iterator iterator = configurations.iterator(); iterator.hasNext(); )
            {
                Element child = (Element) iterator.next();
                String name = child.getAttribute( "name" );
                String value = child.getAttribute( "value" );
                dependencies.put( name, value );
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

    public static String buildSpringId( String role, String roleHint )
    {
        return PlexusToSpringUtils.buildSpringId( role, roleHint );
    }
}
