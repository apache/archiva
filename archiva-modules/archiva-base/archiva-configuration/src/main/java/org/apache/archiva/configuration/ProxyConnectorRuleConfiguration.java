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
 * Class ProxyConnectorRuleConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ProxyConnectorRuleConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The type if this rule: whiteList, blackList
     * etc..
     *           
     */
    private String ruleType;

    /**
     * 
     *             The pattern for this rule: whiteList, blackList
     * etc..
     *           
     */
    private String pattern;

    /**
     * Field proxyConnectors.
     */
    private java.util.List<ProxyConnectorConfiguration> proxyConnectors;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addProxyConnector.
     * 
     * @param proxyConnectorConfiguration
     */
    public void addProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        getProxyConnectors().add( proxyConnectorConfiguration );
    } //-- void addProxyConnector( ProxyConnectorConfiguration )

    /**
     * Get the pattern for this rule: whiteList, blackList etc..
     * 
     * @return String
     */
    public String getPattern()
    {
        return this.pattern;
    } //-- String getPattern()

    /**
     * Method getProxyConnectors.
     * 
     * @return List
     */
    public java.util.List<ProxyConnectorConfiguration> getProxyConnectors()
    {
        if ( this.proxyConnectors == null )
        {
            this.proxyConnectors = new java.util.ArrayList<ProxyConnectorConfiguration>();
        }

        return this.proxyConnectors;
    } //-- java.util.List<ProxyConnectorConfiguration> getProxyConnectors()

    /**
     * Get the type if this rule: whiteList, blackList etc..
     * 
     * @return String
     */
    public String getRuleType()
    {
        return this.ruleType;
    } //-- String getRuleType()

    /**
     * Method removeProxyConnector.
     * 
     * @param proxyConnectorConfiguration
     */
    public void removeProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        getProxyConnectors().remove( proxyConnectorConfiguration );
    } //-- void removeProxyConnector( ProxyConnectorConfiguration )

    /**
     * Set the pattern for this rule: whiteList, blackList etc..
     * 
     * @param pattern
     */
    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    } //-- void setPattern( String )

    /**
     * Set associated proxyConnectors configuration.
     * 
     * @param proxyConnectors
     */
    public void setProxyConnectors( java.util.List<ProxyConnectorConfiguration> proxyConnectors )
    {
        this.proxyConnectors = proxyConnectors;
    } //-- void setProxyConnectors( java.util.List )

    /**
     * Set the type if this rule: whiteList, blackList etc..
     * 
     * @param ruleType
     */
    public void setRuleType( String ruleType )
    {
        this.ruleType = ruleType;
    } //-- void setRuleType( String )

}
