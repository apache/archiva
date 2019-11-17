
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
 * Generate Redback Registry input mechanism for model 'Configuration'.
 */
public class ConfigurationRegistryReader {
    public Configuration read(Registry registry) {
        return readConfiguration("", registry);
    }

    private Configuration readConfiguration(String prefix, Registry registry) {
        Configuration value = new Configuration();

        //String version = registry.getString( prefix + "version", value.getVersion() );

        List<String> versionList = registry.getList(prefix + "version");
        String version = value.getVersion();
        if (versionList != null && !versionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = versionList.size(); i < size; i++) {
                sb.append(versionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            version = sb.toString();
        }

        value.setVersion(version);
        //String metadataStore = registry.getString( prefix + "metadataStore", value.getMetadataStore() );

        List<String> metadataStoreList = registry.getList(prefix + "metadataStore");
        String metadataStore = value.getMetadataStore();
        if (metadataStoreList != null && !metadataStoreList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = metadataStoreList.size(); i < size; i++) {
                sb.append(metadataStoreList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            metadataStore = sb.toString();
        }

        value.setMetadataStore(metadataStore);
        java.util.List repositoryGroups = new java.util.ArrayList/*<RepositoryGroupConfiguration>*/();
        List repositoryGroupsSubsets = registry.getSubsetList(prefix + "repositoryGroups.repositoryGroup");
        for (Iterator i = repositoryGroupsSubsets.iterator(); i.hasNext(); ) {
            RepositoryGroupConfiguration v = readRepositoryGroupConfiguration("", (Registry) i.next());
            repositoryGroups.add(v);
        }
        value.setRepositoryGroups(repositoryGroups);
        java.util.List managedRepositories = new java.util.ArrayList/*<ManagedRepositoryConfiguration>*/();
        List managedRepositoriesSubsets = registry.getSubsetList(prefix + "managedRepositories.managedRepository");
        for (Iterator i = managedRepositoriesSubsets.iterator(); i.hasNext(); ) {
            ManagedRepositoryConfiguration v = readManagedRepositoryConfiguration("", (Registry) i.next());
            managedRepositories.add(v);
        }
        value.setManagedRepositories(managedRepositories);
        java.util.List remoteRepositories = new java.util.ArrayList/*<RemoteRepositoryConfiguration>*/();
        List remoteRepositoriesSubsets = registry.getSubsetList(prefix + "remoteRepositories.remoteRepository");
        for (Iterator i = remoteRepositoriesSubsets.iterator(); i.hasNext(); ) {
            RemoteRepositoryConfiguration v = readRemoteRepositoryConfiguration("", (Registry) i.next());
            remoteRepositories.add(v);
        }
        value.setRemoteRepositories(remoteRepositories);
        java.util.List proxyConnectors = new java.util.ArrayList/*<ProxyConnectorConfiguration>*/();
        List proxyConnectorsSubsets = registry.getSubsetList(prefix + "proxyConnectors.proxyConnector");
        for (Iterator i = proxyConnectorsSubsets.iterator(); i.hasNext(); ) {
            ProxyConnectorConfiguration v = readProxyConnectorConfiguration("", (Registry) i.next());
            proxyConnectors.add(v);
        }
        value.setProxyConnectors(proxyConnectors);
        java.util.List networkProxies = new java.util.ArrayList/*<NetworkProxyConfiguration>*/();
        List networkProxiesSubsets = registry.getSubsetList(prefix + "networkProxies.networkProxy");
        for (Iterator i = networkProxiesSubsets.iterator(); i.hasNext(); ) {
            NetworkProxyConfiguration v = readNetworkProxyConfiguration("", (Registry) i.next());
            networkProxies.add(v);
        }
        value.setNetworkProxies(networkProxies);
        java.util.List legacyArtifactPaths = new java.util.ArrayList/*<LegacyArtifactPath>*/();
        List legacyArtifactPathsSubsets = registry.getSubsetList(prefix + "legacyArtifactPaths.legacyArtifactPath");
        for (Iterator i = legacyArtifactPathsSubsets.iterator(); i.hasNext(); ) {
            LegacyArtifactPath v = readLegacyArtifactPath("", (Registry) i.next());
            legacyArtifactPaths.add(v);
        }
        value.setLegacyArtifactPaths(legacyArtifactPaths);
        RepositoryScanningConfiguration repositoryScanning = readRepositoryScanningConfiguration(prefix + "repositoryScanning.", registry);
        value.setRepositoryScanning(repositoryScanning);
        WebappConfiguration webapp = readWebappConfiguration(prefix + "webapp.", registry);
        value.setWebapp(webapp);
        OrganisationInformation organisationInfo = readOrganisationInformation(prefix + "organisationInfo.", registry);
        value.setOrganisationInfo(organisationInfo);
        NetworkConfiguration networkConfiguration = readNetworkConfiguration(prefix + "networkConfiguration.", registry);
        value.setNetworkConfiguration(networkConfiguration);
        RedbackRuntimeConfiguration redbackRuntimeConfiguration = readRedbackRuntimeConfiguration(prefix + "redbackRuntimeConfiguration.", registry);
        value.setRedbackRuntimeConfiguration(redbackRuntimeConfiguration);
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration = readArchivaRuntimeConfiguration(prefix + "archivaRuntimeConfiguration.", registry);
        value.setArchivaRuntimeConfiguration(archivaRuntimeConfiguration);
        java.util.List proxyConnectorRuleConfigurations = new java.util.ArrayList/*<ProxyConnectorRuleConfiguration>*/();
        List proxyConnectorRuleConfigurationsSubsets = registry.getSubsetList(prefix + "proxyConnectorRuleConfigurations.proxyConnectorRuleConfiguration");
        for (Iterator i = proxyConnectorRuleConfigurationsSubsets.iterator(); i.hasNext(); ) {
            ProxyConnectorRuleConfiguration v = readProxyConnectorRuleConfiguration("", (Registry) i.next());
            proxyConnectorRuleConfigurations.add(v);
        }
        value.setProxyConnectorRuleConfigurations(proxyConnectorRuleConfigurations);
        ArchivaDefaultConfiguration archivaDefaultConfiguration = readArchivaDefaultConfiguration(prefix + "archivaDefaultConfiguration.", registry);
        value.setArchivaDefaultConfiguration(archivaDefaultConfiguration);

        return value;
    }

    private AbstractRepositoryConfiguration readAbstractRepositoryConfiguration(String prefix, Registry registry) {
        AbstractRepositoryConfiguration value = new AbstractRepositoryConfiguration();

        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);
        //String type = registry.getString( prefix + "type", value.getType() );

        List<String> typeList = registry.getList(prefix + "type");
        String type = value.getType();
        if (typeList != null && !typeList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = typeList.size(); i < size; i++) {
                sb.append(typeList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            type = sb.toString();
        }

        value.setType(type);
        //String name = registry.getString( prefix + "name", value.getName() );

        List<String> nameList = registry.getList(prefix + "name");
        String name = value.getName();
        if (nameList != null && !nameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = nameList.size(); i < size; i++) {
                sb.append(nameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            name = sb.toString();
        }

        value.setName(name);
        //String layout = registry.getString( prefix + "layout", value.getLayout() );

        List<String> layoutList = registry.getList(prefix + "layout");
        String layout = value.getLayout();
        if (layoutList != null && !layoutList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = layoutList.size(); i < size; i++) {
                sb.append(layoutList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            layout = sb.toString();
        }

        value.setLayout(layout);
        //String indexDir = registry.getString( prefix + "indexDir", value.getIndexDir() );

        List<String> indexDirList = registry.getList(prefix + "indexDir");
        String indexDir = value.getIndexDir();
        if (indexDirList != null && !indexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = indexDirList.size(); i < size; i++) {
                sb.append(indexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            indexDir = sb.toString();
        }

        value.setIndexDir(indexDir);
        //String packedIndexDir = registry.getString( prefix + "packedIndexDir", value.getPackedIndexDir() );

        List<String> packedIndexDirList = registry.getList(prefix + "packedIndexDir");
        String packedIndexDir = value.getPackedIndexDir();
        if (packedIndexDirList != null && !packedIndexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = packedIndexDirList.size(); i < size; i++) {
                sb.append(packedIndexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            packedIndexDir = sb.toString();
        }

        value.setPackedIndexDir(packedIndexDir);
        //String description = registry.getString( prefix + "description", value.getDescription() );

        List<String> descriptionList = registry.getList(prefix + "description");
        String description = value.getDescription();
        if (descriptionList != null && !descriptionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = descriptionList.size(); i < size; i++) {
                sb.append(descriptionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            description = sb.toString();
        }

        value.setDescription(description);

        return value;
    }

    private RemoteRepositoryConfiguration readRemoteRepositoryConfiguration(String prefix, Registry registry) {
        RemoteRepositoryConfiguration value = new RemoteRepositoryConfiguration();

        //String url = registry.getString( prefix + "url", value.getUrl() );

        List<String> urlList = registry.getList(prefix + "url");
        String url = value.getUrl();
        if (urlList != null && !urlList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = urlList.size(); i < size; i++) {
                sb.append(urlList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            url = sb.toString();
        }

        value.setUrl(url);
        //String username = registry.getString( prefix + "username", value.getUsername() );

        List<String> usernameList = registry.getList(prefix + "username");
        String username = value.getUsername();
        if (usernameList != null && !usernameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = usernameList.size(); i < size; i++) {
                sb.append(usernameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            username = sb.toString();
        }

        value.setUsername(username);
        //String password = registry.getString( prefix + "password", value.getPassword() );

        List<String> passwordList = registry.getList(prefix + "password");
        String password = value.getPassword();
        if (passwordList != null && !passwordList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = passwordList.size(); i < size; i++) {
                sb.append(passwordList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            password = sb.toString();
        }

        value.setPassword(password);
        int timeout = registry.getInt(prefix + "timeout", value.getTimeout());
        value.setTimeout(timeout);
        //String refreshCronExpression = registry.getString( prefix + "refreshCronExpression", value.getRefreshCronExpression() );

        List<String> refreshCronExpressionList = registry.getList(prefix + "refreshCronExpression");
        String refreshCronExpression = value.getRefreshCronExpression();
        if (refreshCronExpressionList != null && !refreshCronExpressionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = refreshCronExpressionList.size(); i < size; i++) {
                sb.append(refreshCronExpressionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            refreshCronExpression = sb.toString();
        }

        value.setRefreshCronExpression(refreshCronExpression);
        boolean downloadRemoteIndex = registry.getBoolean(prefix + "downloadRemoteIndex", value.isDownloadRemoteIndex());
        value.setDownloadRemoteIndex(downloadRemoteIndex);
        //String remoteIndexUrl = registry.getString( prefix + "remoteIndexUrl", value.getRemoteIndexUrl() );

        List<String> remoteIndexUrlList = registry.getList(prefix + "remoteIndexUrl");
        String remoteIndexUrl = value.getRemoteIndexUrl();
        if (remoteIndexUrlList != null && !remoteIndexUrlList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = remoteIndexUrlList.size(); i < size; i++) {
                sb.append(remoteIndexUrlList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            remoteIndexUrl = sb.toString();
        }

        value.setRemoteIndexUrl(remoteIndexUrl);
        //String remoteDownloadNetworkProxyId = registry.getString( prefix + "remoteDownloadNetworkProxyId", value.getRemoteDownloadNetworkProxyId() );

        List<String> remoteDownloadNetworkProxyIdList = registry.getList(prefix + "remoteDownloadNetworkProxyId");
        String remoteDownloadNetworkProxyId = value.getRemoteDownloadNetworkProxyId();
        if (remoteDownloadNetworkProxyIdList != null && !remoteDownloadNetworkProxyIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = remoteDownloadNetworkProxyIdList.size(); i < size; i++) {
                sb.append(remoteDownloadNetworkProxyIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            remoteDownloadNetworkProxyId = sb.toString();
        }

        value.setRemoteDownloadNetworkProxyId(remoteDownloadNetworkProxyId);
        int remoteDownloadTimeout = registry.getInt(prefix + "remoteDownloadTimeout", value.getRemoteDownloadTimeout());
        value.setRemoteDownloadTimeout(remoteDownloadTimeout);
        boolean downloadRemoteIndexOnStartup = registry.getBoolean(prefix + "downloadRemoteIndexOnStartup", value.isDownloadRemoteIndexOnStartup());
        value.setDownloadRemoteIndexOnStartup(downloadRemoteIndexOnStartup);
        java.util.Map extraParameters = registry.getProperties(prefix + "extraParameters");
        value.setExtraParameters(extraParameters);
        java.util.Map extraHeaders = registry.getProperties(prefix + "extraHeaders");
        value.setExtraHeaders(extraHeaders);
        //String checkPath = registry.getString( prefix + "checkPath", value.getCheckPath() );

        List<String> checkPathList = registry.getList(prefix + "checkPath");
        String checkPath = value.getCheckPath();
        if (checkPathList != null && !checkPathList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = checkPathList.size(); i < size; i++) {
                sb.append(checkPathList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            checkPath = sb.toString();
        }

        value.setCheckPath(checkPath);
        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);
        //String type = registry.getString( prefix + "type", value.getType() );

        List<String> typeList = registry.getList(prefix + "type");
        String type = value.getType();
        if (typeList != null && !typeList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = typeList.size(); i < size; i++) {
                sb.append(typeList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            type = sb.toString();
        }

        value.setType(type);
        //String name = registry.getString( prefix + "name", value.getName() );

        List<String> nameList = registry.getList(prefix + "name");
        String name = value.getName();
        if (nameList != null && !nameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = nameList.size(); i < size; i++) {
                sb.append(nameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            name = sb.toString();
        }

        value.setName(name);
        //String layout = registry.getString( prefix + "layout", value.getLayout() );

        List<String> layoutList = registry.getList(prefix + "layout");
        String layout = value.getLayout();
        if (layoutList != null && !layoutList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = layoutList.size(); i < size; i++) {
                sb.append(layoutList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            layout = sb.toString();
        }

        value.setLayout(layout);
        //String indexDir = registry.getString( prefix + "indexDir", value.getIndexDir() );

        List<String> indexDirList = registry.getList(prefix + "indexDir");
        String indexDir = value.getIndexDir();
        if (indexDirList != null && !indexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = indexDirList.size(); i < size; i++) {
                sb.append(indexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            indexDir = sb.toString();
        }

        value.setIndexDir(indexDir);
        //String packedIndexDir = registry.getString( prefix + "packedIndexDir", value.getPackedIndexDir() );

        List<String> packedIndexDirList = registry.getList(prefix + "packedIndexDir");
        String packedIndexDir = value.getPackedIndexDir();
        if (packedIndexDirList != null && !packedIndexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = packedIndexDirList.size(); i < size; i++) {
                sb.append(packedIndexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            packedIndexDir = sb.toString();
        }

        value.setPackedIndexDir(packedIndexDir);
        //String description = registry.getString( prefix + "description", value.getDescription() );

        List<String> descriptionList = registry.getList(prefix + "description");
        String description = value.getDescription();
        if (descriptionList != null && !descriptionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = descriptionList.size(); i < size; i++) {
                sb.append(descriptionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            description = sb.toString();
        }

        value.setDescription(description);

        return value;
    }

    private ManagedRepositoryConfiguration readManagedRepositoryConfiguration(String prefix, Registry registry) {
        ManagedRepositoryConfiguration value = new ManagedRepositoryConfiguration();

        //String location = registry.getString( prefix + "location", value.getLocation() );

        List<String> locationList = registry.getList(prefix + "location");
        String location = value.getLocation();
        if (locationList != null && !locationList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = locationList.size(); i < size; i++) {
                sb.append(locationList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            location = sb.toString();
        }

        value.setLocation(location);
        boolean releases = registry.getBoolean(prefix + "releases", value.isReleases());
        value.setReleases(releases);
        boolean blockRedeployments = registry.getBoolean(prefix + "blockRedeployments", value.isBlockRedeployments());
        value.setBlockRedeployments(blockRedeployments);
        boolean snapshots = registry.getBoolean(prefix + "snapshots", value.isSnapshots());
        value.setSnapshots(snapshots);
        boolean scanned = registry.getBoolean(prefix + "scanned", value.isScanned());
        value.setScanned(scanned);
        //String refreshCronExpression = registry.getString( prefix + "refreshCronExpression", value.getRefreshCronExpression() );

        List<String> refreshCronExpressionList = registry.getList(prefix + "refreshCronExpression");
        String refreshCronExpression = value.getRefreshCronExpression();
        if (refreshCronExpressionList != null && !refreshCronExpressionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = refreshCronExpressionList.size(); i < size; i++) {
                sb.append(refreshCronExpressionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            refreshCronExpression = sb.toString();
        }

        value.setRefreshCronExpression(refreshCronExpression);
        int retentionCount = registry.getInt(prefix + "retentionCount", value.getRetentionCount());
        value.setRetentionCount(retentionCount);
        int retentionPeriod = registry.getInt(prefix + "retentionPeriod", value.getRetentionPeriod());
        value.setRetentionPeriod(retentionPeriod);
        boolean deleteReleasedSnapshots = registry.getBoolean(prefix + "deleteReleasedSnapshots", value.isDeleteReleasedSnapshots());
        value.setDeleteReleasedSnapshots(deleteReleasedSnapshots);
        boolean skipPackedIndexCreation = registry.getBoolean(prefix + "skipPackedIndexCreation", value.isSkipPackedIndexCreation());
        value.setSkipPackedIndexCreation(skipPackedIndexCreation);
        boolean stageRepoNeeded = registry.getBoolean(prefix + "stageRepoNeeded", value.isStageRepoNeeded());
        value.setStageRepoNeeded(stageRepoNeeded);
        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);
        //String type = registry.getString( prefix + "type", value.getType() );

        List<String> typeList = registry.getList(prefix + "type");
        String type = value.getType();
        if (typeList != null && !typeList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = typeList.size(); i < size; i++) {
                sb.append(typeList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            type = sb.toString();
        }

        value.setType(type);
        //String name = registry.getString( prefix + "name", value.getName() );

        List<String> nameList = registry.getList(prefix + "name");
        String name = value.getName();
        if (nameList != null && !nameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = nameList.size(); i < size; i++) {
                sb.append(nameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            name = sb.toString();
        }

        value.setName(name);
        //String layout = registry.getString( prefix + "layout", value.getLayout() );

        List<String> layoutList = registry.getList(prefix + "layout");
        String layout = value.getLayout();
        if (layoutList != null && !layoutList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = layoutList.size(); i < size; i++) {
                sb.append(layoutList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            layout = sb.toString();
        }

        value.setLayout(layout);
        //String indexDir = registry.getString( prefix + "indexDir", value.getIndexDir() );

        List<String> indexDirList = registry.getList(prefix + "indexDir");
        String indexDir = value.getIndexDir();
        if (indexDirList != null && !indexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = indexDirList.size(); i < size; i++) {
                sb.append(indexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            indexDir = sb.toString();
        }

        value.setIndexDir(indexDir);
        //String packedIndexDir = registry.getString( prefix + "packedIndexDir", value.getPackedIndexDir() );

        List<String> packedIndexDirList = registry.getList(prefix + "packedIndexDir");
        String packedIndexDir = value.getPackedIndexDir();
        if (packedIndexDirList != null && !packedIndexDirList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = packedIndexDirList.size(); i < size; i++) {
                sb.append(packedIndexDirList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            packedIndexDir = sb.toString();
        }

        value.setPackedIndexDir(packedIndexDir);
        //String description = registry.getString( prefix + "description", value.getDescription() );

        List<String> descriptionList = registry.getList(prefix + "description");
        String description = value.getDescription();
        if (descriptionList != null && !descriptionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = descriptionList.size(); i < size; i++) {
                sb.append(descriptionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            description = sb.toString();
        }

        value.setDescription(description);

        return value;
    }

    private LegacyArtifactPath readLegacyArtifactPath(String prefix, Registry registry) {
        LegacyArtifactPath value = new LegacyArtifactPath();

        //String path = registry.getString( prefix + "path", value.getPath() );

        List<String> pathList = registry.getList(prefix + "path");
        String path = value.getPath();
        if (pathList != null && !pathList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = pathList.size(); i < size; i++) {
                sb.append(pathList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            path = sb.toString();
        }

        value.setPath(path);
        //String artifact = registry.getString( prefix + "artifact", value.getArtifact() );

        List<String> artifactList = registry.getList(prefix + "artifact");
        String artifact = value.getArtifact();
        if (artifactList != null && !artifactList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = artifactList.size(); i < size; i++) {
                sb.append(artifactList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            artifact = sb.toString();
        }

        value.setArtifact(artifact);

        return value;
    }

    private RepositoryGroupConfiguration readRepositoryGroupConfiguration(String prefix, Registry registry) {
        RepositoryGroupConfiguration value = new RepositoryGroupConfiguration();

        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);

        value.setName(registry.getString(prefix + "name"));
        value.setType(registry.getString(prefix + "type"));

        //String mergedIndexPath = registry.getString( prefix + "mergedIndexPath", value.getMergedIndexPath() );

        List<String> mergedIndexPathList = registry.getList(prefix + "mergedIndexPath");
        String mergedIndexPath = value.getMergedIndexPath();
        if (mergedIndexPathList != null && !mergedIndexPathList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = mergedIndexPathList.size(); i < size; i++) {
                sb.append(mergedIndexPathList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            mergedIndexPath = sb.toString();
        }

        value.setMergedIndexPath(mergedIndexPath);
        int mergedIndexTtl = registry.getInt(prefix + "mergedIndexTtl", value.getMergedIndexTtl());
        value.setMergedIndexTtl(mergedIndexTtl);
        //String cronExpression = registry.getString( prefix + "cronExpression", value.getCronExpression() );

        List<String> cronExpressionList = registry.getList(prefix + "cronExpression");
        String cronExpression = value.getCronExpression();
        if (cronExpressionList != null && !cronExpressionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = cronExpressionList.size(); i < size; i++) {
                sb.append(cronExpressionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            cronExpression = sb.toString();
        }

        value.setCronExpression(cronExpression);
        java.util.List repositories = new java.util.ArrayList/*<String>*/();
        repositories.addAll(registry.getList(prefix + "repositories.repository"));
        value.setRepositories(repositories);

        return value;
    }

    private RepositoryCheckPath readRepositoryCheckPath(String prefix, Registry registry) {
        RepositoryCheckPath value = new RepositoryCheckPath();

        //String url = registry.getString( prefix + "url", value.getUrl() );

        List<String> urlList = registry.getList(prefix + "url");
        String url = value.getUrl();
        if (urlList != null && !urlList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = urlList.size(); i < size; i++) {
                sb.append(urlList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            url = sb.toString();
        }

        value.setUrl(url);
        //String path = registry.getString( prefix + "path", value.getPath() );

        List<String> pathList = registry.getList(prefix + "path");
        String path = value.getPath();
        if (pathList != null && !pathList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = pathList.size(); i < size; i++) {
                sb.append(pathList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            path = sb.toString();
        }

        value.setPath(path);

        return value;
    }

    private AbstractRepositoryConnectorConfiguration readAbstractRepositoryConnectorConfiguration(String prefix, Registry registry) {
        AbstractRepositoryConnectorConfiguration value = new AbstractRepositoryConnectorConfiguration();

        //String sourceRepoId = registry.getString( prefix + "sourceRepoId", value.getSourceRepoId() );

        List<String> sourceRepoIdList = registry.getList(prefix + "sourceRepoId");
        String sourceRepoId = value.getSourceRepoId();
        if (sourceRepoIdList != null && !sourceRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = sourceRepoIdList.size(); i < size; i++) {
                sb.append(sourceRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            sourceRepoId = sb.toString();
        }

        value.setSourceRepoId(sourceRepoId);
        //String targetRepoId = registry.getString( prefix + "targetRepoId", value.getTargetRepoId() );

        List<String> targetRepoIdList = registry.getList(prefix + "targetRepoId");
        String targetRepoId = value.getTargetRepoId();
        if (targetRepoIdList != null && !targetRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = targetRepoIdList.size(); i < size; i++) {
                sb.append(targetRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            targetRepoId = sb.toString();
        }

        value.setTargetRepoId(targetRepoId);
        //String proxyId = registry.getString( prefix + "proxyId", value.getProxyId() );

        List<String> proxyIdList = registry.getList(prefix + "proxyId");
        String proxyId = value.getProxyId();
        if (proxyIdList != null && !proxyIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = proxyIdList.size(); i < size; i++) {
                sb.append(proxyIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            proxyId = sb.toString();
        }

        value.setProxyId(proxyId);
        java.util.List blackListPatterns = new java.util.ArrayList/*<String>*/();
        blackListPatterns.addAll(registry.getList(prefix + "blackListPatterns.blackListPattern"));
        value.setBlackListPatterns(blackListPatterns);
        java.util.List whiteListPatterns = new java.util.ArrayList/*<String>*/();
        whiteListPatterns.addAll(registry.getList(prefix + "whiteListPatterns.whiteListPattern"));
        value.setWhiteListPatterns(whiteListPatterns);
        java.util.Map policies = registry.getProperties(prefix + "policies");
        value.setPolicies(policies);
        java.util.Map properties = registry.getProperties(prefix + "properties");
        value.setProperties(properties);
        boolean disabled = registry.getBoolean(prefix + "disabled", value.isDisabled());
        value.setDisabled(disabled);

        return value;
    }

    private ProxyConnectorRuleConfiguration readProxyConnectorRuleConfiguration(String prefix, Registry registry) {
        ProxyConnectorRuleConfiguration value = new ProxyConnectorRuleConfiguration();

        //String ruleType = registry.getString( prefix + "ruleType", value.getRuleType() );

        List<String> ruleTypeList = registry.getList(prefix + "ruleType");
        String ruleType = value.getRuleType();
        if (ruleTypeList != null && !ruleTypeList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = ruleTypeList.size(); i < size; i++) {
                sb.append(ruleTypeList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            ruleType = sb.toString();
        }

        value.setRuleType(ruleType);
        //String pattern = registry.getString( prefix + "pattern", value.getPattern() );

        List<String> patternList = registry.getList(prefix + "pattern");
        String pattern = value.getPattern();
        if (patternList != null && !patternList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = patternList.size(); i < size; i++) {
                sb.append(patternList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            pattern = sb.toString();
        }

        value.setPattern(pattern);
        java.util.List proxyConnectors = new java.util.ArrayList/*<ProxyConnectorConfiguration>*/();
        List proxyConnectorsSubsets = registry.getSubsetList(prefix + "proxyConnectors.proxyConnector");
        for (Iterator i = proxyConnectorsSubsets.iterator(); i.hasNext(); ) {
            ProxyConnectorConfiguration v = readProxyConnectorConfiguration("", (Registry) i.next());
            proxyConnectors.add(v);
        }
        value.setProxyConnectors(proxyConnectors);

        return value;
    }

    private ProxyConnectorConfiguration readProxyConnectorConfiguration(String prefix, Registry registry) {
        ProxyConnectorConfiguration value = new ProxyConnectorConfiguration();

        int order = registry.getInt(prefix + "order", value.getOrder());
        value.setOrder(order);
        //String sourceRepoId = registry.getString( prefix + "sourceRepoId", value.getSourceRepoId() );

        List<String> sourceRepoIdList = registry.getList(prefix + "sourceRepoId");
        String sourceRepoId = value.getSourceRepoId();
        if (sourceRepoIdList != null && !sourceRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = sourceRepoIdList.size(); i < size; i++) {
                sb.append(sourceRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            sourceRepoId = sb.toString();
        }

        value.setSourceRepoId(sourceRepoId);
        //String targetRepoId = registry.getString( prefix + "targetRepoId", value.getTargetRepoId() );

        List<String> targetRepoIdList = registry.getList(prefix + "targetRepoId");
        String targetRepoId = value.getTargetRepoId();
        if (targetRepoIdList != null && !targetRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = targetRepoIdList.size(); i < size; i++) {
                sb.append(targetRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            targetRepoId = sb.toString();
        }

        value.setTargetRepoId(targetRepoId);
        //String proxyId = registry.getString( prefix + "proxyId", value.getProxyId() );

        List<String> proxyIdList = registry.getList(prefix + "proxyId");
        String proxyId = value.getProxyId();
        if (proxyIdList != null && !proxyIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = proxyIdList.size(); i < size; i++) {
                sb.append(proxyIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            proxyId = sb.toString();
        }

        value.setProxyId(proxyId);
        java.util.List blackListPatterns = new java.util.ArrayList/*<String>*/();
        blackListPatterns.addAll(registry.getList(prefix + "blackListPatterns.blackListPattern"));
        value.setBlackListPatterns(blackListPatterns);
        java.util.List whiteListPatterns = new java.util.ArrayList/*<String>*/();
        whiteListPatterns.addAll(registry.getList(prefix + "whiteListPatterns.whiteListPattern"));
        value.setWhiteListPatterns(whiteListPatterns);
        java.util.Map policies = registry.getProperties(prefix + "policies");
        value.setPolicies(policies);
        java.util.Map properties = registry.getProperties(prefix + "properties");
        value.setProperties(properties);
        boolean disabled = registry.getBoolean(prefix + "disabled", value.isDisabled());
        value.setDisabled(disabled);

        return value;
    }

    private SyncConnectorConfiguration readSyncConnectorConfiguration(String prefix, Registry registry) {
        SyncConnectorConfiguration value = new SyncConnectorConfiguration();

        //String cronExpression = registry.getString( prefix + "cronExpression", value.getCronExpression() );

        List<String> cronExpressionList = registry.getList(prefix + "cronExpression");
        String cronExpression = value.getCronExpression();
        if (cronExpressionList != null && !cronExpressionList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = cronExpressionList.size(); i < size; i++) {
                sb.append(cronExpressionList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            cronExpression = sb.toString();
        }

        value.setCronExpression(cronExpression);
        //String method = registry.getString( prefix + "method", value.getMethod() );

        List<String> methodList = registry.getList(prefix + "method");
        String method = value.getMethod();
        if (methodList != null && !methodList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = methodList.size(); i < size; i++) {
                sb.append(methodList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            method = sb.toString();
        }

        value.setMethod(method);
        //String sourceRepoId = registry.getString( prefix + "sourceRepoId", value.getSourceRepoId() );

        List<String> sourceRepoIdList = registry.getList(prefix + "sourceRepoId");
        String sourceRepoId = value.getSourceRepoId();
        if (sourceRepoIdList != null && !sourceRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = sourceRepoIdList.size(); i < size; i++) {
                sb.append(sourceRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            sourceRepoId = sb.toString();
        }

        value.setSourceRepoId(sourceRepoId);
        //String targetRepoId = registry.getString( prefix + "targetRepoId", value.getTargetRepoId() );

        List<String> targetRepoIdList = registry.getList(prefix + "targetRepoId");
        String targetRepoId = value.getTargetRepoId();
        if (targetRepoIdList != null && !targetRepoIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = targetRepoIdList.size(); i < size; i++) {
                sb.append(targetRepoIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            targetRepoId = sb.toString();
        }

        value.setTargetRepoId(targetRepoId);
        //String proxyId = registry.getString( prefix + "proxyId", value.getProxyId() );

        List<String> proxyIdList = registry.getList(prefix + "proxyId");
        String proxyId = value.getProxyId();
        if (proxyIdList != null && !proxyIdList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = proxyIdList.size(); i < size; i++) {
                sb.append(proxyIdList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            proxyId = sb.toString();
        }

        value.setProxyId(proxyId);
        java.util.List blackListPatterns = new java.util.ArrayList/*<String>*/();
        blackListPatterns.addAll(registry.getList(prefix + "blackListPatterns.blackListPattern"));
        value.setBlackListPatterns(blackListPatterns);
        java.util.List whiteListPatterns = new java.util.ArrayList/*<String>*/();
        whiteListPatterns.addAll(registry.getList(prefix + "whiteListPatterns.whiteListPattern"));
        value.setWhiteListPatterns(whiteListPatterns);
        java.util.Map policies = registry.getProperties(prefix + "policies");
        value.setPolicies(policies);
        java.util.Map properties = registry.getProperties(prefix + "properties");
        value.setProperties(properties);
        boolean disabled = registry.getBoolean(prefix + "disabled", value.isDisabled());
        value.setDisabled(disabled);

        return value;
    }

    private NetworkProxyConfiguration readNetworkProxyConfiguration(String prefix, Registry registry) {
        NetworkProxyConfiguration value = new NetworkProxyConfiguration();

        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);
        //String protocol = registry.getString( prefix + "protocol", value.getProtocol() );

        List<String> protocolList = registry.getList(prefix + "protocol");
        String protocol = value.getProtocol();
        if (protocolList != null && !protocolList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = protocolList.size(); i < size; i++) {
                sb.append(protocolList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            protocol = sb.toString();
        }

        value.setProtocol(protocol);
        //String host = registry.getString( prefix + "host", value.getHost() );

        List<String> hostList = registry.getList(prefix + "host");
        String host = value.getHost();
        if (hostList != null && !hostList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = hostList.size(); i < size; i++) {
                sb.append(hostList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            host = sb.toString();
        }

        value.setHost(host);
        int port = registry.getInt(prefix + "port", value.getPort());
        value.setPort(port);
        //String username = registry.getString( prefix + "username", value.getUsername() );

        List<String> usernameList = registry.getList(prefix + "username");
        String username = value.getUsername();
        if (usernameList != null && !usernameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = usernameList.size(); i < size; i++) {
                sb.append(usernameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            username = sb.toString();
        }

        value.setUsername(username);
        //String password = registry.getString( prefix + "password", value.getPassword() );

        List<String> passwordList = registry.getList(prefix + "password");
        String password = value.getPassword();
        if (passwordList != null && !passwordList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = passwordList.size(); i < size; i++) {
                sb.append(passwordList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            password = sb.toString();
        }

        value.setPassword(password);
        boolean useNtlm = registry.getBoolean(prefix + "useNtlm", value.isUseNtlm());
        value.setUseNtlm(useNtlm);

        return value;
    }

    private RepositoryScanningConfiguration readRepositoryScanningConfiguration(String prefix, Registry registry) {
        RepositoryScanningConfiguration value = new RepositoryScanningConfiguration();

        java.util.List fileTypes = new java.util.ArrayList/*<FileType>*/();
        List fileTypesSubsets = registry.getSubsetList(prefix + "fileTypes.fileType");
        for (Iterator i = fileTypesSubsets.iterator(); i.hasNext(); ) {
            FileType v = readFileType("", (Registry) i.next());
            fileTypes.add(v);
        }
        value.setFileTypes(fileTypes);
        java.util.List knownContentConsumers = new java.util.ArrayList/*<String>*/();
        knownContentConsumers.addAll(registry.getList(prefix + "knownContentConsumers.knownContentConsumer"));
        value.setKnownContentConsumers(knownContentConsumers);
        java.util.List invalidContentConsumers = new java.util.ArrayList/*<String>*/();
        invalidContentConsumers.addAll(registry.getList(prefix + "invalidContentConsumers.invalidContentConsumer"));
        value.setInvalidContentConsumers(invalidContentConsumers);

        return value;
    }

    private FileType readFileType(String prefix, Registry registry) {
        FileType value = new FileType();

        //String id = registry.getString( prefix + "id", value.getId() );

        List<String> idList = registry.getList(prefix + "id");
        String id = value.getId();
        if (idList != null && !idList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = idList.size(); i < size; i++) {
                sb.append(idList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            id = sb.toString();
        }

        value.setId(id);
        java.util.List patterns = new java.util.ArrayList/*<String>*/();
        patterns.addAll(registry.getList(prefix + "patterns.pattern"));
        value.setPatterns(patterns);

        return value;
    }

    private OrganisationInformation readOrganisationInformation(String prefix, Registry registry) {
        OrganisationInformation value = new OrganisationInformation();

        //String name = registry.getString( prefix + "name", value.getName() );

        List<String> nameList = registry.getList(prefix + "name");
        String name = value.getName();
        if (nameList != null && !nameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = nameList.size(); i < size; i++) {
                sb.append(nameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            name = sb.toString();
        }

        value.setName(name);
        //String url = registry.getString( prefix + "url", value.getUrl() );

        List<String> urlList = registry.getList(prefix + "url");
        String url = value.getUrl();
        if (urlList != null && !urlList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = urlList.size(); i < size; i++) {
                sb.append(urlList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            url = sb.toString();
        }

        value.setUrl(url);
        //String logoLocation = registry.getString( prefix + "logoLocation", value.getLogoLocation() );

        List<String> logoLocationList = registry.getList(prefix + "logoLocation");
        String logoLocation = value.getLogoLocation();
        if (logoLocationList != null && !logoLocationList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = logoLocationList.size(); i < size; i++) {
                sb.append(logoLocationList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            logoLocation = sb.toString();
        }

        value.setLogoLocation(logoLocation);

        return value;
    }

    private WebappConfiguration readWebappConfiguration(String prefix, Registry registry) {
        WebappConfiguration value = new WebappConfiguration();

        UserInterfaceOptions ui = readUserInterfaceOptions(prefix + "ui.", registry);
        value.setUi(ui);

        return value;
    }

    private UserInterfaceOptions readUserInterfaceOptions(String prefix, Registry registry) {
        UserInterfaceOptions value = new UserInterfaceOptions();

        boolean showFindArtifacts = registry.getBoolean(prefix + "showFindArtifacts", value.isShowFindArtifacts());
        value.setShowFindArtifacts(showFindArtifacts);
        boolean appletFindEnabled = registry.getBoolean(prefix + "appletFindEnabled", value.isAppletFindEnabled());
        value.setAppletFindEnabled(appletFindEnabled);
        boolean disableEasterEggs = registry.getBoolean(prefix + "disableEasterEggs", value.isDisableEasterEggs());
        value.setDisableEasterEggs(disableEasterEggs);
        //String applicationUrl = registry.getString( prefix + "applicationUrl", value.getApplicationUrl() );

        List<String> applicationUrlList = registry.getList(prefix + "applicationUrl");
        String applicationUrl = value.getApplicationUrl();
        if (applicationUrlList != null && !applicationUrlList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = applicationUrlList.size(); i < size; i++) {
                sb.append(applicationUrlList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            applicationUrl = sb.toString();
        }

        value.setApplicationUrl(applicationUrl);
        boolean disableRegistration = registry.getBoolean(prefix + "disableRegistration", value.isDisableRegistration());
        value.setDisableRegistration(disableRegistration);

        return value;
    }

    private NetworkConfiguration readNetworkConfiguration(String prefix, Registry registry) {
        NetworkConfiguration value = new NetworkConfiguration();

        int maxTotal = registry.getInt(prefix + "maxTotal", value.getMaxTotal());
        value.setMaxTotal(maxTotal);
        int maxTotalPerHost = registry.getInt(prefix + "maxTotalPerHost", value.getMaxTotalPerHost());
        value.setMaxTotalPerHost(maxTotalPerHost);
        boolean usePooling = registry.getBoolean(prefix + "usePooling", value.isUsePooling());
        value.setUsePooling(usePooling);

        return value;
    }

    private ArchivaRuntimeConfiguration readArchivaRuntimeConfiguration(String prefix, Registry registry) {
        ArchivaRuntimeConfiguration value = new ArchivaRuntimeConfiguration();

        CacheConfiguration urlFailureCacheConfiguration = readCacheConfiguration(prefix + "urlFailureCacheConfiguration.", registry);
        value.setUrlFailureCacheConfiguration(urlFailureCacheConfiguration);
        FileLockConfiguration fileLockConfiguration = readFileLockConfiguration(prefix + "fileLockConfiguration.", registry);
        value.setFileLockConfiguration(fileLockConfiguration);
        //String dataDirectory = registry.getString( prefix + "dataDirectory", value.getDataDirectory() );

        List<String> dataDirectoryList = registry.getList(prefix + "dataDirectory");
        String dataDirectory = value.getDataDirectory();
        if (dataDirectoryList != null && !dataDirectoryList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = dataDirectoryList.size(); i < size; i++) {
                sb.append(dataDirectoryList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            dataDirectory = sb.toString();
        }

        value.setDataDirectory(dataDirectory);
        //String repositoryBaseDirectory = registry.getString( prefix + "repositoryBaseDirectory", value.getRepositoryBaseDirectory() );

        List<String> repositoryBaseDirectoryList = registry.getList(prefix + "repositoryBaseDirectory");
        String repositoryBaseDirectory = value.getRepositoryBaseDirectory();
        if (repositoryBaseDirectoryList != null && !repositoryBaseDirectoryList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = repositoryBaseDirectoryList.size(); i < size; i++) {
                sb.append(repositoryBaseDirectoryList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            repositoryBaseDirectory = sb.toString();
        }

        value.setRepositoryBaseDirectory(repositoryBaseDirectory);
        //String remoteRepositoryBaseDirectory = registry.getString( prefix + "remoteRepositoryBaseDirectory", value.getRemoteRepositoryBaseDirectory() );

        List<String> remoteRepositoryBaseDirectoryList = registry.getList(prefix + "remoteRepositoryBaseDirectory");
        String remoteRepositoryBaseDirectory = value.getRemoteRepositoryBaseDirectory();
        if (remoteRepositoryBaseDirectoryList != null && !remoteRepositoryBaseDirectoryList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = remoteRepositoryBaseDirectoryList.size(); i < size; i++) {
                sb.append(remoteRepositoryBaseDirectoryList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            remoteRepositoryBaseDirectory = sb.toString();
        }

        value.setRemoteRepositoryBaseDirectory(remoteRepositoryBaseDirectory);
        //String defaultLanguage = registry.getString( prefix + "defaultLanguage", value.getDefaultLanguage() );


        List<String> repositoryGroupBaseDirectoryList = registry.getList(prefix + "repositoryGroupBaseDirectory");
        String repositoryGroupBaseDirectory = value.getRepositoryGroupBaseDirectory();
        if (repositoryGroupBaseDirectoryList != null && !repositoryGroupBaseDirectoryList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = repositoryGroupBaseDirectoryList.size(); i < size; i++) {
                sb.append(repositoryGroupBaseDirectoryList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            repositoryGroupBaseDirectory = sb.toString();
        }

        value.setRepositoryGroupBaseDirectory(repositoryGroupBaseDirectory);

        List<String> defaultLanguageList = registry.getList(prefix + "defaultLanguage");
        String defaultLanguage = value.getDefaultLanguage();
        if (defaultLanguageList != null && !defaultLanguageList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = defaultLanguageList.size(); i < size; i++) {
                sb.append(defaultLanguageList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            defaultLanguage = sb.toString();
        }

        value.setDefaultLanguage(defaultLanguage);
        //String languageRange = registry.getString( prefix + "languageRange", value.getLanguageRange() );

        List<String> languageRangeList = registry.getList(prefix + "languageRange");
        String languageRange = value.getLanguageRange();
        if (languageRangeList != null && !languageRangeList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = languageRangeList.size(); i < size; i++) {
                sb.append(languageRangeList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            languageRange = sb.toString();
        }

        value.setLanguageRange(languageRange);

        List<String> checksumTypeList = registry.getList(prefix + "checksumTypes.type");
        value.setChecksumTypes(checksumTypeList);

        return value;
    }

    private RedbackRuntimeConfiguration readRedbackRuntimeConfiguration(String prefix, Registry registry) {
        RedbackRuntimeConfiguration value = new RedbackRuntimeConfiguration();

        boolean migratedFromRedbackConfiguration = registry.getBoolean(prefix + "migratedFromRedbackConfiguration", value.isMigratedFromRedbackConfiguration());
        value.setMigratedFromRedbackConfiguration(migratedFromRedbackConfiguration);
        java.util.List userManagerImpls = new java.util.ArrayList/*<String>*/();
        userManagerImpls.addAll(registry.getList(prefix + "userManagerImpls.userManagerImpl"));
        value.setUserManagerImpls(userManagerImpls);
        java.util.List rbacManagerImpls = new java.util.ArrayList/*<String>*/();
        rbacManagerImpls.addAll(registry.getList(prefix + "rbacManagerImpls.rbacManagerImpl"));
        value.setRbacManagerImpls(rbacManagerImpls);
        LdapConfiguration ldapConfiguration = readLdapConfiguration(prefix + "ldapConfiguration.", registry);
        value.setLdapConfiguration(ldapConfiguration);
        java.util.List ldapGroupMappings = new java.util.ArrayList/*<LdapGroupMapping>*/();
        List ldapGroupMappingsSubsets = registry.getSubsetList(prefix + "ldapGroupMappings.ldapGroupMapping");
        for (Iterator i = ldapGroupMappingsSubsets.iterator(); i.hasNext(); ) {
            LdapGroupMapping v = readLdapGroupMapping("", (Registry) i.next());
            ldapGroupMappings.add(v);
        }
        value.setLdapGroupMappings(ldapGroupMappings);
        java.util.Map configurationProperties = registry.getProperties(prefix + "configurationProperties");
        value.setConfigurationProperties(configurationProperties);
        boolean useUsersCache = registry.getBoolean(prefix + "useUsersCache", value.isUseUsersCache());
        value.setUseUsersCache(useUsersCache);
        CacheConfiguration usersCacheConfiguration = readCacheConfiguration(prefix + "usersCacheConfiguration.", registry);
        value.setUsersCacheConfiguration(usersCacheConfiguration);

        return value;
    }

    private ArchivaDefaultConfiguration readArchivaDefaultConfiguration(String prefix, Registry registry) {
        ArchivaDefaultConfiguration value = new ArchivaDefaultConfiguration();

        java.util.List defaultCheckPaths = new java.util.ArrayList/*<RepositoryCheckPath>*/();
        List defaultCheckPathsSubsets = registry.getSubsetList(prefix + "defaultCheckPaths.defaultCheckPath");
        for (Iterator i = defaultCheckPathsSubsets.iterator(); i.hasNext(); ) {
            RepositoryCheckPath v = readRepositoryCheckPath("", (Registry) i.next());
            defaultCheckPaths.add(v);
        }
        value.setDefaultCheckPaths(defaultCheckPaths);

        return value;
    }

    private LdapConfiguration readLdapConfiguration(String prefix, Registry registry) {
        LdapConfiguration value = new LdapConfiguration();

        //String hostName = registry.getString( prefix + "hostName", value.getHostName() );

        List<String> hostNameList = registry.getList(prefix + "hostName");
        String hostName = value.getHostName();
        if (hostNameList != null && !hostNameList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = hostNameList.size(); i < size; i++) {
                sb.append(hostNameList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            hostName = sb.toString();
        }

        value.setHostName(hostName);
        int port = registry.getInt(prefix + "port", value.getPort());
        value.setPort(port);
        boolean ssl = registry.getBoolean(prefix + "ssl", value.isSsl());
        value.setSsl(ssl);
        //String baseDn = registry.getString( prefix + "baseDn", value.getBaseDn() );

        List<String> baseDnList = registry.getList(prefix + "baseDn");
        String baseDn = value.getBaseDn();
        if (baseDnList != null && !baseDnList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = baseDnList.size(); i < size; i++) {
                sb.append(baseDnList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            baseDn = sb.toString();
        }

        value.setBaseDn(baseDn);
        //String baseGroupsDn = registry.getString( prefix + "baseGroupsDn", value.getBaseGroupsDn() );

        List<String> baseGroupsDnList = registry.getList(prefix + "baseGroupsDn");
        String baseGroupsDn = value.getBaseGroupsDn();
        if (baseGroupsDnList != null && !baseGroupsDnList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = baseGroupsDnList.size(); i < size; i++) {
                sb.append(baseGroupsDnList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            baseGroupsDn = sb.toString();
        }

        value.setBaseGroupsDn(baseGroupsDn);
        //String contextFactory = registry.getString( prefix + "contextFactory", value.getContextFactory() );

        List<String> contextFactoryList = registry.getList(prefix + "contextFactory");
        String contextFactory = value.getContextFactory();
        if (contextFactoryList != null && !contextFactoryList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = contextFactoryList.size(); i < size; i++) {
                sb.append(contextFactoryList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            contextFactory = sb.toString();
        }

        value.setContextFactory(contextFactory);
        //String bindDn = registry.getString( prefix + "bindDn", value.getBindDn() );

        List<String> bindDnList = registry.getList(prefix + "bindDn");
        String bindDn = value.getBindDn();
        if (bindDnList != null && !bindDnList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = bindDnList.size(); i < size; i++) {
                sb.append(bindDnList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            bindDn = sb.toString();
        }

        value.setBindDn(bindDn);
        //String password = registry.getString( prefix + "password", value.getPassword() );

        List<String> passwordList = registry.getList(prefix + "password");
        String password = value.getPassword();
        if (passwordList != null && !passwordList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = passwordList.size(); i < size; i++) {
                sb.append(passwordList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            password = sb.toString();
        }

        value.setPassword(password);
        //String authenticationMethod = registry.getString( prefix + "authenticationMethod", value.getAuthenticationMethod() );

        List<String> authenticationMethodList = registry.getList(prefix + "authenticationMethod");
        String authenticationMethod = value.getAuthenticationMethod();
        if (authenticationMethodList != null && !authenticationMethodList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = authenticationMethodList.size(); i < size; i++) {
                sb.append(authenticationMethodList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            authenticationMethod = sb.toString();
        }

        value.setAuthenticationMethod(authenticationMethod);
        boolean bindAuthenticatorEnabled = registry.getBoolean(prefix + "bindAuthenticatorEnabled", value.isBindAuthenticatorEnabled());
        value.setBindAuthenticatorEnabled(bindAuthenticatorEnabled);
        boolean writable = registry.getBoolean(prefix + "writable", value.isWritable());
        value.setWritable(writable);
        boolean useRoleNameAsGroup = registry.getBoolean(prefix + "useRoleNameAsGroup", value.isUseRoleNameAsGroup());
        value.setUseRoleNameAsGroup(useRoleNameAsGroup);
        java.util.Map extraProperties = registry.getProperties(prefix + "extraProperties");
        value.setExtraProperties(extraProperties);

        return value;
    }

    private FileLockConfiguration readFileLockConfiguration(String prefix, Registry registry) {
        FileLockConfiguration value = new FileLockConfiguration();

        boolean skipLocking = registry.getBoolean(prefix + "skipLocking", value.isSkipLocking());
        value.setSkipLocking(skipLocking);
        int lockingTimeout = registry.getInt(prefix + "lockingTimeout", value.getLockingTimeout());
        value.setLockingTimeout(lockingTimeout);

        return value;
    }

    private CacheConfiguration readCacheConfiguration(String prefix, Registry registry) {
        CacheConfiguration value = new CacheConfiguration();

        int timeToIdleSeconds = registry.getInt(prefix + "timeToIdleSeconds", value.getTimeToIdleSeconds());
        value.setTimeToIdleSeconds(timeToIdleSeconds);
        int timeToLiveSeconds = registry.getInt(prefix + "timeToLiveSeconds", value.getTimeToLiveSeconds());
        value.setTimeToLiveSeconds(timeToLiveSeconds);
        int maxElementsInMemory = registry.getInt(prefix + "maxElementsInMemory", value.getMaxElementsInMemory());
        value.setMaxElementsInMemory(maxElementsInMemory);
        int maxElementsOnDisk = registry.getInt(prefix + "maxElementsOnDisk", value.getMaxElementsOnDisk());
        value.setMaxElementsOnDisk(maxElementsOnDisk);

        return value;
    }

    private LdapGroupMapping readLdapGroupMapping(String prefix, Registry registry) {
        LdapGroupMapping value = new LdapGroupMapping();

        //String group = registry.getString( prefix + "group", value.getGroup() );

        List<String> groupList = registry.getList(prefix + "group");
        String group = value.getGroup();
        if (groupList != null && !groupList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = groupList.size(); i < size; i++) {
                sb.append(groupList.get(i));
                if (i < size - 1) {
                    sb.append(',');
                }
            }
            group = sb.toString();
        }

        value.setGroup(group);
        java.util.List roleNames = new java.util.ArrayList/*<String>*/();
        roleNames.addAll(registry.getList(prefix + "roleNames.roleName"));
        value.setRoleNames(roleNames);

        return value;
    }

}
