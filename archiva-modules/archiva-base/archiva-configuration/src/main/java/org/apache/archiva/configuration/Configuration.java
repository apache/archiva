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

/**
 * Class Configuration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class Configuration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * This is the version of the configuration format.
     */
    private String version = "3.0.0";

    /**
     * The type of the metadata storage. Allowed values: jcr, file,
     * cassandra.
     */
    private String metadataStore = "jcr";

    /**
     * Field repositoryGroups.
     */
    private java.util.List<RepositoryGroupConfiguration> repositoryGroups;

    /**
     * Field managedRepositories.
     */
    private java.util.List<ManagedRepositoryConfiguration> managedRepositories;

    /**
     * Field remoteRepositories.
     */
    private java.util.List<RemoteRepositoryConfiguration> remoteRepositories;

    /**
     * Field proxyConnectors.
     */
    private java.util.List<ProxyConnectorConfiguration> proxyConnectors;

    /**
     * Field networkProxies.
     */
    private java.util.List<NetworkProxyConfiguration> networkProxies;

    /**
     * Field legacyArtifactPaths.
     */
    private java.util.List<LegacyArtifactPath> legacyArtifactPaths;

    /**
     * 
     *             The repository scanning configuration.
     *           
     */
    private RepositoryScanningConfiguration repositoryScanning;

    /**
     * 
     *             The webapp configuration.
     *           
     */
    private WebappConfiguration webapp;

    /**
     * 
     *             The organisation info.
     *           
     */
    private OrganisationInformation organisationInfo;

    /**
     * 
     *             The NetworkConfiguration .
     *           
     */
    private NetworkConfiguration networkConfiguration;

    /**
     * The RedbackRuntimeConfiguration.
     */
    private RedbackRuntimeConfiguration redbackRuntimeConfiguration;

    /**
     * The ArchivaRuntimeConfiguration.
     */
    private ArchivaRuntimeConfiguration archivaRuntimeConfiguration;

    /**
     * Field proxyConnectorRuleConfigurations.
     */
    private java.util.List<ProxyConnectorRuleConfiguration> proxyConnectorRuleConfigurations;

    /**
     * Archiva default settings.
     */
    private ArchivaDefaultConfiguration archivaDefaultConfiguration;

    /**
     * Field modelEncoding.
     */
    private String modelEncoding = "UTF-8";


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addLegacyArtifactPath.
     * 
     * @param legacyArtifactPath
     */
    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
    {
        getLegacyArtifactPaths().add( legacyArtifactPath );
    } //-- void addLegacyArtifactPath( LegacyArtifactPath )

    /**
     * Method addManagedRepository.
     * 
     * @param managedRepositoryConfiguration
     */
    public void addManagedRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration )
    {
        getManagedRepositories().add( managedRepositoryConfiguration );
    } //-- void addManagedRepository( ManagedRepositoryConfiguration )

    /**
     * Method addNetworkProxy.
     * 
     * @param networkProxyConfiguration
     */
    public void addNetworkProxy( NetworkProxyConfiguration networkProxyConfiguration )
    {
        getNetworkProxies().add( networkProxyConfiguration );
    } //-- void addNetworkProxy( NetworkProxyConfiguration )

    /**
     * Method addProxyConnector.
     * 
     * @param proxyConnectorConfiguration
     */
    public void addProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        getProxyConnectors().add( proxyConnectorConfiguration );
    } //-- void addProxyConnector( ProxyConnectorConfiguration )

    /**
     * Method addProxyConnectorRuleConfiguration.
     * 
     * @param proxyConnectorRuleConfiguration
     */
    public void addProxyConnectorRuleConfiguration( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration )
    {
        getProxyConnectorRuleConfigurations().add( proxyConnectorRuleConfiguration );
    } //-- void addProxyConnectorRuleConfiguration( ProxyConnectorRuleConfiguration )

    /**
     * Method addRemoteRepository.
     * 
     * @param remoteRepositoryConfiguration
     */
    public void addRemoteRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration )
    {
        getRemoteRepositories().add( remoteRepositoryConfiguration );
    } //-- void addRemoteRepository( RemoteRepositoryConfiguration )

    /**
     * Method addRepositoryGroup.
     * 
     * @param repositoryGroupConfiguration
     */
    public void addRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration )
    {
        getRepositoryGroups().add( repositoryGroupConfiguration );
    } //-- void addRepositoryGroup( RepositoryGroupConfiguration )

    /**
     * Get archiva default settings.
     * 
     * @return ArchivaDefaultConfiguration
     */
    public ArchivaDefaultConfiguration getArchivaDefaultConfiguration()
    {
        return this.archivaDefaultConfiguration;
    } //-- ArchivaDefaultConfiguration getArchivaDefaultConfiguration()

    /**
     * Get the ArchivaRuntimeConfiguration.
     * 
     * @return ArchivaRuntimeConfiguration
     */
    public ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()
    {
        return this.archivaRuntimeConfiguration;
    } //-- ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()

    /**
     * Method getLegacyArtifactPaths.
     * 
     * @return List
     */
    public java.util.List<LegacyArtifactPath> getLegacyArtifactPaths()
    {
        if ( this.legacyArtifactPaths == null )
        {
            this.legacyArtifactPaths = new java.util.ArrayList<LegacyArtifactPath>();
        }

        return this.legacyArtifactPaths;
    } //-- java.util.List<LegacyArtifactPath> getLegacyArtifactPaths()

    /**
     * Method getManagedRepositories.
     * 
     * @return List
     */
    public java.util.List<ManagedRepositoryConfiguration> getManagedRepositories()
    {
        if ( this.managedRepositories == null )
        {
            this.managedRepositories = new java.util.ArrayList<ManagedRepositoryConfiguration>();
        }

        return this.managedRepositories;
    } //-- java.util.List<ManagedRepositoryConfiguration> getManagedRepositories()

    /**
     * Get the type of the metadata storage. Allowed values: jcr,
     * file, cassandra.
     * 
     * @return String
     */
    public String getMetadataStore()
    {
        return this.metadataStore;
    } //-- String getMetadataStore()

    /**
     * Get the modelEncoding field.
     * 
     * @return String
     */
    public String getModelEncoding()
    {
        return this.modelEncoding;
    } //-- String getModelEncoding()

    /**
     * Get the NetworkConfiguration .
     * 
     * @return NetworkConfiguration
     */
    public NetworkConfiguration getNetworkConfiguration()
    {
        return this.networkConfiguration;
    } //-- NetworkConfiguration getNetworkConfiguration()

    /**
     * Method getNetworkProxies.
     * 
     * @return List
     */
    public java.util.List<NetworkProxyConfiguration> getNetworkProxies()
    {
        if ( this.networkProxies == null )
        {
            this.networkProxies = new java.util.ArrayList<NetworkProxyConfiguration>();
        }

        return this.networkProxies;
    } //-- java.util.List<NetworkProxyConfiguration> getNetworkProxies()

    /**
     * Get the organisation info.
     * 
     * @return OrganisationInformation
     */
    public OrganisationInformation getOrganisationInfo()
    {
        return this.organisationInfo;
    } //-- OrganisationInformation getOrganisationInfo()

    /**
     * Method getProxyConnectorRuleConfigurations.
     * 
     * @return List
     */
    public java.util.List<ProxyConnectorRuleConfiguration> getProxyConnectorRuleConfigurations()
    {
        if ( this.proxyConnectorRuleConfigurations == null )
        {
            this.proxyConnectorRuleConfigurations = new java.util.ArrayList<ProxyConnectorRuleConfiguration>();
        }

        return this.proxyConnectorRuleConfigurations;
    } //-- java.util.List<ProxyConnectorRuleConfiguration> getProxyConnectorRuleConfigurations()

    /**
     * Method getProxyConnectors.
     * 
     * @return List
     */
    public java.util.List<ProxyConnectorConfiguration> getProxyConnectors()
    {
        if ( this.proxyConnectors == null )
        {
            this.proxyConnectors = new java.util.ArrayList<ProxyConnectorConfiguration>();
        }

        return this.proxyConnectors;
    } //-- java.util.List<ProxyConnectorConfiguration> getProxyConnectors()

    /**
     * Get the RedbackRuntimeConfiguration.
     * 
     * @return RedbackRuntimeConfiguration
     */
    public RedbackRuntimeConfiguration getRedbackRuntimeConfiguration()
    {
        return this.redbackRuntimeConfiguration;
    } //-- RedbackRuntimeConfiguration getRedbackRuntimeConfiguration()

    /**
     * Method getRemoteRepositories.
     * 
     * @return List
     */
    public java.util.List<RemoteRepositoryConfiguration> getRemoteRepositories()
    {
        if ( this.remoteRepositories == null )
        {
            this.remoteRepositories = new java.util.ArrayList<RemoteRepositoryConfiguration>();
        }

        return this.remoteRepositories;
    } //-- java.util.List<RemoteRepositoryConfiguration> getRemoteRepositories()

    /**
     * Method getRepositoryGroups.
     * 
     * @return List
     */
    public java.util.List<RepositoryGroupConfiguration> getRepositoryGroups()
    {
        if ( this.repositoryGroups == null )
        {
            this.repositoryGroups = new java.util.ArrayList<RepositoryGroupConfiguration>();
        }

        return this.repositoryGroups;
    } //-- java.util.List<RepositoryGroupConfiguration> getRepositoryGroups()

    /**
     * Get the repository scanning configuration.
     * 
     * @return RepositoryScanningConfiguration
     */
    public RepositoryScanningConfiguration getRepositoryScanning()
    {
        return this.repositoryScanning;
    } //-- RepositoryScanningConfiguration getRepositoryScanning()

    /**
     * Get this is the version of the configuration format.
     * 
     * @return String
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion()

    /**
     * Get the webapp configuration.
     * 
     * @return WebappConfiguration
     */
    public WebappConfiguration getWebapp()
    {
        return this.webapp;
    } //-- WebappConfiguration getWebapp()

    /**
     * Method removeLegacyArtifactPath.
     * 
     * @param legacyArtifactPath
     */
    public void removeLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
    {
        getLegacyArtifactPaths().remove( legacyArtifactPath );
    } //-- void removeLegacyArtifactPath( LegacyArtifactPath )

    /**
     * Method removeManagedRepository.
     * 
     * @param managedRepositoryConfiguration
     */
    public void removeManagedRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration )
    {
        getManagedRepositories().remove( managedRepositoryConfiguration );
    } //-- void removeManagedRepository( ManagedRepositoryConfiguration )

    /**
     * Method removeNetworkProxy.
     * 
     * @param networkProxyConfiguration
     */
    public void removeNetworkProxy( NetworkProxyConfiguration networkProxyConfiguration )
    {
        getNetworkProxies().remove( networkProxyConfiguration );
    } //-- void removeNetworkProxy( NetworkProxyConfiguration )

    /**
     * Method removeProxyConnector.
     * 
     * @param proxyConnectorConfiguration
     */
    public void removeProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        getProxyConnectors().remove( proxyConnectorConfiguration );
    } //-- void removeProxyConnector( ProxyConnectorConfiguration )

    /**
     * Method removeProxyConnectorRuleConfiguration.
     * 
     * @param proxyConnectorRuleConfiguration
     */
    public void removeProxyConnectorRuleConfiguration( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration )
    {
        getProxyConnectorRuleConfigurations().remove( proxyConnectorRuleConfiguration );
    } //-- void removeProxyConnectorRuleConfiguration( ProxyConnectorRuleConfiguration )

    /**
     * Method removeRemoteRepository.
     * 
     * @param remoteRepositoryConfiguration
     */
    public void removeRemoteRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration )
    {
        getRemoteRepositories().remove( remoteRepositoryConfiguration );
    } //-- void removeRemoteRepository( RemoteRepositoryConfiguration )

    /**
     * Method removeRepositoryGroup.
     * 
     * @param repositoryGroupConfiguration
     */
    public void removeRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration )
    {
        getRepositoryGroups().remove( repositoryGroupConfiguration );
    } //-- void removeRepositoryGroup( RepositoryGroupConfiguration )

    /**
     * Set archiva default settings.
     * 
     * @param archivaDefaultConfiguration
     */
    public void setArchivaDefaultConfiguration( ArchivaDefaultConfiguration archivaDefaultConfiguration )
    {
        this.archivaDefaultConfiguration = archivaDefaultConfiguration;
    } //-- void setArchivaDefaultConfiguration( ArchivaDefaultConfiguration )

    /**
     * Set the ArchivaRuntimeConfiguration.
     * 
     * @param archivaRuntimeConfiguration
     */
    public void setArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        this.archivaRuntimeConfiguration = archivaRuntimeConfiguration;
    } //-- void setArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration )

    /**
     * Set the list of custom legacy path to artifact.
     * 
     * @param legacyArtifactPaths
     */
    public void setLegacyArtifactPaths( java.util.List<LegacyArtifactPath> legacyArtifactPaths )
    {
        this.legacyArtifactPaths = legacyArtifactPaths;
    } //-- void setLegacyArtifactPaths( java.util.List )

    /**
     * Set the list of repositories that this archiva instance
     * uses.
     * 
     * @param managedRepositories
     */
    public void setManagedRepositories( java.util.List<ManagedRepositoryConfiguration> managedRepositories )
    {
        this.managedRepositories = managedRepositories;
    } //-- void setManagedRepositories( java.util.List )

    /**
     * Set the type of the metadata storage. Allowed values: jcr,
     * file, cassandra.
     * 
     * @param metadataStore
     */
    public void setMetadataStore( String metadataStore )
    {
        this.metadataStore = metadataStore;
    } //-- void setMetadataStore( String )

    /**
     * Set the modelEncoding field.
     * 
     * @param modelEncoding
     */
    public void setModelEncoding( String modelEncoding )
    {
        this.modelEncoding = modelEncoding;
    } //-- void setModelEncoding( String )

    /**
     * Set the NetworkConfiguration .
     * 
     * @param networkConfiguration
     */
    public void setNetworkConfiguration( NetworkConfiguration networkConfiguration )
    {
        this.networkConfiguration = networkConfiguration;
    } //-- void setNetworkConfiguration( NetworkConfiguration )

    /**
     * Set the list of network proxies to use for outgoing
     * requests.
     * 
     * @param networkProxies
     */
    public void setNetworkProxies( java.util.List<NetworkProxyConfiguration> networkProxies )
    {
        this.networkProxies = networkProxies;
    } //-- void setNetworkProxies( java.util.List )

    /**
     * Set the organisation info.
     * 
     * @param organisationInfo
     */
    public void setOrganisationInfo( OrganisationInformation organisationInfo )
    {
        this.organisationInfo = organisationInfo;
    } //-- void setOrganisationInfo( OrganisationInformation )

    /**
     * Set the list of ProxyConnectorRuleConfigurations.
     * 
     * @param proxyConnectorRuleConfigurations
     */
    public void setProxyConnectorRuleConfigurations( java.util.List<ProxyConnectorRuleConfiguration> proxyConnectorRuleConfigurations )
    {
        this.proxyConnectorRuleConfigurations = proxyConnectorRuleConfigurations;
    } //-- void setProxyConnectorRuleConfigurations( java.util.List )

    /**
     * Set the list of proxy connectors for this archiva instance.
     * 
     * @param proxyConnectors
     */
    public void setProxyConnectors( java.util.List<ProxyConnectorConfiguration> proxyConnectors )
    {
        this.proxyConnectors = proxyConnectors;
    } //-- void setProxyConnectors( java.util.List )

    /**
     * Set the RedbackRuntimeConfiguration.
     * 
     * @param redbackRuntimeConfiguration
     */
    public void setRedbackRuntimeConfiguration( RedbackRuntimeConfiguration redbackRuntimeConfiguration )
    {
        this.redbackRuntimeConfiguration = redbackRuntimeConfiguration;
    } //-- void setRedbackRuntimeConfiguration( RedbackRuntimeConfiguration )

    /**
     * Set the list of repositories that this archiva can retrieve
     * from or publish to.
     * 
     * @param remoteRepositories
     */
    public void setRemoteRepositories( java.util.List<RemoteRepositoryConfiguration> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    } //-- void setRemoteRepositories( java.util.List )

    /**
     * Set the list of repository groups.
     * 
     * @param repositoryGroups
     */
    public void setRepositoryGroups( java.util.List<RepositoryGroupConfiguration> repositoryGroups )
    {
        this.repositoryGroups = repositoryGroups;
    } //-- void setRepositoryGroups( java.util.List )

    /**
     * Set the repository scanning configuration.
     * 
     * @param repositoryScanning
     */
    public void setRepositoryScanning( RepositoryScanningConfiguration repositoryScanning )
    {
        this.repositoryScanning = repositoryScanning;
    } //-- void setRepositoryScanning( RepositoryScanningConfiguration )

    /**
     * Set this is the version of the configuration format.
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion( String )

    /**
     * Set the webapp configuration.
     * 
     * @param webapp
     */
    public void setWebapp( WebappConfiguration webapp )
    {
        this.webapp = webapp;
    } //-- void setWebapp( WebappConfiguration )

    
    private java.util.Map<String, java.util.List<String>> repositoryToGroupMap; 
    
    public java.util.Map<String, java.util.List<String>> getRepositoryToGroupMap()
    {
        if ( repositoryGroups != null )
        {
            java.util.Map<String, java.util.List<String>> map = new java.util.HashMap<String, java.util.List<String>>();
            
            for ( RepositoryGroupConfiguration group : (java.util.List<RepositoryGroupConfiguration>) repositoryGroups )
            {
                for ( String repositoryId : (java.util.List<String>) group.getRepositories() )
                {
                    java.util.List<String> groups = map.get( repositoryId );
                    if ( groups == null )
                    {
                        groups = new java.util.ArrayList<String>();
                        map.put( repositoryId, groups );
                    }
                    groups.add( group.getId() );
                }
            }
            
            repositoryToGroupMap = map;
        }
        return repositoryToGroupMap;
    }
    
    public java.util.Map<String, RepositoryGroupConfiguration> getRepositoryGroupsAsMap()
    {
        java.util.Map<String, RepositoryGroupConfiguration> map = new java.util.HashMap<String, RepositoryGroupConfiguration>();
        if ( repositoryGroups != null )
        {
            for ( RepositoryGroupConfiguration group : (java.util.List<RepositoryGroupConfiguration>) repositoryGroups )
            {
                map.put( group.getId(), group );
            }
        }
        return map;
    }
    
    public RepositoryGroupConfiguration findRepositoryGroupById( String id )
    {
        if ( repositoryGroups != null )
        {
            for ( RepositoryGroupConfiguration group : (java.util.List<RepositoryGroupConfiguration>) repositoryGroups )
            {
                if ( group.getId().equals( id ) )
                {
                    return group;
                }
            }
        }
        return null;
    }

    private java.util.Map<String, java.util.List<String>> groupToRepositoryMap;

    public java.util.Map<String, java.util.List<String>> getGroupToRepositoryMap()
    {
        if ( repositoryGroups != null && managedRepositories != null )
        {
            java.util.Map<String, java.util.List<String>> map = new java.util.HashMap<String, java.util.List<String>>();
            
            for ( ManagedRepositoryConfiguration repo : (java.util.List<ManagedRepositoryConfiguration>) managedRepositories )
            {
                for ( RepositoryGroupConfiguration group : (java.util.List<RepositoryGroupConfiguration>) repositoryGroups )
                {
                    if ( !group.getRepositories().contains( repo.getId() ) )
                    {
                        String groupId = group.getId();
                        java.util.List<String> repos = map.get( groupId );
                        if ( repos == null )
                        {
                            repos = new java.util.ArrayList<String>();
                            map.put( groupId, repos );
                        }
                        repos.add( repo.getId() );
                    }
                }
            }
            groupToRepositoryMap = map;
        }
        return groupToRepositoryMap;
    }
          
    
    public java.util.Map<String, NetworkProxyConfiguration> getNetworkProxiesAsMap()
    {
        java.util.Map<String, NetworkProxyConfiguration> map = new java.util.HashMap<String, NetworkProxyConfiguration>();
        if ( networkProxies != null )
        {
            for ( java.util.Iterator<NetworkProxyConfiguration> i = networkProxies.iterator(); i.hasNext(); )
            {
                NetworkProxyConfiguration proxy = i.next();
                map.put( proxy.getId(), proxy );
            }
        }
        return map;
    }

    public java.util.Map<String, java.util.List<ProxyConnectorConfiguration>> getProxyConnectorAsMap()
    {
        java.util.Map<String, java.util.List<ProxyConnectorConfiguration>> proxyConnectorMap =
            new java.util.HashMap<String, java.util.List<ProxyConnectorConfiguration>>();

        if( proxyConnectors != null )
        {
            java.util.Iterator<ProxyConnectorConfiguration> it = proxyConnectors.iterator();
            while ( it.hasNext() )
            {
                ProxyConnectorConfiguration proxyConfig = it.next();
                String key = proxyConfig.getSourceRepoId();

                java.util.List<ProxyConnectorConfiguration> connectors = proxyConnectorMap.get( key );
                if ( connectors == null )
                {
                    connectors = new java.util.ArrayList<ProxyConnectorConfiguration>();
                    proxyConnectorMap.put( key, connectors );
                }

                connectors.add( proxyConfig );
                java.util.Collections.sort( connectors,
                    org.apache.archiva.configuration.functors.ProxyConnectorConfigurationOrderComparator.getInstance() );
            }
        }

        return proxyConnectorMap;
    }

    public java.util.Map<String, RemoteRepositoryConfiguration> getRemoteRepositoriesAsMap()
    {
        java.util.Map<String, RemoteRepositoryConfiguration> map = new java.util.HashMap<String, RemoteRepositoryConfiguration>();
        if ( remoteRepositories != null )
        {
            for ( java.util.Iterator<RemoteRepositoryConfiguration> i = remoteRepositories.iterator(); i.hasNext(); )
            {
                RemoteRepositoryConfiguration repo = i.next();
                map.put( repo.getId(), repo );
            }
        }
        return map;
    }

    public RemoteRepositoryConfiguration findRemoteRepositoryById( String id )
    {
        if ( remoteRepositories != null )
        {
            for ( java.util.Iterator<RemoteRepositoryConfiguration> i = remoteRepositories.iterator(); i.hasNext(); )
            {
                RemoteRepositoryConfiguration repo = i.next();
                if ( repo.getId().equals( id ) )
                {
                    return repo;
                }
            }
        }
        return null;
    }

    public java.util.Map<String, ManagedRepositoryConfiguration> getManagedRepositoriesAsMap()
    {
        java.util.Map<String, ManagedRepositoryConfiguration> map = new java.util.HashMap<String, ManagedRepositoryConfiguration>();
        if ( managedRepositories != null )
        {
            for ( java.util.Iterator<ManagedRepositoryConfiguration> i = managedRepositories.iterator(); i.hasNext(); )
            {
                ManagedRepositoryConfiguration repo = i.next();
                map.put( repo.getId(), repo );
            }
        }
        return map;
    }

    public ManagedRepositoryConfiguration findManagedRepositoryById( String id )
    {
        if ( managedRepositories != null )
        {
            for ( java.util.Iterator<ManagedRepositoryConfiguration> i = managedRepositories.iterator(); i.hasNext(); )
            {
                ManagedRepositoryConfiguration repo = i.next();
                if ( repo.getId().equals( id ) )
                {
                    return repo;
                }
            }
        }
        return null;
    }
          
}
