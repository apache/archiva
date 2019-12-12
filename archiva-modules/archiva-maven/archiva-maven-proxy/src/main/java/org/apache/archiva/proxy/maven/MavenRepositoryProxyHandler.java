package org.apache.archiva.proxy.maven;

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

import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.proxy.DefaultRepositoryProxyHandler;
import org.apache.archiva.proxy.NotFoundException;
import org.apache.archiva.proxy.NotModifiedException;
import org.apache.archiva.proxy.ProxyException;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.proxy.model.ProxyConnector;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.base.PasswordCredentials;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * DefaultRepositoryProxyHandler
 * TODO exception handling needs work - "not modified" is not really an exceptional case, and it has more layers than
 * your average brown onion
 */
@Service( "repositoryProxyHandler#maven" )
public class MavenRepositoryProxyHandler extends DefaultRepositoryProxyHandler {

    private static final Logger log = LoggerFactory.getLogger( MavenRepositoryProxyHandler.class );

    private static final List<RepositoryType> REPOSITORY_TYPES = new ArrayList<>();

    static {
        REPOSITORY_TYPES.add(RepositoryType.MAVEN);
    }

    @Inject
    private WagonFactory wagonFactory;

    private ConcurrentMap<String, ProxyInfo> networkProxyMap = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        super.initialize();
    }

    private void updateWagonProxyInfo(Map<String, NetworkProxy> proxyList) {
        this.networkProxyMap.clear();
        for (Map.Entry<String, NetworkProxy> proxyEntry : proxyList.entrySet()) {
            String key = proxyEntry.getKey();
            NetworkProxy networkProxyDef = proxyEntry.getValue();

            ProxyInfo proxy = new ProxyInfo();

            proxy.setType(networkProxyDef.getProtocol());
            proxy.setHost(networkProxyDef.getHost());
            proxy.setPort(networkProxyDef.getPort());
            proxy.setUserName(networkProxyDef.getUsername());
            proxy.setPassword(new String(networkProxyDef.getPassword()));

            this.networkProxyMap.put(key, proxy);
        }
    }

    @Override
    public void setNetworkProxies(Map<String, NetworkProxy> networkProxies ) {
        super.setNetworkProxies( networkProxies );
        updateWagonProxyInfo( networkProxies );
    }

    /**
     * @param connector
     * @param remoteRepository
     * @param tmpResource
     * @param checksumFiles
     * @param url
     * @param remotePath
     * @param resource
     * @param workingDirectory
     * @param repository
     * @throws ProxyException
     * @throws NotModifiedException
     */
    protected void transferResources( ProxyConnector connector, RemoteRepository remoteRepository,
                                      StorageAsset tmpResource, StorageAsset[] checksumFiles, String url, String remotePath, StorageAsset resource,
                                      Path workingDirectory, ManagedRepository repository )
            throws ProxyException, NotModifiedException {
        Wagon wagon = null;
        try {
            RepositoryURL repoUrl = remoteRepository.getContent().getURL();
            String protocol = repoUrl.getProtocol();
            NetworkProxy networkProxy = null;
            String proxyId = connector.getProxyId();
            if (StringUtils.isNotBlank(proxyId)) {

                networkProxy = getNetworkProxy(proxyId);
            }
            WagonFactoryRequest wagonFactoryRequest = new WagonFactoryRequest("wagon#" + protocol,
                    remoteRepository.getExtraHeaders());
            if (networkProxy == null) {

                log.warn("No network proxy with id {} found for connector {}->{}", proxyId,
                        connector.getSourceRepository().getId(), connector.getTargetRepository().getId());
            } else {
                wagonFactoryRequest = wagonFactoryRequest.networkProxy(networkProxy);
            }
            wagon = wagonFactory.getWagon(wagonFactoryRequest);
            if (wagon == null) {
                throw new ProxyException("Unsupported target repository protocol: " + protocol);
            }

            if (wagon == null) {
                throw new ProxyException("Unsupported target repository protocol: " + protocol);
            }

            boolean connected = connectToRepository(connector, wagon, remoteRepository);
            if (connected) {
                transferArtifact(wagon, remoteRepository, remotePath, repository, resource.getFilePath(), workingDirectory,
                        tmpResource);

                // TODO: these should be used to validate the download based on the policies, not always downloaded
                // to
                // save on connections since md5 is rarely used
                for (int i=0; i<checksumFiles.length; i++) {
                    String ext = "."+StringUtils.substringAfterLast(checksumFiles[i].getName( ), "." );
                    transferChecksum(wagon, remoteRepository, remotePath, repository, resource.getFilePath(), ext,
                        checksumFiles[i].getFilePath());
                }
            }
        } catch (NotFoundException e) {
            urlFailureCache.cacheFailure(url);
            throw e;
        } catch (NotModifiedException e) {
            // Do not cache url here.
            throw e;
        } catch (ProxyException e) {
            urlFailureCache.cacheFailure(url);
            throw e;
        } catch (WagonFactoryException e) {
            throw new ProxyException(e.getMessage(), e);
        } finally {
            if (wagon != null) {
                try {
                    wagon.disconnect();
                } catch (ConnectionException e) {
                    log.warn("Unable to disconnect wagon.", e);
                }
            }
        }
    }

    protected void transferArtifact(Wagon wagon, RemoteRepository remoteRepository, String remotePath,
                                    ManagedRepository repository, Path resource, Path tmpDirectory,
                                    StorageAsset destFile)
            throws ProxyException {
        transferSimpleFile(wagon, remoteRepository, remotePath, repository, resource, destFile.getFilePath());
    }

    /**
     * <p>
     * Quietly transfer the checksum file from the remote repository to the local file.
     * </p>
     *
     * @param wagon            the wagon instance (should already be connected) to use.
     * @param remoteRepository the remote repository to transfer from.
     * @param remotePath       the remote path to the resource to get.
     * @param repository       the managed repository that will hold the file
     * @param resource         the local file that should contain the downloaded contents
     * @param ext              the type of checksum to transfer (example: ".md5" or ".sha1")
     * @throws ProxyException if copying the downloaded file into place did not succeed.
     */
    protected void transferChecksum( Wagon wagon, RemoteRepository remoteRepository, String remotePath,
                                     ManagedRepository repository, Path resource, String ext,
                                     Path destFile )
            throws ProxyException {
        String url = remoteRepository.getLocation().toString() + remotePath + ext;

        // Transfer checksum does not use the policy.
        if (urlFailureCache.hasFailedBefore(url)) {
            return;
        }

        try {
            transferSimpleFile(wagon, remoteRepository, remotePath + ext, repository, resource, destFile);
            log.debug("Checksum {} Downloaded: {} to move to {}", url, destFile, resource);
        } catch (NotFoundException e) {
            urlFailureCache.cacheFailure(url);
            log.debug("Transfer failed, checksum not found: {}", url);
            // Consume it, do not pass this on.
        } catch (NotModifiedException e) {
            log.debug("Transfer skipped, checksum not modified: {}", url);
            // Consume it, do not pass this on.
        } catch (ProxyException e) {
            urlFailureCache.cacheFailure(url);
            log.warn("Transfer failed on checksum: {} : {}", url, e.getMessage(), e);
            // Critical issue, pass it on.
            throw e;
        }
    }

    /**
     * Perform the transfer of the remote file to the local file specified.
     *
     * @param wagon            the wagon instance to use.
     * @param remoteRepository the remote repository to use
     * @param remotePath       the remote path to attempt to get
     * @param repository       the managed repository that will hold the file
     * @param origFile         the local file to save to
     * @throws ProxyException if there was a problem moving the downloaded file into place.
     */
    protected void transferSimpleFile(Wagon wagon, RemoteRepository remoteRepository, String remotePath,
                                      ManagedRepository repository, Path origFile, Path destFile)
            throws ProxyException {
        assert (remotePath != null);

        // Transfer the file.
        try {
            boolean success = false;

            if (!Files.exists(origFile)) {
                log.debug("Retrieving {} from {}", remotePath, remoteRepository.getId());
                wagon.get(addParameters(remotePath, remoteRepository), destFile.toFile());
                success = true;

                // You wouldn't get here on failure, a WagonException would have been thrown.
                log.debug("Downloaded successfully.");
            } else {
                log.debug("Retrieving {} from {} if updated", remotePath, remoteRepository.getId());
                try {
                    success = wagon.getIfNewer(addParameters(remotePath, remoteRepository), destFile.toFile(),
                            Files.getLastModifiedTime(origFile).toMillis());
                } catch (IOException e) {
                    throw new ProxyException("Failed to the modification time of " + origFile.toAbsolutePath());
                }
                if (!success) {
                    throw new NotModifiedException(
                            "Not downloaded, as local file is newer than remote side: " + origFile.toAbsolutePath());
                }

                if (Files.exists(destFile)) {
                    log.debug("Downloaded successfully.");
                }
            }
        } catch (ResourceDoesNotExistException e) {
            throw new NotFoundException(
                    "Resource [" + remoteRepository.getLocation() + "/" + remotePath + "] does not exist: " + e.getMessage(),
                    e);
        } catch (WagonException e) {
            // TODO: shouldn't have to drill into the cause, but TransferFailedException is often not descriptive enough

            String msg =
                    "Download failure on resource [" + remoteRepository.getLocation() + "/" + remotePath + "]:" + e.getMessage();
            if (e.getCause() != null) {
                msg += " (cause: " + e.getCause() + ")";
            }
            throw new ProxyException(msg, e);
        }
    }

    /**
     * Using wagon, connect to the remote repository.
     *
     * @param connector        the connector configuration to utilize (for obtaining network proxy configuration from)
     * @param wagon            the wagon instance to establish the connection on.
     * @param remoteRepository the remote repository to connect to.
     * @return true if the connection was successful. false if not connected.
     */
    protected boolean connectToRepository(ProxyConnector connector, Wagon wagon,
                                          RemoteRepository remoteRepository) {
        boolean connected = false;

        final ProxyInfo networkProxy =
                connector.getProxyId() == null ? null : this.networkProxyMap.get(connector.getProxyId());

        if (log.isDebugEnabled()) {
            if (networkProxy != null) {
                // TODO: move to proxyInfo.toString()
                String msg = "Using network proxy " + networkProxy.getHost() + ":" + networkProxy.getPort()
                        + " to connect to remote repository " + remoteRepository.getLocation();
                if (networkProxy.getNonProxyHosts() != null) {
                    msg += "; excluding hosts: " + networkProxy.getNonProxyHosts();
                }
                if (StringUtils.isNotBlank(networkProxy.getUserName())) {
                    msg += "; as user: " + networkProxy.getUserName();
                }
                log.debug(msg);
            }
        }

        AuthenticationInfo authInfo = null;
        String username = "";
        String password = "";
        RepositoryCredentials repCred = remoteRepository.getLoginCredentials();
        if (repCred != null && repCred instanceof PasswordCredentials ) {
            PasswordCredentials pwdCred = (PasswordCredentials) repCred;
            username = pwdCred.getUsername();
            password = pwdCred.getPassword() == null ? "" : new String(pwdCred.getPassword());
        }

        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            log.debug("Using username {} to connect to remote repository {}", username, remoteRepository.getLocation());
            authInfo = new AuthenticationInfo();
            authInfo.setUserName(username);
            authInfo.setPassword(password);
        }

        // Convert seconds to milliseconds

        long timeoutInMilliseconds = remoteRepository.getTimeout().toMillis();

        // Set timeout  read and connect
        // FIXME olamy having 2 config values
        wagon.setReadTimeout((int) timeoutInMilliseconds);
        wagon.setTimeout((int) timeoutInMilliseconds);

        try {
            Repository wagonRepository =
                    new Repository(remoteRepository.getId(), remoteRepository.getLocation().toString());
            wagon.connect(wagonRepository, authInfo, networkProxy);
            connected = true;
        } catch (ConnectionException | AuthenticationException e) {
            log.warn("Could not connect to {}: {}", remoteRepository.getId(), e.getMessage());
            connected = false;
        }

        return connected;
    }


    public WagonFactory getWagonFactory() {
        return wagonFactory;
    }

    public void setWagonFactory(WagonFactory wagonFactory) {
        this.wagonFactory = wagonFactory;
    }

    @Override
    public List<RepositoryType> supports() {
        return REPOSITORY_TYPES;
    }

    @Override
    public void addNetworkproxy( String id, NetworkProxy networkProxy )
    {

    }

    @Override
    public <T extends RepositoryProxyHandler> T getHandler( Class<T> clazz ) throws IllegalArgumentException
    {
        if (clazz.isAssignableFrom( this.getClass() )) {
            return (T)this;
        } else {
            throw new IllegalArgumentException( "This Proxy Handler is no subclass of " + clazz );
        }
    }
}
