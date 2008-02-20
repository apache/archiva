package org.apache.maven.archiva.common.spring;

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

import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

/**
 * XPathFunction to convert plexus property-name to Spring propertyName.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class CamelCaseXpathFunction
    implements XPathFunction, XPathFunctionResolver
{

    private static final QName name = new QName( "http://plexus.codehaus.org/", "camelCase" );

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.xpath.XPathFunctionResolver#resolveFunction(javax.xml.namespace.QName,
     * int)
     */
    public XPathFunction resolveFunction( QName functionName, int arity )
    {
        if ( name.equals( functionName.getLocalPart() ) && arity >= 1 )
        {
            return new CamelCaseXpathFunction();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.xpath.XPathFunction#evaluate(java.util.List)
     */
    public Object evaluate( List args )
        throws XPathFunctionException
    {
        return PlexusToSpringUtils.toCamelCase( (String) args.get( 0 ) );
    }


}
