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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusBeanFactory
    extends DefaultListableBeanFactory
{
    private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader( this );

    public PlexusBeanFactory( Resource resource )
    {
        this( resource, null );
    }

    public PlexusBeanFactory( Resource resource, BeanFactory parentBeanFactory )
    {
        super( parentBeanFactory );
        this.reader.setDocumentReaderClass( PlexusBeanDefinitionDocumentReader.class );
        this.reader.setValidationMode( XmlBeanDefinitionReader.VALIDATION_NONE );
        this.reader.loadBeanDefinitions( resource );
    }

}
