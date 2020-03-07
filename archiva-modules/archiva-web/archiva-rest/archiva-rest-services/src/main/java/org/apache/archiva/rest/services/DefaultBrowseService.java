package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.repository.maven.dependency.tree.DependencyTreeBuilder;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.maven2.model.TreeEntry;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMetadataVersionComparator;
import org.apache.archiva.repository.maven.metadata.storage.MavenProjectFacet;
import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.storage.fs.FsStorageUtil;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.rest.api.model.*;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.services.utils.ArtifactContentEntryComparator;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service( "browseService#rest" )
public class DefaultBrowseService
    extends AbstractRestService
    implements BrowseService
{

    private final Charset ARTIFACT_CONTENT_ENCODING=Charset.forName( "UTF-8" );

    @Inject
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Inject
    ProxyRegistry proxyRegistry;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    @Named( value = "browse#versionMetadata" )
    private Cache<String, ProjectVersionMetadata> versionMetadataCache;

    private ManagedRepositoryContent getManagedRepositoryContent(String id) throws RepositoryException
    {
        org.apache.archiva.repository.ManagedRepository repo = repositoryRegistry.getManagedRepository( id );
        if (repo==null) {
            throw new RepositoryException( "Could not find repository "+id );
        }
        return repo.getContent();
    }

    @Override
    public BrowseResult getRootGroups( String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        Set<String> namespaces = new LinkedHashSet<String>();

        // TODO: this logic should be optional, particularly remembering we want to keep this code simple
        //       it is located here to avoid the content repository implementation needing to do too much for what
        //       is essentially presentation code
        Set<String> namespacesToCollapse = new LinkedHashSet<String>();
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveRootNamespaces( repositorySession, repoId ) );
            }
            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add( collapseNamespaces( repositorySession, metadataResolver, selectedRepos, n ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            repositorySession.close();
        }

        List<BrowseResultEntry> browseGroupResultEntries = new ArrayList<>( namespaces.size() );
        for ( String namespace : namespaces )
        {
            browseGroupResultEntries.add( new BrowseResultEntry( namespace, false ) );
        }

        Collections.sort( browseGroupResultEntries );
        return new BrowseResult( browseGroupResultEntries );
    }

    @Override
    public BrowseResult browseGroupId( String groupId, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        Set<String> projects = new LinkedHashSet<>();

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        Set<String> namespaces;
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> namespacesToCollapse = new LinkedHashSet<>();
            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, groupId ) );

                projects.addAll( metadataResolver.resolveProjects( repositorySession, repoId, groupId ) );
            }

            // TODO: this logic should be optional, particularly remembering we want to keep this code simple
            // it is located here to avoid the content repository implementation needing to do too much for what
            // is essentially presentation code
            namespaces = new LinkedHashSet<>();
            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add(
                    collapseNamespaces( repositorySession, metadataResolver, selectedRepos, groupId + "." + n ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            repositorySession.close();
        }
        List<BrowseResultEntry> browseGroupResultEntries = new ArrayList<>( namespaces.size() + projects.size() );
        for ( String namespace : namespaces )
        {
            browseGroupResultEntries.add( new BrowseResultEntry( namespace, false ).groupId( namespace ) );
        }
        for ( String project : projects )
        {
            browseGroupResultEntries.add(
                new BrowseResultEntry( groupId + '.' + project, true ).groupId( groupId ).artifactId( project ) );
        }
        Collections.sort( browseGroupResultEntries );
        return new BrowseResult( browseGroupResultEntries );

    }

    @Override
    public VersionsList getVersionsList( String groupId, String artifactId, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        try
        {
            Collection<String> versions = getVersions( selectedRepos, groupId, artifactId );
            return new VersionsList( new ArrayList<>( versions ) );
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }

    }

    private Collection<String> getVersions( List<String> selectedRepos, String groupId, String artifactId )
        throws MetadataResolutionException

    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> versions = new LinkedHashSet<String>();

            for ( String repoId : selectedRepos )
            {
                Collection<String> projectVersions =
                    metadataResolver.resolveProjectVersions( repositorySession, repoId, groupId, artifactId );
                versions.addAll( projectVersions );
            }

            List<String> sortedVersions = new ArrayList<>( versions );

            Collections.sort( sortedVersions, VersionComparator.getInstance() );

            return sortedVersions;
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public ProjectVersionMetadata getProjectMetadata( String groupId, String artifactId, String version,
                                                      String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();

            MetadataResolver metadataResolver = repositorySession.getResolver();

            ProjectVersionMetadata versionMetadata = null;
            for ( String repoId : selectedRepos )
            {
                if ( versionMetadata == null || versionMetadata.isIncomplete() )
                {
                    try
                    {
                        ProjectVersionMetadata versionMetadataTmp =
                            metadataResolver.resolveProjectVersion( repositorySession, repoId, groupId, artifactId,
                                                                    version );

                        if ( versionMetadata == null && versionMetadataTmp != null )
                        {
                            versionMetadata = versionMetadataTmp;
                        }


                    }
                    catch ( MetadataResolutionException e )
                    {
                        log.warn( "Skipping invalid metadata while compiling shared model for {}:{} in repo {}: {}",
                                  groupId, artifactId, repoId, e.getMessage() );
                    }
                }
            }

            return versionMetadata;
        } catch (MetadataRepositoryException e) {
            throw new ArchivaRestServiceException(e.getMessage(), e);
        } finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }

    }

    @Override
    public ProjectVersionMetadata getProjectVersionMetadata( String groupId, String artifactId, String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getSelectedRepos( repositoryId );

        RepositorySession repositorySession = null;
        try
        {

            Collection<String> projectVersions = getVersions( selectedRepos, groupId, artifactId );

            repositorySession = repositorySessionFactory.createSession();

            MetadataResolver metadataResolver = repositorySession.getResolver();

            ProjectVersionMetadata sharedModel = new ProjectVersionMetadata();

            MavenProjectFacet mavenFacet = new MavenProjectFacet();
            mavenFacet.setGroupId( groupId );
            mavenFacet.setArtifactId( artifactId );
            sharedModel.addFacet( mavenFacet );

            boolean isFirstVersion = true;

            for ( String version : projectVersions )
            {
                ProjectVersionMetadata versionMetadata = null;
                for ( String repoId : selectedRepos )
                {
                    if ( versionMetadata == null || versionMetadata.isIncomplete() )
                    {
                        try
                        {
                            ProjectVersionMetadata projectVersionMetadataResolved = null;
                            boolean useCache = !StringUtils.endsWith( version, VersionUtil.SNAPSHOT );
                            String cacheKey = null;
                            boolean cacheToUpdate = false;
                            // FIXME a bit maven centric!!!
                            // not a snapshot so get it from cache
                            if ( useCache )
                            {
                                cacheKey = repoId + groupId + artifactId + version;
                                projectVersionMetadataResolved = versionMetadataCache.get( cacheKey );
                            }
                            if ( useCache && projectVersionMetadataResolved != null )
                            {
                                versionMetadata = projectVersionMetadataResolved;
                            }
                            else
                            {
                                projectVersionMetadataResolved =
                                    metadataResolver.resolveProjectVersion( repositorySession, repoId, groupId,
                                                                            artifactId, version );
                                versionMetadata = projectVersionMetadataResolved;
                                cacheToUpdate = true;
                            }

                            if ( useCache && cacheToUpdate )
                            {
                                versionMetadataCache.put( cacheKey, projectVersionMetadataResolved );
                            }

                        }
                        catch ( MetadataResolutionException e )
                        {
                            log.error( "Skipping invalid metadata while compiling shared model for " + groupId + ":"
                                           + artifactId + " in repo " + repoId + ": " + e.getMessage() );
                        }
                    }
                }

                if ( versionMetadata == null )
                {
                    continue;
                }

                if ( isFirstVersion )
                {
                    sharedModel = versionMetadata;
                    sharedModel.setId( null );
                }
                else
                {
                    MavenProjectFacet versionMetadataMavenFacet =
                        (MavenProjectFacet) versionMetadata.getFacet( MavenProjectFacet.FACET_ID );
                    if ( versionMetadataMavenFacet != null )
                    {
                        if ( mavenFacet.getPackaging() != null //
                            && !StringUtils.equalsIgnoreCase( mavenFacet.getPackaging(),
                                                              versionMetadataMavenFacet.getPackaging() ) )
                        {
                            mavenFacet.setPackaging( null );
                        }
                    }

                    if ( StringUtils.isEmpty( sharedModel.getName() ) //
                        && !StringUtils.isEmpty( versionMetadata.getName() ) )
                    {
                        sharedModel.setName( versionMetadata.getName() );
                    }

                    if ( sharedModel.getDescription() != null //
                        && !StringUtils.equalsIgnoreCase( sharedModel.getDescription(),
                                                          versionMetadata.getDescription() ) )
                    {
                        sharedModel.setDescription( StringUtils.isNotEmpty( versionMetadata.getDescription() )
                                                        ? versionMetadata.getDescription()
                                                        : "" );
                    }

                    if ( sharedModel.getIssueManagement() != null //
                        && versionMetadata.getIssueManagement() != null //
                        && !StringUtils.equalsIgnoreCase( sharedModel.getIssueManagement().getUrl(),
                                                          versionMetadata.getIssueManagement().getUrl() ) )
                    {
                        sharedModel.setIssueManagement( versionMetadata.getIssueManagement() );
                    }

                    if ( sharedModel.getCiManagement() != null //
                        && versionMetadata.getCiManagement() != null //
                        && !StringUtils.equalsIgnoreCase( sharedModel.getCiManagement().getUrl(),
                                                          versionMetadata.getCiManagement().getUrl() ) )
                    {
                        sharedModel.setCiManagement( versionMetadata.getCiManagement() );
                    }

                    if ( sharedModel.getOrganization() != null //
                        && versionMetadata.getOrganization() != null //
                        && !StringUtils.equalsIgnoreCase( sharedModel.getOrganization().getName(),
                                                          versionMetadata.getOrganization().getName() ) )
                    {
                        sharedModel.setOrganization( versionMetadata.getOrganization() );
                    }

                    if ( sharedModel.getUrl() != null //
                        && !StringUtils.equalsIgnoreCase( sharedModel.getUrl(), versionMetadata.getUrl() ) )
                    {
                        sharedModel.setUrl( versionMetadata.getUrl() );
                    }
                }

                isFirstVersion = false;
            }
            return sharedModel;
        }
        catch (MetadataResolutionException | MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
    }

    @Override
    public List<TreeEntry> getTreeEntries( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        try
        {
            return dependencyTreeBuilder.buildDependencyTree( selectedRepos, groupId, artifactId, version );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }

        return Collections.emptyList();
    }

    @Override
    public List<ManagedRepository> getUserRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            return userRepositories.getAccessibleRepositories( getPrincipal() );
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( "repositories.read.observable.error",
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }

    @Override
    public List<ManagedRepository> getUserManagableRepositories() throws ArchivaRestServiceException {
        try
        {
            return userRepositories.getManagableRepositories( getPrincipal() );
        }
        catch ( ArchivaSecurityException e )
        {
            throw new ArchivaRestServiceException( "repositories.read.managable.error",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }

    @Override
    public List<Artifact> getDependees( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<ProjectVersionReference> references = new ArrayList<>();
        // TODO: what if we get duplicates across repositories?
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();
            for ( String repoId : getObservableRepos() )
            {
                // TODO: what about if we want to see this irrespective of version?
                references.addAll(
                    metadataResolver.resolveProjectReferences( repositorySession, repoId, groupId, artifactId,
                                                               version ) );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            repositorySession.close();
        }

        List<Artifact> artifacts = new ArrayList<>( references.size() );

        for ( ProjectVersionReference projectVersionReference : references )
        {
            artifacts.add( new Artifact( projectVersionReference.getNamespace(), projectVersionReference.getProjectId(),
                                         projectVersionReference.getProjectVersion() ) );
        }
        return artifacts;
    }

    @Override
    public List<Entry> getMetadatas( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );
        if ( projectVersionMetadata == null )
        {
            return Collections.emptyList();
        }
        MetadataFacet metadataFacet = projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet == null )
        {
            return Collections.emptyList();
        }
        Map<String, String> map = metadataFacet.toProperties();
        List<Entry> entries = new ArrayList<>( map.size() );

        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            entries.add( new Entry( entry.getKey(), entry.getValue() ) );
        }

        return entries;
    }

    @Override
    public Boolean addMetadata( String groupId, String artifactId, String version, String key, String value,
                                String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );

        if ( projectVersionMetadata == null )
        {
            return Boolean.FALSE;
        }

        Map<String, String> properties = new HashMap<>();

        MetadataFacet metadataFacet = projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet != null && metadataFacet.toProperties() != null )
        {
            properties.putAll( metadataFacet.toProperties() );
        }
        else
        {
            metadataFacet = new GenericMetadataFacet();
        }

        properties.put( key, value );

        metadataFacet.fromProperties( properties );

        projectVersionMetadata.addFacet( metadataFacet );

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.updateProjectVersion(repositorySession , repositoryId, groupId, artifactId, projectVersionMetadata );

            repositorySession.save();
        }
        catch (MetadataRepositoryException | MetadataSessionException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteMetadata( String groupId, String artifactId, String version, String key, String repositoryId )
        throws ArchivaRestServiceException
    {
        ProjectVersionMetadata projectVersionMetadata =
            getProjectMetadata( groupId, artifactId, version, repositoryId );

        if ( projectVersionMetadata == null )
        {
            return Boolean.FALSE;
        }

        GenericMetadataFacet metadataFacet =
            (GenericMetadataFacet) projectVersionMetadata.getFacet( GenericMetadataFacet.FACET_ID );

        if ( metadataFacet != null && metadataFacet.toProperties() != null )
        {
            Map<String, String> properties = metadataFacet.toProperties();
            properties.remove( key );
            metadataFacet.setAdditionalProperties( properties );
        }
        else
        {
            return Boolean.TRUE;
        }

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            metadataRepository.updateProjectVersion(repositorySession , repositoryId, groupId, artifactId, projectVersionMetadata );

            repositorySession.save();
        }
        catch (MetadataRepositoryException | MetadataSessionException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        finally
        {
            repositorySession.close();
        }
        return Boolean.TRUE;
    }

    @Override
    public List<ArtifactContentEntry> getArtifactContentEntries( String groupId, String artifactId, String version,
                                                                 String classifier, String type, String path,
                                                                 String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );
        try
        {
            for ( String repoId : selectedRepos )
            {

                ManagedRepositoryContent managedRepositoryContent =
                    getManagedRepositoryContent( repoId );
                ArchivaArtifact archivaArtifact = new ArchivaArtifact( groupId, artifactId, version, classifier,
                                                                       StringUtils.isEmpty( type ) ? "jar" : type,
                                                                       repoId );
                StorageAsset file = managedRepositoryContent.toFile( archivaArtifact );
                if ( file.exists() )
                {
                    return readFileEntries( file, path, repoId );
                }
            }
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        catch ( RepositoryNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        return Collections.emptyList();
    }

    @Override
    public List<Artifact> getArtifactDownloadInfos( String groupId, String artifactId, String version,
                                                    String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        List<Artifact> artifactDownloadInfos = new ArrayList<>();

        try (RepositorySession session = repositorySessionFactory.createSession())
        {
            MetadataResolver metadataResolver = session.getResolver();
            for ( String repoId : selectedRepos )
            {
                List<ArtifactMetadata> artifacts = new ArrayList<>(
                    metadataResolver.resolveArtifacts( session, repoId, groupId, artifactId, version ) );
                Collections.sort( artifacts, ArtifactMetadataVersionComparator.INSTANCE );
                if ( artifacts != null && !artifacts.isEmpty() )
                {
                    return buildArtifacts( artifacts, repoId );
                }
            }
        }
        catch ( MetadataResolutionException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        return artifactDownloadInfos;
    }

    @Override
    public ArtifactContent getArtifactContentText( String groupId, String artifactId, String version, String classifier,
                                                   String type, String path, String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );
        try
        {
            for ( String repoId : selectedRepos )
            {

                ManagedRepositoryContent managedRepositoryContent = null;
                try
                {
                    managedRepositoryContent = getManagedRepositoryContent( repoId );
                }
                catch ( RepositoryException e )
                {
                    log.error("No repository content found for "+repoId);
                    continue;
                }
                ArchivaArtifact archivaArtifact = new ArchivaArtifact( groupId, artifactId, version, classifier,
                                                                       StringUtils.isEmpty( type ) ? "jar" : type,
                                                                       repoId );
                StorageAsset file = managedRepositoryContent.toFile( archivaArtifact );
                if ( !file.exists() )
                {
                    log.debug( "file: {} not exists for repository: {} try next repository", file, repoId );
                    continue;
                }
                if ( StringUtils.isNotBlank( path ) )
                {
                    // zip entry of the path -> path must a real file entry of the archive
                    FsStorageUtil.PathInformation pathInfo = FsStorageUtil.getAssetDataAsPath(file);
                    JarFile jarFile = new JarFile( pathInfo.getPath().toFile());
                    ZipEntry zipEntry = jarFile.getEntry( path );
                    try (InputStream inputStream = jarFile.getInputStream( zipEntry ))
                    {
                        return new ArtifactContent( IOUtils.toString( inputStream, ARTIFACT_CONTENT_ENCODING ), repoId );
                    }
                    finally
                    {
                        closeQuietly( jarFile );
                        if (pathInfo.isTmpFile()) {
                            Files.deleteIfExists(pathInfo.getPath());
                        }
                    }
                }
                try(InputStream readStream = file.getReadStream()) {
                    return new ArtifactContent(IOUtils.toString(readStream, ARTIFACT_CONTENT_ENCODING), repoId);
                }
            }
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
        log.debug( "artifact: {}:{}:{}:{}:{} not found", groupId, artifactId, version, classifier, type );
        // 404 ?
        return new ArtifactContent();
    }

    @Override
    public Boolean artifactAvailable( String groupId, String artifactId, String version, String classifier,
                                      String repositoryId )
        throws ArchivaRestServiceException
    {
        List<String> selectedRepos = getSelectedRepos( repositoryId );

        boolean snapshot = VersionUtil.isSnapshot( version );

        try
        {
            for ( String repoId : selectedRepos )
            {

                org.apache.archiva.repository.ManagedRepository managedRepo = repositoryRegistry.getManagedRepository(repoId);
                if (!proxyRegistry.hasHandler(managedRepo.getType())) {
                    throw new RepositoryException( "No proxy handler found for repository type "+managedRepo.getType());
                }
                RepositoryProxyHandler proxyHandler = proxyRegistry.getHandler(managedRepo.getType()).get(0);
                if ( ( snapshot && !managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.SNAPSHOT) ) || ( !snapshot
                    && managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.SNAPSHOT) ) )
                {
                    continue;
                }
                ManagedRepositoryContent managedRepositoryContent = getManagedRepositoryContent( repoId );

                // FIXME default to jar which can be wrong for war zip etc....
                ArchivaArtifact archivaArtifact = new ArchivaArtifact( groupId, artifactId, version,
                                                                       StringUtils.isEmpty( classifier )
                                                                           ? ""
                                                                           : classifier, "jar", repoId );
                StorageAsset file = managedRepositoryContent.toFile( archivaArtifact );

                if ( file != null && file.exists() )
                {
                    return true;
                }

                // in case of SNAPSHOT we can have timestamped version locally !
                if ( StringUtils.endsWith( version, VersionUtil.SNAPSHOT ) )
                {
                    StorageAsset metadataFile = file.getStorage().getAsset(file.getParent().getPath()+"/"+MetadataTools.MAVEN_METADATA );
                    if ( metadataFile.exists() )
                    {
                        MetadataReader metadataReader = repositoryRegistry.getMetadataReader( managedRepositoryContent.getRepository( ).getType( ) );
                        ArchivaRepositoryMetadata archivaRepositoryMetadata =
                            metadataReader.read( metadataFile );
                        int buildNumber = archivaRepositoryMetadata.getSnapshotVersion().getBuildNumber();
                        String timeStamp = archivaRepositoryMetadata.getSnapshotVersion().getTimestamp();
                        // rebuild file name with timestamped version and build number
                        String timeStampFileName = new StringBuilder( artifactId ).append( '-' ) //
                            .append( StringUtils.remove( version, "-" + VersionUtil.SNAPSHOT ) ) //
                            .append( '-' ).append( timeStamp ) //
                            .append( '-' ).append( Integer.toString( buildNumber ) ) //
                            .append( ( StringUtils.isEmpty( classifier ) ? "" : "-" + classifier ) ) //
                            .append( ".jar" ).toString();

                        StorageAsset timeStampFile = file.getStorage().getAsset(file.getParent().getPath() + "/" + timeStampFileName );
                        log.debug( "try to find timestamped snapshot version file: {}", timeStampFile.getPath() );
                        if ( timeStampFile.exists() )
                        {
                            return true;
                        }
                    }
                }

                String path = managedRepositoryContent.toPath( archivaArtifact );

                file = proxyHandler.fetchFromProxies( managedRepositoryContent.getRepository(), path );

                if ( file != null && file.exists() )
                {
                    // download pom now
                    String pomPath = StringUtils.substringBeforeLast( path, ".jar" ) + ".pom";
                    proxyHandler.fetchFromProxies( managedRepositoryContent.getRepository(), pomPath );
                    return true;
                }
            }
        } catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }

        return false;
    }

    @Override
    public Boolean artifactAvailable( String groupId, String artifactId, String version, String repositoryId )
        throws ArchivaRestServiceException
    {
        return artifactAvailable( groupId, artifactId, version, null, repositoryId );
    }

    @Override
    public List<Artifact> getArtifacts( String repositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas = repositorySession.getRepository().getArtifacts(repositorySession , repositoryId );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public List<Artifact> getArtifactsByProjectVersionMetadata( String key, String value, String repositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas = repositorySession.getRepository().getArtifactsByProjectVersionFacet(repositorySession , key, value, repositoryId );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public List<Artifact> getArtifactsByMetadata( String key, String value, String repositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas = repositorySession.getRepository().getArtifactsByAttribute(repositorySession , key, value, repositoryId );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public List<Artifact> getArtifactsByProperty( String key, String value, String repositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas = repositorySession.getRepository().getArtifactsByProjectVersionAttribute(repositorySession , key, value, repositoryId );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public Boolean importMetadata( MetadataAddRequest metadataAddRequest, String repositoryId )
        throws ArchivaRestServiceException
    {
        boolean result = true;
        for ( Map.Entry<String, String> metadata : metadataAddRequest.getMetadatas().entrySet() )
        {
            result = addMetadata( metadataAddRequest.getGroupId(), metadataAddRequest.getArtifactId(),
                                  metadataAddRequest.getVersion(), metadata.getKey(), metadata.getValue(),
                                  repositoryId );
            if ( !result )
            {
                break;
            }
        }
        return result;
    }

    @Override
    public List<Artifact> searchArtifacts( String text, String repositoryId, Boolean exact )
        throws ArchivaRestServiceException
    {
        try(RepositorySession repositorySession = repositorySessionFactory.createSession())
        {
            List<ArtifactMetadata> artifactMetadatas =
                repositorySession.getRepository().searchArtifacts(repositorySession , repositoryId, text, exact == null ? false : exact );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<Artifact> searchArtifacts( String key, String text, String repositoryId, Boolean exact )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas =
                repositorySession.getRepository().searchArtifacts(repositorySession , repositoryId, key, text, exact == null ? false : exact );
            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    //---------------------------
    // internals
    //---------------------------

    private void closeQuietly( JarFile jarFile )
    {
        if ( jarFile != null )
        {
            try
            {
                jarFile.close();
            }
            catch ( IOException e )
            {
                log.warn( "ignore error closing jarFile {}", jarFile.getName() );
            }
        }
    }

    protected List<ArtifactContentEntry> readFileEntries(final StorageAsset file, final String filterPath, final String repoId )
        throws IOException
    {
        String cleanedfilterPath = filterPath==null ? "" : (StringUtils.startsWith(filterPath, "/") ?
                StringUtils.substringAfter(filterPath, "/") : filterPath);
        Map<String, ArtifactContentEntry> artifactContentEntryMap = new HashMap<>();
        int filterDepth = StringUtils.countMatches( cleanedfilterPath, "/" );
        if (!StringUtils.endsWith(cleanedfilterPath,"/") && !StringUtils.isEmpty(cleanedfilterPath)) {
            filterDepth++;
        }

        FsStorageUtil.PathInformation pathInfo = FsStorageUtil.getAssetDataAsPath(file);
        JarFile jarFile = new JarFile(pathInfo.getPath().toFile());
        try
        {
            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
            while ( jarEntryEnumeration.hasMoreElements() )
            {
                JarEntry currentEntry = jarEntryEnumeration.nextElement();
                String cleanedEntryName = StringUtils.endsWith( currentEntry.getName(), "/" ) ? //
                    StringUtils.substringBeforeLast( currentEntry.getName(), "/" ) : currentEntry.getName();
                String entryRootPath = getRootPath( cleanedEntryName );
                int depth = StringUtils.countMatches( cleanedEntryName, "/" );
                if ( StringUtils.isEmpty( cleanedfilterPath ) //
                    && !artifactContentEntryMap.containsKey( entryRootPath ) //
                    && depth == filterDepth )
                {

                    artifactContentEntryMap.put( entryRootPath,
                                                 new ArtifactContentEntry( entryRootPath, !currentEntry.isDirectory(),
                                                                           depth, repoId ) );
                }
                else
                {
                    if ( StringUtils.startsWith( cleanedEntryName, cleanedfilterPath ) //
                        && ( depth == filterDepth || ( !currentEntry.isDirectory() && depth == filterDepth ) ) )
                    {
                        artifactContentEntryMap.put( cleanedEntryName, new ArtifactContentEntry( cleanedEntryName,
                                                                                                 !currentEntry.isDirectory(),
                                                                                                 depth, repoId ) );
                    }
                }
            }

            if ( StringUtils.isNotEmpty( cleanedfilterPath ) )
            {
                Map<String, ArtifactContentEntry> filteredArtifactContentEntryMap = new HashMap<>();

                for ( Map.Entry<String, ArtifactContentEntry> entry : artifactContentEntryMap.entrySet() )
                {
                    filteredArtifactContentEntryMap.put( entry.getKey(), entry.getValue() );
                }

                List<ArtifactContentEntry> sorted = getSmallerDepthEntries( filteredArtifactContentEntryMap );
                if ( sorted == null )
                {
                    return Collections.emptyList();
                }
                Collections.sort( sorted, ArtifactContentEntryComparator.INSTANCE );
                return sorted;
            }
        }
        finally
        {
            if ( jarFile != null )
            {
                jarFile.close();
            }
            if (pathInfo.isTmpFile()) {
                Files.deleteIfExists(pathInfo.getPath());
            }
        }
        List<ArtifactContentEntry> sorted = new ArrayList<>( artifactContentEntryMap.values() );
        Collections.sort( sorted, ArtifactContentEntryComparator.INSTANCE );
        return sorted;
    }

    private List<ArtifactContentEntry> getSmallerDepthEntries( Map<String, ArtifactContentEntry> entries )
    {
        int smallestDepth = Integer.MAX_VALUE;
        Map<Integer, List<ArtifactContentEntry>> perDepthList = new HashMap<>();
        for ( Map.Entry<String, ArtifactContentEntry> entry : entries.entrySet() )
        {

            ArtifactContentEntry current = entry.getValue();

            if ( current.getDepth() < smallestDepth )
            {
                smallestDepth = current.getDepth();
            }

            List<ArtifactContentEntry> currentList = perDepthList.get( current.getDepth() );

            if ( currentList == null )
            {
                currentList = new ArrayList<>();
                currentList.add( current );
                perDepthList.put( current.getDepth(), currentList );
            }
            else
            {
                currentList.add( current );
            }

        }

        return perDepthList.get( smallestDepth );
    }

    /**
     * @param path
     * @return org/apache -&gt; org , org -&gt; org
     */
    private String getRootPath( String path )
    {
        if ( StringUtils.contains( path, '/' ) )
        {
            return StringUtils.substringBefore( path, "/" );
        }
        return path;
    }

    private List<String> getSelectedRepos( String repositoryId )
        throws ArchivaRestServiceException
    {

        List<String> selectedRepos = getObservableRepos();

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return Collections.emptyList();
        }

        if ( StringUtils.isNotEmpty( repositoryId ) )
        {
            // check user has karma on the repository
            if ( !selectedRepos.contains( repositoryId ) )
            {
                throw new ArchivaRestServiceException( "browse.root.groups.repositoy.denied",
                                                       Response.Status.FORBIDDEN.getStatusCode(), null );
            }
            selectedRepos = Collections.singletonList( repositoryId );
        }
        return selectedRepos;
    }


    private String collapseNamespaces( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                       Collection<String> repoIds, String n )
        throws MetadataResolutionException
    {
        Set<String> subNamespaces = new LinkedHashSet<String>();
        for ( String repoId : repoIds )
        {
            subNamespaces.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, n ) );
        }
        if ( subNamespaces.size() != 1 )
        {
            log.debug( "{} is not collapsible as it has sub-namespaces: {}", n, subNamespaces );
            return n;
        }
        else
        {
            for ( String repoId : repoIds )
            {
                Collection<String> projects = metadataResolver.resolveProjects( repositorySession, repoId, n );
                if ( projects != null && !projects.isEmpty() )
                {
                    log.debug( "{} is not collapsible as it has projects", n );
                    return n;
                }
            }
            return collapseNamespaces( repositorySession, metadataResolver, repoIds,
                                       n + "." + subNamespaces.iterator().next() );
        }
    }

    public Cache<String, ProjectVersionMetadata> getVersionMetadataCache()
    {
        return versionMetadataCache;
    }

    public void setVersionMetadataCache( Cache<String, ProjectVersionMetadata> versionMetadataCache )
    {
        this.versionMetadataCache = versionMetadataCache;
    }
}
