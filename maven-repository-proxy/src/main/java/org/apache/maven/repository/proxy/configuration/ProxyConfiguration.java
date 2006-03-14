package org.apache.maven.repository.proxy.configuration;

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

import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to represent the configuration file for the proxy
 *
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.proxy.configuration.ProxyConfiguration"
 */
public class ProxyConfiguration
{
    public static final String ROLE = ProxyConfiguration.class.getName();

    private List repositories = new ArrayList();

    private String cachePath;

    private String layout;

    private ProxyInfo httpProxy;

    /**
     * Used to set the location where the proxy should cache the configured repositories
     *
     * @param path
     */
    public void setRepositoryCachePath( String path )
    {
        cachePath = new File( path ).getAbsolutePath();
    }

    /**
     * Used to retrieved the absolute path of the repository cache
     *
     * @return path to the proxy cache
     */
    public String getRepositoryCachePath()
    {
        return cachePath;
    }

    public void setHttpProxy( ProxyInfo httpProxy )
    {
        this.httpProxy = httpProxy;
    }

    public void setHttpProxy( String host, int port )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setPort( port );

        setHttpProxy( proxyInfo );
    }

    public void setHttpProxy( String host, int port, String username, String password )
    {
        setHttpProxy( host, port );
        httpProxy.setUserName( username );
        httpProxy.setPassword( password );
    }

    public void setHttpProxy( String host, int port, String username, String password, String ntlmHost, String ntlmDomain )
    {
        setHttpProxy( host, port );
        httpProxy.setUserName( username );
        httpProxy.setPassword( password );
        httpProxy.setNtlmHost( ntlmHost );
        httpProxy.setNtlmDomain( ntlmDomain );
    }

    public ProxyInfo getHttpProxy()
    {
        return httpProxy;
    }

    /**
     * Used to add proxied repositories.
     *
     * @param repository the repository to be proxied
     */
    public void addRepository( ProxyRepository repository )
    {
        repositories.add( repository );
    }

    /**
     * Used to retrieve an unmodifyable list of proxied repositories. They returned list determines the search sequence
     * for retrieving artifacts.
     *
     * @return a list of ProxyRepository objects representing proxied repositories
     */
    public List getRepositories()
    {
        return Collections.unmodifiableList( repositories );
    }

    /**
     * Used to set the list of repositories to be proxied.  This replaces any repositories already added to this
     * configuraion instance.  Useful for re-arranging an existing proxied list.
     *
     * @param repositories
     */
    public void setRepositories( List repositories )
    {
        this.repositories = repositories;
    }

    public String getLayout()
    {
        if ( layout == null )
        {
            layout = "default";
        }

        return layout;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }
}
