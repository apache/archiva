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

import org.apache.archiva.rest.api.model.ArchivaRuntimeInfo;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.redback.rest.api.services.RedbackServiceException;
import org.codehaus.redback.rest.api.services.UtilServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 */
@Service( "commonServices#rest" )
public class DefaultCommonServices
    implements CommonServices
{

    private static final String RESOURCE_NAME = "org/apache/archiva/i18n/default";

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private UtilServices utilServices;

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
            log.warn( "skip error loading properties {}", resourceName.toString() );
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
        InputStream is = null;
        Properties properties = new Properties();
        try
        {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourceName.toString() );
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
        finally
        {
            IOUtils.closeQuietly( is );
        }
    }

    public String getAllI18nResources( String locale )
        throws ArchivaRestServiceException
    {
        try
        {

            Properties all = utilServices.getI18nProperties( locale );
            StringBuilder resourceName = new StringBuilder( RESOURCE_NAME );
            loadResource( all, resourceName, locale );

            return fromProperties( all );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        catch ( RedbackServiceException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getHttpErrorCode() );
        }
    }

    private void loadFromString( String propsStr, Properties properties )
        throws ArchivaRestServiceException
    {
        InputStream inputStream = null;
        try
        {
            inputStream = new ByteArrayInputStream( propsStr.getBytes() );
            properties.load( inputStream );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
    }

    public ArchivaRuntimeInfo archivaRuntimeInfo()
    {
        return new ArchivaRuntimeInfo();
    }
}
