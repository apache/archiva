package org.apache.archiva.repository.maven.dependency.tree;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.maven2.model.TreeEntry;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.maven.MavenSystemManager;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
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
    private Logger log = LoggerFactory.getLogger( Maven3DependencyTreeBuilder.class );

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    private MavenRepositorySystem mavenRepositorySystem;

    @Inject
    @Named( "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    @Named("metadataReader#maven")
    private MavenMetadataReader metadataReader;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    MavenSystemManager mavenSystemManager;


    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        mavenRepositorySystem = plexusSisuBridge.lookup(MavenRepositorySystem.class);
    }



    public void buildDependencyTree( List<String> repositoryIds, String groupId, String artifactId, String version,
                                     DependencyVisitor dependencyVisitor )
        throws DependencyTreeBuilderException
    {

        Artifact projectArtifact = mavenRepositorySystem.createProjectArtifact(groupId, artifactId, version);
        ManagedRepository repository = findArtifactInRepositories( repositoryIds, projectArtifact );

        if ( repository == null )
        {
            // metadata could not be resolved
            log.info("Did not find repository with artifact {}/{}/{}", groupId, artifactId, version);
            return;
        }

        List<org.apache.archiva.repository.RemoteRepository> remoteRepositories = new ArrayList<>();
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
                        repositoryRegistry.getRemoteRepository( proxyConnector.getTargetRepoId() ) );

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
        resolveRequest.localRepoDir = repository.getContent().getRepoRoot();
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

        log.debug( "treeEntries: {}", treeEntries );
        return treeEntries;
    }

    private static class ResolveRequest
    {
        String localRepoDir, groupId, artifactId, version;

        DependencyVisitor dependencyVisitor;

        List<org.apache.archiva.repository.RemoteRepository> remoteRepositories;

        Map<String, NetworkProxy> networkProxies;

    }


    private void resolve( ResolveRequest resolveRequest )
    {

        RepositorySystem system = mavenSystemManager.getRepositorySystem();
        RepositorySystemSession session = MavenSystemManager.newRepositorySystemSession( resolveRequest.localRepoDir );

        org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
            resolveRequest.groupId + ":" + resolveRequest.artifactId + ":" + resolveRequest.version );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, "" ) );

        // add remote repositories
        for ( RemoteRepository remoteRepository : resolveRequest.remoteRepositories )
        {
            org.eclipse.aether.repository.RemoteRepository repo = new org.eclipse.aether.repository.RemoteRepository.Builder( remoteRepository.getId( ), "default", remoteRepository.getLocation( ).toString() ).build( );
            collectRequest.addRepository(repo);
        }
        collectRequest.setRequestContext( "project" );

        //collectRequest.addRepository( repo );

        try
        {
            CollectResult collectResult = system.collectDependencies( session, collectRequest );
            collectResult.getRoot().accept( resolveRequest.dependencyVisitor );
            log.debug("Collected dependency results for resolve");
        }
        catch ( DependencyCollectionException e )
        {
            log.error( "Error while collecting dependencies (resolve): {}", e.getMessage(), e );
        }



    }

    private ManagedRepository findArtifactInRepositories( List<String> repositoryIds, Artifact projectArtifact ) {
        for ( String repoId : repositoryIds )
        {
            ManagedRepository managedRepo = repositoryRegistry.getManagedRepository(repoId);
            StorageAsset repoDir = managedRepo.getAsset("");

            StorageAsset file = pathTranslator.toFile( repoDir, projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                                               projectArtifact.getBaseVersion(),
                                               projectArtifact.getArtifactId() + "-" + projectArtifact.getVersion()
                                                   + ".pom" );

            if ( file.exists() )
            {
                return managedRepo;
            }
            // try with snapshot version
            if ( StringUtils.endsWith( projectArtifact.getBaseVersion(), VersionUtil.SNAPSHOT ) )
            {
                StorageAsset metadataFile = file.getParent().resolve( MetadataTools.MAVEN_METADATA );
                if ( metadataFile.exists() )
                {
                    try
                    {
                        ArchivaRepositoryMetadata archivaRepositoryMetadata = metadataReader.read( metadataFile);
                        int buildNumber = archivaRepositoryMetadata.getSnapshotVersion().getBuildNumber();
                        String timeStamp = archivaRepositoryMetadata.getSnapshotVersion().getTimestamp();
                        // rebuild file name with timestamped version and build number
                        String timeStampFileName =
                            new StringBuilder( projectArtifact.getArtifactId() ).append( '-' ).append(
                                StringUtils.remove( projectArtifact.getBaseVersion(),
                                                    "-" + VersionUtil.SNAPSHOT ) ).append( '-' ).append(
                                timeStamp ).append( '-' ).append( Integer.toString( buildNumber ) ).append(
                                ".pom" ).toString();
                        StorageAsset timeStampFile = file.getParent().resolve( timeStampFileName );
                        log.debug( "try to find timestamped snapshot version file: {}", timeStampFile);
                        if ( timeStampFile.exists() )
                        {
                            return managedRepo;
                        }
                    }
                    catch ( RepositoryMetadataException e )
                    {
                        log.warn( "skip fail to find timestamped snapshot pom: {}", e.getMessage() );
                    }
                }
            }
        }
        return null;
    }

}
