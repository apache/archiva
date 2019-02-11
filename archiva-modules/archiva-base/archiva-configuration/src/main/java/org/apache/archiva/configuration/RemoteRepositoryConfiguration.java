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
 * Class RemoteRepositoryConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class RemoteRepositoryConfiguration
    extends AbstractRepositoryConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The URL for this repository.
     *           
     */
    private String url;

    /**
     * 
     *             The Username for this repository.
     *           
     */
    private String username;

    /**
     * 
     *             The Password for this repository.
     *           
     */
    private String password;

    /**
     * 
     *             Timeout in seconds for connections to this
     * repository
     *           .
     */
    private int timeout = 60;

    /**
     * 
     *             When to run the refresh task.
     *             Default is every sunday at 8H00.
     *           
     */
    private String refreshCronExpression = "0 0 08 ? * SUN";

    /**
     * 
     *             Activate download of remote index if
     * remoteIndexUrl is set too.
     *           
     */
    private boolean downloadRemoteIndex = false;

    /**
     * 
     *             Remote Index Url : if not starting with http
     * will be relative to the remote repository url.
     *           
     */
    private String remoteIndexUrl;

    /**
     * 
     *             Id of the networkProxy to use when downloading
     * remote index.
     *           
     */
    private String remoteDownloadNetworkProxyId;

    /**
     * 
     *             Timeout in seconds for download remote index.
     * Default is more long than artifact download.
     *           
     */
    private int remoteDownloadTimeout = 300;

    /**
     * 
     *             Schedule download of remote index when archiva
     * start
     *           .
     */
    private boolean downloadRemoteIndexOnStartup = false;

    /**
     * Field extraParameters.
     */
    private java.util.Map extraParameters;

    /**
     * Field extraHeaders.
     */
    private java.util.Map extraHeaders;

    /**
     * The path to check the repository availability (relative to
     * the repository URL). Some repositories do not allow
     * browsing, so a certain artifact must be checked.
     */
    private String checkPath;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addExtraHeader.
     * 
     * @param key
     * @param value
     */
    public void addExtraHeader( Object key, String value )
    {
        getExtraHeaders().put( key, value );
    } //-- void addExtraHeader( Object, String )

    /**
     * Method addExtraParameter.
     * 
     * @param key
     * @param value
     */
    public void addExtraParameter( Object key, String value )
    {
        getExtraParameters().put( key, value );
    } //-- void addExtraParameter( Object, String )

    /**
     * Get the path to check the repository availability (relative
     * to the repository URL). Some repositories do not allow
     * browsing, so a certain artifact must be checked.
     * 
     * @return String
     */
    public String getCheckPath()
    {
        return this.checkPath;
    } //-- String getCheckPath()

    /**
     * Method getExtraHeaders.
     * 
     * @return Map
     */
    public java.util.Map getExtraHeaders()
    {
        if ( this.extraHeaders == null )
        {
            this.extraHeaders = new java.util.HashMap();
        }

        return this.extraHeaders;
    } //-- java.util.Map getExtraHeaders()

    /**
     * Method getExtraParameters.
     * 
     * @return Map
     */
    public java.util.Map getExtraParameters()
    {
        if ( this.extraParameters == null )
        {
            this.extraParameters = new java.util.HashMap();
        }

        return this.extraParameters;
    } //-- java.util.Map getExtraParameters()

    /**
     * Get the Password for this repository.
     * 
     * @return String
     */
    public String getPassword()
    {
        return this.password;
    } //-- String getPassword()

    /**
     * Get when to run the refresh task.
     *             Default is every sunday at 8H00.
     * 
     * @return String
     */
    public String getRefreshCronExpression()
    {
        return this.refreshCronExpression;
    } //-- String getRefreshCronExpression()

    /**
     * Get id of the networkProxy to use when downloading remote
     * index.
     * 
     * @return String
     */
    public String getRemoteDownloadNetworkProxyId()
    {
        return this.remoteDownloadNetworkProxyId;
    } //-- String getRemoteDownloadNetworkProxyId()

    /**
     * Get timeout in seconds for download remote index. Default is
     * more long than artifact download.
     * 
     * @return int
     */
    public int getRemoteDownloadTimeout()
    {
        return this.remoteDownloadTimeout;
    } //-- int getRemoteDownloadTimeout()

    /**
     * Get remote Index Url : if not starting with http will be
     * relative to the remote repository url.
     * 
     * @return String
     */
    public String getRemoteIndexUrl()
    {
        return this.remoteIndexUrl;
    } //-- String getRemoteIndexUrl()

    /**
     * Get timeout in seconds for connections to this repository.
     * 
     * @return int
     */
    public int getTimeout()
    {
        return this.timeout;
    } //-- int getTimeout()

    /**
     * Get the URL for this repository.
     * 
     * @return String
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl()

    /**
     * Get the Username for this repository.
     * 
     * @return String
     */
    public String getUsername()
    {
        return this.username;
    } //-- String getUsername()

    /**
     * Get activate download of remote index if remoteIndexUrl is
     * set too.
     * 
     * @return boolean
     */
    public boolean isDownloadRemoteIndex()
    {
        return this.downloadRemoteIndex;
    } //-- boolean isDownloadRemoteIndex()

    /**
     * Get schedule download of remote index when archiva start.
     * 
     * @return boolean
     */
    public boolean isDownloadRemoteIndexOnStartup()
    {
        return this.downloadRemoteIndexOnStartup;
    } //-- boolean isDownloadRemoteIndexOnStartup()

    /**
     * Set the path to check the repository availability (relative
     * to the repository URL). Some repositories do not allow
     * browsing, so a certain artifact must be checked.
     * 
     * @param checkPath
     */
    public void setCheckPath( String checkPath )
    {
        this.checkPath = checkPath;
    } //-- void setCheckPath( String )

    /**
     * Set activate download of remote index if remoteIndexUrl is
     * set too.
     * 
     * @param downloadRemoteIndex
     */
    public void setDownloadRemoteIndex( boolean downloadRemoteIndex )
    {
        this.downloadRemoteIndex = downloadRemoteIndex;
    } //-- void setDownloadRemoteIndex( boolean )

    /**
     * Set schedule download of remote index when archiva start.
     * 
     * @param downloadRemoteIndexOnStartup
     */
    public void setDownloadRemoteIndexOnStartup( boolean downloadRemoteIndexOnStartup )
    {
        this.downloadRemoteIndexOnStartup = downloadRemoteIndexOnStartup;
    } //-- void setDownloadRemoteIndexOnStartup( boolean )

    /**
     * Set additional http headers to add to url when requesting
     * remote repositories.
     * 
     * @param extraHeaders
     */
    public void setExtraHeaders( java.util.Map extraHeaders )
    {
        this.extraHeaders = extraHeaders;
    } //-- void setExtraHeaders( java.util.Map )

    /**
     * Set additionnal request parameters to add to url when
     * requesting remote repositories.
     * 
     * @param extraParameters
     */
    public void setExtraParameters( java.util.Map extraParameters )
    {
        this.extraParameters = extraParameters;
    } //-- void setExtraParameters( java.util.Map )

    /**
     * Set the Password for this repository.
     * 
     * @param password
     */
    public void setPassword( String password )
    {
        this.password = password;
    } //-- void setPassword( String )

    /**
     * Set when to run the refresh task.
     *             Default is every sunday at 8H00.
     * 
     * @param refreshCronExpression
     */
    public void setRefreshCronExpression( String refreshCronExpression )
    {
        this.refreshCronExpression = refreshCronExpression;
    } //-- void setRefreshCronExpression( String )

    /**
     * Set id of the networkProxy to use when downloading remote
     * index.
     * 
     * @param remoteDownloadNetworkProxyId
     */
    public void setRemoteDownloadNetworkProxyId( String remoteDownloadNetworkProxyId )
    {
        this.remoteDownloadNetworkProxyId = remoteDownloadNetworkProxyId;
    } //-- void setRemoteDownloadNetworkProxyId( String )

    /**
     * Set timeout in seconds for download remote index. Default is
     * more long than artifact download.
     * 
     * @param remoteDownloadTimeout
     */
    public void setRemoteDownloadTimeout( int remoteDownloadTimeout )
    {
        this.remoteDownloadTimeout = remoteDownloadTimeout;
    } //-- void setRemoteDownloadTimeout( int )

    /**
     * Set remote Index Url : if not starting with http will be
     * relative to the remote repository url.
     * 
     * @param remoteIndexUrl
     */
    public void setRemoteIndexUrl( String remoteIndexUrl )
    {
        this.remoteIndexUrl = remoteIndexUrl;
    } //-- void setRemoteIndexUrl( String )

    /**
     * Set timeout in seconds for connections to this repository.
     * 
     * @param timeout
     */
    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    } //-- void setTimeout( int )

    /**
     * Set the URL for this repository.
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl( String )

    /**
     * Set the Username for this repository.
     * 
     * @param username
     */
    public void setUsername( String username )
    {
        this.username = username;
    } //-- void setUsername( String )

    
            public String toString()
            {
                return "RemoteRepositoryConfiguration id:'" + getId() + "',name:'" + getName() +"'";
            }


       
}
