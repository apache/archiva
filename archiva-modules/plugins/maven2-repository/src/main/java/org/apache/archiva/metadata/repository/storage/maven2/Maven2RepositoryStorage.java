package org.apache.archiva.metadata.repository.storage.maven2;

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
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.facets.RepositoryProblemFacet;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.ReadMetadataRequest;
import org.apache.archiva.metadata.repository.storage.RelocationException;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageRuntimeException;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.archiva.proxy.model.RepositoryProxyConnectors;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.PathParser;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Scm;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Maven 2 repository format storage implementation. This class currently takes parameters to indicate the repository to
 * deal with rather than being instantiated per-repository.
 * FIXME: instantiate one per repository and allocate permanently from a factory (which can be obtained within the session).
 * </p>
 * <p>
 * The session is passed in as an argument to obtain any necessary resources, rather than the class being instantiated
 * within the session in the context of a single managed repository's resolution needs.
 * </p>
 */
@Service( "repositoryStorage#maven2" )
public class Maven2RepositoryStorage
    implements RepositoryStorage
{

    private static final Logger LOGGER = LoggerFactory.getLogger( Maven2RepositoryStorage.class );

    private ModelBuilder builder;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    @Named( "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;

    @Inject
    private WagonFactory wagonFactory;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Named( "pathParser#default" )
    private PathParser pathParser;

    private static final String METADATA_FILENAME_START = "maven-metadata";

    private static final String METADATA_FILENAME = METADATA_FILENAME_START + ".xml";

    // This array must be lexically sorted
    private static final String[] IGNORED_FILES = { METADATA_FILENAME, "resolver-status.properties" };

    private static final MavenXpp3Reader MAVEN_XPP_3_READER = new MavenXpp3Reader();


    @PostConstruct
    public void initialize()
    {
        builder = new DefaultModelBuilderFactory().newInstance();

    }

    @Override
    public ProjectMetadata readProjectMetadata( String repoId, String namespace, String projectId )
    {
        // TODO: could natively implement the "shared model" concept from the browse action to avoid needing it there?
        return null;
    }

    @Override
    public ProjectVersionMetadata readProjectVersionMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageMetadataNotFoundException, RepositoryStorageMetadataInvalidException,
        RepositoryStorageRuntimeException
    {
        try
        {
            ManagedRepository managedRepository =
                managedRepositoryAdmin.getManagedRepository( readMetadataRequest.getRepositoryId() );
            String artifactVersion = readMetadataRequest.getProjectVersion();
            // olamy: in case of browsing via the ui we can mix repos (parent of a SNAPSHOT can come from release repo)
            if ( !readMetadataRequest.isBrowsingRequest() )
            {
                if ( VersionUtil.isSnapshot( artifactVersion ) )
                {
                    // skygo trying to improve speed by honoring managed configuration MRM-1658
                    if ( managedRepository.isReleases() && !managedRepository.isSnapshots() )
                    {
                        throw new RepositoryStorageRuntimeException( "lookforsnaponreleaseonly",
                                                                     "managed repo is configured for release only" );
                    }
                }
                else
                {
                    if ( !managedRepository.isReleases() && managedRepository.isSnapshots() )
                    {
                        throw new RepositoryStorageRuntimeException( "lookforsreleaseonsneponly",
                                                                     "managed repo is configured for snapshot only" );
                    }
                }
            }
            File basedir = new File( managedRepository.getLocation() );
            if ( VersionUtil.isSnapshot( artifactVersion ) )
            {
                File metadataFile = pathTranslator.toFile( basedir, readMetadataRequest.getNamespace(),
                                                           readMetadataRequest.getProjectId(), artifactVersion,
                                                           METADATA_FILENAME );
                try
                {
                    ArchivaRepositoryMetadata metadata = MavenMetadataReader.read( metadataFile );

                    // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
                    SnapshotVersion snapshotVersion = metadata.getSnapshotVersion();
                    if ( snapshotVersion != null )
                    {
                        artifactVersion =
                            artifactVersion.substring( 0, artifactVersion.length() - 8 ); // remove SNAPSHOT from end
                        artifactVersion =
                            artifactVersion + snapshotVersion.getTimestamp() + "-" + snapshotVersion.getBuildNumber();
                    }
                }
                catch ( XMLException e )
                {
                    // unable to parse metadata - LOGGER it, and continue with the version as the original SNAPSHOT version
                    LOGGER.warn( "Invalid metadata: {} - {}", metadataFile, e.getMessage() );
                }
            }

            // TODO: won't work well with some other layouts, might need to convert artifact parts to ID by path translator
            String id = readMetadataRequest.getProjectId() + "-" + artifactVersion + ".pom";
            File file =
                pathTranslator.toFile( basedir, readMetadataRequest.getNamespace(), readMetadataRequest.getProjectId(),
                                       readMetadataRequest.getProjectVersion(), id );

            if ( !file.exists() )
            {
                // metadata could not be resolved
                throw new RepositoryStorageMetadataNotFoundException(
                    "The artifact's POM file '" + file.getAbsolutePath() + "' was missing" );
            }

            // TODO: this is a workaround until we can properly resolve using proxies as well - this doesn't cache
            //       anything locally!
            List<RemoteRepository> remoteRepositories = new ArrayList<>();
            Map<String, NetworkProxy> networkProxies = new HashMap<>();

            Map<String, List<ProxyConnector>> proxyConnectorsMap = proxyConnectorAdmin.getProxyConnectorAsMap();
            List<ProxyConnector> proxyConnectors = proxyConnectorsMap.get( readMetadataRequest.getRepositoryId() );
            if ( proxyConnectors != null )
            {
                for ( ProxyConnector proxyConnector : proxyConnectors )
                {
                    RemoteRepository remoteRepoConfig =
                        remoteRepositoryAdmin.getRemoteRepository( proxyConnector.getTargetRepoId() );

                    if ( remoteRepoConfig != null )
                    {
                        remoteRepositories.add( remoteRepoConfig );

                        NetworkProxy networkProxyConfig =
                            networkProxyAdmin.getNetworkProxy( proxyConnector.getProxyId() );

                        if ( networkProxyConfig != null )
                        {
                            // key/value: remote repo ID/proxy info
                            networkProxies.put( proxyConnector.getTargetRepoId(), networkProxyConfig );
                        }
                    }
                }
            }

            // That's a browsing request so we can a mix of SNAPSHOT and release artifacts (especially with snapshots which
            // can have released parent pom
            if ( readMetadataRequest.isBrowsingRequest() )
            {
                remoteRepositories.addAll( remoteRepositoryAdmin.getRemoteRepositories() );
            }

            ModelBuildingRequest req =
                new DefaultModelBuildingRequest().setProcessPlugins( false ).setPomFile( file ).setTwoPhaseBuilding(
                    false ).setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

            //MRM-1607. olamy this will resolve jdk profiles on the current running archiva jvm
            req.setSystemProperties( System.getProperties() );

            // MRM-1411
            req.setModelResolver(
                new RepositoryModelResolver( managedRepository, pathTranslator, wagonFactory, remoteRepositories,
                                             networkProxies, managedRepository ) );

            Model model;
            try
            {
                model = builder.build( req ).getEffectiveModel();
            }
            catch ( ModelBuildingException e )
            {
                String msg = "The artifact's POM file '" + file + "' was invalid: " + e.getMessage();

                List<ModelProblem> modelProblems = e.getProblems();
                for ( ModelProblem problem : modelProblems )
                {
                    // MRM-1411, related to MRM-1335
                    // this means that the problem was that the parent wasn't resolved!
                    // olamy really hackhish but fail with java profile so use error message
                    // || ( StringUtils.startsWith( problem.getMessage(), "Failed to determine Java version for profile" ) )
                    // but setTwoPhaseBuilding(true) fix that
                    if ( ( problem.getException() instanceof FileNotFoundException && e.getModelId() != null &&
                        !e.getModelId().equals( problem.getModelId() ) ) )
                    {
                        LOGGER.warn( "The artifact's parent POM file '{}' cannot be resolved. "
                                         + "Using defaults for project version metadata..", file );

                        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
                        metadata.setId( readMetadataRequest.getProjectVersion() );

                        MavenProjectFacet facet = new MavenProjectFacet();
                        facet.setGroupId( readMetadataRequest.getNamespace() );
                        facet.setArtifactId( readMetadataRequest.getProjectId() );
                        facet.setPackaging( "jar" );
                        metadata.addFacet( facet );

                        String errMsg =
                            "Error in resolving artifact's parent POM file. " + ( problem.getException() == null
                                ? problem.getMessage()
                                : problem.getException().getMessage() );
                        RepositoryProblemFacet repoProblemFacet = new RepositoryProblemFacet();
                        repoProblemFacet.setRepositoryId( readMetadataRequest.getRepositoryId() );
                        repoProblemFacet.setId( readMetadataRequest.getRepositoryId() );
                        repoProblemFacet.setMessage( errMsg );
                        repoProblemFacet.setProblem( errMsg );
                        repoProblemFacet.setProject( readMetadataRequest.getProjectId() );
                        repoProblemFacet.setVersion( readMetadataRequest.getProjectVersion() );
                        repoProblemFacet.setNamespace( readMetadataRequest.getNamespace() );

                        metadata.addFacet( repoProblemFacet );

                        return metadata;
                    }
                }

                throw new RepositoryStorageMetadataInvalidException( "invalid-pom", msg, e );
            }

            // Check if the POM is in the correct location
            boolean correctGroupId = readMetadataRequest.getNamespace().equals( model.getGroupId() );
            boolean correctArtifactId = readMetadataRequest.getProjectId().equals( model.getArtifactId() );
            boolean correctVersion = readMetadataRequest.getProjectVersion().equals( model.getVersion() );
            if ( !correctGroupId || !correctArtifactId || !correctVersion )
            {
                StringBuilder message = new StringBuilder( "Incorrect POM coordinates in '" + file + "':" );
                if ( !correctGroupId )
                {
                    message.append( "\nIncorrect group ID: " ).append( model.getGroupId() );
                }
                if ( !correctArtifactId )
                {
                    message.append( "\nIncorrect artifact ID: " ).append( model.getArtifactId() );
                }
                if ( !correctVersion )
                {
                    message.append( "\nIncorrect version: " ).append( model.getVersion() );
                }

                throw new RepositoryStorageMetadataInvalidException( "mislocated-pom", message.toString() );
            }

            ProjectVersionMetadata metadata = new ProjectVersionMetadata();
            metadata.setCiManagement( convertCiManagement( model.getCiManagement() ) );
            metadata.setDescription( model.getDescription() );
            metadata.setId( readMetadataRequest.getProjectVersion() );
            metadata.setIssueManagement( convertIssueManagement( model.getIssueManagement() ) );
            metadata.setLicenses( convertLicenses( model.getLicenses() ) );
            metadata.setMailingLists( convertMailingLists( model.getMailingLists() ) );
            metadata.setDependencies( convertDependencies( model.getDependencies() ) );
            metadata.setName( model.getName() );
            metadata.setOrganization( convertOrganization( model.getOrganization() ) );
            metadata.setScm( convertScm( model.getScm() ) );
            metadata.setUrl( model.getUrl() );
            metadata.setProperties( model.getProperties() );

            MavenProjectFacet facet = new MavenProjectFacet();
            facet.setGroupId( model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId() );
            facet.setArtifactId( model.getArtifactId() );
            facet.setPackaging( model.getPackaging() );
            if ( model.getParent() != null )
            {
                MavenProjectParent parent = new MavenProjectParent();
                parent.setGroupId( model.getParent().getGroupId() );
                parent.setArtifactId( model.getParent().getArtifactId() );
                parent.setVersion( model.getParent().getVersion() );
                facet.setParent( parent );
            }
            metadata.addFacet( facet );

            return metadata;
        }
        catch ( RepositoryAdminException e )
        {
            throw new RepositoryStorageRuntimeException( "repo-admin", e.getMessage(), e );
        }
    }

    public void setWagonFactory( WagonFactory wagonFactory )
    {
        this.wagonFactory = wagonFactory;
    }

    private List<org.apache.archiva.metadata.model.Dependency> convertDependencies( List<Dependency> dependencies )
    {
        List<org.apache.archiva.metadata.model.Dependency> l = new ArrayList<>();
        for ( Dependency dependency : dependencies )
        {
            org.apache.archiva.metadata.model.Dependency newDependency =
                new org.apache.archiva.metadata.model.Dependency();
            newDependency.setArtifactId( dependency.getArtifactId() );
            newDependency.setClassifier( dependency.getClassifier() );
            newDependency.setGroupId( dependency.getGroupId() );
            newDependency.setOptional( dependency.isOptional() );
            newDependency.setScope( dependency.getScope() );
            newDependency.setSystemPath( dependency.getSystemPath() );
            newDependency.setType( dependency.getType() );
            newDependency.setVersion( dependency.getVersion() );
            l.add( newDependency );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.Scm convertScm( Scm scm )
    {
        org.apache.archiva.metadata.model.Scm newScm = null;
        if ( scm != null )
        {
            newScm = new org.apache.archiva.metadata.model.Scm();
            newScm.setConnection( scm.getConnection() );
            newScm.setDeveloperConnection( scm.getDeveloperConnection() );
            newScm.setUrl( scm.getUrl() );
        }
        return newScm;
    }

    private org.apache.archiva.metadata.model.Organization convertOrganization( Organization organization )
    {
        org.apache.archiva.metadata.model.Organization org = null;
        if ( organization != null )
        {
            org = new org.apache.archiva.metadata.model.Organization();
            org.setName( organization.getName() );
            org.setUrl( organization.getUrl() );
        }
        return org;
    }

    private List<org.apache.archiva.metadata.model.License> convertLicenses( List<License> licenses )
    {
        List<org.apache.archiva.metadata.model.License> l = new ArrayList<>();
        for ( License license : licenses )
        {
            org.apache.archiva.metadata.model.License newLicense = new org.apache.archiva.metadata.model.License();
            newLicense.setName( license.getName() );
            newLicense.setUrl( license.getUrl() );
            l.add( newLicense );
        }
        return l;
    }

    private List<org.apache.archiva.metadata.model.MailingList> convertMailingLists( List<MailingList> mailingLists )
    {
        List<org.apache.archiva.metadata.model.MailingList> l = new ArrayList<>();
        for ( MailingList mailingList : mailingLists )
        {
            org.apache.archiva.metadata.model.MailingList newMailingList =
                new org.apache.archiva.metadata.model.MailingList();
            newMailingList.setName( mailingList.getName() );
            newMailingList.setMainArchiveUrl( mailingList.getArchive() );
            newMailingList.setPostAddress( mailingList.getPost() );
            newMailingList.setSubscribeAddress( mailingList.getSubscribe() );
            newMailingList.setUnsubscribeAddress( mailingList.getUnsubscribe() );
            newMailingList.setOtherArchives( mailingList.getOtherArchives() );
            l.add( newMailingList );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.IssueManagement convertIssueManagement( IssueManagement issueManagement )
    {
        org.apache.archiva.metadata.model.IssueManagement im = null;
        if ( issueManagement != null )
        {
            im = new org.apache.archiva.metadata.model.IssueManagement();
            im.setSystem( issueManagement.getSystem() );
            im.setUrl( issueManagement.getUrl() );
        }
        return im;
    }

    private org.apache.archiva.metadata.model.CiManagement convertCiManagement( CiManagement ciManagement )
    {
        org.apache.archiva.metadata.model.CiManagement ci = null;
        if ( ciManagement != null )
        {
            ci = new org.apache.archiva.metadata.model.CiManagement();
            ci.setSystem( ciManagement.getSystem() );
            ci.setUrl( ciManagement.getUrl() );
        }
        return ci;
    }

    @Override
    public Collection<String> listRootNamespaces( String repoId, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        File dir = getRepositoryBasedir( repoId );

        return getSortedFiles( dir, filter );
    }

    private static Collection<String> getSortedFiles( File dir, Filter<String> filter )
    {
        List<String> fileNames;
        String[] files = dir.list( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            fileNames = new ArrayList<>( Arrays.asList( files ) );
            Collections.sort( fileNames );
        }
        else
        {
            fileNames = Collections.emptyList();
        }
        return fileNames;
    }

    private File getRepositoryBasedir( String repoId )
        throws RepositoryStorageRuntimeException
    {
        try
        {
            ManagedRepository repositoryConfiguration = managedRepositoryAdmin.getManagedRepository( repoId );

            return new File( repositoryConfiguration.getLocation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new RepositoryStorageRuntimeException( "repo-admin", e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> listNamespaces( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace );

        // scan all the directories which are potential namespaces. Any directories known to be projects are excluded
        List<String> namespaces = new ArrayList<>();
        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( !isProject( file, filter ) )
                {
                    namespaces.add( file.getName() );
                }
            }
        }
        Collections.sort( namespaces );
        return namespaces;
    }

    @Override
    public Collection<String> listProjects( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace );

        // scan all directories in the namespace, and only include those that are known to be projects
        List<String> projects = new ArrayList<>();

        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( isProject( file, filter ) )
                {
                    projects.add( file.getName() );
                }
            }
        }
        Collections.sort( projects );
        return projects;
    }

    @Override
    public Collection<String> listProjectVersions( String repoId, String namespace, String projectId,
                                                   Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace, projectId );

        // all directories in a project directory can be considered a version
        return getSortedFiles( dir, filter );
    }

    @Override
    public Collection<ArtifactMetadata> readArtifactsMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageRuntimeException
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( readMetadataRequest.getRepositoryId() ),
                                          readMetadataRequest.getNamespace(), readMetadataRequest.getProjectId(),
                                          readMetadataRequest.getProjectVersion() );

        // all files that are not metadata and not a checksum / signature are considered artifacts
        File[] files = dir.listFiles( new ArtifactDirectoryFilter( readMetadataRequest.getFilter() ) );

        List<ArtifactMetadata> artifacts = new ArrayList<>();
        if ( files != null )
        {
            int errorCount=0;
            for ( File file : files )
            {
                try {
                    ArtifactMetadata metadata =
                            getArtifactFromFile(readMetadataRequest.getRepositoryId(), readMetadataRequest.getNamespace(),
                                    readMetadataRequest.getProjectId(), readMetadataRequest.getProjectVersion(),
                                    file);
                    artifacts.add(metadata);
                } catch (Exception ex) {
                    LOGGER.error("Error while retrieving metadata of file {} (Project: {}, Repository: {}): {}",
                            file.getName(), readMetadataRequest.getProjectId(), readMetadataRequest.getRepositoryId(),
                            ex.getMessage());
                    errorCount++;
                }
            }
            // We throw only an error, if the number of errors equals the number of files
            if (errorCount>0 && errorCount==files.length) {
                throw new RepositoryStorageRuntimeException(readMetadataRequest.getRepositoryId(), "Could not retrieve metadata of the files");
            }
        }
        return artifacts;
    }

    @Override
    public ArtifactMetadata readArtifactMetadataFromPath( String repoId, String path )
        throws RepositoryStorageRuntimeException
    {
        ArtifactMetadata metadata = pathTranslator.getArtifactForPath( repoId, path );

        populateArtifactMetadataFromFile( metadata, new File( getRepositoryBasedir( repoId ), path ) );

        return metadata;
    }

    private ArtifactMetadata getArtifactFromFile( String repoId, String namespace, String projectId,
                                                  String projectVersion, File file )
    {
        ArtifactMetadata metadata =
            pathTranslator.getArtifactFromId( repoId, namespace, projectId, projectVersion, file.getName() );

        populateArtifactMetadataFromFile( metadata, file );

        return metadata;
    }

    @Override
    public void applyServerSideRelocation( ManagedRepositoryContent managedRepository, ArtifactReference artifact )
        throws ProxyDownloadException
    {
        if ( "pom".equals( artifact.getType() ) )
        {
            return;
        }

        // Build the artifact POM reference
        ArtifactReference pomReference = new ArtifactReference();
        pomReference.setGroupId( artifact.getGroupId() );
        pomReference.setArtifactId( artifact.getArtifactId() );
        pomReference.setVersion( artifact.getVersion() );
        pomReference.setType( "pom" );

        RepositoryProxyConnectors connectors =
            applicationContext.getBean( "repositoryProxyConnectors#default", RepositoryProxyConnectors.class );

        // Get the artifact POM from proxied repositories if needed
        connectors.fetchFromProxies( managedRepository, pomReference );

        // Open and read the POM from the managed repo
        File pom = managedRepository.toFile( pomReference );

        if ( !pom.exists() )
        {
            return;
        }

        try
        {
            // MavenXpp3Reader leaves the file open, so we need to close it ourselves.

            Model model = null;
            try (Reader reader = Files.newBufferedReader( pom.toPath(), Charset.defaultCharset() ))
            {
                model = MAVEN_XPP_3_READER.read( reader );
            }

            DistributionManagement dist = model.getDistributionManagement();
            if ( dist != null )
            {
                Relocation relocation = dist.getRelocation();
                if ( relocation != null )
                {
                    // artifact is relocated : update the repositoryPath
                    if ( relocation.getGroupId() != null )
                    {
                        artifact.setGroupId( relocation.getGroupId() );
                    }
                    if ( relocation.getArtifactId() != null )
                    {
                        artifact.setArtifactId( relocation.getArtifactId() );
                    }
                    if ( relocation.getVersion() != null )
                    {
                        artifact.setVersion( relocation.getVersion() );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            // Unable to read POM : ignore.
        }
        catch ( XmlPullParserException e )
        {
            // Invalid POM : ignore
        }
    }


    @Override
    public String getFilePath( String requestPath, ManagedRepository managedRepository )
    {
        // managedRepository can be null
        // extract artifact reference from url
        // groupId:artifactId:version:packaging:classifier
        //org/apache/archiva/archiva-checksum/1.4-M4-SNAPSHOT/archiva-checksum-1.4-M4-SNAPSHOT.jar
        String logicalResource = null;
        String requestPathInfo = StringUtils.defaultString( requestPath );

        //remove prefix ie /repository/blah becomes /blah
        requestPathInfo = removePrefix( requestPathInfo );

        // Remove prefixing slash as the repository id doesn't contain it;
        if ( requestPathInfo.startsWith( "/" ) )
        {
            requestPathInfo = requestPathInfo.substring( 1 );
        }

        int slash = requestPathInfo.indexOf( '/' );
        if ( slash > 0 )
        {
            logicalResource = requestPathInfo.substring( slash );

            if ( logicalResource.endsWith( "/.." ) )
            {
                logicalResource += "/";
            }

            if ( logicalResource != null && logicalResource.startsWith( "//" ) )
            {
                logicalResource = logicalResource.substring( 1 );
            }

            if ( logicalResource == null )
            {
                logicalResource = "/";
            }
        }
        else
        {
            logicalResource = "/";
        }
        return logicalResource;

    }

    @Override
    public String getFilePathWithVersion( final String requestPath, ManagedRepositoryContent managedRepositoryContent )
        throws XMLException, RelocationException
    {

        if ( StringUtils.endsWith( requestPath, METADATA_FILENAME ) )
        {
            return getFilePath( requestPath, managedRepositoryContent.getRepository() );
        }

        String filePath = getFilePath( requestPath, managedRepositoryContent.getRepository() );

        ArtifactReference artifactReference = null;
        try
        {
            artifactReference = pathParser.toArtifactReference( filePath );
        }
        catch ( LayoutException e )
        {
            return filePath;
        }

        if ( StringUtils.endsWith( artifactReference.getVersion(), VersionUtil.SNAPSHOT ) )
        {
            // read maven metadata to get last timestamp
            File metadataDir = new File( managedRepositoryContent.getRepoRoot(), filePath ).getParentFile();
            if ( !metadataDir.exists() )
            {
                return filePath;
            }
            File metadataFile = new File( metadataDir, METADATA_FILENAME );
            if ( !metadataFile.exists() )
            {
                return filePath;
            }
            ArchivaRepositoryMetadata archivaRepositoryMetadata = MavenMetadataReader.read( metadataFile );
            int buildNumber = archivaRepositoryMetadata.getSnapshotVersion().getBuildNumber();
            String timestamp = archivaRepositoryMetadata.getSnapshotVersion().getTimestamp();

            // MRM-1846
            if ( buildNumber < 1 && timestamp == null )
            {
                return filePath;
            }

            // org/apache/archiva/archiva-checksum/1.4-M4-SNAPSHOT/archiva-checksum-1.4-M4-SNAPSHOT.jar
            // ->  archiva-checksum-1.4-M4-20130425.081822-1.jar

            filePath = StringUtils.replace( filePath, //
                                            artifactReference.getArtifactId() //
                                                + "-" + artifactReference.getVersion(), //
                                            artifactReference.getArtifactId() //
                                                + "-" + StringUtils.remove( artifactReference.getVersion(),
                                                                            "-" + VersionUtil.SNAPSHOT ) //
                                                + "-" + timestamp //
                                                + "-" + buildNumber );

            throw new RelocationException( "/repository/" + managedRepositoryContent.getRepository().getId() +
                                               ( StringUtils.startsWith( filePath, "/" ) ? "" : "/" ) + filePath,
                                           RelocationException.RelocationType.TEMPORARY );

        }

        return filePath;
    }

    //-----------------------------
    // internal
    //-----------------------------

    /**
     * FIXME remove
     *
     * @param href
     * @return
     */
    private static String removePrefix( final String href )
    {
        String[] parts = StringUtils.split( href, '/' );
        parts = (String[]) ArrayUtils.subarray( parts, 1, parts.length );
        if ( parts == null || parts.length == 0 )
        {
            return "/";
        }

        String joinedString = StringUtils.join( parts, '/' );
        if ( href.endsWith( "/" ) )
        {
            joinedString = joinedString + "/";
        }

        return joinedString;
    }

    private static void populateArtifactMetadataFromFile( ArtifactMetadata metadata, File file )
    {
        metadata.setWhenGathered( new Date() );
        metadata.setFileLastModified( file.lastModified() );
        ChecksummedFile checksummedFile = new ChecksummedFile( file );
        try
        {
            metadata.setMd5( checksummedFile.calculateChecksum( ChecksumAlgorithm.MD5 ) );
        }
        catch ( IOException e )
        {
            LOGGER.error( "Unable to checksum file {}: {},MD5", file, e.getMessage() );
        }
        try
        {
            metadata.setSha1( checksummedFile.calculateChecksum( ChecksumAlgorithm.SHA1 ) );
        }
        catch ( IOException e )
        {
            LOGGER.error( "Unable to checksum file {}: {},SHA1", file, e.getMessage() );
        }
        metadata.setSize( file.length() );
    }

    private boolean isProject( File dir, Filter<String> filter )
    {
        // scan directories for a valid project version subdirectory, meaning this must be a project directory
        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( isProjectVersion( file ) )
                {
                    return true;
                }
            }
        }

        // if a metadata file is present, check if this is the "artifactId" directory, marking it as a project
        ArchivaRepositoryMetadata metadata = readMetadata( dir );
        if ( metadata != null && dir.getName().equals( metadata.getArtifactId() ) )
        {
            return true;
        }

        return false;
    }

    private boolean isProjectVersion( File dir )
    {
        final String artifactId = dir.getParentFile().getName();
        final String projectVersion = dir.getName();

        // check if there is a POM artifact file to ensure it is a version directory
        File[] files;
        if ( VersionUtil.isSnapshot( projectVersion ) )
        {
            files = dir.listFiles( new PomFilenameFilter( artifactId, projectVersion ) );
        }
        else
        {
            final String pomFile = artifactId + "-" + projectVersion + ".pom";
            files = dir.listFiles( new PomFileFilter( pomFile ) );
        }
        if ( files != null && files.length > 0 )
        {
            return true;
        }

        // if a metadata file is present, check if this is the "version" directory, marking it as a project version
        ArchivaRepositoryMetadata metadata = readMetadata( dir );
        if ( metadata != null && projectVersion.equals( metadata.getVersion() ) )
        {
            return true;
        }

        return false;
    }

    private ArchivaRepositoryMetadata readMetadata( File directory )
    {
        ArchivaRepositoryMetadata metadata = null;
        File metadataFile = new File( directory, METADATA_FILENAME );
        if ( metadataFile.exists() )
        {
            try
            {
                metadata = MavenMetadataReader.read( metadataFile );
            }
            catch ( XMLException e )
            {
                // ignore missing or invalid metadata
            }
        }
        return metadata;
    }

    private static class DirectoryFilter
        implements FilenameFilter
    {
        private final Filter<String> filter;

        public DirectoryFilter( Filter<String> filter )
        {
            this.filter = filter;
        }

        @Override
        public boolean accept( File dir, String name )
        {
            if ( !filter.accept( name ) )
            {
                return false;
            }
            else if ( name.startsWith( "." ) )
            {
                return false;
            }
            else if ( !new File( dir, name ).isDirectory() )
            {
                return false;
            }
            return true;
        }
    }

    private static class ArtifactDirectoryFilter
        implements FilenameFilter
    {
        private final Filter<String> filter;

        private ArtifactDirectoryFilter( Filter<String> filter )
        {
            this.filter = filter;
        }

        @Override
        public boolean accept( File dir, String name )
        {
            // TODO compare to logic in maven-repository-layer
            if ( !filter.accept( name ) )
            {
                return false;
            }
            else if ( name.startsWith( "." ) )
            {
                return false;
            }
            else if ( name.endsWith( ".md5" ) || name.endsWith( ".sha1" ) || name.endsWith( ".asc" ) )
            {
                return false;
            }
            else if ( Arrays.binarySearch(IGNORED_FILES, name)>=0 )
            {
                return false;
            }
            else if ( new File( dir, name ).isDirectory() )
            {
                return false;
            }
            // some files from remote repositories can have name like maven-metadata-archiva-vm-all-public.xml
            else if ( StringUtils.startsWith( name, METADATA_FILENAME_START ) && StringUtils.endsWith( name, ".xml" ) )
            {
                return false;
            }

            return true;

        }
    }


    private static final class PomFilenameFilter
        implements FilenameFilter
    {

        private final String artifactId, projectVersion;

        private PomFilenameFilter( String artifactId, String projectVersion )
        {
            this.artifactId = artifactId;
            this.projectVersion = projectVersion;
        }

        @Override
        public boolean accept( File dir, String name )
        {
            if ( name.startsWith( artifactId + "-" ) && name.endsWith( ".pom" ) )
            {
                String v = name.substring( artifactId.length() + 1, name.length() - 4 );
                v = VersionUtil.getBaseVersion( v );
                if ( v.equals( projectVersion ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static class PomFileFilter
        implements FilenameFilter
    {
        private final String pomFile;

        private PomFileFilter( String pomFile )
        {
            this.pomFile = pomFile;
        }

        @Override
        public boolean accept( File dir, String name )
        {
            return pomFile.equals( name );
        }
    }


    public PathParser getPathParser()
    {
        return pathParser;
    }

    public void setPathParser( PathParser pathParser )
    {
        this.pathParser = pathParser;
    }
}
