package org.apache.archiva.admin.model;
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

import org.apache.archiva.admin.model.beans.PropertyEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public abstract class AbstractRepositoryConnector
    implements Serializable
{
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
     * field to ease json mapping wrapper on <code>policies</code> field
     *
     * @since 1.4-M3
     */
    private List<PropertyEntry> policiesEntries;

    /**
     * Field properties.
     */
    private Map<String, String> properties;

    /**
     * field to ease json mapping wrapper on <code>properties</code> field
     *
     * @since 1.4-M3
     */
    private List<PropertyEntry> propertiesEntries;

    /**
     * If the the repository proxy connector is disabled or not
     */
    private boolean disabled = false;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addBlackListPattern.
     *
     * @param string
     */
    public void addBlackListPattern( String string )
    {
        getBlackListPatterns().add( string );
    }

    /**
     * Method addPolicy.
     *
     * @param key
     * @param value
     */
    public void addPolicy( String key, String value )
    {
        getPolicies().put( key, value );
    }

    /**
     * Method addProperty.
     *
     * @param key
     * @param value
     */
    public void addProperty( String key, String value )
    {
        getProperties().put( key, value );
    }

    /**
     * Method addWhiteListPattern.
     *
     * @param string
     */
    public void addWhiteListPattern( String string )
    {
        getWhiteListPatterns().add( string );
    }

    /**
     * Method getBlackListPatterns.
     *
     * @return List
     */
    public List<String> getBlackListPatterns()
    {
        if ( this.blackListPatterns == null )
        {
            this.blackListPatterns = new ArrayList<>( 0 );
        }

        return this.blackListPatterns;
    }

    /**
     * Method getPolicies.
     *
     * @return Map
     */
    public Map<String, String> getPolicies()
    {
        if ( this.policies == null )
        {
            this.policies = new HashMap<>();
        }

        return this.policies;
    }

    /**
     * Method getProperties.
     *
     * @return Map
     */
    public Map<String, String> getProperties()
    {
        if ( this.properties == null )
        {
            this.properties = new HashMap<>();
        }

        return this.properties;
    }

    /**
     * Get the network proxy ID to use for this connector.
     *
     * @return String
     */
    public String getProxyId()
    {
        return this.proxyId;
    }

    /**
     * Get the Repository Source for this connector.
     *
     * @return String
     */
    public String getSourceRepoId()
    {
        return this.sourceRepoId;
    }

    /**
     * Get the Repository Target for this connector.
     *
     * @return String
     */
    public String getTargetRepoId()
    {
        return this.targetRepoId;
    }

    /**
     * Method getWhiteListPatterns.
     *
     * @return List
     */
    public List<String> getWhiteListPatterns()
    {
        if ( this.whiteListPatterns == null )
        {
            this.whiteListPatterns = new ArrayList<>( 0 );
        }

        return this.whiteListPatterns;
    }

    /**
     * Get if the the repository proxy connector is disabled or not
     * .
     *
     * @return boolean
     */
    public boolean isDisabled()
    {
        return this.disabled;
    }

    /**
     * Method removeBlackListPattern.
     *
     * @param string
     */
    public void removeBlackListPattern( String string )
    {
        getBlackListPatterns().remove( string );
    }

    /**
     * Method removeWhiteListPattern.
     *
     * @param string
     */
    public void removeWhiteListPattern( String string )
    {
        getWhiteListPatterns().remove( string );
    }

    /**
     * Set the list of blacklisted patterns for this connector.
     *
     * @param blackListPatterns
     */
    public void setBlackListPatterns( List<String> blackListPatterns )
    {
        this.blackListPatterns = blackListPatterns;
    }

    /**
     * Set if the the repository proxy connector is
     * disabled or not
     * .
     *
     * @param disabled
     */
    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }

    /**
     * Set policy configuration for the connector.
     *
     * @param policies
     */
    public void setPolicies( Map<String, String> policies )
    {
        this.policies = policies;
    }

    /**
     * Set configuration for the connector.
     *
     * @param properties
     */
    public void setProperties( Map<String, String> properties )
    {
        this.properties = properties;
    }

    /**
     * Set the network proxy ID to use for this connector.
     *
     * @param proxyId
     */
    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    }

    /**
     * Set the Repository Source for this connector.
     *
     * @param sourceRepoId
     */
    public void setSourceRepoId( String sourceRepoId )
    {
        this.sourceRepoId = sourceRepoId;
    }

    /**
     * Set the Repository Target for this connector.
     *
     * @param targetRepoId
     */
    public void setTargetRepoId( String targetRepoId )
    {
        this.targetRepoId = targetRepoId;
    }

    /**
     * Set
     * The list of whitelisted patterns for this
     * connector.
     *
     * @param whiteListPatterns
     */
    public void setWhiteListPatterns( List<String> whiteListPatterns )
    {
        this.whiteListPatterns = whiteListPatterns;
    }


    /**
     * Obtain a specific policy from the underlying connector.
     *
     * @param policyId     the policy id to fetch.
     * @param defaultValue the default value for the policy id.
     * @return the configured policy value (or default value if not found).
     */
    public String getPolicy( String policyId, String defaultValue )
    {
        if ( this.getPolicies() == null )
        {
            return null;
        }

        String value = this.getPolicies().get( policyId );

        if ( value == null )
        {
            return defaultValue;
        }

        return value;
    }

    public List<PropertyEntry> getPoliciesEntries()
    {
        policiesEntries = new ArrayList<>( getPolicies().size() );
        for ( Map.Entry<String, String> entry : getPolicies().entrySet() )
        {
            policiesEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        return policiesEntries;
    }

    public void setPoliciesEntries( List<PropertyEntry> policiesEntries )
    {
        for ( PropertyEntry propertyEntry : policiesEntries )
        {
            addPolicy( propertyEntry.getKey(), propertyEntry.getValue() );
        }
    }

    public List<PropertyEntry> getPropertiesEntries()
    {
        propertiesEntries = new ArrayList<>( getProperties().size() );
        for ( Map.Entry<String, String> entry : getProperties().entrySet() )
        {
            propertiesEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        return propertiesEntries;
    }

    public void setPropertiesEntries( List<PropertyEntry> propertiesEntries )
    {
        for ( PropertyEntry propertyEntry : propertiesEntries )
        {
            addProperty( propertyEntry.getKey(), propertyEntry.getValue() );
        }
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        AbstractRepositoryConnector that = (AbstractRepositoryConnector) o;

        if ( sourceRepoId != null ? !sourceRepoId.equals( that.sourceRepoId ) : that.sourceRepoId != null )
        {
            return false;
        }
        if ( targetRepoId != null ? !targetRepoId.equals( that.targetRepoId ) : that.targetRepoId != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = sourceRepoId != null ? sourceRepoId.hashCode() : 0;
        result = 31 * result + ( targetRepoId != null ? targetRepoId.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "AbstractRepositoryConnector" );
        sb.append( "{sourceRepoId='" ).append( sourceRepoId ).append( '\'' );
        sb.append( ", targetRepoId='" ).append( targetRepoId ).append( '\'' );
        sb.append( ", proxyId='" ).append( proxyId ).append( '\'' );
        sb.append( ", blackListPatterns=" ).append( blackListPatterns );
        sb.append( ", whiteListPatterns=" ).append( whiteListPatterns );
        sb.append( ", policies=" ).append( policies );
        sb.append( ", properties=" ).append( properties );
        sb.append( ", disabled=" ).append( disabled );
        sb.append( '}' );
        return sb.toString();
    }
}

