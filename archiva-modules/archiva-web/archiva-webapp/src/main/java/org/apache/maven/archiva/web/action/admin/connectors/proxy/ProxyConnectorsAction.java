package org.apache.maven.archiva.web.action.admin.connectors.proxy;

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

import com.opensymphony.xwork2.Preparable;

import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProxyConnectorsAction
 *
 * @version $Id$
 *
 */
@Controller( "proxyConnectorsAction" )
@Scope( "prototype" )
public class ProxyConnectorsAction
    extends AbstractProxyConnectorAction
    implements Preparable
{
    private Map<String, AbstractRepositoryConfiguration> repoMap;

    /**
     * boolean to indicate that remote repo is present. Used for Add Link
     */
    private boolean remoteRepoExists=false;
    
    /**
     * Map of Proxy Connectors.
     */
    private Map<String, List<ProxyConnectorConfiguration>> proxyConnectorMap;

    public void prepare()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        repoMap = new HashMap<String, AbstractRepositoryConfiguration>();
        repoMap.putAll( config.getRemoteRepositoriesAsMap() );
        repoMap.putAll( config.getManagedRepositoriesAsMap() );

        proxyConnectorMap = createProxyConnectorMap();
        
        remoteRepoExists=config.getRemoteRepositories().size()>0;
    }

    public Map<String, AbstractRepositoryConfiguration> getRepoMap()
    {
        return repoMap;
    }

    public Map<String, List<ProxyConnectorConfiguration>> getProxyConnectorMap()
    {
        return proxyConnectorMap;
    }

	public boolean getRemoteRepoExists()
    {
        return remoteRepoExists;
    }
}
