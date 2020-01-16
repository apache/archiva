package org.apache.archiva.rest.services;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.maven.WagonFactoryRequest;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "remoteRepositoriesService#rest" )
public class DefaultRemoteRepositoriesService
    extends AbstractRestService
    implements RemoteRepositoriesService {

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private WagonFactory wagonFactory;

    @Inject
    private ProxyRegistry proxyRegistry;

    int checkReadTimeout = 10000;
    int checkTimeout = 9000;


    @Override
    public List<RemoteRepository> getRemoteRepositories()
            throws ArchivaRestServiceException {
        try {
            List<RemoteRepository> remoteRepositories = remoteRepositoryAdmin.getRemoteRepositories();
            return remoteRepositories == null ? Collections.<RemoteRepository>emptyList() : remoteRepositories;
        } catch (RepositoryAdminException e) {
            log.error(e.getMessage(), e);
            throw new ArchivaRestServiceException(e.getMessage(), e.getFieldName(), e);
        }
    }

    @Override
    public RemoteRepository getRemoteRepository(String repositoryId)
            throws ArchivaRestServiceException {

        List<RemoteRepository> remoteRepositories = getRemoteRepositories();
        for (RemoteRepository repository : remoteRepositories) {
            if (StringUtils.equals(repositoryId, repository.getId())) {
                return repository;
            }
        }
        return null;
    }

    @Override
    public Boolean deleteRemoteRepository(String repositoryId)
            throws ArchivaRestServiceException {
        try {
            return remoteRepositoryAdmin.deleteRemoteRepository(repositoryId, getAuditInformation());
        } catch (RepositoryAdminException e) {
            log.error(e.getMessage(), e);
            throw new ArchivaRestServiceException(e.getMessage(), e.getFieldName(), e);
        }
    }

    @Override
    public Boolean addRemoteRepository(RemoteRepository remoteRepository)
            throws ArchivaRestServiceException {
        try {
            return remoteRepositoryAdmin.addRemoteRepository(remoteRepository, getAuditInformation());
        } catch (RepositoryAdminException e) {
            log.error(e.getMessage(), e);
            throw new ArchivaRestServiceException(e.getMessage(), e.getFieldName(), e);
        }
    }

    @Override
    public Boolean updateRemoteRepository(RemoteRepository remoteRepository)
            throws ArchivaRestServiceException {
        try {
            return remoteRepositoryAdmin.updateRemoteRepository(remoteRepository, getAuditInformation());
        } catch (RepositoryAdminException e) {
            log.error(e.getMessage(), e);
            throw new ArchivaRestServiceException(e.getMessage(), e.getFieldName(), e);
        }
    }

    @Override
    public Boolean checkRemoteConnectivity(String repositoryId)
            throws ArchivaRestServiceException {
        try {
            RemoteRepository remoteRepository = remoteRepositoryAdmin.getRemoteRepository(repositoryId);
            if (remoteRepository == null) {
                log.warn("Remote repository {} does not exist. Connectivity check returns false.", repositoryId);
                return Boolean.FALSE;
            }
            NetworkProxy networkProxy = null;
            if (StringUtils.isNotBlank(remoteRepository.getRemoteDownloadNetworkProxyId())) {
                networkProxy = proxyRegistry.getNetworkProxy(remoteRepository.getRemoteDownloadNetworkProxyId());
                if (networkProxy == null) {
                    log.warn(
                            "A network proxy {} was configured for repository {}. But the proxy with the given id does not exist.",
                            remoteRepository.getRemoteDownloadNetworkProxyId(), repositoryId);
                }
            }

            String wagonProtocol = new URL(remoteRepository.getUrl()).getProtocol();

            final Wagon wagon =
                    wagonFactory.getWagon(new WagonFactoryRequest(wagonProtocol, remoteRepository.getExtraHeaders()) //
                            .networkProxy(networkProxy));

            // hardcoded value as it's a check of the remote repo connectivity
            wagon.setReadTimeout(checkReadTimeout);
            wagon.setTimeout(checkTimeout);

            if (wagon instanceof AbstractHttpClientWagon ) {
                HttpMethodConfiguration httpMethodConfiguration = new HttpMethodConfiguration() //
                        .setUsePreemptive(true) //
                        .setReadTimeout(checkReadTimeout);
                HttpConfiguration httpConfiguration = new HttpConfiguration().setGet( httpMethodConfiguration);
                AbstractHttpClientWagon.class.cast(wagon).setHttpConfiguration(httpConfiguration);
            }

            ProxyInfo proxyInfo = null;
            if (networkProxy != null) {
                proxyInfo = new ProxyInfo();
                proxyInfo.setType(networkProxy.getProtocol());
                proxyInfo.setHost(networkProxy.getHost());
                proxyInfo.setPort(networkProxy.getPort());
                proxyInfo.setUserName(networkProxy.getUsername());
                proxyInfo.setPassword(new String(networkProxy.getPassword()));
            }
            String url = StringUtils.stripEnd(remoteRepository.getUrl(), "/");
            wagon.connect(new Repository(remoteRepository.getId(), url), proxyInfo);

            // MRM-1933, there are certain servers that do not allow browsing
            if (!(StringUtils.isEmpty(remoteRepository.getCheckPath()) ||
                    "/".equals(remoteRepository.getCheckPath()))) {
                return wagon.resourceExists(remoteRepository.getCheckPath());
            } else {
                // we only check connectivity as remote repo can be empty
                // MRM-1909: Wagon implementation appends a slash already
                wagon.getFileList("");
            }

            return Boolean.TRUE;
        } catch (TransferFailedException e) {
            log.info("TransferFailedException :{}", e.getMessage());
            return Boolean.FALSE;
        } catch (Exception e) {
            // This service returns either true or false, Exception cannot be handled by the clients
            log.debug("Exception occured on connectivity test.", e);
            log.info("Connection exception: {}", e.getMessage());
            return Boolean.FALSE;
        }

    }

    public int getCheckReadTimeout() {
        return checkReadTimeout;
    }

    public void setCheckReadTimeout(int checkReadTimeout) {
        this.checkReadTimeout = checkReadTimeout;
    }

    public int getCheckTimeout() {
        return checkTimeout;
    }

    public void setCheckTimeout(int checkTimeout) {
        this.checkTimeout = checkTimeout;
    }

}
