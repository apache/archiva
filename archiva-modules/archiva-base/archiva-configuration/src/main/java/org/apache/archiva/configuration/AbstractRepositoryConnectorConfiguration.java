package org.apache.archiva.configuration;

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

/**
 * Class AbstractRepositoryConnectorConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class AbstractRepositoryConnectorConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The Repository Source for this connector.
     *           
     */
    private String sourceRepoId;

    /**
     * 
     *             The Repository Target for this connector.
     *           
     */
    private String targetRepoId;

    /**
     * 
     *             The network proxy ID to use for this connector.
     *           
     */
    private String proxyId;

    /**
     * Field blackListPatterns.
     */
    private java.util.List<String> blackListPatterns;

    /**
     * Field whiteListPatterns.
     */
    private java.util.List<String> whiteListPatterns;

    /**
     * Field policies.
     */
    private java.util.Map policies;

    /**
     * Field properties.
     */
    private java.util.Map properties;

    /**
     * 
     *             If the the repository proxy connector is
     * disabled or not
     *           .
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
    } //-- void addBlackListPattern( String )

    /**
     * Method addPolicy.
     * 
     * @param key
     * @param value
     */
    public void addPolicy( String key, String value )
    {
        getPolicies().put( key, value );
    } //-- void addPolicy( Object, String )

    /**
     * Method addProperty.
     * 
     * @param key
     * @param value
     */
    public void addProperty( String key, String value )
    {
        getProperties().put( key, value );
    } //-- void addProperty( Object, String )

    /**
     * Method addWhiteListPattern.
     * 
     * @param string
     */
    public void addWhiteListPattern( String string )
    {
        getWhiteListPatterns().add( string );
    } //-- void addWhiteListPattern( String )

    /**
     * Method getBlackListPatterns.
     * 
     * @return List
     */
    public java.util.List<String> getBlackListPatterns()
    {
        if ( this.blackListPatterns == null )
        {
            this.blackListPatterns = new java.util.ArrayList<String>();
        }

        return this.blackListPatterns;
    } //-- java.util.List<String> getBlackListPatterns()

    /**
     * Method getPolicies.
     * 
     * @return Map
     */
    public java.util.Map<String, String> getPolicies()
    {
        if ( this.policies == null )
        {
            this.policies = new java.util.HashMap();
        }

        return this.policies;
    } //-- java.util.Map getPolicies()

    /**
     * Method getProperties.
     * 
     * @return Map
     */
    public java.util.Map<String, String> getProperties()
    {
        if ( this.properties == null )
        {
            this.properties = new java.util.HashMap();
        }

        return this.properties;
    } //-- java.util.Map getProperties()

    /**
     * Get the network proxy ID to use for this connector.
     * 
     * @return String
     */
    public String getProxyId()
    {
        return this.proxyId;
    } //-- String getProxyId()

    /**
     * Get the Repository Source for this connector.
     * 
     * @return String
     */
    public String getSourceRepoId()
    {
        return this.sourceRepoId;
    } //-- String getSourceRepoId()

    /**
     * Get the Repository Target for this connector.
     * 
     * @return String
     */
    public String getTargetRepoId()
    {
        return this.targetRepoId;
    } //-- String getTargetRepoId()

    /**
     * Method getWhiteListPatterns.
     * 
     * @return List
     */
    public java.util.List<String> getWhiteListPatterns()
    {
        if ( this.whiteListPatterns == null )
        {
            this.whiteListPatterns = new java.util.ArrayList<String>();
        }

        return this.whiteListPatterns;
    } //-- java.util.List<String> getWhiteListPatterns()

    /**
     * Get if the the repository proxy connector is disabled or
     * not.
     * 
     * @return boolean
     */
    public boolean isDisabled()
    {
        return this.disabled;
    } //-- boolean isDisabled()

    /**
     * Method removeBlackListPattern.
     * 
     * @param string
     */
    public void removeBlackListPattern( String string )
    {
        getBlackListPatterns().remove( string );
    } //-- void removeBlackListPattern( String )

    /**
     * Method removeWhiteListPattern.
     * 
     * @param string
     */
    public void removeWhiteListPattern( String string )
    {
        getWhiteListPatterns().remove( string );
    } //-- void removeWhiteListPattern( String )

    /**
     * Set the list of blacklisted patterns for this connector.
     * 
     * @param blackListPatterns
     */
    public void setBlackListPatterns( java.util.List<String> blackListPatterns )
    {
        this.blackListPatterns = blackListPatterns;
    } //-- void setBlackListPatterns( java.util.List )

    /**
     * Set if the the repository proxy connector is disabled or
     * not.
     * 
     * @param disabled
     */
    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    } //-- void setDisabled( boolean )

    /**
     * Set policy configuration for the connector.
     * 
     * @param policies
     */
    public void setPolicies( java.util.Map policies )
    {
        this.policies = policies;
    } //-- void setPolicies( java.util.Map )

    /**
     * Set configuration for the connector.
     * 
     * @param properties
     */
    public void setProperties( java.util.Map properties )
    {
        this.properties = properties;
    } //-- void setProperties( java.util.Map )

    /**
     * Set the network proxy ID to use for this connector.
     * 
     * @param proxyId
     */
    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    } //-- void setProxyId( String )

    /**
     * Set the Repository Source for this connector.
     * 
     * @param sourceRepoId
     */
    public void setSourceRepoId( String sourceRepoId )
    {
        this.sourceRepoId = sourceRepoId;
    } //-- void setSourceRepoId( String )

    /**
     * Set the Repository Target for this connector.
     * 
     * @param targetRepoId
     */
    public void setTargetRepoId( String targetRepoId )
    {
        this.targetRepoId = targetRepoId;
    } //-- void setTargetRepoId( String )

    /**
     * Set the list of whitelisted patterns for this connector.
     * 
     * @param whiteListPatterns
     */
    public void setWhiteListPatterns( java.util.List<String> whiteListPatterns )
    {
        this.whiteListPatterns = whiteListPatterns;
    } //-- void setWhiteListPatterns( java.util.List )

    
    /**
     * Obtain a specific policy from the underlying connector.
     *
     * @param policyId the policy id to fetch.
     * @param defaultValue the default value for the policy id.
     * @return the configured policy value (or default value if not found).
     */
    public String getPolicy( String policyId, String defaultValue )
    {
        if ( this.getPolicies() == null )
        {
            return null;
        }

        Object value = this.getPolicies().get( policyId );

        if ( value == null )
        {
            return defaultValue;
        }

        return (String) value;
    }
          
}
