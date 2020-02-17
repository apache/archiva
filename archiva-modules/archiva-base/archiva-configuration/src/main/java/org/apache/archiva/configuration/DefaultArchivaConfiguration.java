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

import org.apache.archiva.configuration.functors.ProxyConnectorConfigurationOrderComparator;
import org.apache.archiva.configuration.io.registry.ConfigurationRegistryReader;
import org.apache.archiva.configuration.io.registry.ConfigurationRegistryWriter;
import org.apache.archiva.policies.AbstractUpdatePolicy;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.components.evaluator.DefaultExpressionEvaluator;
import org.apache.archiva.components.evaluator.EvaluatorException;
import org.apache.archiva.components.evaluator.ExpressionEvaluator;
import org.apache.archiva.components.evaluator.sources.SystemPropertyExpressionSource;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.components.registry.commons.CommonsConfigurationRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * Implementation of configuration holder that retrieves it from the registry.
 * </p>
 * <p>
 * The registry layers and merges the 2 configuration files: user, and application server.
 * </p>
 * <p>
 * Instead of relying on the model defaults, if the registry is empty a default configuration file is loaded and
 * applied from a resource. The defaults are not loaded into the registry as the lists (eg repositories) could no longer
 * be removed if that was the case.
 * </p>
 * <p>
 * When saving the configuration, it is saved to the location it was read from. If it was read from the defaults, it
 * will be saved to the user location.
 * However, if the configuration contains information from both sources, an exception is raised as this is currently
 * unsupported. The reason for this is that it is not possible to identify where to re-save elements, and can result
 * in list configurations (eg repositories) becoming inconsistent.
 * </p>
 * <p>
 * If the configuration is outdated, it will be upgraded when it is loaded. This is done by checking the version flag
 * before reading it from the registry.
 * <p>
 * FIXME: The synchronization must be improved, the current impl may lead to inconsistent data or multiple getConfiguration() calls (martin_s@apache.org)
 * </p>
 */
