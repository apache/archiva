package org.apache.archiva.repository.features;

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


import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * Feature for remote index download.
 */
public class RemoteIndexFeature implements RepositoryFeature<RemoteIndexFeature> {

    private boolean downloadRemoteIndex = false;
    private URI indexUri;

    {
        try {
            indexUri = new URI(".index");
        } catch (URISyntaxException e) {
            // Ignore
        }
    }

    private boolean downloadRemoteIndexOnStartup = false;
    private Duration downloadTimeout = Duration.ofSeconds( 600 );
    private String proxyId = "";


    @Override
    public RemoteIndexFeature get() {
        return this;
    }

    /**
     * True, if the remote index should be downloaded.
     * @return True if download, otherwise false.
     */
    public boolean isDownloadRemoteIndex() {
        return downloadRemoteIndex;
    }

    public void setDownloadRemoteIndex(boolean downloadRemoteIndex) {
        this.downloadRemoteIndex = downloadRemoteIndex;
    }

    /**
     * The URI to access the remote index. May be a relative URI that is relative to the
     * repository URI.
     *
     * @return
     */
    public URI getIndexUri() {
        return indexUri;
    }

    /**
     * Sets the URI to access the remote index. May be a relative URI that is relative to the
     * repository URI. The allowed URI schemes are dependent on the repository type.
     *
     * @param indexUri The URI of the index
     */
    public void setIndexUri(URI indexUri) {
        this.indexUri = indexUri;
    }

    /**
     * Returns true, if the remote index should be downloaded on startup of the repository.
     * @return true, if the index should be downloaded during startup, otherwise false.
     */
    public boolean isDownloadRemoteIndexOnStartup() {
        return downloadRemoteIndexOnStartup;
    }

    /**
     * Sets the flag for download of the remote repository index.
     *
     * @param downloadRemoteIndexOnStartup
     */
    public void setDownloadRemoteIndexOnStartup(boolean downloadRemoteIndexOnStartup) {
        this.downloadRemoteIndexOnStartup = downloadRemoteIndexOnStartup;
    }

    /**
     * Returns the timeout after that the remote index download is aborted.
     * @return the time duration after that, the download is aborted.
     */
    public Duration getDownloadTimeout() {
        return this.downloadTimeout;
    }

    /**
     * Sets the timeout after that a remote index download will be aborted.
     * @param timeout The duration
     */
    public void setDownloadTimeout(Duration timeout) {
        this.downloadTimeout = timeout;
    }

    /**
     * Returns the id of the proxy, that should be used to download the remote index.
     * @return The proxy id
     */
    public String getProxyId( )
    {
        return proxyId;
    }

    /**
     * Sets the id of the proxy that should be used to download the remote index.
     * @param proxyId
     */
    public void setProxyId( String proxyId )
    {
        this.proxyId = proxyId;
    }

    /**
     * Returns true, if there is a index available.
     *
     * @return
     */
    public boolean hasIndex() {
        return this.indexUri!=null && !StringUtils.isEmpty( this.indexUri.getPath() );
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        return str.append("RemoteIndexFeature:{downloadRemoteIndex=").append(downloadRemoteIndex)
                .append(",indexURI=").append(indexUri)
                .append(",downloadOnStartup=").append(downloadRemoteIndexOnStartup)
                .append(",timeout=").append(downloadTimeout).append("}").toString();
    }
}
