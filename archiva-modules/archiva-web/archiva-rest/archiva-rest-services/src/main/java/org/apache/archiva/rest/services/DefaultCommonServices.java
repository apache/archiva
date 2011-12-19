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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    private Logger log = LoggerFactory.getLogger( getClass() );

    public String getI18nResources( String locale )
        throws ArchivaRestServiceException
    {
        Properties properties = new Properties();

        StringBuilder resourceName = new StringBuilder( "org/apache/archiva/i18n/default" );
        try
        {

            loadResource( properties, resourceName, locale );

        }
        catch ( IOException e )
        {
            log.warn( "skip error loading properties {}", resourceName.toString() );
        }

        StringBuilder output = new StringBuilder();

        for ( Map.Entry<Object, Object> entry : properties.entrySet() )
        {
            output.append( (String) entry.getKey() ).append( '=' ).append( (String) entry.getValue() );
            output.append( '\n' );
        }

        return output.toString();
    }

    private void loadResource( Properties properties, StringBuilder resourceName, String locale )
        throws IOException
    {
        // load default
        loadResource( properties, new StringBuilder( resourceName ).append( ".properties" ).toString() );
        // if locale override with locale content
        if ( StringUtils.isNotEmpty( locale ) )
        {
            loadResource( properties,
                          new StringBuilder( resourceName ).append( "_" + locale ).append( ".properties" ).toString() );
        }

    }

    private void loadResource( Properties properties, String resourceName )
        throws IOException
    {
        InputStream is = null;

        try
        {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourceName.toString() );
            if ( is != null )
            {
                properties.load( is );
            }
        }
        finally
        {
            IOUtils.closeQuietly( is );
        }
    }
}
