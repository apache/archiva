package org.apache.archiva.proxy.maven;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.Wagon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service ("wagonFactory")
public class DefaultWagonFactory
    implements WagonFactory
{

    private ApplicationContext applicationContext;

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private DebugTransferListener debugTransferListener = new DebugTransferListener();

    @Inject
    public DefaultWagonFactory( ApplicationContext applicationContext )
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public Wagon getWagon( WagonFactoryRequest wagonFactoryRequest )
        throws WagonFactoryException
    {
        try
        {
            String protocol = StringUtils.startsWith( wagonFactoryRequest.getProtocol(), "wagon#" )
                ? wagonFactoryRequest.getProtocol()
                : "wagon#" + wagonFactoryRequest.getProtocol();

            // if it's a ntlm proxy we have to lookup the wagon light which support thats
            // wagon http client doesn't support that
            if ( wagonFactoryRequest.getNetworkProxy() != null && wagonFactoryRequest.getNetworkProxy().isUseNtlm() )
            {
                protocol = protocol + "-ntlm";
            }

            Wagon wagon = applicationContext.getBean( protocol, Wagon.class );
            wagon.addTransferListener( debugTransferListener );
            configureUserAgent( wagon, wagonFactoryRequest );
            return wagon;
        }
        catch ( BeansException e )
        {
            throw new WagonFactoryException( e.getMessage(), e );
        }
    }

    protected void configureUserAgent( Wagon wagon, WagonFactoryRequest wagonFactoryRequest )
    {
        try
        {
            Class<? extends Wagon> clazz = wagon.getClass();
            Method getHttpHeaders = clazz.getMethod( "getHttpHeaders" );

            Properties headers = (Properties) getHttpHeaders.invoke( wagon );
            if ( headers == null )
            {
                headers = new Properties();
            }

            headers.put( "User-Agent", wagonFactoryRequest.getUserAgent() );

            if ( !wagonFactoryRequest.getHeaders().isEmpty() )
            {
                for ( Map.Entry<String, String> entry : wagonFactoryRequest.getHeaders().entrySet() )
                {
                    headers.put( entry.getKey(), entry.getValue() );
                }
            }

            Method setHttpHeaders = clazz.getMethod( "setHttpHeaders", new Class[]{ Properties.class } );
            setHttpHeaders.invoke( wagon, headers );

            logger.debug( "http headers set to: {}", headers );
        }
        catch ( Exception e )
        {
            logger.warn( "fail to configure User-Agent: {}", e.getMessage(), e );
        }
    }
}
