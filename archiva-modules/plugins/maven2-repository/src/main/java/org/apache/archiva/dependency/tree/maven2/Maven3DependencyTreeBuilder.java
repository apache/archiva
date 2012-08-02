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
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.RepositoryModelResolver;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.artifact.ArtifactType;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.DefaultRequestTrace;
import org.sonatype.aether.util.artifact.ArtifacIdUtils;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.version.VersionConstraint;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
@Service( "dependencyTreeBuilder#maven3" )
public class Maven3DependencyTreeBuilder
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    @Named( value = "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    private WagonFactory wagonFactory;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    private ArtifactFactory factory;

    private ModelBuilder builder;


    private RepositorySystem repoSystem;

    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        factory = plexusSisuBridge.lookup( ArtifactFactory.class, "default" );

        repoSystem = plexusSisuBridge.lookup( RepositorySystem.class );
        DefaultModelBuilderFactory defaultModelBuilderFactory = new DefaultModelBuilderFactory();
        builder = defaultModelBuilderFactory.newInstance();
    }

    public DependencyResolutionResult buildDependencyTree( List<String> repositoryIds, String groupId,
                                                           String artifactId, String version,
                                                           DependencyVisitor dependencyVisitor )
        throws Exception
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
            throw new Exception( "Cannot build project dependency tree " + e.getMessage(), e );
        }

        if ( repository == null )
        {
            // metadata could not be resolved
            return new DefaultDependencyResolutionResult();
        }

        // MRM-1411
        // TODO: this is a workaround for a lack of proxy capability in the resolvers - replace when it can all be
        //       handled there. It doesn't cache anything locally!
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        Map<String, NetworkProxy> networkProxies = new HashMap<String, NetworkProxy>();

        Map<String, List<ProxyConnector>> proxyConnectorsMap = proxyConnectorAdmin.getProxyConnectorAsMap();
        List<ProxyConnector> proxyConnectors = proxyConnectorsMap.get( repository.getId() );
        if ( proxyConnectors != null )
        {
            for ( ProxyConnector proxyConnector : proxyConnectors )
            {
                remoteRepositories.add( remoteRepositoryAdmin.getRemoteRepository( proxyConnector.getTargetRepoId() ) );

                NetworkProxy networkProxyConfig = networkProxyAdmin.getNetworkProxy( proxyConnector.getProxyId() );

                if ( networkProxyConfig != null )
                {
                    // key/value: remote repo ID/proxy info
                    networkProxies.put( proxyConnector.getTargetRepoId(), networkProxyConfig );
                }
            }
        }

        Model model = buildProject(
            new RepositoryModelResolver( repository, pathTranslator, wagonFactory, remoteRepositories, networkProxies,
                                         repository ), groupId, artifactId, version );

        MavenProject project = new MavenProject( model );

        DefaultRepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession();

        // FIXME take care of relative path for getLocation
        repositorySystemSession.setLocalRepositoryManager(
            new SimpleLocalRepositoryManager( new File( repository.getLocation() ) ) );

        DefaultProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest();

        project.setProjectBuildingRequest( projectBuildingRequest );

        projectBuildingRequest.setRepositorySession( repositorySystemSession );

        DefaultDependencyResolutionRequest request =
            new DefaultDependencyResolutionRequest( project, projectBuildingRequest.getRepositorySession() );

        //DependencyFilter dependencyFilter
        //request.setResolutionFilter(  )

        //DependencyResolutionResult result = projectDependenciesResolver.resolve( request );

        //DependencyNode dependencyNode = buildDependencyNode( null, result.getDependencyGraph(), projectArtifact, null );
        /*DependencyNode dependencyNode = dependencyGraphBuilder.buildDependencyGraph( project, new ArtifactFilter()
        {
            public boolean include( Artifact artifact )
            {
                return true;
            }
        } );*/

        DependencyResolutionResult resolutionResult = resolve( request );

        log.debug( "dependency graph build" );

        // FIXME take care of relative path
        test( repository.getLocation(), groupId, artifactId, version, dependencyVisitor );

        return resolutionResult;
    }

    private DependencyResolutionResult resolve( DefaultDependencyResolutionRequest request )
        throws DependencyResolutionException
    {

        RequestTrace trace = DefaultRequestTrace.newChild( null, request );

        DefaultDependencyResolutionResult result = new DefaultDependencyResolutionResult();

        MavenProject project = request.getMavenProject();
        RepositorySystemSession session = request.getRepositorySession();
        DependencyFilter filter = request.getResolutionFilter();

        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        CollectRequest collect = new CollectRequest();
        collect.setRequestContext( "project" );
        collect.setRepositories( project.getRemoteProjectRepositories() );

        if ( project.getDependencyArtifacts() == null )
        {
            for ( org.apache.maven.model.Dependency dependency : project.getDependencies() )
            {
                if ( StringUtils.isEmpty( dependency.getGroupId() ) || StringUtils.isEmpty( dependency.getArtifactId() )
                    || StringUtils.isEmpty( dependency.getVersion() ) )
                {
                    // guard against case where best-effort resolution for invalid models is requested
                    continue;
                }
                collect.addDependency( RepositoryUtils.toDependency( dependency, stereotypes ) );
            }
        }
        else
        {
            Map<String, org.apache.maven.model.Dependency> dependencies =
                new HashMap<String, org.apache.maven.model.Dependency>();
            for ( org.apache.maven.model.Dependency dependency : project.getDependencies() )
            {
                String classifier = dependency.getClassifier();
                if ( classifier == null )
                {
                    ArtifactType type = stereotypes.get( dependency.getType() );
                    if ( type != null )
                    {
                        classifier = type.getClassifier();
                    }
                }
                String key = ArtifacIdUtils.toVersionlessId( dependency.getGroupId(), dependency.getArtifactId(),
                                                             dependency.getType(), classifier );
                dependencies.put( key, dependency );
            }
            for ( Artifact artifact : project.getDependencyArtifacts() )
            {
                String key = artifact.getDependencyConflictId();
                org.apache.maven.model.Dependency dependency = dependencies.get( key );
                Collection<Exclusion> exclusions = dependency != null ? dependency.getExclusions() : null;
                org.sonatype.aether.graph.Dependency dep = RepositoryUtils.toDependency( artifact, exclusions );
                if ( !JavaScopes.SYSTEM.equals( dep.getScope() ) && dep.getArtifact().getFile() != null )
                {
                    // enable re-resolution
                    org.sonatype.aether.artifact.Artifact art = dep.getArtifact();
                    art = art.setFile( null ).setVersion( art.getBaseVersion() );
                    dep = dep.setArtifact( art );
                }
                collect.addDependency( dep );
            }
        }

        DependencyManagement depMngt = project.getDependencyManagement();
        if ( depMngt != null )
        {
            for ( org.apache.maven.model.Dependency dependency : depMngt.getDependencies() )
            {
                collect.addManagedDependency( RepositoryUtils.toDependency( dependency, stereotypes ) );
            }
        }

        collect.setRoot( new org.sonatype.aether.graph.Dependency(
            new org.sonatype.aether.util.artifact.DefaultArtifact( project.getGroupId(), project.getArtifactId(), null,
                                                                   project.getVersion() ), "compile" ) );

        DependencyRequest depRequest = new DependencyRequest( collect, filter );
        depRequest.setTrace( trace );

        org.sonatype.aether.graph.DependencyNode node;
        try
        {
            collect.setTrace( DefaultRequestTrace.newChild( trace, depRequest ) );
            node = repoSystem.collectDependencies( session, collect ).getRoot();
            result.setDependencyGraph( node );
        }
        catch ( DependencyCollectionException e )
        {
            result.setDependencyGraph( e.getResult().getRoot() );
            result.setCollectionErrors( e.getResult().getExceptions() );

            throw new DependencyResolutionException( result,
                                                     "Could not resolve dependencies for project " + project.getId()
                                                         + ": " + e.getMessage(), e );
        }

        depRequest.setRoot( node );

        return result;
    }

    private void test( String localRepoDir, String groupId, String artifactId, String version,
                       DependencyVisitor dependencyVisitor )
    {

        RepositorySystem system = newRepositorySystem();

        RepositorySystemSession session = newRepositorySystemSession( system, localRepoDir );

        org.sonatype.aether.artifact.Artifact artifact =
            new DefaultArtifact( groupId + ":" + artifactId + ":" + version );

        //RemoteRepository repo = Booter.newCentralRepository();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, "" ) );
        //collectRequest.addRepository( repo );

        try
        {
            CollectResult collectResult = system.collectDependencies( session, collectRequest );
            collectResult.getRoot().accept( dependencyVisitor );
            log.debug( "test" );
        }
        catch ( DependencyCollectionException e )
        {
            log.error( e.getMessage(), e );
        }


    }

    public static class MyFileRepositoryConnectorFactory
        extends FileRepositoryConnectorFactory
    {

        public MyFileRepositoryConnectorFactory()
        {

        }

        public RepositoryConnector newInstance( RepositorySystemSession session,
                                                org.sonatype.aether.repository.RemoteRepository repository )
            throws NoRepositoryConnectorException
        {

            try
            {
                return super.newInstance( session, repository );
            }
            catch ( NoRepositoryConnectorException e )
            {

            }

            return new RepositoryConnector()
            {

                private Logger log = LoggerFactory.getLogger( getClass() );

                public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                                 Collection<? extends MetadataDownload> metadataDownloads )
                {
                    log.debug( "get" );
                }

                public void put( Collection<? extends ArtifactUpload> artifactUploads,
                                 Collection<? extends MetadataUpload> metadataUploads )
                {
                    log.debug( "put" );
                }

                public void close()
                {
                    log.debug( "close" );
                }
            };
        }
    }

    public static RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService( RepositoryConnectorFactory.class,
                            MyFileRepositoryConnectorFactory.class );// FileRepositoryConnectorFactory.class );
        locator.addService( VersionResolver.class, DefaultVersionResolver.class );
        locator.addService( VersionRangeResolver.class, DefaultVersionRangeResolver.class );
        locator.addService( ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class );
        //locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );
        //locator.setServices( WagonProvider.class,  );

        return locator.getService( RepositorySystem.class );
    }

    public static RepositorySystemSession newRepositorySystemSession( RepositorySystem system, String localRepoDir )
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository( localRepoDir );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        //session.setTransferListener( new ConsoleTransferListener() );
        //session.setRepositoryListener( new ConsoleRepositoryListener() );

        return session;
    }

    private String getVersionSelectedFromRange( VersionConstraint constraint )
    {
        if ( ( constraint == null ) || ( constraint.getVersion() != null ) )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for ( org.sonatype.aether.version.VersionRange range : constraint.getRanges() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( range );
        }

        return sb.toString();
    }

    private Artifact getDependencyArtifact( Dependency dep )
    {
        org.sonatype.aether.artifact.Artifact artifact = dep.getArtifact();

        return factory.createDependencyArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                 VersionRange.createFromVersion( artifact.getVersion() ),
                                                 artifact.getExtension(), artifact.getClassifier(), dep.getScope(),
                                                 dep.isOptional() );
    }

    private Model buildProject( RepositoryModelResolver modelResolver, String groupId, String artifactId,
                                String version )
        throws ModelBuildingException, UnresolvableModelException
    {
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins( false );
        req.setModelSource( modelResolver.resolveModel( groupId, artifactId, version ) );
        req.setModelResolver( modelResolver );
        req.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        //MRM-1607. olamy this will resolve jdk profiles on the current running archiva jvm
        req.setSystemProperties( System.getProperties() );

        return builder.build( req ).getEffectiveModel();
    }

    private ManagedRepository findArtifactInRepositories( List<String> repositoryIds, Artifact projectArtifact )
        throws RepositoryAdminException
    {
        for ( String repoId : repositoryIds )
        {
            ManagedRepository managedRepository = managedRepositoryAdmin.getManagedRepository( repoId );

            File repoDir = new File( managedRepository.getLocation() );
            File file = pathTranslator.toFile( repoDir, projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                                               projectArtifact.getBaseVersion(),
                                               projectArtifact.getArtifactId() + "-" + projectArtifact.getVersion()
                                                   + ".pom" );

            if ( file.exists() )
            {
                return managedRepository;
            }
        }
        return null;
    }

    public static class DefaultDependencyResolutionResult
        implements DependencyResolutionResult
    {

        private org.sonatype.aether.graph.DependencyNode root;

        private List<Dependency> dependencies = new ArrayList<Dependency>();

        private List<Dependency> resolvedDependencies = new ArrayList<Dependency>();

        private List<Dependency> unresolvedDependencies = new ArrayList<Dependency>();

        private List<Exception> collectionErrors = new ArrayList<Exception>();

        private Map<Dependency, List<Exception>> resolutionErrors = new IdentityHashMap<Dependency, List<Exception>>();

        public org.sonatype.aether.graph.DependencyNode getDependencyGraph()
        {
            return root;
        }

        public void setDependencyGraph( org.sonatype.aether.graph.DependencyNode root )
        {
            this.root = root;
        }

        public List<Dependency> getDependencies()
        {
            return dependencies;
        }

        public List<Dependency> getResolvedDependencies()
        {
            return resolvedDependencies;
        }

        public void addResolvedDependency( Dependency dependency )
        {
            dependencies.add( dependency );
            resolvedDependencies.add( dependency );
        }

        public List<Dependency> getUnresolvedDependencies()
        {
            return unresolvedDependencies;
        }

        public List<Exception> getCollectionErrors()
        {
            return collectionErrors;
        }

        public void setCollectionErrors( List<Exception> exceptions )
        {
            if ( exceptions != null )
            {
                this.collectionErrors = exceptions;
            }
            else
            {
                this.collectionErrors = new ArrayList<Exception>();
            }
        }

        public List<Exception> getResolutionErrors( Dependency dependency )
        {
            List<Exception> errors = resolutionErrors.get( dependency );
            return ( errors != null ) ? errors : Collections.<Exception>emptyList();
        }

        public void setResolutionErrors( Dependency dependency, List<Exception> errors )
        {
            dependencies.add( dependency );
            unresolvedDependencies.add( dependency );
            resolutionErrors.put( dependency, errors );
        }

    }
}