@Service("archivaConfiguration#default")
public class DefaultArchivaConfiguration
        implements ArchivaConfiguration, RegistryListener {
    private final Logger log = LoggerFactory.getLogger(DefaultArchivaConfiguration.class);

    private static String FILE_ENCODING = "UTF-8";

    /**
     * Plexus registry to read the configuration from.
     */
    @Inject
    @Named(value = "commons-configuration")
    private Registry registry;

    /**
     * The configuration that has been converted.
     */
    private Configuration configuration;

    /**
     * see #initialize
     * default-value="${user.home}/.m2/archiva.xml"
     */
    private String userConfigFilename = "${user.home}/.m2/archiva.xml";

    /**
     * see #initialize
     * default-value="${appserver.base}/conf/archiva.xml"
     */
    private String altConfigFilename = "${appserver.base}/conf/archiva.xml";

    /**
     * Configuration Listeners we've registered.
     */
    private Set<ConfigurationListener> listeners = new HashSet<>();

    /**
     * Registry Listeners we've registered.
     */
    private Set<RegistryListener> registryListeners = new HashSet<>();

    /**
     * Boolean to help determine if the configuration exists as a result of pulling in
     * the default-archiva.xml
     */
    private boolean isConfigurationDefaulted = false;

    private static final String KEY = "org.apache.archiva";

    // Section used for default only configuration
    private static final String KEY_DEFAULT_ONLY = "org.apache.archiva_default";

    private Locale defaultLocale = Locale.getDefault();

    private List<Locale.LanguageRange> languagePriorities = new ArrayList<>();

    private volatile Path dataDirectory;
    private volatile Path repositoryBaseDirectory;
    private volatile Path remoteRepositoryBaseDirectory;
    private volatile Path repositoryGroupBaseDirectory;

    @PostConstruct
    private void init() {
        languagePriorities = Locale.LanguageRange.parse("en,fr,de");
    }


    @Override
    public Configuration getConfiguration() {
        return loadConfiguration();
    }

    private synchronized Configuration loadConfiguration() {
        if (configuration == null) {
            configuration = load();
            configuration = unescapeExpressions(configuration);
            if (isConfigurationDefaulted) {
                configuration = checkRepositoryLocations(configuration);
            }
        }

        return configuration;
    }

    private boolean hasConfigVersionChanged(Configuration current, Registry defaultOnlyConfiguration) {
        return current == null || current.getVersion() == null ||
                !current.getVersion().trim().equals(defaultOnlyConfiguration.getString("version", "").trim());
    }

    @SuppressWarnings("unchecked")
    private Configuration load() {
        // TODO: should this be the same as section? make sure unnamed sections still work (eg, sys properties)
        Registry subset = registry.getSubset(KEY);
        if (subset.getString("version") == null) {
            if (subset.getSubset("repositoryScanning").isEmpty()) {
                // only for empty
                subset = readDefaultConfiguration();
            } else {
                throw new RuntimeException("No version tag found in configuration. Archiva configuration version 1.x is not longer supported.");
            }
        }

        Configuration config = new ConfigurationRegistryReader().read(subset);

        // Resolving data and repositories directories
        // If the config entries are absolute, the path is used as it is
        // if the config entries are empty, they are resolved:
        //   dataDirectory = ${appserver.base}/data
        //   repositoryDirectory = ${dataDirectory}/repositories
        // If the entries are relative they are resolved
        //   relative to the appserver.base, for dataDirectory
        //   relative to dataDirectory for repositoryBase
        String dataDir = config.getArchivaRuntimeConfiguration().getDataDirectory();
        if (StringUtils.isEmpty(dataDir)) {
            dataDirectory = getAppServerBaseDir().resolve("data");
        } else {
            Path tmpDataDir = Paths.get(dataDir);
            if (tmpDataDir.isAbsolute()) {
                dataDirectory = tmpDataDir;
            } else {
                dataDirectory = getAppServerBaseDir().resolve(tmpDataDir);
            }
        }
        config.getArchivaRuntimeConfiguration().setDataDirectory(dataDirectory.normalize().toString());
        String repoBaseDir = config.getArchivaRuntimeConfiguration().getRepositoryBaseDirectory();
        if (StringUtils.isEmpty(repoBaseDir)) {
            repositoryBaseDirectory = dataDirectory.resolve("repositories");

        } else {
            Path tmpRepoBaseDir = Paths.get(repoBaseDir);
            if (tmpRepoBaseDir.isAbsolute()) {
                repositoryBaseDirectory = tmpRepoBaseDir;
            } else {
                dataDirectory.resolve(tmpRepoBaseDir);
            }
        }

        String remoteRepoBaseDir = config.getArchivaRuntimeConfiguration().getRemoteRepositoryBaseDirectory();
        if (StringUtils.isEmpty(remoteRepoBaseDir)) {
            remoteRepositoryBaseDirectory = dataDirectory.resolve("remotes");
        } else {
            Path tmpRemoteRepoDir = Paths.get(remoteRepoBaseDir);
            if (tmpRemoteRepoDir.isAbsolute()) {
                remoteRepositoryBaseDirectory = tmpRemoteRepoDir;
            } else {
                dataDirectory.resolve(tmpRemoteRepoDir);
            }
        }

        String repositoryGroupBaseDir = config.getArchivaRuntimeConfiguration().getRepositoryGroupBaseDirectory();
        if (StringUtils.isEmpty(repositoryGroupBaseDir)) {
            repositoryGroupBaseDirectory = dataDirectory.resolve("groups");
        } else {
            Path tmpGroupDir = Paths.get(repositoryGroupBaseDir);
            if (tmpGroupDir.isAbsolute()) {
                repositoryGroupBaseDirectory = tmpGroupDir;
            } else {
                dataDirectory.resolve(tmpGroupDir);
            }
        }


        config.getRepositoryGroups();
        config.getRepositoryGroupsAsMap();
        if (!CollectionUtils.isEmpty(config.getRemoteRepositories())) {
            List<RemoteRepositoryConfiguration> remoteRepos = config.getRemoteRepositories();
            for (RemoteRepositoryConfiguration repo : remoteRepos) {
                // [MRM-582] Remote Repositories with empty <username> and <password> fields shouldn't be created in configuration.
                if (StringUtils.isBlank(repo.getUsername())) {
                    repo.setUsername(null);
                }

                if (StringUtils.isBlank(repo.getPassword())) {
                    repo.setPassword(null);
                }
            }
        }

        if (!config.getProxyConnectors().isEmpty()) {
            // Fix Proxy Connector Settings.

            // Create a copy of the list to read from (to prevent concurrent modification exceptions)
            List<ProxyConnectorConfiguration> proxyConnectorList = new ArrayList<>(config.getProxyConnectors());
            // Remove the old connector list.
            config.getProxyConnectors().clear();

            for (ProxyConnectorConfiguration connector : proxyConnectorList) {
                // Fix policies
                boolean connectorValid = true;

                Map<String, String> policies = new HashMap<>();
                // Make copy of policies
                policies.putAll(connector.getPolicies());
                // Clear out policies
                connector.getPolicies().clear();

                // Work thru policies. cleaning them up.
                for (Entry<String, String> entry : policies.entrySet()) {
                    String policyId = entry.getKey();
                    String setting = entry.getValue();

                    // Upgrade old policy settings.
                    if ("releases".equals(policyId) || "snapshots".equals(policyId)) {
                        if ("ignored".equals(setting)) {
                            setting = AbstractUpdatePolicy.ALWAYS.getId();
                        } else if ("disabled".equals(setting)) {
                            setting = AbstractUpdatePolicy.NEVER.getId();
                        }
                    } else if ("cache-failures".equals(policyId)) {
                        if ("ignored".equals(setting)) {
                            setting = CachedFailuresPolicy.NO.getId();
                        } else if ("cached".equals(setting)) {
                            setting = CachedFailuresPolicy.YES.getId();
                        }
                    } else if ("checksum".equals(policyId)) {
                        if ("ignored".equals(setting)) {
                            setting = ChecksumPolicy.IGNORE.getId();
                        }
                    }

                    // Validate existance of policy key.
                    connector.addPolicy(policyId, setting);
                }

                if (connectorValid) {
                    config.addProxyConnector(connector);
                }
            }

            // Normalize the order fields in the proxy connectors.
            Map<String, java.util.List<ProxyConnectorConfiguration>> proxyConnectorMap =
                    config.getProxyConnectorAsMap();

            for (List<ProxyConnectorConfiguration> connectors : proxyConnectorMap.values()) {
                // Sort connectors by order field.
                Collections.sort(connectors, ProxyConnectorConfigurationOrderComparator.getInstance());

                // Normalize the order field values.
                int order = 1;
                for (ProxyConnectorConfiguration connector : connectors) {
                    connector.setOrder(order++);
                }
            }
        }

        this.defaultLocale = Locale.forLanguageTag(config.getArchivaRuntimeConfiguration().getDefaultLanguage());
        this.languagePriorities = Locale.LanguageRange.parse(config.getArchivaRuntimeConfiguration().getLanguageRange());
        return config;
    }

    /*
     * Updates the checkpath list for repositories.
     *
     * We are replacing existing ones and adding new ones. This allows to update the list with new releases.
     *
     * We are also updating existing remote repositories, if they exist already.
     *
     * This update method should only be called, if the config version changes to avoid overwriting
     * user repository settings all the time.
     */
    private void updateCheckPathDefaults(Configuration config, Registry defaultConfiguration) {
        List<RepositoryCheckPath> existingCheckPathList = config.getArchivaDefaultConfiguration().getDefaultCheckPaths();
        HashMap<String, RepositoryCheckPath> existingCheckPaths = new HashMap<>();
        HashMap<String, RepositoryCheckPath> newCheckPaths = new HashMap<>();
        for (RepositoryCheckPath path : config.getArchivaDefaultConfiguration().getDefaultCheckPaths()) {
            existingCheckPaths.put(path.getUrl(), path);
        }
        List defaultCheckPathsSubsets = defaultConfiguration.getSubsetList("archivaDefaultConfiguration.defaultCheckPaths.defaultCheckPath");
        for (Iterator i = defaultCheckPathsSubsets.iterator(); i.hasNext(); ) {
            RepositoryCheckPath v = readRepositoryCheckPath((Registry) i.next());
            if (existingCheckPaths.containsKey(v.getUrl())) {
                existingCheckPathList.remove(existingCheckPaths.get(v.getUrl()));
            }
            existingCheckPathList.add(v);
            newCheckPaths.put(v.getUrl(), v);
        }
        // Remote repositories update
        for (RemoteRepositoryConfiguration remoteRepositoryConfiguration : config.getRemoteRepositories()) {
            String url = remoteRepositoryConfiguration.getUrl().toLowerCase();
            if (newCheckPaths.containsKey(url)) {
                String currentPath = remoteRepositoryConfiguration.getCheckPath();
                String newPath = newCheckPaths.get(url).getPath();
                log.info("Updating connection check path for repository {}, from '{}' to '{}'.", remoteRepositoryConfiguration.getId(),
                        currentPath, newPath);
                remoteRepositoryConfiguration.setCheckPath(newPath);
            }
        }
    }

    private RepositoryCheckPath readRepositoryCheckPath(Registry registry) {
        RepositoryCheckPath value = new RepositoryCheckPath();

        String url = registry.getString("url", value.getUrl());

        value.setUrl(url);
        String path = registry.getString("path", value.getPath());
        value.setPath(path);
        return value;
    }


    private Registry readDefaultConfiguration() {
        // if it contains some old configuration, remove it (Archiva 0.9)
        registry.removeSubset(KEY);

        try {
            registry.addConfigurationFromResource("org/apache/archiva/configuration/default-archiva.xml", KEY);
            this.isConfigurationDefaulted = true;
        } catch (RegistryException e) {
            throw new ConfigurationRuntimeException(
                    "Fatal error: Unable to find the built-in default configuration and load it into the registry", e);
        }
        return registry.getSubset(KEY);
    }

    /*
     * Reads the default only configuration into a special prefix. This allows to check for changes
     * of the default configuration.
     */
    private Registry readDefaultOnlyConfiguration() {
        registry.removeSubset(KEY_DEFAULT_ONLY);
        try {
            registry.addConfigurationFromResource("org/apache/archiva/configuration/default-archiva.xml", KEY_DEFAULT_ONLY);
        } catch (RegistryException e) {
            throw new ConfigurationRuntimeException(
                    "Fatal error: Unable to find the built-in default configuration and load it into the registry", e);
        }
        return registry.getSubset(KEY_DEFAULT_ONLY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void save(Configuration configuration)
            throws IndeterminateConfigurationException, RegistryException {
        Registry section = registry.getSection(KEY + ".user");
        Registry baseSection = registry.getSection(KEY + ".base");
        if (section == null) {
            section = baseSection;
            if (section == null) {
                section = createDefaultConfigurationFile();
            }
        } else if (baseSection != null) {
            Collection<String> keys = baseSection.getKeys();
            boolean foundList = false;
            for (Iterator<String> i = keys.iterator(); i.hasNext() && !foundList; ) {
                String key = i.next();

                // a little aggressive with the repositoryScanning and databaseScanning - should be no need to split
                // that configuration
                if (key.startsWith("repositories") //
                        || key.startsWith("proxyConnectors") //
                        || key.startsWith("networkProxies") //
                        || key.startsWith("repositoryScanning") //
                        || key.startsWith("remoteRepositories") //
                        || key.startsWith("managedRepositories") //
                        || key.startsWith("repositoryGroups")) //
                {
                    foundList = true;
                }
            }

            if (foundList) {
                this.configuration = null;

                throw new IndeterminateConfigurationException(
                        "Configuration can not be saved when it is loaded from two sources");
            }
        }

        // escape all cron expressions to handle ','
        escapeCronExpressions(configuration);

        // [MRM-661] Due to a bug in the modello registry writer, we need to take these out by hand. They'll be put back by the writer.
        if (section != null) {
            if (configuration.getManagedRepositories().isEmpty()) {
                section.removeSubset("managedRepositories");
            }
            if (configuration.getRemoteRepositories().isEmpty()) {
                section.removeSubset("remoteRepositories");

            }
            if (configuration.getProxyConnectors().isEmpty()) {
                section.removeSubset("proxyConnectors");
            }
            if (configuration.getNetworkProxies().isEmpty()) {
                section.removeSubset("networkProxies");
            }
            if (configuration.getLegacyArtifactPaths().isEmpty()) {
                section.removeSubset("legacyArtifactPaths");
            }
            if (configuration.getRepositoryGroups().isEmpty()) {
                section.removeSubset("repositoryGroups");
            }
            if (configuration.getRepositoryScanning() != null) {
                if (configuration.getRepositoryScanning().getKnownContentConsumers().isEmpty()) {
                    section.removeSubset("repositoryScanning.knownContentConsumers");
                }
                if (configuration.getRepositoryScanning().getInvalidContentConsumers().isEmpty()) {
                    section.removeSubset("repositoryScanning.invalidContentConsumers");
                }
            }
            if (configuration.getArchivaRuntimeConfiguration() != null) {
                section.removeSubset("archivaRuntimeConfiguration.defaultCheckPaths");
            }

            new ConfigurationRegistryWriter().write(configuration, section);
            section.save();
        }


        this.configuration = unescapeExpressions(configuration);
        isConfigurationDefaulted = false;

        triggerEvent(ConfigurationEvent.SAVED);
    }

    private void escapeCronExpressions(Configuration configuration) {
        for (ManagedRepositoryConfiguration c : configuration.getManagedRepositories()) {
            c.setRefreshCronExpression(escapeCronExpression(c.getRefreshCronExpression()));
        }
    }

    private Registry createDefaultConfigurationFile()
            throws RegistryException {
        // TODO: may not be needed under commons-configuration 1.4 - check

        String contents = "<configuration />";

        String fileLocation = userConfigFilename;

        if (!writeFile("user configuration", userConfigFilename, contents)) {
            fileLocation = altConfigFilename;
            if (!writeFile("alternative configuration", altConfigFilename, contents, true)) {
                throw new RegistryException(
                        "Unable to create configuration file in either user [" + userConfigFilename + "] or alternative ["
                                + altConfigFilename
                                + "] locations on disk, usually happens when not allowed to write to those locations.");
            }
        }

        // olamy hackish I know :-)
        contents = "<configuration><xml fileName=\"" + fileLocation
                + "\" config-forceCreate=\"true\" config-name=\"org.apache.archiva.user\"/>" + "</configuration>";

        ((CommonsConfigurationRegistry) registry).setInitialConfiguration(contents);

        registry.initialize();

        for (RegistryListener regListener : registryListeners) {
            addRegistryChangeListener(regListener);
        }

        triggerEvent(ConfigurationEvent.SAVED);

        Registry section = registry.getSection(KEY + ".user");
        if (section == null) {
            return new CommonsConfigurationRegistry( );
        } else {
            return section;
        }
    }

    private boolean writeFile(String filetype, String path, String contents) {
        return writeFile( filetype, path, contents, false );
    }

    /**
     * Attempts to write the contents to a file, if an IOException occurs, return false.
     * <p/>
     * The file will be created if the directory to the file exists, otherwise this will return false.
     *
     * @param filetype the filetype (freeform text) to use in logging messages when failure to write.
     * @param path     the path to write to.
     * @param contents the contents to write.
     * @return true if write successful.
     */
    private boolean writeFile(String filetype, String path, String contents, boolean createDirs) {                
        try {
            Path file = Paths.get(path);
            // Check parent directory (if it is declared)
            final Path parent = file.getParent();
            if (parent != null) {
                // Check that directory exists
                if (!Files.exists( parent ) && createDirs) {
                    Files.createDirectories( parent );
                }
                if (!Files.isDirectory(parent)) {
                    // Directory to file must exist for file to be created
                    return false;
                }
            }
            FileUtils.writeStringToFile(file.toFile(), contents, FILE_ENCODING);
            return true;
        } catch (IOException e) {
            log.error("Unable to create {} file: {}", filetype, e.getMessage(), e);
            return false;
        } catch (InvalidPathException ipe) {
            log.error("Unable to read {} file: {}", path, ipe.getMessage(), ipe);
            return false;
        }
    }

    private void triggerEvent(int type) {
        ConfigurationEvent evt = new ConfigurationEvent(type);
        for (ConfigurationListener listener : listeners) {
            listener.configurationEvent(evt);
        }
    }

    @Override
    public void addListener(ConfigurationListener listener) {
        if (listener == null) {
            return;
        }

        listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
        if (listener == null) {
            return;
        }

        listeners.remove(listener);
    }


    @Override
    public void addChangeListener(RegistryListener listener) {
        addRegistryChangeListener(listener);

        // keep track for later
        registryListeners.add(listener);
    }

    private void addRegistryChangeListener(RegistryListener listener) {
        Registry section = registry.getSection(KEY + ".user");
        if (section != null) {
            section.addChangeListener(listener);
        }
        section = registry.getSection(KEY + ".base");
        if (section != null) {
            section.addChangeListener(listener);
        }
    }

    @Override
    public void removeChangeListener(RegistryListener listener) {
        boolean removed = registryListeners.remove(listener);
        log.debug("RegistryListener: '{}' removed {}", listener, removed);

        Registry section = registry.getSection(KEY + ".user");
        if (section != null) {
            section.removeChangeListener(listener);
        }
        section = registry.getSection(KEY + ".base");
        if (section != null) {
            section.removeChangeListener(listener);
        }

    }

    @PostConstruct
    public void initialize() {

        // Resolve expressions in the userConfigFilename and altConfigFilename
        try {
            ExpressionEvaluator expressionEvaluator = new DefaultExpressionEvaluator();
            expressionEvaluator.addExpressionSource(new SystemPropertyExpressionSource());
            String userConfigFileNameSysProps = System.getProperty(USER_CONFIG_PROPERTY);
            if (StringUtils.isNotBlank(userConfigFileNameSysProps)) {
                userConfigFilename = userConfigFileNameSysProps;
            } else {
                String userConfigFileNameEnv = System.getenv(USER_CONFIG_ENVVAR);
                if (StringUtils.isNotBlank(userConfigFileNameEnv)) {
                    userConfigFilename = userConfigFileNameEnv;
                } else {
                    userConfigFilename = expressionEvaluator.expand(userConfigFilename);
                }
            }
            altConfigFilename = expressionEvaluator.expand(altConfigFilename);
            loadConfiguration();
            handleUpgradeConfiguration();
        } catch (IndeterminateConfigurationException | RegistryException e) {
            throw new RuntimeException("failed during upgrade from previous version" + e.getMessage(), e);
        } catch (EvaluatorException e) {
            throw new RuntimeException(
                    "Unable to evaluate expressions found in " + "userConfigFilename or altConfigFilename.", e);
        }
        registry.addChangeListener(this);
    }

    /**
     * Handle upgrade to newer version
     */
    private void handleUpgradeConfiguration()
            throws RegistryException, IndeterminateConfigurationException {

        List<String> dbConsumers = Arrays.asList("update-db-artifact", "update-db-repository-metadata");

        // remove database consumers if here
        List<String> intersec =
                ListUtils.intersection(dbConsumers, configuration.getRepositoryScanning().getKnownContentConsumers());

        if (!intersec.isEmpty()) {

            List<String> knowContentConsumers =
                    new ArrayList<>(configuration.getRepositoryScanning().getKnownContentConsumers().size());
            for (String knowContentConsumer : configuration.getRepositoryScanning().getKnownContentConsumers()) {
                if (!dbConsumers.contains(knowContentConsumer)) {
                    knowContentConsumers.add(knowContentConsumer);
                }
            }

            configuration.getRepositoryScanning().setKnownContentConsumers(knowContentConsumers);
        }

        // ensure create-archiva-metadata is here
        if (!configuration.getRepositoryScanning().getKnownContentConsumers().contains("create-archiva-metadata")) {
            List<String> knowContentConsumers =
                    new ArrayList<>(configuration.getRepositoryScanning().getKnownContentConsumers());
            knowContentConsumers.add("create-archiva-metadata");
            configuration.getRepositoryScanning().setKnownContentConsumers(knowContentConsumers);
        }

        // ensure duplicate-artifacts is here
        if (!configuration.getRepositoryScanning().getKnownContentConsumers().contains("duplicate-artifacts")) {
            List<String> knowContentConsumers =
                    new ArrayList<>(configuration.getRepositoryScanning().getKnownContentConsumers());
            knowContentConsumers.add("duplicate-artifacts");
            configuration.getRepositoryScanning().setKnownContentConsumers(knowContentConsumers);
        }

        Registry defaultOnlyConfiguration = readDefaultOnlyConfiguration();
        // Currently we check only for configuration version change, not certain version numbers.
        if (hasConfigVersionChanged(configuration, defaultOnlyConfiguration)) {
            updateCheckPathDefaults(configuration, defaultOnlyConfiguration);
            String newVersion = defaultOnlyConfiguration.getString("version");
            if (newVersion == null) {
                throw new IndeterminateConfigurationException("The default configuration has no version information!");
            }
            configuration.setVersion(newVersion);
            try {
                save(configuration);
            } catch (IndeterminateConfigurationException e) {
                log.error("Error occured during configuration update to new version: {}", e.getMessage());
            } catch (RegistryException e) {
                log.error("Error occured during configuration update to new version: {}", e.getMessage());
            }
        }
    }

    @Override
    public void reload() {
        this.configuration = null;
        try {
            this.registry.initialize();
        } catch (RegistryException e) {
            throw new ConfigurationRuntimeException(e.getMessage(), e);
        }
        this.initialize();
    }

    @Override
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public List<Locale.LanguageRange> getLanguagePriorities() {
        return languagePriorities;
    }

    @Override
    public Path getAppServerBaseDir() {
        String basePath = registry.getString("appserver.base");
        if (!StringUtils.isEmpty(basePath)) {
            return Paths.get(basePath);
        } else {
            return Paths.get("");
        }
    }

    @Override
    public Path getRepositoryBaseDir() {
        if (repositoryBaseDirectory == null) {
            getConfiguration();
        }
        return repositoryBaseDirectory;

    }

    @Override
    public Path getRemoteRepositoryBaseDir() {
        if (remoteRepositoryBaseDirectory == null) {
            getConfiguration();
        }
        return remoteRepositoryBaseDirectory;
    }

    @Override
    public Path getRepositoryGroupBaseDir() {
        if (repositoryGroupBaseDirectory == null) {
            getConfiguration();
        }
        return repositoryGroupBaseDirectory;
    }

    @Override
    public Path getDataDirectory() {
        if (dataDirectory == null) {
            getConfiguration();
        }
        return dataDirectory;
    }

    @Override
    public void beforeConfigurationChange(Registry registry, String propertyName, Object propertyValue) {
        // nothing to do here
    }

    @Override
    public synchronized void afterConfigurationChange(Registry registry, String propertyName, Object propertyValue) {
        // configuration = null;
        // this.dataDirectory = null;
        // this.repositoryBaseDirectory = null;
    }

    private String removeExpressions(String directory) {
        String value = StringUtils.replace(directory, "${appserver.base}",
                registry.getString("appserver.base", "${appserver.base}"));
        value = StringUtils.replace(value, "${appserver.home}",
                registry.getString("appserver.home", "${appserver.home}"));
        return value;
    }

    private String unescapeCronExpression(String cronExpression) {
        return StringUtils.replace(cronExpression, "\\,", ",");
    }

    private String escapeCronExpression(String cronExpression) {
        return StringUtils.replace(cronExpression, ",", "\\,");
    }

    private Configuration unescapeExpressions(Configuration config) {
        // TODO: for commons-configuration 1.3 only
        for (ManagedRepositoryConfiguration c : config.getManagedRepositories()) {
            c.setLocation(removeExpressions(c.getLocation()));
            c.setRefreshCronExpression(unescapeCronExpression(c.getRefreshCronExpression()));
        }

        return config;
    }

    private Configuration checkRepositoryLocations(Configuration config) {
        // additional check for [MRM-789], ensure that the location of the default repositories 
        // are not installed in the server installation        
        for (ManagedRepositoryConfiguration repo : (List<ManagedRepositoryConfiguration>) config.getManagedRepositories()) {
            String repoPath = repo.getLocation();
            Path repoLocation = Paths.get(repoPath);

            if (Files.exists(repoLocation) && Files.isDirectory(repoLocation) && !repoPath.endsWith(
                    "data/repositories/" + repo.getId())) {
                repo.setLocation(repoPath + "/data/repositories/" + repo.getId());
            }
        }

        return config;
    }

    public String getUserConfigFilename() {
        return userConfigFilename;
    }

    public String getAltConfigFilename() {
        return altConfigFilename;
    }

    @Override
    public boolean isDefaulted() {
        return this.isConfigurationDefaulted;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }


    public void setUserConfigFilename(String userConfigFilename) {
        this.userConfigFilename = userConfigFilename;
    }

    public void setAltConfigFilename(String altConfigFilename) {
        this.altConfigFilename = altConfigFilename;
    }
}
