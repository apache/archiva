package org.apache.archiva.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "proxyConnector" )
public class ProxyConnector
    implements Serializable
{
    /**
     * The order of the proxy connectors. (0 means no order specified)
     * .
     */
    private int order = 0;

    /**
     * The Repository Source for this connector.
     */
    private String sourceRepoId;

    /**
     * The Repository Target for this connector.
     */
    private String targetRepoId;

    /**
     * The network proxy ID to use for this connector.
     */
    private String proxyId;

    /**
     * Field blackListPatterns.
     */
    private List<String> blackListPatterns;

    /**
     * Field whiteListPatterns.
     */
    private List<String> whiteListPatterns;

    /**
     * Field policies.
     */
    private Map<String, String> policies;

    /**
     * Field properties.
     */
    private Map<String, String> properties;

    /**
     * If the the repository proxy connector is disabled or not
     */
    private boolean disabled = false;

    /**
     * Get the order of the proxy connectors. (0 means no order specified)
     *
     * @return int
     */
    public int getOrder()
    {
        return this.order;
    }


    /**
     * Set the order of the proxy connectors. (0 means no order specified)
     *
     * @param order
     */
    public void setOrder( int order )
    {
        this.order = order;
    }

    public String getSourceRepoId()
    {
        return sourceRepoId;
    }

    public void setSourceRepoId( String sourceRepoId )
    {
        this.sourceRepoId = sourceRepoId;
    }

    public String getTargetRepoId()
    {
        return targetRepoId;
    }

    public void setTargetRepoId( String targetRepoId )
    {
        this.targetRepoId = targetRepoId;
    }

    public String getProxyId()
    {
        return proxyId;
    }

    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    }

    public List<String> getBlackListPatterns()
    {
        return blackListPatterns;
    }

    public void setBlackListPatterns( List<String> blackListPatterns )
    {
        this.blackListPatterns = blackListPatterns;
    }

    public List<String> getWhiteListPatterns()
    {
        return whiteListPatterns;
    }

    public void setWhiteListPatterns( List<String> whiteListPatterns )
    {
        this.whiteListPatterns = whiteListPatterns;
    }

    public Map<String, String> getPolicies()
    {
        return policies;
    }

    public void setPolicies( Map<String, String> policies )
    {
        this.policies = policies;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties( Map<String, String> properties )
    {
        this.properties = properties;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }
}
