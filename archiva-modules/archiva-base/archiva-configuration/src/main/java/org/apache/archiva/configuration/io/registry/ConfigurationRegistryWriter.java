
package org.apache.archiva.configuration.io.registry;

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

import org.apache.archiva.configuration.*;
import org.apache.archiva.components.registry.Registry;

import java.util.Iterator;
import java.util.List;

// Util imports
// Model class imports


/**
 * Generate Plexus Registry output mechanism for model 'Configuration'.
 */
public class ConfigurationRegistryWriter {
    public void write(Configuration model, Registry registry) {
        writeConfiguration("", model, registry);
    }

    private void writeList(Registry registry, List<String> subList, String subsetPath, String elementName) {
        if (subList != null && subList.size() > 0
        ) {
            registry.removeSubset(subsetPath);

            int count = 0;
            for (Iterator<String> iter = subList.iterator(); iter.hasNext(); count++) {
                String name = subsetPath + "." + elementName + "(" + count + ")";
                String value = iter.next();
                registry.setString(name, value);
            }
        }
    }

    private void writeConfiguration(String prefix, Configuration value, Registry registry) {
        if (value != null) {
            if (value.getVersion() != null
            ) {
                String version = "version";
                registry.setString(prefix + version, value.getVersion());
            }
            if (value.getMetadataStore() != null && !value.getMetadataStore().equals("jcr")
            ) {
                String metadataStore = "metadataStore";
                registry.setString(prefix + metadataStore, value.getMetadataStore());
            }
            if (value.getRepositoryGroups() != null && value.getRepositoryGroups().size() > 0
            ) {
                registry.removeSubset(prefix + "repositoryGroups");

                int count = 0;
                for (Iterator iter = value.getRepositoryGroups().iterator(); iter.hasNext(); count++) {
                    String name = "repositoryGroups.repositoryGroup(" + count + ")";
                    RepositoryGroupConfiguration o = (RepositoryGroupConfiguration) iter.next();
                    writeRepositoryGroupConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getManagedRepositories() != null && value.getManagedRepositories().size() > 0
            ) {
                registry.removeSubset(prefix + "managedRepositories");

                int count = 0;
                for (Iterator iter = value.getManagedRepositories().iterator(); iter.hasNext(); count++) {
                    String name = "managedRepositories.managedRepository(" + count + ")";
                    ManagedRepositoryConfiguration o = (ManagedRepositoryConfiguration) iter.next();
                    writeManagedRepositoryConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getRemoteRepositories() != null && value.getRemoteRepositories().size() > 0
            ) {
                registry.removeSubset(prefix + "remoteRepositories");

                int count = 0;
                for (Iterator iter = value.getRemoteRepositories().iterator(); iter.hasNext(); count++) {
                    String name = "remoteRepositories.remoteRepository(" + count + ")";
                    RemoteRepositoryConfiguration o = (RemoteRepositoryConfiguration) iter.next();
                    writeRemoteRepositoryConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getProxyConnectors() != null && value.getProxyConnectors().size() > 0
            ) {
                registry.removeSubset(prefix + "proxyConnectors");

                int count = 0;
                for (Iterator iter = value.getProxyConnectors().iterator(); iter.hasNext(); count++) {
                    String name = "proxyConnectors.proxyConnector(" + count + ")";
                    ProxyConnectorConfiguration o = (ProxyConnectorConfiguration) iter.next();
                    writeProxyConnectorConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getNetworkProxies() != null && value.getNetworkProxies().size() > 0
            ) {
                registry.removeSubset(prefix + "networkProxies");

                int count = 0;
                for (Iterator iter = value.getNetworkProxies().iterator(); iter.hasNext(); count++) {
                    String name = "networkProxies.networkProxy(" + count + ")";
                    NetworkProxyConfiguration o = (NetworkProxyConfiguration) iter.next();
                    writeNetworkProxyConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getLegacyArtifactPaths() != null && value.getLegacyArtifactPaths().size() > 0
            ) {
                registry.removeSubset(prefix + "legacyArtifactPaths");

                int count = 0;
                for (Iterator iter = value.getLegacyArtifactPaths().iterator(); iter.hasNext(); count++) {
                    String name = "legacyArtifactPaths.legacyArtifactPath(" + count + ")";
                    LegacyArtifactPath o = (LegacyArtifactPath) iter.next();
                    writeLegacyArtifactPath(prefix + name + ".", o, registry);
                }
            }
            if (value.getRepositoryScanning() != null
            ) {
                writeRepositoryScanningConfiguration(prefix + "repositoryScanning.", value.getRepositoryScanning(), registry);
            }
            if (value.getWebapp() != null
            ) {
                writeWebappConfiguration(prefix + "webapp.", value.getWebapp(), registry);
            }
            if (value.getOrganisationInfo() != null
            ) {
                writeOrganisationInformation(prefix + "organisationInfo.", value.getOrganisationInfo(), registry);
            }
            if (value.getNetworkConfiguration() != null
            ) {
                writeNetworkConfiguration(prefix + "networkConfiguration.", value.getNetworkConfiguration(), registry);
            }
            if (value.getRedbackRuntimeConfiguration() != null
            ) {
                writeRedbackRuntimeConfiguration(prefix + "redbackRuntimeConfiguration.", value.getRedbackRuntimeConfiguration(), registry);
            }
            if (value.getArchivaRuntimeConfiguration() != null
            ) {
                writeArchivaRuntimeConfiguration(prefix + "archivaRuntimeConfiguration.", value.getArchivaRuntimeConfiguration(), registry);
            }
            if (value.getProxyConnectorRuleConfigurations() != null && value.getProxyConnectorRuleConfigurations().size() > 0
            ) {
                registry.removeSubset(prefix + "proxyConnectorRuleConfigurations");

                int count = 0;
                for (Iterator iter = value.getProxyConnectorRuleConfigurations().iterator(); iter.hasNext(); count++) {
                    String name = "proxyConnectorRuleConfigurations.proxyConnectorRuleConfiguration(" + count + ")";
                    ProxyConnectorRuleConfiguration o = (ProxyConnectorRuleConfiguration) iter.next();
                    writeProxyConnectorRuleConfiguration(prefix + name + ".", o, registry);
                }
            }
            if (value.getArchivaDefaultConfiguration() != null
            ) {
                writeArchivaDefaultConfiguration(prefix + "archivaDefaultConfiguration.", value.getArchivaDefaultConfiguration(), registry);
            }
        }
    }

    private void writeAbstractRepositoryConfiguration(String prefix, AbstractRepositoryConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getType() != null && !value.getType().equals("MAVEN")
            ) {
                String type = "type";
                registry.setString(prefix + type, value.getType());
            }
            if (value.getName() != null
            ) {
                String name = "name";
                registry.setString(prefix + name, value.getName());
            }
            if (value.getLayout() != null && !value.getLayout().equals("default")
            ) {
                String layout = "layout";
                registry.setString(prefix + layout, value.getLayout());
            }
            if (value.getIndexDir() != null && !value.getIndexDir().equals("")
            ) {
                String indexDir = "indexDir";
                registry.setString(prefix + indexDir, value.getIndexDir());
            }
            if (value.getPackedIndexDir() != null && !value.getPackedIndexDir().equals("")
            ) {
                String packedIndexDir = "packedIndexDir";
                registry.setString(prefix + packedIndexDir, value.getPackedIndexDir());
            }
            if (value.getDescription() != null && !value.getDescription().equals("")
            ) {
                String description = "description";
                registry.setString(prefix + description, value.getDescription());
            }
        }
    }

    private void writeRemoteRepositoryConfiguration(String prefix, RemoteRepositoryConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getUrl() != null
            ) {
                String url = "url";
                registry.setString(prefix + url, value.getUrl());
            }
            if (value.getUsername() != null
            ) {
                String username = "username";
                registry.setString(prefix + username, value.getUsername());
            }
            if (value.getPassword() != null
            ) {
                String password = "password";
                registry.setString(prefix + password, value.getPassword());
            }
            if (value.getTimeout() != 60
            ) {
                String timeout = "timeout";
                registry.setInt(prefix + timeout, value.getTimeout());
            }
            if (value.getRefreshCronExpression() != null && !value.getRefreshCronExpression().equals("0 0 08 ? * SUN")
            ) {
                String refreshCronExpression = "refreshCronExpression";
                registry.setString(prefix + refreshCronExpression, value.getRefreshCronExpression());
            }
            String downloadRemoteIndex = "downloadRemoteIndex";
            registry.setBoolean(prefix + downloadRemoteIndex, value.isDownloadRemoteIndex());
            if (value.getRemoteIndexUrl() != null
            ) {
                String remoteIndexUrl = "remoteIndexUrl";
                registry.setString(prefix + remoteIndexUrl, value.getRemoteIndexUrl());
            }
            if (value.getRemoteDownloadNetworkProxyId() != null
            ) {
                String remoteDownloadNetworkProxyId = "remoteDownloadNetworkProxyId";
                registry.setString(prefix + remoteDownloadNetworkProxyId, value.getRemoteDownloadNetworkProxyId());
            }
            if (value.getRemoteDownloadTimeout() != 300
            ) {
                String remoteDownloadTimeout = "remoteDownloadTimeout";
                registry.setInt(prefix + remoteDownloadTimeout, value.getRemoteDownloadTimeout());
            }
            String downloadRemoteIndexOnStartup = "downloadRemoteIndexOnStartup";
            registry.setBoolean(prefix + downloadRemoteIndexOnStartup, value.isDownloadRemoteIndexOnStartup());
            if (value.getExtraParameters() != null && value.getExtraParameters().size() > 0
            ) {
                registry.removeSubset(prefix + "extraParameters");

                for (Iterator iter = value.getExtraParameters().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getExtraParameters().get(key);

                    registry.setString(prefix + "extraParameters." + key, v);
                }
            }
            if (value.getExtraHeaders() != null && value.getExtraHeaders().size() > 0
            ) {
                registry.removeSubset(prefix + "extraHeaders");

                for (Iterator iter = value.getExtraHeaders().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getExtraHeaders().get(key);

                    registry.setString(prefix + "extraHeaders." + key, v);
                }
            }
            if (value.getCheckPath() != null
            ) {
                String checkPath = "checkPath";
                registry.setString(prefix + checkPath, value.getCheckPath());
            }
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getType() != null && !value.getType().equals("MAVEN")
            ) {
                String type = "type";
                registry.setString(prefix + type, value.getType());
            }
            if (value.getName() != null
            ) {
                String name = "name";
                registry.setString(prefix + name, value.getName());
            }
            if (value.getLayout() != null && !value.getLayout().equals("default")
            ) {
                String layout = "layout";
                registry.setString(prefix + layout, value.getLayout());
            }
            if (value.getIndexDir() != null && !value.getIndexDir().equals("")
            ) {
                String indexDir = "indexDir";
                registry.setString(prefix + indexDir, value.getIndexDir());
            }
            if (value.getPackedIndexDir() != null && !value.getPackedIndexDir().equals("")
            ) {
                String packedIndexDir = "packedIndexDir";
                registry.setString(prefix + packedIndexDir, value.getPackedIndexDir());
            }
            if (value.getDescription() != null && !value.getDescription().equals("")
            ) {
                String description = "description";
                registry.setString(prefix + description, value.getDescription());
            }
        }
    }

