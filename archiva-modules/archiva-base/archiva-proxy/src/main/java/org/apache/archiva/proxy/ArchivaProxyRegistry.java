package org.apache.archiva.proxy;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.NetworkProxyConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.policies.Policy;
import org.apache.archiva.policies.PolicyOption;
import org.apache.archiva.policies.PolicyUtil;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.proxy.model.ProxyConnector;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.repository.event.RepositoryRegistryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default proxy registry implementation. Uses the archiva configuration for accessing and storing the
 * proxy information.
 *
 */
@SuppressWarnings( "SpringJavaInjectionPointsAutowiringInspection" )
@Service("proxyRegistry#default")
public class ArchivaProxyRegistry implements ProxyRegistry, EventHandler<RepositoryRegistryEvent> {

    private static final Logger log = LoggerFactory.getLogger(ArchivaProxyRegistry.class);

    @Inject
    ArchivaConfiguration archivaConfiguration;

    @Inject
    List<RepositoryProxyHandler> repositoryProxyHandlers;

    @Inject
    List<Policy> policies;

    @Inject
    ArchivaRepositoryRegistry repositoryRegistry;

    private Map<String, NetworkProxy> networkProxyMap = new HashMap<>();
    private Map<RepositoryType, List<RepositoryProxyHandler>> handlerMap = new HashMap<>();
    private ProxyConnectorOrderComparator comparator = ProxyConnectorOrderComparator.getInstance();

    private Map<String, List<ProxyConnector>> connectorMap = new HashMap<>();
    private List<ProxyConnector> connectorList = new ArrayList<>();
    private Map<Policy, PolicyOption> policyMap = new HashMap<>( );


    @PostConstruct
    private void init() {
        if (repositoryProxyHandlers == null) {
            repositoryProxyHandlers = new ArrayList<>();
        }
        updateHandler();
        updateConnectors();
        updateNetworkProxies();
        repositoryRegistry.registerEventHandler(RepositoryRegistryEvent.RELOADED, this);
    }

    private ArchivaConfiguration getArchivaConfiguration() {
        return archivaConfiguration;
    }

    private void updateNetworkProxies() {
        this.networkProxyMap.clear();
        List<NetworkProxyConfiguration> networkProxies = getArchivaConfiguration().getConfiguration().getNetworkProxies();
        for (NetworkProxyConfiguration networkProxyConfig : networkProxies) {
            String key = networkProxyConfig.getId();

            NetworkProxy proxy = new NetworkProxy();

            proxy.setProtocol(networkProxyConfig.getProtocol());
            proxy.setHost(networkProxyConfig.getHost());
            proxy.setPort(networkProxyConfig.getPort());
            proxy.setUsername(networkProxyConfig.getUsername());
            proxy.setPassword(networkProxyConfig.getPassword()==null? new char[0] : networkProxyConfig.getPassword().toCharArray());
            proxy.setUseNtlm(networkProxyConfig.isUseNtlm());

            this.networkProxyMap.put(key, proxy);
        }
        for (RepositoryProxyHandler proxyHandler : repositoryProxyHandlers) {
            proxyHandler.setNetworkProxies(this.networkProxyMap);
        }
    }

    private void updateHandler( ) {

        for (RepositoryProxyHandler handler : repositoryProxyHandlers) {
            List<RepositoryType> types = handler.supports();
            for (RepositoryType type : types) {
                if (!handlerMap.containsKey(type)) {
                    handlerMap.put(type, new ArrayList<>());
                }
                handlerMap.get(type).add(handler);
            }
            handler.setPolicies( policies );
        }
    }

    private void updateConnectors() {
        List<ProxyConnectorConfiguration> proxyConnectorConfigurations =
            getArchivaConfiguration().getConfiguration().getProxyConnectors();

        connectorList = proxyConnectorConfigurations.stream()
                .map(this::buildProxyConnector)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(comparator).collect(Collectors.toList());
        connectorMap = connectorList.stream().collect(Collectors.groupingBy(a -> a.getSourceRepository().getId()));
        for (RepositoryProxyHandler handler : repositoryProxyHandlers) {
            handler.setProxyConnectors( connectorList );
        }
    }


    private Map<Policy, PolicyOption> getPolicyMap(ProxyConnectorConfiguration configuration) {
        Map<String, String> policyConfig = configuration.getPolicies( );
        return policies.stream().collect( Collectors.toMap( Function.identity(), p -> PolicyUtil.findOption( policyConfig.get(p.getId()), p ) ) );
    }

    private Optional<ProxyConnector> buildProxyConnector(ProxyConnectorConfiguration configuration) {
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setOrder(configuration.getOrder());
        proxyConnector.setBlacklist(configuration.getBlackListPatterns());
        proxyConnector.setWhitelist(configuration.getWhiteListPatterns());
        if (configuration.isDisabled()) {
            proxyConnector.disable();
        } else {
            proxyConnector.enable();
        }
        proxyConnector.setPolicies(getPolicyMap( configuration ));
        proxyConnector.setProperties(configuration.getProperties());
        proxyConnector.setProxyId(configuration.getProxyId());
        ManagedRepository srcRepo = repositoryRegistry.getManagedRepository(configuration.getSourceRepoId());
        if (srcRepo==null) {
            return Optional.empty();
        }
        proxyConnector.setSourceRepository(srcRepo);
        RemoteRepository targetRepo = repositoryRegistry.getRemoteRepository(configuration.getTargetRepoId());
        if (targetRepo==null) {
            return Optional.empty();
        }
        proxyConnector.setTargetRepository(targetRepo);
        return Optional.of(proxyConnector);
    }

    @Override
    public NetworkProxy getNetworkProxy(String id) {
        return this.networkProxyMap.get(id);
    }

    @Override
    public Map<RepositoryType, List<RepositoryProxyHandler>> getAllHandler() {
        return this.handlerMap;
    }

    @Override
    public List<RepositoryProxyHandler> getHandler(RepositoryType type) {
        if (this.handlerMap.containsKey(type)) {
            return this.handlerMap.get(type);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasHandler(RepositoryType type) {
        return this.handlerMap.containsKey(type);
    }


    @Override
    public List<ProxyConnector> getProxyConnectors() {
        return connectorList;

    }

    @Override
    public Map<String, List<ProxyConnector>> getProxyConnectorAsMap() {
        return connectorMap;
    }

    @Override
    public void reload( )
    {
        init();
    }

    @Override
    public void handle(RepositoryRegistryEvent event) {
        log.debug("Reload happened, updating proxy list");
        if (event.getType()== RepositoryRegistryEvent.RELOADED) {
            init();
        }
    }
}
