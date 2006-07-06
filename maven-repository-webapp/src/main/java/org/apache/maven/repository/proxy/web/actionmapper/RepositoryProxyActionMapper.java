package org.apache.maven.repository.proxy.web.actionmapper;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.webwork.dispatcher.mapper.ActionMapping;
import com.opensymphony.webwork.dispatcher.mapper.DefaultActionMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

public class RepositoryProxyActionMapper
    extends DefaultActionMapper
{
    /**
     * logger instance
     */
    protected static final Log log = LogFactory.getLog( RepositoryProxyActionMapper.class );

    private static final String configFileName = "maven-proxy-complete.conf";

    private static final String defaultProxyAction = "proxy";

    /**
     * the keyword that will be checked on the http request to determine if proxy
     * is requested
     * <p/>
     * the default prefix is "/proxy/"
     */
    private String prefix = "/proxy/";

    private String requestedArtifact;

    String configFile = null;

    public String getPrefix()
    {
        return prefix;
    }

    public String getRequestedArtifact()
    {
        return requestedArtifact;
    }

    public ActionMapping getDefaultActionMapping( HttpServletRequest request )
    {
        ActionMapping mapping = super.getMapping( request );

        return mapping;
    }

    public void setConfigfile( String fileName )
    {
        configFile = fileName;
    }

    /**
     * only process the request that matches the prefix all other request
     * will be hand over to the default action mapper
     * <p/>
     * if the configuration file is missing the request will also be channeled
     * to the default action mapper
     */
    public ActionMapping getMapping( HttpServletRequest request )
    {
        Properties config = new Properties();
        String uri = request.getServletPath();
        URL configURL = getClass().getClassLoader().getResource( configFileName );

        if ( ( configURL != null ) && ( configFile == null ) )
        {
            configFile = configURL.getFile();
            log.info( configFile );
        }

        try
        {
            config.load( new FileInputStream( configFile ) );
        }
        catch ( IOException ex )
        {
            log.info( "[config error] " + ex.getMessage() );
            return getDefaultActionMapping( request );
        }

        if ( config.getProperty( "prefix" ) != null )
        {
            prefix = "/" + config.getProperty( "prefix" ) + "/";
        }

        log.info( "prefix : " + prefix );

        if ( uri.startsWith( prefix ) )
        {
            requestedArtifact = uri.substring( prefix.length() );

            if ( ( requestedArtifact == null ) || ( requestedArtifact.length() < 0 ) )
            {
                return getDefaultActionMapping( request );
            }

            HashMap parameterMap = new HashMap();

            parameterMap.put( "requestedFile", requestedArtifact );
            parameterMap.put( "configFile", configFile );

            return new ActionMapping( defaultProxyAction, "/", "", parameterMap );
        }

        return getDefaultActionMapping( request );
    }

}
