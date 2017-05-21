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
import org.apache.archiva.policies.DownloadErrorPolicy;
import org.apache.archiva.policies.Policy;
import org.apache.archiva.policies.PostDownloadPolicy;
import org.apache.archiva.policies.PreDownloadPolicy;
import org.apache.archiva.redback.components.evaluator.DefaultExpressionEvaluator;
import org.apache.archiva.redback.components.evaluator.EvaluatorException;
import org.apache.archiva.redback.components.evaluator.ExpressionEvaluator;
import org.apache.archiva.redback.components.evaluator.sources.SystemPropertyExpressionSource;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.apache.archiva.redback.components.registry.commons.CommonsConfigurationRegistry;
import org.apache.archiva.redback.components.springutils.ComponentContainer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * </p>
 */
@Service("archivaConfiguration#default")
public class DefaultArchivaConfiguration
    implements ArchivaConfiguration, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( DefaultArchivaConfiguration.class );

    /**
     * Plexus registry to read the configuration from.
     */
    @Inject
    @Named(value = "commons-configuration")
    private Registry registry;

    @Inject
    private ComponentContainer componentContainer;

    /**
     * The configuration that has been converted.
     */
    private Configuration configuration;

    /**
     * see #initialize
     *
     * @todo these don't strictly belong in here
     */
    private Map<String, PreDownloadPolicy> prePolicies;

    /**
     * see #initialize
     *
     * @todo these don't strictly belong in here
     */
    private Map<String, PostDownloadPolicy> postPolicies;

    /**
     * see #initialize
     *
     * @todo these don't strictly belong in here
     */
    private Map<String, DownloadErrorPolicy> downloadErrorPolicies;


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

    @Override
    public Configuration getConfiguration()
    {
        return loadConfiguration();
    }

    private synchronized Configuration loadConfiguration()
    {
        if ( configuration == null )
        {
            configuration = load();
            configuration = unescapeExpressions( configuration );
            if ( isConfigurationDefaulted )
            {
                configuration = checkRepositoryLocations( configuration );
            }
        }

        return configuration;
    }

    @SuppressWarnings("unchecked")
    private Configuration load()
    {
        // TODO: should this be the same as section? make sure unnamed sections still work (eg, sys properties)
        Registry subset = registry.getSubset( KEY );
        if ( subset.getString( "version" ) == null )
        {
            // a little autodetection of v1, even if version is omitted (this was previously allowed)
            if ( subset.getSubset( "repositoryScanning" ).isEmpty() )
            {
                // only for empty, or v < 1
                subset = readDefaultConfiguration();
            }
        }

        Configuration config = new ConfigurationRegistryReader().read( subset );

        config.getRepositoryGroups();
        config.getRepositoryGroupsAsMap();
        if ( !config.getRepositories().isEmpty() )
        {
            for ( V1RepositoryConfiguration r : config.getRepositories() )
            {
                r.setScanned( r.isIndexed() );

                if ( StringUtils.startsWith( r.getUrl(), "file://" ) )
                {
                    r.setLocation( r.getUrl().substring( 7 ) );
                    config.addManagedRepository( r );
                }
                else if ( StringUtils.startsWith( r.getUrl(), "file:" ) )
                {
                    r.setLocation( r.getUrl().substring( 5 ) );
                    config.addManagedRepository( r );
                }
                else if ( StringUtils.isEmpty( r.getUrl() ) )
                {
                    // in case of empty url we can consider it as a managed one
                    // check if location is null
                    //file://${appserver.base}/repositories/${id}
                    if ( StringUtils.isEmpty( r.getLocation() ) )
                    {
                        r.setLocation( "file://${appserver.base}/repositories/" + r.getId() );
                    }
                    config.addManagedRepository( r );
                }
                else
                {
                    RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
                    repo.setId( r.getId() );
                    repo.setLayout( r.getLayout() );
                    repo.setName( r.getName() );
                    repo.setUrl( r.getUrl() );
                    config.addRemoteRepository( repo );
                }
            }

            // Prevent duplicate repositories from showing up.
            config.getRepositories().clear();

            registry.removeSubset( KEY + ".repositories" );
        }

        if ( !CollectionUtils.isEmpty( config.getRemoteRepositories() ) )
        {
            List<RemoteRepositoryConfiguration> remoteRepos = config.getRemoteRepositories();
            for ( RemoteRepositoryConfiguration repo : remoteRepos )
            {
                // [MRM-582] Remote Repositories with empty <username> and <password> fields shouldn't be created in configuration.
                if ( StringUtils.isBlank( repo.getUsername() ) )
                {
                    repo.setUsername( null );
                }

                if ( StringUtils.isBlank( repo.getPassword() ) )
                {
                    repo.setPassword( null );
                }
            }
        }

        if ( !config.getProxyConnectors().isEmpty() )
        {
            // Fix Proxy Connector Settings.

            // Create a copy of the list to read from (to prevent concurrent modification exceptions)
            List<ProxyConnectorConfiguration> proxyConnectorList = new ArrayList<>( config.getProxyConnectors() );
            // Remove the old connector list.
            config.getProxyConnectors().clear();

            for ( ProxyConnectorConfiguration connector : proxyConnectorList )
            {
                // Fix policies
                boolean connectorValid = true;

                Map<String, String> policies = new HashMap<>();
                // Make copy of policies
                policies.putAll( connector.getPolicies() );
                // Clear out policies
                connector.getPolicies().clear();

                // Work thru policies. cleaning them up.
                for ( Entry<String, String> entry : policies.entrySet() )
                {
                    String policyId = entry.getKey();
                    String setting = entry.getValue();

                    // Upgrade old policy settings.
                    if ( "releases".equals( policyId ) || "snapshots".equals( policyId ) )
                    {
                        if ( "ignored".equals( setting ) )
                        {
                            setting = AbstractUpdatePolicy.ALWAYS;
                        }
                        else if ( "disabled".equals( setting ) )
                        {
                            setting = AbstractUpdatePolicy.NEVER;
                        }
                    }
                    else if ( "cache-failures".equals( policyId ) )
                    {
                        if ( "ignored".equals( setting ) )
                        {
                            setting = CachedFailuresPolicy.NO;
                        }
                        else if ( "cached".equals( setting ) )
                        {
                            setting = CachedFailuresPolicy.YES;
                        }
                    }
                    else if ( "checksum".equals( policyId ) )
                    {
                        if ( "ignored".equals( setting ) )
                        {
                            setting = ChecksumPolicy.IGNORE;
                        }
                    }

                    // Validate existance of policy key.
                    if ( policyExists( policyId ) )
                    {
                        Policy policy = findPolicy( policyId );
                        // Does option exist?
                        if ( !policy.getOptions().contains( setting ) )
                        {
                            setting = policy.getDefaultOption();
                        }
                        connector.addPolicy( policyId, setting );
                    }
                    else
                    {
                        // Policy key doesn't exist. Don't add it to golden version.
                        log.warn( "Policy [{}] does not exist.", policyId );
                    }
                }

                if ( connectorValid )
                {
                    config.addProxyConnector( connector );
                }
            }

            // Normalize the order fields in the proxy connectors.
            Map<String, java.util.List<ProxyConnectorConfiguration>> proxyConnectorMap =
                config.getProxyConnectorAsMap();

            for ( List<ProxyConnectorConfiguration> connectors : proxyConnectorMap.values() )
            {
                // Sort connectors by order field.
                Collections.sort( connectors, ProxyConnectorConfigurationOrderComparator.getInstance() );

                // Normalize the order field values.
                int order = 1;
                for ( ProxyConnectorConfiguration connector : connectors )
                {
                    connector.setOrder( order++ );
                }
            }
        }

        return config;
    }

    private Policy findPolicy( String policyId )
    {
        if ( MapUtils.isEmpty( prePolicies ) )
        {
            log.error( "No PreDownloadPolicies found!" );
            return null;
        }

        if ( MapUtils.isEmpty( postPolicies ) )
        {
            log.error( "No PostDownloadPolicies found!" );
            return null;
        }

        Policy policy;

        policy = prePolicies.get( policyId );
        if ( policy != null )
        {
            return policy;
        }

        policy = postPolicies.get( policyId );
        if ( policy != null )
        {
            return policy;
        }

        policy = downloadErrorPolicies.get( policyId );
        if ( policy != null )
        {
            return policy;
        }

        return null;
    }

    private boolean policyExists( String policyId )
    {
        if ( MapUtils.isEmpty( prePolicies ) )
        {
            log.error( "No PreDownloadPolicies found!" );
            return false;
        }

        if ( MapUtils.isEmpty( postPolicies ) )
        {
            log.error( "No PostDownloadPolicies found!" );
            return false;
        }

        return ( prePolicies.containsKey( policyId ) || postPolicies.containsKey( policyId )
            || downloadErrorPolicies.containsKey( policyId ) );
    }

    private Registry readDefaultConfiguration()
    {
        // if it contains some old configuration, remove it (Archiva 0.9)
        registry.removeSubset( KEY );

        try
        {
            registry.addConfigurationFromResource( "org/apache/archiva/configuration/default-archiva.xml", KEY );
            this.isConfigurationDefaulted = true;
        }
        catch ( RegistryException e )
        {
            throw new ConfigurationRuntimeException(
                "Fatal error: Unable to find the built-in default configuration and load it into the registry", e );
        }
        return registry.getSubset( KEY );
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void save( Configuration configuration )
        throws IndeterminateConfigurationException, RegistryException
    {
        Registry section = registry.getSection( KEY + ".user" );
        Registry baseSection = registry.getSection( KEY + ".base" );
        if ( section == null )
        {
            section = baseSection;
            if ( section == null )
            {
                section = createDefaultConfigurationFile();
            }
        }
        else if ( baseSection != null )
        {
            Collection<String> keys = baseSection.getKeys();
            boolean foundList = false;
            for ( Iterator<String> i = keys.iterator(); i.hasNext() && !foundList; )
            {
                String key = i.next();

                // a little aggressive with the repositoryScanning and databaseScanning - should be no need to split
                // that configuration
                if ( key.startsWith( "repositories" ) //
                    || key.startsWith( "proxyConnectors" ) //
                    || key.startsWith( "networkProxies" ) //
                    || key.startsWith( "repositoryScanning" ) //
                    || key.startsWith( "remoteRepositories" ) //
                    || key.startsWith( "managedRepositories" ) //
                    || key.startsWith( "repositoryGroups" ) ) //
                {
                    foundList = true;
                }
            }

            if ( foundList )
            {
                this.configuration = null;

                throw new IndeterminateConfigurationException(
                    "Configuration can not be saved when it is loaded from two sources" );
            }
        }

        // escape all cron expressions to handle ','
        escapeCronExpressions( configuration );

        // [MRM-661] Due to a bug in the modello registry writer, we need to take these out by hand. They'll be put back by the writer.
        if ( section != null )
        {
            if ( configuration.getManagedRepositories().isEmpty() )
            {
                section.removeSubset( "managedRepositories" );
            }
            if ( configuration.getRemoteRepositories().isEmpty() )
            {
                section.removeSubset( "remoteRepositories" );

            }
            if ( configuration.getProxyConnectors().isEmpty() )
            {
                section.removeSubset( "proxyConnectors" );
            }
            if ( configuration.getNetworkProxies().isEmpty() )
            {
                section.removeSubset( "networkProxies" );
            }
            if ( configuration.getLegacyArtifactPaths().isEmpty() )
            {
                section.removeSubset( "legacyArtifactPaths" );
            }
            if ( configuration.getRepositoryGroups().isEmpty() )
            {
                section.removeSubset( "repositoryGroups" );
            }
            if ( configuration.getRepositoryScanning() != null )
            {
                if ( configuration.getRepositoryScanning().getKnownContentConsumers().isEmpty() )
                {
                    section.removeSubset( "repositoryScanning.knownContentConsumers" );
                }
                if ( configuration.getRepositoryScanning().getInvalidContentConsumers().isEmpty() )
                {
                    section.removeSubset( "repositoryScanning.invalidContentConsumers" );
                }
            }

            new ConfigurationRegistryWriter().write( configuration, section );
            section.save();
        }



        this.configuration = unescapeExpressions( configuration );

        triggerEvent( ConfigurationEvent.SAVED );
    }

    private void escapeCronExpressions( Configuration configuration )
    {
        for ( ManagedRepositoryConfiguration c : configuration.getManagedRepositories() )
        {
            c.setRefreshCronExpression( escapeCronExpression( c.getRefreshCronExpression() ) );
        }
    }

    private Registry createDefaultConfigurationFile()
        throws RegistryException
    {
        // TODO: may not be needed under commons-configuration 1.4 - check

        String contents = "<configuration />";

        String fileLocation = userConfigFilename;

        if ( !writeFile( "user configuration", userConfigFilename, contents ) )
        {
            fileLocation = altConfigFilename;
            if ( !writeFile( "alternative configuration", altConfigFilename, contents ) )
            {
                throw new RegistryException(
                    "Unable to create configuration file in either user [" + userConfigFilename + "] or alternative ["
                        + altConfigFilename
                        + "] locations on disk, usually happens when not allowed to write to those locations." );
            }
        }

        // olamy hackish I know :-)
        contents = "<configuration><xml fileName=\"" + fileLocation
            + "\" config-forceCreate=\"true\" config-name=\"org.apache.archiva.user\"/>" + "</configuration>";

        ( (CommonsConfigurationRegistry) registry ).setProperties( contents );

        registry.initialize();

        for ( RegistryListener regListener : registryListeners )
        {
            addRegistryChangeListener( regListener );
        }

        triggerEvent( ConfigurationEvent.SAVED );

        Registry section = registry.getSection( KEY + ".user" );
        return section == null ? new CommonsConfigurationRegistry( new BaseConfiguration() ) : section;
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
    private boolean writeFile( String filetype, String path, String contents )
    {
        File file = new File( path );

        try
        {
            // Check parent directory (if it is declared)
            if ( file.getParentFile() != null )
            {
                // Check that directory exists
                if ( !file.getParentFile().isDirectory() )
                {
                    // Directory to file must exist for file to be created
                    return false;
                }
            }

            FileUtils.writeStringToFile( file, contents, "UTF-8" );
            return true;
        }
        catch ( IOException e )
        {
            log.error( "Unable to create " + filetype + " file: " + e.getMessage(), e );
            return false;
        }
    }

    private void triggerEvent( int type )
    {
        ConfigurationEvent evt = new ConfigurationEvent( type );
        for ( ConfigurationListener listener : listeners )
        {
            listener.configurationEvent( evt );
        }
    }

    @Override
    public void addListener( ConfigurationListener listener )
    {
        if ( listener == null )
        {
            return;
        }

        listeners.add( listener );
    }

    @Override
    public void removeListener( ConfigurationListener listener )
    {
        if ( listener == null )
        {
            return;
        }

        listeners.remove( listener );
    }


    @Override
    public void addChangeListener( RegistryListener listener )
    {
        addRegistryChangeListener( listener );

        // keep track for later
        registryListeners.add( listener );
    }

    private void addRegistryChangeListener( RegistryListener listener )
    {
        Registry section = registry.getSection( KEY + ".user" );
        if ( section != null )
        {
            section.addChangeListener( listener );
        }
        section = registry.getSection( KEY + ".base" );
        if ( section != null )
        {
            section.addChangeListener( listener );
        }
    }

    @Override
    public void removeChangeListener( RegistryListener listener )
    {
        boolean removed = registryListeners.remove( listener );
        log.debug( "RegistryListener: '{}' removed {}", listener, removed );

        Registry section = registry.getSection( KEY + ".user" );
        if ( section != null )
        {
            section.removeChangeListener( listener );
        }
        section = registry.getSection( KEY + ".base" );
        if ( section != null )
        {
            section.removeChangeListener( listener );
        }

    }

    @PostConstruct
    public void initialize()
    {

        this.postPolicies = componentContainer.buildMapWithRole( PostDownloadPolicy.class );
        this.prePolicies = componentContainer.buildMapWithRole( PreDownloadPolicy.class );
        this.downloadErrorPolicies = componentContainer.buildMapWithRole( DownloadErrorPolicy.class );
        // Resolve expressions in the userConfigFilename and altConfigFilename
        try
        {
            ExpressionEvaluator expressionEvaluator = new DefaultExpressionEvaluator();
            expressionEvaluator.addExpressionSource( new SystemPropertyExpressionSource() );
            String userConfigFileNameSysProps = System.getProperty( "archiva.user.configFileName" );
            if ( StringUtils.isNotBlank( userConfigFileNameSysProps ) )
            {
                userConfigFilename = userConfigFileNameSysProps;
            }
            else
            {
                userConfigFilename = expressionEvaluator.expand( userConfigFilename );
            }
            altConfigFilename = expressionEvaluator.expand( altConfigFilename );
            loadConfiguration();
            handleUpgradeConfiguration();
        }
        catch ( IndeterminateConfigurationException | RegistryException e )
        {
            throw new RuntimeException( "failed during upgrade from previous version" + e.getMessage(), e );
        }
        catch ( EvaluatorException e )
        {
            throw new RuntimeException(
                "Unable to evaluate expressions found in " + "userConfigFilename or altConfigFilename.", e );
        }
        registry.addChangeListener( this );
    }

    /**
     * upgrade from 1.3
     */
    private void handleUpgradeConfiguration()
        throws RegistryException, IndeterminateConfigurationException
    {

        List<String> dbConsumers = Arrays.asList( "update-db-artifact", "update-db-repository-metadata" );

        // remove database consumers if here
        List<String> intersec =
            ListUtils.intersection( dbConsumers, configuration.getRepositoryScanning().getKnownContentConsumers() );

        if ( !intersec.isEmpty() )
        {

            List<String> knowContentConsumers =
                new ArrayList<>( configuration.getRepositoryScanning().getKnownContentConsumers().size() );
            for ( String knowContentConsumer : configuration.getRepositoryScanning().getKnownContentConsumers() )
            {
                if ( !dbConsumers.contains( knowContentConsumer ) )
                {
                    knowContentConsumers.add( knowContentConsumer );
                }
            }

            configuration.getRepositoryScanning().setKnownContentConsumers( knowContentConsumers );
        }

        // ensure create-archiva-metadata is here
        if ( !configuration.getRepositoryScanning().getKnownContentConsumers().contains( "create-archiva-metadata" ) )
        {
            List<String> knowContentConsumers =
                new ArrayList<>( configuration.getRepositoryScanning().getKnownContentConsumers() );
            knowContentConsumers.add( "create-archiva-metadata" );
            configuration.getRepositoryScanning().setKnownContentConsumers( knowContentConsumers );
        }

        // ensure duplicate-artifacts is here
        if ( !configuration.getRepositoryScanning().getKnownContentConsumers().contains( "duplicate-artifacts" ) )
        {
            List<String> knowContentConsumers =
                new ArrayList<>( configuration.getRepositoryScanning().getKnownContentConsumers() );
            knowContentConsumers.add( "duplicate-artifacts" );
            configuration.getRepositoryScanning().setKnownContentConsumers( knowContentConsumers );
        }
        // save ??
        //save( configuration );
    }

    @Override
    public void reload()
    {
        this.configuration = null;
        try
        {
            this.registry.initialize();
        }
        catch ( RegistryException e )
        {
            throw new ConfigurationRuntimeException( e.getMessage(), e );
        }
        this.initialize();
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // nothing to do here
    }

    @Override
    public synchronized void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        configuration = null;
    }

    private String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
                                            registry.getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
                                     registry.getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }

    private String unescapeCronExpression( String cronExpression )
    {
        return StringUtils.replace( cronExpression, "\\,", "," );
    }

    private String escapeCronExpression( String cronExpression )
    {
        return StringUtils.replace( cronExpression, ",", "\\," );
    }

    private Configuration unescapeExpressions( Configuration config )
    {
        // TODO: for commons-configuration 1.3 only
        for ( ManagedRepositoryConfiguration c : config.getManagedRepositories() )
        {
            c.setLocation( removeExpressions( c.getLocation() ) );
            c.setRefreshCronExpression( unescapeCronExpression( c.getRefreshCronExpression() ) );
        }

        return config;
    }

    private Configuration checkRepositoryLocations( Configuration config )
    {
        // additional check for [MRM-789], ensure that the location of the default repositories 
        // are not installed in the server installation        
        for ( ManagedRepositoryConfiguration repo : (List<ManagedRepositoryConfiguration>) config.getManagedRepositories() )
        {
            String repoPath = repo.getLocation();
            File repoLocation = new File( repoPath );

            if ( repoLocation.exists() && repoLocation.isDirectory() && !repoPath.endsWith(
                "data/repositories/" + repo.getId() ) )
            {
                repo.setLocation( repoPath + "/data/repositories/" + repo.getId() );
            }
        }

        return config;
    }

    public String getUserConfigFilename()
    {
        return userConfigFilename;
    }

    public String getAltConfigFilename()
    {
        return altConfigFilename;
    }

    @Override
    public boolean isDefaulted()
    {
        return this.isConfigurationDefaulted;
    }

    public Registry getRegistry()
    {
        return registry;
    }

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }


    public void setUserConfigFilename( String userConfigFilename )
    {
        this.userConfigFilename = userConfigFilename;
    }

    public void setAltConfigFilename( String altConfigFilename )
    {
        this.altConfigFilename = altConfigFilename;
    }
}
