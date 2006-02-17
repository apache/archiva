package org.apache.maven.repository.proxy.repository;

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

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * Class to represent the Proxy repository.  Currently does not provide additional methods from
 * DefaultArtifactRepository but is expected to do so like enabled/disabled when a UI is present.
 *
 * @author Edwin Punzalan
 */
public class ProxyRepository
    extends DefaultArtifactRepository
{
    // zero does not cache
    private long cachePeriod = 0;

    private boolean cacheFailures = false;

    private ProxyInfo proxy;

    public ProxyRepository( String id, String url, ArtifactRepositoryLayout layout, boolean cacheFailures,
                            long cachePeriod )
    {
        this( id, url, layout );

        setCacheFailures( cacheFailures );

        setCachePeriod( cachePeriod );
    }

    public ProxyRepository( String id, String url, ArtifactRepositoryLayout layout )
    {
        super( id, url, layout );
    }

    public long getCachePeriod()
    {
        return cachePeriod;
    }

    public void setCachePeriod( long cachePeriod )
    {
        this.cachePeriod = cachePeriod;
    }

    public boolean isCacheFailures()
    {
        return cacheFailures;
    }

    public void setCacheFailures( boolean cacheFailures )
    {
        this.cacheFailures = cacheFailures;
    }

    public boolean isProxied()
    {
        return ( proxy != null );
    }

    public ProxyInfo getProxy()
    {
        return proxy;
    }

    public void setProxy( String host, int port )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setPort( port );

        setProxy( proxyInfo );
    }

    public void setProxy( String host, int port, String username, String password )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setPort( port );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        setProxy( proxyInfo );
    }

    public void setProxy( String host, int port, String username, String password, String ntlmHost, String ntlmDomain )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setPort( port );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );
        proxyInfo.setNtlmHost( ntlmHost );
        proxyInfo.setNtlmDomain( ntlmDomain );

        setProxy( proxyInfo );
    }

    public void setProxy( ProxyInfo proxy )
    {
        this.proxy = proxy;
    }
}
