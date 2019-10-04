package org.apache.archiva.repository.base;

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


import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.RepositoryCredentials;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Abstract implementation of a remote repository. Abstract classes must implement the
 * features and capabilities by themselves.
 */
public abstract class AbstractRemoteRepository extends AbstractRepository implements EditableRemoteRepository
{

    private RepositoryCredentials credentials;
    private String checkPath;
    private Map<String,String> extraParameters = new HashMap<>(  );
    private Map<String,String> uExtraParameters = Collections.unmodifiableMap( extraParameters );
    private Map<String,String> extraHeaders = new HashMap<>(  );
    private Map<String,String> uExtraHeaders = Collections.unmodifiableMap( extraHeaders );
    private Duration timeout = Duration.ofSeconds( 60 );
    private String proxyId;
    private RemoteRepositoryContent content;

    public AbstractRemoteRepository( RepositoryType type, String id, String name , RepositoryStorage storage)
    {
        super( type, id, name, storage );
    }

    public AbstractRemoteRepository( Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage storage )
    {
        super( primaryLocale, type, id, name, storage );
    }

    @Override
    public void setCredentials( RepositoryCredentials credentials )
    {
        this.credentials = credentials;
    }

    @Override
    public void setCheckPath( String path )
    {
        this.checkPath = path;
    }

    @Override
    public void setExtraParameters( Map<String, String> params )
    {
        this.extraParameters.clear();
        this.extraParameters.putAll(params);
    }

    @Override
    public void addExtraParameter( String key, String value )
    {
        this.extraParameters.put(key, value);
    }

    @Override
    public void setExtraHeaders( Map<String, String> headers )
    {
        this.extraHeaders.clear();
        this.extraHeaders.putAll(headers);
    }

    @Override
    public void addExtraHeader( String header, String value )
    {
        this.extraHeaders.put(header, value);
    }

    @Override
    public void setTimeout( Duration duration )
    {
        this.timeout = duration;
    }

    @Override
    public RemoteRepositoryContent getContent( )
    {
        return content;
    }

    @Override
    public void setContent(RemoteRepositoryContent content) {
        this.content = content;
    }

    @Override
    public RepositoryCredentials getLoginCredentials( )
    {
        return credentials;
    }

    @Override
    public String getCheckPath( )
    {
        return checkPath;
    }

    @Override
    public Map<String, String> getExtraParameters( )
    {
        return uExtraParameters;
    }

    @Override
    public Map<String, String> getExtraHeaders( )
    {
        return uExtraHeaders;
    }

    @Override
    public Duration getTimeout( )
    {
        return timeout;
    }

    /**
     * Remote repositories resolve always relative to the base directory.
     * @return
     */
    @Override
    public StorageAsset getLocalPath() {
        return getStorage().getAsset("");
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        return str.append("checkPath=").append(checkPath)
                .append(",creds:").append(credentials).toString();
    }

    @Override
    public void setLocation(URI location) {
        // Location of remote repositories is not for the local filestore
        super.location = location;
    }
}
