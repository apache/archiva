package org.apache.archiva.dependency.tree.maven2;
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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.maven2.model.TreeEntry;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service("dependencyTreeBuilder#maven3")
public class Maven3DependencyTreeBuilder
    implements DependencyTreeBuilder
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    @Named( "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    private ArtifactFactory factory;


    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        factory = plexusSisuBridge.lookup( ArtifactFactory.class, "default" );

        DefaultModelBuilderFactory defaultModelBuilderFactory = new DefaultModelBuilderFactory();
        defaultModelBuilderFactory.newInstance();
    }



    public void buildDependencyTree( List<String> repositoryIds, String groupId, String artifactId, String version,
                                     DependencyVisitor dependencyVisitor )
        throws DependencyTreeBuilderException
    {
        Artifact projectArtifact = factory.createProjectArtifact( groupId, artifactId, version );
        ManagedRepository repository = null;
        try
        {
            repository = findArtifactInRepositories( repositoryIds, projectArtifact );
        }
        catch ( RepositoryAdminException e )
        {
            // FIXME better exception
            throw new DependencyTreeBuilderException( "Cannot build project dependency tree " + e.getMessage(), e );
        }

        if ( repository == null )
        {
            // metadata could not be resolved
            return;
        }

        List<RemoteRepository> remoteRepositories = new ArrayList<>();
        Map<String, NetworkProxy> networkProxies = new HashMap<>();

        try
        {
            // MRM-1411
            // TODO: this is a workaround for a lack of proxy capability in the resolvers - replace when it can all be
            //       handled there. It doesn't cache anything locally!

            Map<String, List<ProxyConnector>> proxyConnectorsMap = proxyConnectorAdmin.getProxyConnectorAsMap();
            List<ProxyConnector> proxyConnectors = proxyConnectorsMap.get( repository.getId() );
            if ( proxyConnectors != null )
            {
                for ( ProxyConnector proxyConnector : proxyConnectors )
                {
                    remoteRepositories.add(
                        remoteRepositoryAdmin.getRemoteRepository( proxyConnector.getTargetRepoId() ) );

                    NetworkProxy networkProxyConfig = networkProxyAdmin.getNetworkProxy( proxyConnector.getProxyId() );

                    if ( networkProxyConfig != null )
                    {
                        // key/value: remote repo ID/proxy info
                        networkProxies.put( proxyConnector.getTargetRepoId(), networkProxyConfig );
                    }
                }
            }
        }
        catch ( RepositoryAdminException e )
        {
            throw new DependencyTreeBuilderException( e.getMessage(), e );
        }

        // FIXME take care of relative path
        ResolveRequest resolveRequest = new ResolveRequest();
        resolveRequest.dependencyVisitor = dependencyVisitor;
        resolveRequest.localRepoDir = repository.getLocation();
        resolveRequest.groupId = groupId;
        resolveRequest.artifactId = artifactId;
        resolveRequest.version = version;
        resolveRequest.remoteRepositories = remoteRepositories;
        resolveRequest.networkProxies = networkProxies;
        resolve( resolveRequest );
    }


    @Override
    public List<TreeEntry> buildDependencyTree( List<String> repositoryIds, String groupId, String artifactId,
                                                String version )
        throws DependencyTreeBuilderException
    {

        List<TreeEntry> treeEntries = new ArrayList<>();
        TreeDependencyNodeVisitor treeDependencyNodeVisitor = new TreeDependencyNodeVisitor( treeEntries );

        buildDependencyTree( repositoryIds, groupId, artifactId, version, treeDependencyNodeVisitor );

        log.debug( "treeEntrie: {}", treeEntries );
        return treeEntries;
    }

    private static class ResolveRequest
    {
        String localRepoDir, groupId, artifactId, version;

        DependencyVisitor dependencyVisitor;

        List<RemoteRepository> remoteRepositories;

        Map<String, NetworkProxy> networkProxies;

    }


    private void resolve( ResolveRequest resolveRequest )
    {

        RepositorySystem system = newRepositorySystem();

        RepositorySystemSession session = newRepositorySystemSession( system, resolveRequest.localRepoDir );

        org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
            resolveRequest.groupId + ":" + resolveRequest.artifactId + ":" + resolveRequest.version );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, "" ) );

        // add remote repositories
        for ( RemoteRepository remoteRepository : resolveRequest.remoteRepositories )
        {
            org.eclipse.aether.repository.RemoteRepository repo = new org.eclipse.aether.repository.RemoteRepository.Builder( remoteRepository.getId( ), "default", remoteRepository.getUrl( ) ).build( );
            collectRequest.addRepository(repo);
        }
        collectRequest.setRequestContext( "project" );

        //collectRequest.addRepository( repo );

        try
        {
            CollectResult collectResult = system.collectDependencies( session, collectRequest );
            collectResult.getRoot().accept( resolveRequest.dependencyVisitor );
            log.debug( "test" );
        }
        catch ( DependencyCollectionException e )
        {
            log.error( e.getMessage(), e );
        }



    }

    private RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator( );
        locator.addService( RepositoryConnectorFactory.class,
                            ArchivaRepositoryConnectorFactory.class );// FileRepositoryConnectorFactory.class );
        locator.addService( VersionResolver.class, DefaultVersionResolver.class );
        locator.addService( VersionRangeResolver.class, DefaultVersionRangeResolver.class );
        locator.addService( ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class );
        //locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );
        //locator.setServices( WagonProvider.class,  );

        return locator.getService( RepositorySystem.class );
    }

    private RepositorySystemSession newRepositorySystemSession( RepositorySystem system, String localRepoDir )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession( );

        LocalRepository repo = new LocalRepository( localRepoDir );

        DependencySelector depFilter = new AndDependencySelector( new ExclusionDependencySelector() );
        session.setDependencySelector( depFilter );
        SimpleLocalRepositoryManagerFactory repFactory = new SimpleLocalRepositoryManagerFactory( );
        try
        {
            LocalRepositoryManager manager = repFactory.newInstance( session, repo );
            session.setLocalRepositoryManager(manager);
        }
        catch ( NoLocalRepositoryManagerException e )
        {
            e.printStackTrace( );
        }

        return session;
    }


    private ManagedRepository findArtifactInRepositories( List<String> repositoryIds, Artifact projectArtifact )
        throws RepositoryAdminException
    {
        for ( String repoId : repositoryIds )
        {
            ManagedRepository managedRepository = managedRepositoryAdmin.getManagedRepository( repoId );

            Path repoDir = Paths.get( managedRepository.getLocation() );
            Path file = pathTranslator.toFile( repoDir, projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                                               projectArtifact.getBaseVersion(),
                                               projectArtifact.getArtifactId() + "-" + projectArtifact.getVersion()
                                                   + ".pom" );

            if ( Files.exists(file) )
            {
                return managedRepository;
            }
            // try with snapshot version
            if ( StringUtils.endsWith( projectArtifact.getBaseVersion(), VersionUtil.SNAPSHOT ) )
            {
                Path metadataFile = file.getParent().resolve( MetadataTools.MAVEN_METADATA );
                if ( Files.exists(metadataFile) )
                {
                    try
                    {
                        ArchivaRepositoryMetadata archivaRepositoryMetadata = MavenMetadataReader.read( metadataFile);
                        int buildNumber = archivaRepositoryMetadata.getSnapshotVersion().getBuildNumber();
                        String timeStamp = archivaRepositoryMetadata.getSnapshotVersion().getTimestamp();
                        // rebuild file name with timestamped version and build number
                        String timeStampFileName =
                            new StringBuilder( projectArtifact.getArtifactId() ).append( '-' ).append(
                                StringUtils.remove( projectArtifact.getBaseVersion(),
                                                    "-" + VersionUtil.SNAPSHOT ) ).append( '-' ).append(
                                timeStamp ).append( '-' ).append( Integer.toString( buildNumber ) ).append(
                                ".pom" ).toString();
                        Path timeStampFile = file.getParent().resolve( timeStampFileName );
                        log.debug( "try to find timestamped snapshot version file: {}", timeStampFile);
                        if ( Files.exists(timeStampFile) )
                        {
                            return managedRepository;
                        }
                    }
                    catch ( XMLException e )
                    {
                        log.warn( "skip fail to find timestamped snapshot pom: {}", e.getMessage() );
                    }
                }
            }
        }
        return null;
    }

}
