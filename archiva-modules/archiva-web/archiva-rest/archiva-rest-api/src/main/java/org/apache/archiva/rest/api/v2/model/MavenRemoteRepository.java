package org.apache.archiva.rest.api.v2.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Schema(name="MavenRemoteRepository",description = "A remote repository definition is used to pull artifacts from other repositories")
public class MavenRemoteRepository extends Repository
{
    private static final long serialVersionUID = 5625043398766480265L;
    String loginUser;
    String loginPassword;
    String checkPath;
    Map<String,String> extraParameters = new TreeMap<>(  );
    Map<String,String> requestHeader = new TreeMap<>(  );
    long timeoutMs;

    @Schema(name="login_user",description = "Username for login to the remote repository")
    public String getLoginUser( )
    {
        return loginUser;
    }

    public void setLoginUser( String loginUser )
    {
        this.loginUser = loginUser;
    }

    @Schema(name="login_password",description = "Password for connecting to the remote repository")
    public String getLoginPassword( )
    {
        return loginPassword;
    }

    public void setLoginPassword( String loginPassword )
    {
        this.loginPassword = loginPassword;
    }

    @Schema(name="check_path",description = "Path relative to the repository URL that is used to check of availability of the remote repository.")
    public String getCheckPath( )
    {
        return checkPath;
    }

    public void setCheckPath( String checkPath )
    {
        this.checkPath = checkPath;
    }

    @Schema(name="extra_parameters", description = "Key-Value map with extra parameters sent to the remote repository")
    public Map<String, String> getExtraParameters( )
    {
        return extraParameters;
    }

    public void setExtraParameters( Map<String, String> extraParameters )
    {
        this.extraParameters = new TreeMap<>( extraParameters );
    }

    public void addExtraParameter(String key, String value) {
        this.extraParameters.put( key, value );
    }

    @Schema(name="request_header",description = "Key-Value map with request headers that are sent to the remote repository")
    public Map<String, String> getRequestHeader( )
    {
        return requestHeader;
    }

    public void setRequestHeader( Map<String, String> requestHeader )
    {
        this.requestHeader = new TreeMap<>( requestHeader );
    }

    public void addRequestHeader(String headerName, String headerValue) {
        this.requestHeader.put( headerName, headerValue );
    }

    @Schema(name="timeout_ms", description = "The time in milliseconds after that a request to the remote repository is aborted")
    public long getTimeoutMs( )
    {
        return timeoutMs;
    }

    public void setTimeoutMs( long timeoutMs )
    {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        MavenRemoteRepository that = (MavenRemoteRepository) o;

        if ( timeoutMs != that.timeoutMs ) return false;
        if ( loginUser != null ? !loginUser.equals( that.loginUser ) : that.loginUser != null ) return false;
        if ( loginPassword != null ? !loginPassword.equals( that.loginPassword ) : that.loginPassword != null )
            return false;
        if ( checkPath != null ? !checkPath.equals( that.checkPath ) : that.checkPath != null ) return false;
        if ( extraParameters != null ? !extraParameters.equals( that.extraParameters ) : that.extraParameters != null )
            return false;
        return requestHeader != null ? requestHeader.equals( that.requestHeader ) : that.requestHeader == null;
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + ( loginUser != null ? loginUser.hashCode( ) : 0 );
        result = 31 * result + ( loginPassword != null ? loginPassword.hashCode( ) : 0 );
        result = 31 * result + ( checkPath != null ? checkPath.hashCode( ) : 0 );
        result = 31 * result + ( extraParameters != null ? extraParameters.hashCode( ) : 0 );
        result = 31 * result + ( requestHeader != null ? requestHeader.hashCode( ) : 0 );
        result = 31 * result + (int) ( timeoutMs ^ ( timeoutMs >>> 32 ) );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "RemoteRepository{" );
        sb.append( "loginUser='" ).append( loginUser ).append( '\'' );
        sb.append( ", loginPassword='" ).append( loginPassword ).append( '\'' );
        sb.append( ", checkPath='" ).append( checkPath ).append( '\'' );
        sb.append( ", extraParameters=" ).append( extraParameters );
        sb.append( ", requestHeader=" ).append( requestHeader );
        sb.append( ", timeOut=" ).append( timeoutMs );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", scanned=" ).append( scanned );
        sb.append( ", schedulingDefinition='" ).append( schedulingDefinition ).append( '\'' );
        sb.append( ", index=" ).append( index );
        sb.append( ", layout='" ).append( layout ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
