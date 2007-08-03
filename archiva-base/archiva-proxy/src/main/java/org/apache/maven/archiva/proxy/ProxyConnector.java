package org.apache.maven.archiva.proxy;

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

import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.connector.RepositoryConnector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This represents a connector for a repository to repository proxy.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProxyConnector
    implements RepositoryConnector
{
    private ArchivaRepository sourceRepository;

    private ArchivaRepository targetRepository;

    private List blacklist;

    private List whitelist;

    private String proxyId;

    private Map policies;

    public List getBlacklist()
    {
        return blacklist;
    }

    public void setBlacklist( List blacklist )
    {
        this.blacklist = blacklist;
    }

    public ArchivaRepository getSourceRepository()
    {
        return sourceRepository;
    }

    public void setSourceRepository( ArchivaRepository sourceRepository )
    {
        this.sourceRepository = sourceRepository;
    }

    public ArchivaRepository getTargetRepository()
    {
        return targetRepository;
    }

    public void setTargetRepository( ArchivaRepository targetRepository )
    {
        this.targetRepository = targetRepository;
    }

    public List getWhitelist()
    {
        return whitelist;
    }

    public void setWhitelist( List whitelist )
    {
        this.whitelist = whitelist;
    }

    public Map getPolicies()
    {
        return policies;
    }

    public void setPolicies( Map policies )
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

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "ProxyConnector[\n" );
        sb.append( "  source:" ).append( this.sourceRepository ).append( "\n" );
        sb.append( "  target:" ).append( this.targetRepository ).append( "\n" );
        sb.append( "  proxyId:" ).append( this.proxyId ).append( "\n" );

        Iterator keys = this.policies.keySet().iterator();
        while ( keys.hasNext() )
        {
            String name = (String) keys.next();
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
}
