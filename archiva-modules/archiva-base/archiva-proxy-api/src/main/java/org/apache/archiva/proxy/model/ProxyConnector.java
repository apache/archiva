package org.apache.archiva.proxy.model;

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

import org.apache.archiva.policies.Policy;
import org.apache.archiva.policies.PolicyOption;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.connector.RepositoryConnector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This represents a connector for a repository to a remote repository that is proxied.
 */
public class ProxyConnector
    implements RepositoryConnector
{
    private ManagedRepository sourceRepository;

    private RemoteRepository targetRepository;

    private List<String> blacklist;

    private List<String> whitelist;

    private String proxyId;

    private int order;

    private Map<Policy, PolicyOption> policies;

    private boolean enabled;

    private Map<String, String> properties;

    public ProxyConnector()
    {
        // no op
    }

    /**
     * @see RepositoryConnector#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * @see RepositoryConnector#enable()
     */
    @Override
    public void enable()
    {
        this.enabled = true;
    }

    /**
     * @see RepositoryConnector#disable()
     */
    @Override
    public void disable( )
    {
        this.enabled = false;
    }

    /**
     * @see RepositoryConnector#getBlacklist()
     */
    @Override
    public List<String> getBlacklist()
    {
        return blacklist;
    }

    /**
     * Sets the blacklist. The list is a string of paths.
     *
     * @param blacklist List of paths.
     */
    public void setBlacklist( List<String> blacklist )
    {
        this.blacklist = blacklist;
    }

    /**
     * @see RepositoryConnector#getSourceRepository()
     */
    @Override
    public ManagedRepository getSourceRepository()
    {
        return sourceRepository;
    }

    /**
     * Sets the source repository.
     * @param sourceRepository The managed repository which is the local representation of the proxy.
     */
    public void setSourceRepository( ManagedRepository sourceRepository )
    {
        this.sourceRepository = sourceRepository;
    }

    /**
     * @see ProxyConnector#getTargetRepository()
     */
    @Override
    public RemoteRepository getTargetRepository()
    {
        return targetRepository;
    }

    /**
     * Sets the target repository.
     * @param targetRepository The remote repository, where the artifacts are downloaded from.
     */
    public void setTargetRepository( RemoteRepository targetRepository )
    {
        this.targetRepository = targetRepository;
    }

    /**
     * @see ProxyConnector#getWhitelist()
     */
    @Override
    public List<String> getWhitelist()
    {
        return whitelist;
    }

    /**
     * Sets the list of paths that are proxied.
     * @param whitelist List of paths.
     */
    public void setWhitelist( List<String> whitelist )
    {
        this.whitelist = whitelist;
    }

    /**
     * Returns the policies that are defined
     * @return
     */
    public Map<Policy, PolicyOption> getPolicies()
    {
        return policies;
    }

    /**
     * Sets policies that set the behaviour of this proxy connector.
     * @param policies A map of policies with each option.
     */
    public void setPolicies( Map<Policy, PolicyOption> policies )
    {
        this.policies = policies;
    }

    /**
     * Adds a new policy.
     * @param policy The policy to add.
     * @param option  The option for the policy.
     */
    public void addPolicy( Policy policy, PolicyOption option )
    {
        this.policies.put( policy, option );
    }

    /**
     * Returns the id of this proxy connector.
     * @return The id string.
     */
    public String getProxyId()
    {
        return proxyId;
    }

    /**
     * Sets the id of this proxy connector.
     * @param proxyId A id string.
     */
    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ProxyConnector[\n" );
        sb.append( "  source: [managed] " ).append( this.sourceRepository.getId() ).append( "\n" );
        sb.append( "  target: [remote] " ).append( this.targetRepository.getId() ).append( "\n" );
        sb.append( "  proxyId:" ).append( this.proxyId ).append( "\n" );

        Iterator<Policy> keys = this.policies.keySet().iterator();
        while ( keys.hasNext() )
        {
            String name = keys.next().getId();
            sb.append( "  policy[" ).append( name ).append( "]:" );
            sb.append( this.policies.get( name ) ).append( "\n" );
        }

        sb.append( "]" );

        return sb.toString();
    }

    /**
     * Returns a number that orders the proxy connectors numerically.
     * @return The order number of this connector.
     */
    public int getOrder()
    {
        return order;
    }

    /**
     * Set the order number of this proxy connector.
     *
     * @param order The order number.
     */
    public void setOrder( int order )
    {
        this.order = order;
    }

    /**
     * Returns additional properties defined for this connector.
     * @return Map of key, value pairs.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets additional properties for this connector.
     * @param properties Map of key, value pairs.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
