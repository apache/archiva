package org.apache.archiva.rest.services;
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

import org.apache.archiva.components.scheduler.CronExpressionValidator;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.UtilServices;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Olivier Lamy
 */
@Service("commonServices#rest")
public class DefaultCommonServices
    implements CommonServices
{

    private static final String RESOURCE_NAME = "org/apache/archiva/i18n/default";

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private UtilServices utilServices;

    private Map<String, String> cachei18n = new ConcurrentHashMap<String, String>();

    @Inject
    protected CronExpressionValidator cronExpressionValidator;

    @PostConstruct
    public void init()
        throws ArchivaRestServiceException
    {

        // preload i18n en and fr
        getAllI18nResources( "en" );
        getAllI18nResources( "fr" );
    }

    @Override
    public String getI18nResources( String locale )
        throws ArchivaRestServiceException
    {
        Properties properties = new Properties();

        StringBuilder resourceName = new StringBuilder( RESOURCE_NAME );
        try
        {

            loadResource( properties, resourceName, locale );

        }
        catch ( IOException e )
        {
            log.warn( "skip error loading properties {}", resourceName );
        }

        return fromProperties( properties );
    }

    private void loadResource( Properties properties, StringBuilder resourceName, String locale )
        throws IOException
    {
        // load default
        loadResource( properties, new StringBuilder( resourceName ).append( ".properties" ).toString(), locale );
        // if locale override with locale content
        if ( StringUtils.isNotEmpty( locale ) )
        {
            loadResource( properties,
                          new StringBuilder( resourceName ).append( "_" + locale ).append( ".properties" ).toString(),
                          locale );
        }

    }

    private String fromProperties( final Properties properties )
    {
        StringBuilder output = new StringBuilder();

        for ( Map.Entry<Object, Object> entry : properties.entrySet() )
        {
            output.append( (String) entry.getKey() ).append( '=' ).append( (String) entry.getValue() );
            output.append( '\n' );
        }

        return output.toString();
    }

    private void loadResource( final Properties finalProperties, String resourceName, String locale )
        throws IOException
    {
        Properties properties = new Properties();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourceName ))
        {
            if ( is != null )
            {
                properties.load( is );
                finalProperties.putAll( properties );
            }
            else
            {
                if ( !StringUtils.equalsIgnoreCase( locale, "en" ) )
                {
                    log.info( "cannot load resource {}", resourceName );
                }
            }
        }
    }

    @Override
    public String getAllI18nResources( String locale )
        throws ArchivaRestServiceException
    {

        String cachedi18n = cachei18n.get( StringUtils.isEmpty( locale ) ? "en" : StringUtils.lowerCase( locale ) );
        if ( cachedi18n != null )
        {
            return cachedi18n;
        }

        try
        {

            Properties all = utilServices.getI18nProperties( locale );
            StringBuilder resourceName = new StringBuilder( RESOURCE_NAME );
            loadResource( all, resourceName, locale );

            String i18n = fromProperties( all );
            cachei18n.put( StringUtils.isEmpty( locale ) ? "en" : StringUtils.lowerCase( locale ), i18n );
            return i18n;
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        catch ( RedbackServiceException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getHttpErrorCode(), e );
        }
    }

    private void loadFromString( String propsStr, Properties properties )
        throws ArchivaRestServiceException
    {
        try (InputStream inputStream = new ByteArrayInputStream( propsStr.getBytes() ))
        {
            properties.load( inputStream );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }


    @Override
    public Boolean validateCronExpression( String cronExpression )
        throws ArchivaRestServiceException
    {
        return cronExpressionValidator.validate( cronExpression );
    }
}