    private void writeManagedRepositoryConfiguration(String prefix, ManagedRepositoryConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getLocation() != null
            ) {
                String location = "location";
                registry.setString(prefix + location, value.getLocation());
            }
            String releases = "releases";
            registry.setBoolean(prefix + releases, value.isReleases());
            String blockRedeployments = "blockRedeployments";
            registry.setBoolean(prefix + blockRedeployments, value.isBlockRedeployments());
            String snapshots = "snapshots";
            registry.setBoolean(prefix + snapshots, value.isSnapshots());
            String scanned = "scanned";
            registry.setBoolean(prefix + scanned, value.isScanned());
            if (value.getRefreshCronExpression() != null && !value.getRefreshCronExpression().equals("0 0 * * * ?")
            ) {
                String refreshCronExpression = "refreshCronExpression";
                registry.setString(prefix + refreshCronExpression, value.getRefreshCronExpression());
            }
            if (value.getRetentionCount() != 2
            ) {
                String retentionCount = "retentionCount";
                registry.setInt(prefix + retentionCount, value.getRetentionCount());
            }
            if (value.getRetentionPeriod() != 100
            ) {
                String retentionPeriod = "retentionPeriod";
                registry.setInt(prefix + retentionPeriod, value.getRetentionPeriod());
            }
            String deleteReleasedSnapshots = "deleteReleasedSnapshots";
            registry.setBoolean(prefix + deleteReleasedSnapshots, value.isDeleteReleasedSnapshots());
            String skipPackedIndexCreation = "skipPackedIndexCreation";
            registry.setBoolean(prefix + skipPackedIndexCreation, value.isSkipPackedIndexCreation());
            String stageRepoNeeded = "stageRepoNeeded";
            registry.setBoolean(prefix + stageRepoNeeded, value.isStageRepoNeeded());
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getType() != null && !value.getType().equals("MAVEN")
            ) {
                String type = "type";
                registry.setString(prefix + type, value.getType());
            }
            if (value.getName() != null
            ) {
                String name = "name";
                registry.setString(prefix + name, value.getName());
            }
            if (value.getLayout() != null && !value.getLayout().equals("default")
            ) {
                String layout = "layout";
                registry.setString(prefix + layout, value.getLayout());
            }
            if (value.getIndexDir() != null && !value.getIndexDir().equals("")
            ) {
                String indexDir = "indexDir";
                registry.setString(prefix + indexDir, value.getIndexDir());
            }
            if (value.getPackedIndexDir() != null && !value.getPackedIndexDir().equals("")
            ) {
                String packedIndexDir = "packedIndexDir";
                registry.setString(prefix + packedIndexDir, value.getPackedIndexDir());
            }
            if (value.getDescription() != null && !value.getDescription().equals("")
            ) {
                String description = "description";
                registry.setString(prefix + description, value.getDescription());
            }
        }
    }

    private void writeLegacyArtifactPath(String prefix, LegacyArtifactPath value, Registry registry) {
        if (value != null) {
            if (value.getPath() != null
            ) {
                String path = "path";
                registry.setString(prefix + path, value.getPath());
            }
            if (value.getArtifact() != null
            ) {
                String artifact = "artifact";
                registry.setString(prefix + artifact, value.getArtifact());
            }
        }
    }

    private void writeRepositoryGroupConfiguration(String prefix, RepositoryGroupConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getName() != null) {
                registry.setString(prefix + "name", value.getName());
            }
            if (value.getType() != null) {
                registry.setString(prefix + "type", value.getType());
            }
            if (value.getMergedIndexPath() != null && !value.getMergedIndexPath().equals(".indexer")
            ) {
                String mergedIndexPath = "mergedIndexPath";
                registry.setString(prefix + mergedIndexPath, value.getMergedIndexPath());
            }
            if (value.getMergedIndexTtl() != 30
            ) {
                String mergedIndexTtl = "mergedIndexTtl";
                registry.setInt(prefix + mergedIndexTtl, value.getMergedIndexTtl());
            }
            if (value.getCronExpression() != null && !value.getCronExpression().equals("")
            ) {
                String cronExpression = "cronExpression";
                registry.setString(prefix + cronExpression, value.getCronExpression());
            }
            if (value.getRepositories() != null && value.getRepositories().size() > 0
            ) {
                registry.removeSubset(prefix + "repositories");

                int count = 0;
                for (Iterator iter = value.getRepositories().iterator(); iter.hasNext(); count++) {
                    String name = "repositories.repository(" + count + ")";
                    String repository = (String) iter.next();
                    registry.setString(prefix + name, repository);
                }
            }
        }
    }

    private void writeRepositoryCheckPath(String prefix, RepositoryCheckPath value, Registry registry) {
        if (value != null) {
            if (value.getUrl() != null
            ) {
                String url = "url";
                registry.setString(prefix + url, value.getUrl());
            }
            if (value.getPath() != null
            ) {
                String path = "path";
                registry.setString(prefix + path, value.getPath());
            }
        }
    }

    private void writeAbstractRepositoryConnectorConfiguration(String prefix, AbstractRepositoryConnectorConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getSourceRepoId() != null
            ) {
                String sourceRepoId = "sourceRepoId";
                registry.setString(prefix + sourceRepoId, value.getSourceRepoId());
            }
            if (value.getTargetRepoId() != null
            ) {
                String targetRepoId = "targetRepoId";
                registry.setString(prefix + targetRepoId, value.getTargetRepoId());
            }
            if (value.getProxyId() != null
            ) {
                String proxyId = "proxyId";
                registry.setString(prefix + proxyId, value.getProxyId());
            }
            if (value.getBlackListPatterns() != null && value.getBlackListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "blackListPatterns");

                int count = 0;
                for (Iterator iter = value.getBlackListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "blackListPatterns.blackListPattern(" + count + ")";
                    String blackListPattern = (String) iter.next();
                    registry.setString(prefix + name, blackListPattern);
                }
            }
            if (value.getWhiteListPatterns() != null && value.getWhiteListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "whiteListPatterns");

                int count = 0;
                for (Iterator iter = value.getWhiteListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "whiteListPatterns.whiteListPattern(" + count + ")";
                    String whiteListPattern = (String) iter.next();
                    registry.setString(prefix + name, whiteListPattern);
                }
            }
            if (value.getPolicies() != null && value.getPolicies().size() > 0
            ) {
                registry.removeSubset(prefix + "policies");

                for (Iterator iter = value.getPolicies().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getPolicies().get(key);

                    registry.setString(prefix + "policies." + key, v);
                }
            }
            if (value.getProperties() != null && value.getProperties().size() > 0
            ) {
                registry.removeSubset(prefix + "properties");

                for (Iterator iter = value.getProperties().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getProperties().get(key);

                    registry.setString(prefix + "properties." + key, v);
                }
            }
            String disabled = "disabled";
            registry.setBoolean(prefix + disabled, value.isDisabled());
        }
    }

    private void writeProxyConnectorRuleConfiguration(String prefix, ProxyConnectorRuleConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getRuleType() != null
            ) {
                String ruleType = "ruleType";
                registry.setString(prefix + ruleType, value.getRuleType());
            }
            if (value.getPattern() != null
            ) {
                String pattern = "pattern";
                registry.setString(prefix + pattern, value.getPattern());
            }
            if (value.getProxyConnectors() != null && value.getProxyConnectors().size() > 0
            ) {
                registry.removeSubset(prefix + "proxyConnectors");

                int count = 0;
                for (Iterator iter = value.getProxyConnectors().iterator(); iter.hasNext(); count++) {
                    String name = "proxyConnectors.proxyConnector(" + count + ")";
                    ProxyConnectorConfiguration o = (ProxyConnectorConfiguration) iter.next();
                    writeProxyConnectorConfiguration(prefix + name + ".", o, registry);
                }
            }
        }
    }

    private void writeProxyConnectorConfiguration(String prefix, ProxyConnectorConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getOrder() != 0
            ) {
                String order = "order";
                registry.setInt(prefix + order, value.getOrder());
            }
            if (value.getSourceRepoId() != null
            ) {
                String sourceRepoId = "sourceRepoId";
                registry.setString(prefix + sourceRepoId, value.getSourceRepoId());
            }
            if (value.getTargetRepoId() != null
            ) {
                String targetRepoId = "targetRepoId";
                registry.setString(prefix + targetRepoId, value.getTargetRepoId());
            }
            if (value.getProxyId() != null
            ) {
                String proxyId = "proxyId";
                registry.setString(prefix + proxyId, value.getProxyId());
            }
            if (value.getBlackListPatterns() != null && value.getBlackListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "blackListPatterns");

                int count = 0;
                for (Iterator iter = value.getBlackListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "blackListPatterns.blackListPattern(" + count + ")";
                    String blackListPattern = (String) iter.next();
                    registry.setString(prefix + name, blackListPattern);
                }
            }
            if (value.getWhiteListPatterns() != null && value.getWhiteListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "whiteListPatterns");

                int count = 0;
                for (Iterator iter = value.getWhiteListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "whiteListPatterns.whiteListPattern(" + count + ")";
                    String whiteListPattern = (String) iter.next();
                    registry.setString(prefix + name, whiteListPattern);
                }
            }
            if (value.getPolicies() != null && value.getPolicies().size() > 0
            ) {
                registry.removeSubset(prefix + "policies");

                for (Iterator iter = value.getPolicies().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getPolicies().get(key);

                    registry.setString(prefix + "policies." + key, v);
                }
            }
            if (value.getProperties() != null && value.getProperties().size() > 0
            ) {
                registry.removeSubset(prefix + "properties");

                for (Iterator iter = value.getProperties().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getProperties().get(key);

                    registry.setString(prefix + "properties." + key, v);
                }
            }
            String disabled = "disabled";
            registry.setBoolean(prefix + disabled, value.isDisabled());
        }
    }

    private void writeSyncConnectorConfiguration(String prefix, SyncConnectorConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getCronExpression() != null && !value.getCronExpression().equals("0 0 * * * ?")
            ) {
                String cronExpression = "cronExpression";
                registry.setString(prefix + cronExpression, value.getCronExpression());
            }
            if (value.getMethod() != null && !value.getMethod().equals("rsync")
            ) {
                String method = "method";
                registry.setString(prefix + method, value.getMethod());
            }
            if (value.getSourceRepoId() != null
            ) {
                String sourceRepoId = "sourceRepoId";
                registry.setString(prefix + sourceRepoId, value.getSourceRepoId());
            }
            if (value.getTargetRepoId() != null
            ) {
                String targetRepoId = "targetRepoId";
                registry.setString(prefix + targetRepoId, value.getTargetRepoId());
            }
            if (value.getProxyId() != null
            ) {
                String proxyId = "proxyId";
                registry.setString(prefix + proxyId, value.getProxyId());
            }
            if (value.getBlackListPatterns() != null && value.getBlackListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "blackListPatterns");

                int count = 0;
                for (Iterator iter = value.getBlackListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "blackListPatterns.blackListPattern(" + count + ")";
                    String blackListPattern = (String) iter.next();
                    registry.setString(prefix + name, blackListPattern);
                }
            }
            if (value.getWhiteListPatterns() != null && value.getWhiteListPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "whiteListPatterns");

                int count = 0;
                for (Iterator iter = value.getWhiteListPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "whiteListPatterns.whiteListPattern(" + count + ")";
                    String whiteListPattern = (String) iter.next();
                    registry.setString(prefix + name, whiteListPattern);
                }
            }
            if (value.getPolicies() != null && value.getPolicies().size() > 0
            ) {
                registry.removeSubset(prefix + "policies");

                for (Iterator iter = value.getPolicies().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getPolicies().get(key);

                    registry.setString(prefix + "policies." + key, v);
                }
            }
            if (value.getProperties() != null && value.getProperties().size() > 0
            ) {
                registry.removeSubset(prefix + "properties");

                for (Iterator iter = value.getProperties().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getProperties().get(key);

                    registry.setString(prefix + "properties." + key, v);
                }
            }
            String disabled = "disabled";
            registry.setBoolean(prefix + disabled, value.isDisabled());
        }
    }

    private void writeNetworkProxyConfiguration(String prefix, NetworkProxyConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getProtocol() != null && !value.getProtocol().equals("http")
            ) {
                String protocol = "protocol";
                registry.setString(prefix + protocol, value.getProtocol());
            }
            if (value.getHost() != null
            ) {
                String host = "host";
                registry.setString(prefix + host, value.getHost());
            }
            if (value.getPort() != 8080
            ) {
                String port = "port";
                registry.setInt(prefix + port, value.getPort());
            }
            if (value.getUsername() != null
            ) {
                String username = "username";
                registry.setString(prefix + username, value.getUsername());
            }
            if (value.getPassword() != null
            ) {
                String password = "password";
                registry.setString(prefix + password, value.getPassword());
            }
            String useNtlm = "useNtlm";
            registry.setBoolean(prefix + useNtlm, value.isUseNtlm());
        }
    }

    private void writeRepositoryScanningConfiguration(String prefix, RepositoryScanningConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getFileTypes() != null && value.getFileTypes().size() > 0
            ) {
                registry.removeSubset(prefix + "fileTypes");

                int count = 0;
                for (Iterator iter = value.getFileTypes().iterator(); iter.hasNext(); count++) {
                    String name = "fileTypes.fileType(" + count + ")";
                    FileType o = (FileType) iter.next();
                    writeFileType(prefix + name + ".", o, registry);
                }
            }
            if (value.getKnownContentConsumers() != null && value.getKnownContentConsumers().size() > 0
            ) {
                registry.removeSubset(prefix + "knownContentConsumers");

                int count = 0;
                for (Iterator iter = value.getKnownContentConsumers().iterator(); iter.hasNext(); count++) {
                    String name = "knownContentConsumers.knownContentConsumer(" + count + ")";
                    String knownContentConsumer = (String) iter.next();
                    registry.setString(prefix + name, knownContentConsumer);
                }
            }
            if (value.getInvalidContentConsumers() != null && value.getInvalidContentConsumers().size() > 0
            ) {
                registry.removeSubset(prefix + "invalidContentConsumers");

                int count = 0;
                for (Iterator iter = value.getInvalidContentConsumers().iterator(); iter.hasNext(); count++) {
                    String name = "invalidContentConsumers.invalidContentConsumer(" + count + ")";
                    String invalidContentConsumer = (String) iter.next();
                    registry.setString(prefix + name, invalidContentConsumer);
                }
            }
        }
    }

    private void writeFileType(String prefix, FileType value, Registry registry) {
        if (value != null) {
            if (value.getId() != null
            ) {
                String id = "id";
                registry.setString(prefix + id, value.getId());
            }
            if (value.getPatterns() != null && value.getPatterns().size() > 0
            ) {
                registry.removeSubset(prefix + "patterns");

                int count = 0;
                for (Iterator iter = value.getPatterns().iterator(); iter.hasNext(); count++) {
                    String name = "patterns.pattern(" + count + ")";
                    String pattern = (String) iter.next();
                    registry.setString(prefix + name, pattern);
                }
            }
        }
    }

    private void writeOrganisationInformation(String prefix, OrganisationInformation value, Registry registry) {
        if (value != null) {
            if (value.getName() != null
            ) {
                String name = "name";
                registry.setString(prefix + name, value.getName());
            }
            if (value.getUrl() != null
            ) {
                String url = "url";
                registry.setString(prefix + url, value.getUrl());
            }
            if (value.getLogoLocation() != null
            ) {
                String logoLocation = "logoLocation";
                registry.setString(prefix + logoLocation, value.getLogoLocation());
            }
        }
    }

    private void writeWebappConfiguration(String prefix, WebappConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getUi() != null
            ) {
                writeUserInterfaceOptions(prefix + "ui.", value.getUi(), registry);
            }
        }
    }

    private void writeUserInterfaceOptions(String prefix, UserInterfaceOptions value, Registry registry) {
        if (value != null) {
            String showFindArtifacts = "showFindArtifacts";
            registry.setBoolean(prefix + showFindArtifacts, value.isShowFindArtifacts());
            String appletFindEnabled = "appletFindEnabled";
            registry.setBoolean(prefix + appletFindEnabled, value.isAppletFindEnabled());
            String disableEasterEggs = "disableEasterEggs";
            registry.setBoolean(prefix + disableEasterEggs, value.isDisableEasterEggs());
            if (value.getApplicationUrl() != null
            ) {
                String applicationUrl = "applicationUrl";
                registry.setString(prefix + applicationUrl, value.getApplicationUrl());
            }
            String disableRegistration = "disableRegistration";
            registry.setBoolean(prefix + disableRegistration, value.isDisableRegistration());
        }
    }

    private void writeNetworkConfiguration(String prefix, NetworkConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getMaxTotal() != 30
            ) {
                String maxTotal = "maxTotal";
                registry.setInt(prefix + maxTotal, value.getMaxTotal());
            }
            if (value.getMaxTotalPerHost() != 30
            ) {
                String maxTotalPerHost = "maxTotalPerHost";
                registry.setInt(prefix + maxTotalPerHost, value.getMaxTotalPerHost());
            }
            String usePooling = "usePooling";
            registry.setBoolean(prefix + usePooling, value.isUsePooling());
        }
    }

    private void writeArchivaRuntimeConfiguration(String prefix, ArchivaRuntimeConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getUrlFailureCacheConfiguration() != null
            ) {
                writeCacheConfiguration(prefix + "urlFailureCacheConfiguration.", value.getUrlFailureCacheConfiguration(), registry);
            }
            if (value.getFileLockConfiguration() != null
            ) {
                writeFileLockConfiguration(prefix + "fileLockConfiguration.", value.getFileLockConfiguration(), registry);
            }
            if (value.getDataDirectory() != null
            ) {
                String dataDirectory = "dataDirectory";
                registry.setString(prefix + dataDirectory, value.getDataDirectory());
            }
            if (value.getRepositoryBaseDirectory() != null
            ) {
                String repositoryBaseDirectory = "repositoryBaseDirectory";
                registry.setString(prefix + repositoryBaseDirectory, value.getRepositoryBaseDirectory());
            }
            if (value.getRemoteRepositoryBaseDirectory() != null
            ) {
                String remoteRepositoryBaseDirectory = "remoteRepositoryBaseDirectory";
                registry.setString(prefix + remoteRepositoryBaseDirectory, value.getRemoteRepositoryBaseDirectory());
            }
            if (value.getRepositoryGroupBaseDirectory() != null
            ) {
                String repositoryGroupBaseDirectory = "repositoryGroupBaseDirectory";
                registry.setString(prefix + repositoryGroupBaseDirectory, value.getRepositoryGroupBaseDirectory());
            }

            if (value.getDefaultLanguage() != null && !value.getDefaultLanguage().equals("en-US")
            ) {
                String defaultLanguage = "defaultLanguage";
                registry.setString(prefix + defaultLanguage, value.getDefaultLanguage());
            }
            if (value.getLanguageRange() != null && !value.getLanguageRange().equals("en,fr,de")
            ) {
                String languageRange = "languageRange";
                registry.setString(prefix + languageRange, value.getLanguageRange());
            }
            writeList(registry, value.getChecksumTypes(), prefix+"checksumTypes", "type");
        }
    }

    private void writeRedbackRuntimeConfiguration(String prefix, RedbackRuntimeConfiguration value, Registry registry) {
        if (value != null) {
            String migratedFromRedbackConfiguration = "migratedFromRedbackConfiguration";
            registry.setBoolean(prefix + migratedFromRedbackConfiguration, value.isMigratedFromRedbackConfiguration());
            if (value.getUserManagerImpls() != null && value.getUserManagerImpls().size() > 0
            ) {
                registry.removeSubset(prefix + "userManagerImpls");

                int count = 0;
                for (Iterator iter = value.getUserManagerImpls().iterator(); iter.hasNext(); count++) {
                    String name = "userManagerImpls.userManagerImpl(" + count + ")";
                    String userManagerImpl = (String) iter.next();
                    registry.setString(prefix + name, userManagerImpl);
                }
            }
            if (value.getRbacManagerImpls() != null && value.getRbacManagerImpls().size() > 0
            ) {
                registry.removeSubset(prefix + "rbacManagerImpls");

                int count = 0;
                for (Iterator iter = value.getRbacManagerImpls().iterator(); iter.hasNext(); count++) {
                    String name = "rbacManagerImpls.rbacManagerImpl(" + count + ")";
                    String rbacManagerImpl = (String) iter.next();
                    registry.setString(prefix + name, rbacManagerImpl);
                }
            }
            if (value.getLdapConfiguration() != null
            ) {
                writeLdapConfiguration(prefix + "ldapConfiguration.", value.getLdapConfiguration(), registry);
            }
            if (value.getLdapGroupMappings() != null && value.getLdapGroupMappings().size() > 0
            ) {
                registry.removeSubset(prefix + "ldapGroupMappings");

                int count = 0;
                for (Iterator iter = value.getLdapGroupMappings().iterator(); iter.hasNext(); count++) {
                    String name = "ldapGroupMappings.ldapGroupMapping(" + count + ")";
                    LdapGroupMapping o = (LdapGroupMapping) iter.next();
                    writeLdapGroupMapping(prefix + name + ".", o, registry);
                }
            }
            if (value.getConfigurationProperties() != null && value.getConfigurationProperties().size() > 0
            ) {
                registry.removeSubset(prefix + "configurationProperties");

                for (Iterator iter = value.getConfigurationProperties().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getConfigurationProperties().get(key);

                    registry.setString(prefix + "configurationProperties." + key, v);
                }
            }
            String useUsersCache = "useUsersCache";
            registry.setBoolean(prefix + useUsersCache, value.isUseUsersCache());
            if (value.getUsersCacheConfiguration() != null
            ) {
                writeCacheConfiguration(prefix + "usersCacheConfiguration.", value.getUsersCacheConfiguration(), registry);
            }
        }
    }

    private void writeArchivaDefaultConfiguration(String prefix, ArchivaDefaultConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getDefaultCheckPaths() != null && value.getDefaultCheckPaths().size() > 0
            ) {
                registry.removeSubset(prefix + "defaultCheckPaths");

                int count = 0;
                for (Iterator iter = value.getDefaultCheckPaths().iterator(); iter.hasNext(); count++) {
                    String name = "defaultCheckPaths.defaultCheckPath(" + count + ")";
                    RepositoryCheckPath o = (RepositoryCheckPath) iter.next();
                    writeRepositoryCheckPath(prefix + name + ".", o, registry);
                }
            }
        }
    }

    private void writeLdapConfiguration(String prefix, LdapConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getHostName() != null
            ) {
                String hostName = "hostName";
                registry.setString(prefix + hostName, value.getHostName());
            }
            if (value.getPort() != 0
            ) {
                String port = "port";
                registry.setInt(prefix + port, value.getPort());
            }
            String ssl = "ssl";
            registry.setBoolean(prefix + ssl, value.isSsl());
            if (value.getBaseDn() != null
            ) {
                String baseDn = "baseDn";
                registry.setString(prefix + baseDn, value.getBaseDn());
            }
            if (value.getBaseGroupsDn() != null
            ) {
                String baseGroupsDn = "baseGroupsDn";
                registry.setString(prefix + baseGroupsDn, value.getBaseGroupsDn());
            }
            if (value.getContextFactory() != null
            ) {
                String contextFactory = "contextFactory";
                registry.setString(prefix + contextFactory, value.getContextFactory());
            }
            if (value.getBindDn() != null
            ) {
                String bindDn = "bindDn";
                registry.setString(prefix + bindDn, value.getBindDn());
            }
            if (value.getPassword() != null
            ) {
                String password = "password";
                registry.setString(prefix + password, value.getPassword());
            }
            if (value.getAuthenticationMethod() != null
            ) {
                String authenticationMethod = "authenticationMethod";
                registry.setString(prefix + authenticationMethod, value.getAuthenticationMethod());
            }
            String bindAuthenticatorEnabled = "bindAuthenticatorEnabled";
            registry.setBoolean(prefix + bindAuthenticatorEnabled, value.isBindAuthenticatorEnabled());
            String writable = "writable";
            registry.setBoolean(prefix + writable, value.isWritable());
            String useRoleNameAsGroup = "useRoleNameAsGroup";
            registry.setBoolean(prefix + useRoleNameAsGroup, value.isUseRoleNameAsGroup());
            if (value.getExtraProperties() != null && value.getExtraProperties().size() > 0
            ) {
                registry.removeSubset(prefix + "extraProperties");

                for (Iterator iter = value.getExtraProperties().keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    String v = (String) value.getExtraProperties().get(key);

                    registry.setString(prefix + "extraProperties." + key, v);
                }
            }
        }
    }

    private void writeFileLockConfiguration(String prefix, FileLockConfiguration value, Registry registry) {
        if (value != null) {
            String skipLocking = "skipLocking";
            registry.setBoolean(prefix + skipLocking, value.isSkipLocking());
            if (value.getLockingTimeout() != 0
            ) {
                String lockingTimeout = "lockingTimeout";
                registry.setInt(prefix + lockingTimeout, value.getLockingTimeout());
            }
        }
    }

    private void writeCacheConfiguration(String prefix, CacheConfiguration value, Registry registry) {
        if (value != null) {
            if (value.getTimeToIdleSeconds() != -1
            ) {
                String timeToIdleSeconds = "timeToIdleSeconds";
                registry.setInt(prefix + timeToIdleSeconds, value.getTimeToIdleSeconds());
            }
            if (value.getTimeToLiveSeconds() != -1
            ) {
                String timeToLiveSeconds = "timeToLiveSeconds";
                registry.setInt(prefix + timeToLiveSeconds, value.getTimeToLiveSeconds());
            }
            if (value.getMaxElementsInMemory() != -1
            ) {
                String maxElementsInMemory = "maxElementsInMemory";
                registry.setInt(prefix + maxElementsInMemory, value.getMaxElementsInMemory());
            }
            if (value.getMaxElementsOnDisk() != -1
            ) {
                String maxElementsOnDisk = "maxElementsOnDisk";
                registry.setInt(prefix + maxElementsOnDisk, value.getMaxElementsOnDisk());
            }
        }
    }

    private void writeLdapGroupMapping(String prefix, LdapGroupMapping value, Registry registry) {
        if (value != null) {
            if (value.getGroup() != null
            ) {
                String group = "group";
                registry.setString(prefix + group, value.getGroup());
            }
            if (value.getRoleNames() != null && value.getRoleNames().size() > 0
            ) {
                registry.removeSubset(prefix + "roleNames");

                int count = 0;
                for (Iterator iter = value.getRoleNames().iterator(); iter.hasNext(); count++) {
                    String name = "roleNames.roleName(" + count + ")";
                    String roleName = (String) iter.next();
                    registry.setString(prefix + name, roleName);
                }
            }
        }
    }

}