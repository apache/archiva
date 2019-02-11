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
 * 
 *         The LDAP configuration.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class LdapConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * The LDAP host.
     */
    private String hostName;

    /**
     * The LDAP port.
     */
    private int port = 0;

    /**
     * ssl LDAP connection.
     */
    private boolean ssl = false;

    /**
     * The LDAP base dn.
     */
    private String baseDn;

    /**
     * The LDAP base dn for groups (if empty baseDn is used).
     */
    private String baseGroupsDn;

    /**
     * contextFactory to use.
     */
    private String contextFactory;

    /**
     * The LDAP bind dn.
     */
    private String bindDn;

    /**
     * The LDAP password.
     */
    private String password;

    /**
     * The LDAP authenticationMethod.
     */
    private String authenticationMethod;

    /**
     * The LDAP authenticator enabled.
     */
    private boolean bindAuthenticatorEnabled = false;

    /**
     * LDAP writable.
     */
    private boolean writable = false;

    /**
     * Will use role name as LDAP group.
     */
    private boolean useRoleNameAsGroup = false;

    /**
     * Field extraProperties.
     */
    private java.util.Map extraProperties;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addExtraProperty.
     * 
     * @param key
     * @param value
     */
    public void addExtraProperty( Object key, String value )
    {
        getExtraProperties().put( key, value );
    } //-- void addExtraProperty( Object, String )

    /**
     * Get the LDAP authenticationMethod.
     * 
     * @return String
     */
    public String getAuthenticationMethod()
    {
        return this.authenticationMethod;
    } //-- String getAuthenticationMethod()

    /**
     * Get the LDAP base dn.
     * 
     * @return String
     */
    public String getBaseDn()
    {
        return this.baseDn;
    } //-- String getBaseDn()

    /**
     * Get the LDAP base dn for groups (if empty baseDn is used).
     * 
     * @return String
     */
    public String getBaseGroupsDn()
    {
        return this.baseGroupsDn;
    } //-- String getBaseGroupsDn()

    /**
     * Get the LDAP bind dn.
     * 
     * @return String
     */
    public String getBindDn()
    {
        return this.bindDn;
    } //-- String getBindDn()

    /**
     * Get contextFactory to use.
     * 
     * @return String
     */
    public String getContextFactory()
    {
        return this.contextFactory;
    } //-- String getContextFactory()

    /**
     * Method getExtraProperties.
     * 
     * @return Map
     */
    public java.util.Map getExtraProperties()
    {
        if ( this.extraProperties == null )
        {
            this.extraProperties = new java.util.HashMap();
        }

        return this.extraProperties;
    } //-- java.util.Map getExtraProperties()

    /**
     * Get the LDAP host.
     * 
     * @return String
     */
    public String getHostName()
    {
        return this.hostName;
    } //-- String getHostName()

    /**
     * Get the LDAP password.
     * 
     * @return String
     */
    public String getPassword()
    {
        return this.password;
    } //-- String getPassword()

    /**
     * Get the LDAP port.
     * 
     * @return int
     */
    public int getPort()
    {
        return this.port;
    } //-- int getPort()

    /**
     * Get the LDAP authenticator enabled.
     * 
     * @return boolean
     */
    public boolean isBindAuthenticatorEnabled()
    {
        return this.bindAuthenticatorEnabled;
    } //-- boolean isBindAuthenticatorEnabled()

    /**
     * Get ssl LDAP connection.
     * 
     * @return boolean
     */
    public boolean isSsl()
    {
        return this.ssl;
    } //-- boolean isSsl()

    /**
     * Get will use role name as LDAP group.
     * 
     * @return boolean
     */
    public boolean isUseRoleNameAsGroup()
    {
        return this.useRoleNameAsGroup;
    } //-- boolean isUseRoleNameAsGroup()

    /**
     * Get lDAP writable.
     * 
     * @return boolean
     */
    public boolean isWritable()
    {
        return this.writable;
    } //-- boolean isWritable()

    /**
     * Set the LDAP authenticationMethod.
     * 
     * @param authenticationMethod
     */
    public void setAuthenticationMethod( String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod;
    } //-- void setAuthenticationMethod( String )

    /**
     * Set the LDAP base dn.
     * 
     * @param baseDn
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    } //-- void setBaseDn( String )

    /**
     * Set the LDAP base dn for groups (if empty baseDn is used).
     * 
     * @param baseGroupsDn
     */
    public void setBaseGroupsDn( String baseGroupsDn )
    {
        this.baseGroupsDn = baseGroupsDn;
    } //-- void setBaseGroupsDn( String )

    /**
     * Set the LDAP authenticator enabled.
     * 
     * @param bindAuthenticatorEnabled
     */
    public void setBindAuthenticatorEnabled( boolean bindAuthenticatorEnabled )
    {
        this.bindAuthenticatorEnabled = bindAuthenticatorEnabled;
    } //-- void setBindAuthenticatorEnabled( boolean )

    /**
     * Set the LDAP bind dn.
     * 
     * @param bindDn
     */
    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn;
    } //-- void setBindDn( String )

    /**
     * Set contextFactory to use.
     * 
     * @param contextFactory
     */
    public void setContextFactory( String contextFactory )
    {
        this.contextFactory = contextFactory;
    } //-- void setContextFactory( String )

    /**
     * Set additional properties to use for ldap connection.
     * 
     * @param extraProperties
     */
    public void setExtraProperties( java.util.Map extraProperties )
    {
        this.extraProperties = extraProperties;
    } //-- void setExtraProperties( java.util.Map )

    /**
     * Set the LDAP host.
     * 
     * @param hostName
     */
    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    } //-- void setHostName( String )

    /**
     * Set the LDAP password.
     * 
     * @param password
     */
    public void setPassword( String password )
    {
        this.password = password;
    } //-- void setPassword( String )

    /**
     * Set the LDAP port.
     * 
     * @param port
     */
    public void setPort( int port )
    {
        this.port = port;
    } //-- void setPort( int )

    /**
     * Set ssl LDAP connection.
     * 
     * @param ssl
     */
    public void setSsl( boolean ssl )
    {
        this.ssl = ssl;
    } //-- void setSsl( boolean )

    /**
     * Set will use role name as LDAP group.
     * 
     * @param useRoleNameAsGroup
     */
    public void setUseRoleNameAsGroup( boolean useRoleNameAsGroup )
    {
        this.useRoleNameAsGroup = useRoleNameAsGroup;
    } //-- void setUseRoleNameAsGroup( boolean )

    /**
     * Set lDAP writable.
     * 
     * @param writable
     */
    public void setWritable( boolean writable )
    {
        this.writable = writable;
    } //-- void setWritable( boolean )

}
