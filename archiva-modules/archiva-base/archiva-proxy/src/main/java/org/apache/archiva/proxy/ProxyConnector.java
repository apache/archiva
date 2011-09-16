package org.apache.archiva.proxy;

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

import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.connector.RepositoryConnector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This represents a connector for a repository to repository proxy.
 *
 * @version $Id$
 */
public class ProxyConnector
    implements RepositoryConnector
{
    private ManagedRepositoryContent sourceRepository;

    private RemoteRepositoryContent targetRepository;

    private List<String> blacklist;

    private List<String> whitelist;

    private String proxyId;
    
    private int order;

    private Map<String, String> policies;
    
    private boolean disabled;

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled) 
    {
        this.disabled = disabled;
    }

    public List<String> getBlacklist()
    {
        return blacklist;
    }

    public void setBlacklist( List<String> blacklist )
    {
        this.blacklist = blacklist;
    }

    public ManagedRepositoryContent getSourceRepository()
    {
        return sourceRepository;
    }

    public void setSourceRepository( ManagedRepositoryContent sourceRepository )
    {
        this.sourceRepository = sourceRepository;
    }

    public RemoteRepositoryContent getTargetRepository()
    {
        return targetRepository;
    }

    public void setTargetRepository( RemoteRepositoryContent targetRepository )
    {
        this.targetRepository = targetRepository;
    }

    public List<String> getWhitelist()
    {
        return whitelist;
    }

    public void setWhitelist( List<String> whitelist )
    {
        this.whitelist = whitelist;
    }

    public Map<String, String> getPolicies()
    {
        return policies;
    }

    public void setPolicies( Map<String, String> policies )
    {
        this.policies = policies;
    }

    public String getProxyId()
    {
        return proxyId;
    }

    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "ProxyConnector[\n" );
        sb.append( "  source: [managed] " ).append( this.sourceRepository.getRepoRoot() ).append( "\n" );
        sb.append( "  target: [remote] " ).append( this.targetRepository.getRepository().getUrl() ).append( "\n" );
        sb.append( "  proxyId:" ).append( this.proxyId ).append( "\n" );

        Iterator<String> keys = this.policies.keySet().iterator();
        while ( keys.hasNext() )
        {
            String name = keys.next();
            sb.append( "  policy[" ).append( name ).append( "]:" );
            sb.append( this.policies.get( name ) ).append( "\n" );
        }

        sb.append( "]" );

        return sb.toString();
    }

    public void setPolicy( String policyId, String policySetting )
    {
        this.policies.put( policyId, policySetting );
    }

    public int getOrder()
    {
        return order;
    }

    public void setOrder( int order )
    {
        this.order = order;
    }
}
