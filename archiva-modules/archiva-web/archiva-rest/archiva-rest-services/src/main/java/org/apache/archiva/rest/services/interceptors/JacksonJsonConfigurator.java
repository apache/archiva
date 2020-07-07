package org.apache.archiva.rest.services.interceptors;
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


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;

/**
 * class to setup Jackson Json configuration
 *
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service("archivaJacksonJsonConfigurator")
public class JacksonJsonConfigurator
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    public JacksonJsonConfigurator( @Named( "redbackJacksonJsonMapper" ) ObjectMapper objectMapper,
                                    @Named( "redbackJacksonXMLMapper" ) XmlMapper xmlMapper )
    {

        log.info( "configure jackson ObjectMapper" );
        objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setAnnotationIntrospector( new JaxbAnnotationIntrospector( objectMapper.getTypeFactory() ) );
        objectMapper.registerModule( new JavaTimeModule( ) );
        objectMapper.setDateFormat( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ) );

        xmlMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
    }
}
