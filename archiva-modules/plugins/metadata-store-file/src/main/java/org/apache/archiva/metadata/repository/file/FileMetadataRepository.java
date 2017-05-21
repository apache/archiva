package org.apache.archiva.metadata.repository.file;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class FileMetadataRepository
    implements MetadataRepository
{
    private final Map<String, MetadataFacetFactory> metadataFacetFactories;

    private final ArchivaConfiguration configuration;

    private Logger log = LoggerFactory.getLogger( FileMetadataRepository.class );

    private static final String PROJECT_METADATA_KEY = "project-metadata";

    private static final String PROJECT_VERSION_METADATA_KEY = "version-metadata";

    private static final String NAMESPACE_METADATA_KEY = "namespace-metadata";

    private static final String METADATA_KEY = "metadata";

    public FileMetadataRepository( Map<String, MetadataFacetFactory> metadataFacetFactories,
                                   ArchivaConfiguration configuration )
    {
        this.metadataFacetFactories = metadataFacetFactories;
        this.configuration = configuration;
    }

    private File getBaseDirectory( String repoId )
        throws IOException
    {
        // TODO: should be configurable, like the index
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            configuration.getConfiguration().getManagedRepositoriesAsMap().get( repoId );
        if ( managedRepositoryConfiguration == null )
        {
            return Files.createTempDirectory( repoId ).toFile();
        }
        String basedir = managedRepositoryConfiguration.getLocation();
        return new File( basedir, ".archiva" );
    }

    private File getDirectory( String repoId )
        throws IOException
    {
        return new File( getBaseDirectory( repoId ), "content" );
    }

    @Override
    public void updateProject( String repoId, ProjectMetadata project )
    {
        updateProject( repoId, project.getNamespace(), project.getId() );
    }

    private void updateProject( String repoId, String namespace, String id )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        updateNamespace( repoId, namespace );

        try
        {
            File namespaceDirectory = new File( getDirectory( repoId ), namespace );
            Properties properties = new Properties();
            properties.setProperty( "namespace", namespace );
            properties.setProperty( "id", id );
            writeProperties( properties, new File( namespaceDirectory, id ), PROJECT_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO!
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {

        try
        {
            updateProject( repoId, namespace, projectId );

            File directory =
                new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + versionMetadata.getId() );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
            // remove properties that are not references or artifacts
            for ( Object key : new ArrayList( properties.keySet() ) )
            {
                String name = (String) key;
                if ( !name.contains( ":" ) && !name.equals( "facetIds" ) )
                {
                    properties.remove( name );
                }

                // clear the facet contents so old properties are no longer written
                clearMetadataFacetProperties( versionMetadata.getFacetList(), properties, "" );
            }
            properties.setProperty( "id", versionMetadata.getId() );
            setProperty( properties, "name", versionMetadata.getName() );
            setProperty( properties, "description", versionMetadata.getDescription() );
            setProperty( properties, "url", versionMetadata.getUrl() );
            setProperty( properties, "incomplete", String.valueOf( versionMetadata.isIncomplete() ) );
            if ( versionMetadata.getScm() != null )
            {
                setProperty( properties, "scm.connection", versionMetadata.getScm().getConnection() );
                setProperty( properties, "scm.developerConnection", versionMetadata.getScm().getDeveloperConnection() );
                setProperty( properties, "scm.url", versionMetadata.getScm().getUrl() );
            }
            if ( versionMetadata.getCiManagement() != null )
            {
                setProperty( properties, "ci.system", versionMetadata.getCiManagement().getSystem() );
                setProperty( properties, "ci.url", versionMetadata.getCiManagement().getUrl() );
            }
            if ( versionMetadata.getIssueManagement() != null )
            {
                setProperty( properties, "issue.system", versionMetadata.getIssueManagement().getSystem() );
                setProperty( properties, "issue.url", versionMetadata.getIssueManagement().getUrl() );
            }
            if ( versionMetadata.getOrganization() != null )
            {
                setProperty( properties, "org.name", versionMetadata.getOrganization().getName() );
                setProperty( properties, "org.url", versionMetadata.getOrganization().getUrl() );
            }
            int i = 0;
            for ( License license : versionMetadata.getLicenses() )
            {
                setProperty( properties, "license." + i + ".name", license.getName() );
                setProperty( properties, "license." + i + ".url", license.getUrl() );
                i++;
            }
            i = 0;
            for ( MailingList mailingList : versionMetadata.getMailingLists() )
            {
                setProperty( properties, "mailingList." + i + ".archive", mailingList.getMainArchiveUrl() );
                setProperty( properties, "mailingList." + i + ".name", mailingList.getName() );
                setProperty( properties, "mailingList." + i + ".post", mailingList.getPostAddress() );
                setProperty( properties, "mailingList." + i + ".unsubscribe", mailingList.getUnsubscribeAddress() );
                setProperty( properties, "mailingList." + i + ".subscribe", mailingList.getSubscribeAddress() );
                setProperty( properties, "mailingList." + i + ".otherArchives",
                             join( mailingList.getOtherArchives() ) );
                i++;
            }
            i = 0;
            ProjectVersionReference reference = new ProjectVersionReference();
            reference.setNamespace( namespace );
            reference.setProjectId( projectId );
            reference.setProjectVersion( versionMetadata.getId() );
            reference.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );
            for ( Dependency dependency : versionMetadata.getDependencies() )
            {
                setProperty( properties, "dependency." + i + ".classifier", dependency.getClassifier() );
                setProperty( properties, "dependency." + i + ".scope", dependency.getScope() );
                setProperty( properties, "dependency." + i + ".systemPath", dependency.getSystemPath() );
                setProperty( properties, "dependency." + i + ".artifactId", dependency.getArtifactId() );
                setProperty( properties, "dependency." + i + ".groupId", dependency.getGroupId() );
                setProperty( properties, "dependency." + i + ".version", dependency.getVersion() );
                setProperty( properties, "dependency." + i + ".type", dependency.getType() );
                setProperty( properties, "dependency." + i + ".optional", String.valueOf( dependency.isOptional() ) );

                updateProjectReference( repoId, dependency.getGroupId(), dependency.getArtifactId(),
                                        dependency.getVersion(), reference );

                i++;
            }
            Set<String> facetIds = new LinkedHashSet<String>( versionMetadata.getFacetIds() );
            facetIds.addAll( Arrays.asList( properties.getProperty( "facetIds", "" ).split( "," ) ) );
            properties.setProperty( "facetIds", join( facetIds ) );

            updateProjectVersionFacets( versionMetadata, properties );

            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            log.error( e.getMessage(), e );
        }
    }

    private void updateProjectVersionFacets( ProjectVersionMetadata versionMetadata, Properties properties )
    {
        for ( MetadataFacet facet : versionMetadata.getFacetList() )
        {
            for ( Map.Entry<String, String> entry : facet.toProperties().entrySet() )
            {
                properties.setProperty( facet.getFacetId() + ":" + entry.getKey(), entry.getValue() );
            }
        }
    }

    private static void clearMetadataFacetProperties( Collection<MetadataFacet> facetList, Properties properties,
                                                      String prefix )
    {
        List<Object> propsToRemove = new ArrayList<>();
        for ( MetadataFacet facet : facetList )
        {
            for ( Object key : new ArrayList( properties.keySet() ) )
            {
                String keyString = (String) key;
                if ( keyString.startsWith( prefix + facet.getFacetId() + ":" ) )
                {
                    propsToRemove.add( key );
                }
            }
        }

        for ( Object key : propsToRemove )
        {
            properties.remove( key );
        }
    }

    private void updateProjectReference( String repoId, String namespace, String projectId, String projectVersion,
                                         ProjectVersionReference reference )
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
            int i = Integer.parseInt( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;
            setProperty( properties, "ref:lastReferenceNum", Integer.toString( i ) );
            setProperty( properties, "ref:reference." + i + ".namespace", reference.getNamespace() );
            setProperty( properties, "ref:reference." + i + ".projectId", reference.getProjectId() );
            setProperty( properties, "ref:reference." + i + ".projectVersion", reference.getProjectVersion() );
            setProperty( properties, "ref:reference." + i + ".referenceType", reference.getReferenceType().toString() );

            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public void updateNamespace( String repoId, String namespace )
    {
        try
        {
            File namespaceDirectory = new File( getDirectory( repoId ), namespace );
            Properties properties = new Properties();
            properties.setProperty( "namespace", namespace );
            writeProperties( properties, namespaceDirectory, NAMESPACE_METADATA_KEY );

        }
        catch ( IOException e )
        {
            // TODO!
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public List<String> getMetadataFacets( String repoId, String facetId )
        throws MetadataRepositoryException
    {
        try
        {
            File directory = getMetadataDirectory( repoId, facetId );
            List<String> facets = new ArrayList<>();
            recurse( facets, "", directory );
            return facets;
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public boolean hasMetadataFacet( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        // TODO could be improved a bit
        return !getMetadataFacets( repositoryId, facetId ).isEmpty();
    }

    private void recurse( List<String> facets, String prefix, File directory )
    {
        File[] list = directory.listFiles();
        if ( list != null )
        {
            for ( File dir : list )
            {
                if ( dir.isDirectory() )
                {
                    recurse( facets, prefix + "/" + dir.getName(), dir );
                }
                else if ( dir.getName().equals( METADATA_KEY + ".properties" ) )
                {
                    facets.add( prefix.substring( 1 ) );
                }
            }
        }
    }

    @Override
    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
    {
        Properties properties;
        try
        {
            properties =
                readProperties( new File( getMetadataDirectory( repositoryId, facetId ), name ), METADATA_KEY );
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
        catch ( IOException e )
        {
            // TODO
            log.error( e.getMessage(), e );
            return null;
        }
        MetadataFacet metadataFacet = null;
        MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( facetId );
        if ( metadataFacetFactory != null )
        {
            metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
            Map<String, String> map = new HashMap<>();
            for ( Object key : new ArrayList( properties.keySet() ) )
            {
                String property = (String) key;
                map.put( property, properties.getProperty( property ) );
            }
            metadataFacet.fromProperties( map );
        }
        return metadataFacet;
    }

    @Override
    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        Properties properties = new Properties();
        properties.putAll( metadataFacet.toProperties() );

        try
        {
            File directory =
                new File( getMetadataDirectory( repositoryId, metadataFacet.getFacetId() ), metadataFacet.getName() );
            writeProperties( properties, directory, METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO!
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public void removeMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        try
        {
            File dir = getMetadataDirectory( repositoryId, facetId );
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeMetadataFacet( String repoId, String facetId, String name )
        throws MetadataRepositoryException
    {
        try
        {
            File dir = new File( getMetadataDirectory( repoId, facetId ), name );
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        try
        {
            // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
            //  of this information (eg. in Lucene, as before)

            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for ( String ns : getRootNamespaces( repoId ) )
            {
                getArtifactsByDateRange( artifacts, repoId, ns, startTime, endTime );
            }
            Collections.sort( artifacts, new ArtifactComparator() );
            return artifacts;
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void getArtifactsByDateRange( List<ArtifactMetadata> artifacts, String repoId, String ns, Date startTime,
                                          Date endTime )
        throws MetadataRepositoryException
    {
        try
        {
            for ( String namespace : getNamespaces( repoId, ns ) )
            {
                getArtifactsByDateRange( artifacts, repoId, ns + "." + namespace, startTime, endTime );
            }

            for ( String project : getProjects( repoId, ns ) )
            {
                for ( String version : getProjectVersions( repoId, ns, project ) )
                {
                    for ( ArtifactMetadata artifact : getArtifacts( repoId, ns, project, version ) )
                    {
                        if ( startTime == null || startTime.before( artifact.getWhenGathered() ) )
                        {
                            if ( endTime == null || endTime.after( artifact.getWhenGathered() ) )
                            {
                                artifacts.add( artifact );
                            }
                        }
                    }
                }
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            Map<String, ArtifactMetadata> artifacts = new HashMap<>();

            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

            for ( Map.Entry entry : properties.entrySet() )
            {
                String name = (String) entry.getKey();
                StringTokenizer tok = new StringTokenizer( name, ":" );
                if ( tok.hasMoreTokens() && "artifact".equals( tok.nextToken() ) )
                {
                    String field = tok.nextToken();
                    String id = tok.nextToken();

                    ArtifactMetadata artifact = artifacts.get( id );
                    if ( artifact == null )
                    {
                        artifact = new ArtifactMetadata();
                        artifact.setRepositoryId( repoId );
                        artifact.setNamespace( namespace );
                        artifact.setProject( projectId );
                        artifact.setProjectVersion( projectVersion );
                        artifact.setVersion( projectVersion );
                        artifact.setId( id );
                        artifacts.put( id, artifact );
                    }

                    String value = (String) entry.getValue();
                    if ( "updated".equals( field ) )
                    {
                        artifact.setFileLastModified( Long.parseLong( value ) );
                    }
                    else if ( "size".equals( field ) )
                    {
                        artifact.setSize( Long.valueOf( value ) );
                    }
                    else if ( "whenGathered".equals( field ) )
                    {
                        artifact.setWhenGathered( new Date( Long.parseLong( value ) ) );
                    }
                    else if ( "version".equals( field ) )
                    {
                        artifact.setVersion( value );
                    }
                    else if ( "md5".equals( field ) )
                    {
                        artifact.setMd5( value );
                    }
                    else if ( "sha1".equals( field ) )
                    {
                        artifact.setSha1( value );
                    }
                    else if ( "facetIds".equals( field ) )
                    {
                        if ( value.length() > 0 )
                        {
                            String propertyPrefix = "artifact:facet:" + id + ":";
                            for ( String facetId : value.split( "," ) )
                            {
                                MetadataFacetFactory factory = metadataFacetFactories.get( facetId );
                                if ( factory == null )
                                {
                                    log.error( "Attempted to load unknown artifact metadata facet: " + facetId );
                                }
                                else
                                {
                                    MetadataFacet facet = factory.createMetadataFacet();
                                    String prefix = propertyPrefix + facet.getFacetId();
                                    Map<String, String> map = new HashMap<>();
                                    for ( Object key : new ArrayList( properties.keySet() ) )
                                    {
                                        String property = (String) key;
                                        if ( property.startsWith( prefix ) )
                                        {
                                            map.put( property.substring( prefix.length() + 1 ),
                                                     properties.getProperty( property ) );
                                        }
                                    }
                                    facet.fromProperties( map );
                                    artifact.addFacet( facet );
                                }
                            }
                        }

                        updateArtifactFacets( artifact, properties );
                    }
                }
            }
            return artifacts.values();
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public void save()
    {
        // it's all instantly persisted
    }

    @Override
    public void close()
    {
        // nothing additional to close
    }

    @Override
    public void revert()
    {
        log.warn( "Attempted to revert a session, but the file-based repository storage doesn't support it" );
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        return false;
    }

    @Override
    public <T> T obtainAccess( Class<T> aClass )
    {
        throw new IllegalArgumentException(
            "Access using " + aClass + " is not supported on the file metadata storage" );
    }

    private void updateArtifactFacets( ArtifactMetadata artifact, Properties properties )
    {
        String propertyPrefix = "artifact:facet:" + artifact.getId() + ":";
        for ( MetadataFacet facet : artifact.getFacetList() )
        {
            for ( Map.Entry<String, String> e : facet.toProperties().entrySet() )
            {
                String key = propertyPrefix + facet.getFacetId() + ":" + e.getKey();
                properties.setProperty( key, e.getValue() );
            }
        }
    }

    @Override
    public Collection<String> getRepositories()
    {
        List<String> repositories = new ArrayList<>();
        for ( ManagedRepositoryConfiguration managedRepositoryConfiguration : configuration.getConfiguration().getManagedRepositories() )
        {
            repositories.add( managedRepositoryConfiguration.getId() );
        }
        return repositories;
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
        throws MetadataRepositoryException
    {
        try
        {
            // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
            //  of this information (eg. in Lucene, as before)
            // alternatively, we could build a referential tree in the content repository, however it would need some levels
            // of depth to avoid being too broad to be useful (eg. /repository/checksums/a/ab/abcdef1234567)

            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for ( String ns : getRootNamespaces( repositoryId ) )
            {
                getArtifactsByChecksum( artifacts, repositoryId, ns, checksum );
            }
            return artifacts;
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeNamespace( String repositoryId, String project )
        throws MetadataRepositoryException
    {
        try
        {
            File namespaceDirectory = new File( getDirectory( repositoryId ), project );
            FileUtils.deleteDirectory( namespaceDirectory );
            //Properties properties = new Properties();
            //properties.setProperty( "namespace", namespace );
            //writeProperties( properties, namespaceDirectory, NAMESPACE_METADATA_KEY );

        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeArtifact( ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {

        try
        {
            File directory = new File( getDirectory( artifactMetadata.getRepositoryId() ),
                                       artifactMetadata.getNamespace() + "/" + artifactMetadata.getProject() + "/"
                                           + baseVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

            String id = artifactMetadata.getId();

            properties.remove( "artifact:updated:" + id );
            properties.remove( "artifact:whenGathered:" + id );
            properties.remove( "artifact:size:" + id );
            properties.remove( "artifact:md5:" + id );
            properties.remove( "artifact:sha1:" + id );
            properties.remove( "artifact:version:" + id );
            properties.remove( "artifact:facetIds:" + id );

            String prefix = "artifact:facet:" + id + ":";
            for ( Object key : new ArrayList( properties.keySet() ) )
            {
                String property = (String) key;
                if ( property.startsWith( prefix ) )
                {
                    properties.remove( property );
                }
            }

            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }

    @Override
    public void removeArtifact( String repoId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + project + "/" + version );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

            properties.remove( "artifact:updated:" + id );
            properties.remove( "artifact:whenGathered:" + id );
            properties.remove( "artifact:size:" + id );
            properties.remove( "artifact:md5:" + id );
            properties.remove( "artifact:sha1:" + id );
            properties.remove( "artifact:version:" + id );
            properties.remove( "artifact:facetIds:" + id );

            String prefix = "artifact:facet:" + id + ":";
            for ( Object key : new ArrayList( properties.keySet() ) )
            {
                String property = (String) key;
                if ( property.startsWith( prefix ) )
                {
                    properties.remove( property );
                }
            }

            FileUtils.deleteDirectory( directory );
            //writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    /**
     * FIXME implements this !!!!
     *
     * @param repositoryId
     * @param namespace
     * @param project
     * @param projectVersion
     * @param metadataFacet  will remove artifacts which have this {@link MetadataFacet} using equals
     * @throws MetadataRepositoryException
     */
    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String projectVersion,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    @Override
    public void removeRepository( String repoId )
        throws MetadataRepositoryException
    {
        try
        {
            File dir = getDirectory( repoId );
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void getArtifactsByChecksum( List<ArtifactMetadata> artifacts, String repositoryId, String ns,
                                         String checksum )
        throws MetadataRepositoryException
    {
        try
        {
            for ( String namespace : getNamespaces( repositoryId, ns ) )
            {
                getArtifactsByChecksum( artifacts, repositoryId, ns + "." + namespace, checksum );
            }

            for ( String project : getProjects( repositoryId, ns ) )
            {
                for ( String version : getProjectVersions( repositoryId, ns, project ) )
                {
                    for ( ArtifactMetadata artifact : getArtifacts( repositoryId, ns, project, version ) )
                    {
                        if ( checksum.equals( artifact.getMd5() ) || checksum.equals( artifact.getSha1() ) )
                        {
                            artifacts.add( artifact );
                        }
                    }
                }
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "not yet implemented in File backend" );
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByMetadata( String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "not yet implemented in File backend" );
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProperty( String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "getArtifactsByProperty not yet implemented in File backend" );
    }

    private File getMetadataDirectory( String repoId, String facetId )
        throws IOException
    {
        return new File( getBaseDirectory( repoId ), "facets/" + facetId );
    }

    private String join( Collection<String> ids )
    {
        if ( ids != null && !ids.isEmpty() )
        {
            StringBuilder s = new StringBuilder();
            for ( String id : ids )
            {
                s.append( id );
                s.append( "," );
            }
            return s.substring( 0, s.length() - 1 );
        }
        return "";
    }

    private void setProperty( Properties properties, String name, String value )
    {
        if ( value != null )
        {
            properties.setProperty( name, value );
        }
    }

    @Override
    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifact )
    {
        try
        {
            ProjectVersionMetadata metadata = new ProjectVersionMetadata();
            metadata.setId( projectVersion );
            updateProjectVersion( repoId, namespace, projectId, metadata );

            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

            clearMetadataFacetProperties( artifact.getFacetList(), properties,
                                          "artifact:facet:" + artifact.getId() + ":" );

            String id = artifact.getId();
            properties.setProperty( "artifact:updated:" + id,
                                    Long.toString( artifact.getFileLastModified().getTime() ) );
            properties.setProperty( "artifact:whenGathered:" + id,
                                    Long.toString( artifact.getWhenGathered().getTime() ) );
            properties.setProperty( "artifact:size:" + id, Long.toString( artifact.getSize() ) );
            if ( artifact.getMd5() != null )
            {
                properties.setProperty( "artifact:md5:" + id, artifact.getMd5() );
            }
            if ( artifact.getSha1() != null )
            {
                properties.setProperty( "artifact:sha1:" + id, artifact.getSha1() );
            }
            properties.setProperty( "artifact:version:" + id, artifact.getVersion() );

            Set<String> facetIds = new LinkedHashSet<String>( artifact.getFacetIds() );
            String property = "artifact:facetIds:" + id;
            facetIds.addAll( Arrays.asList( properties.getProperty( property, "" ).split( "," ) ) );
            properties.setProperty( property, join( facetIds ) );

            updateArtifactFacets( artifact, properties );

            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            log.error( e.getMessage(), e );
        }
    }

    private Properties readOrCreateProperties( File directory, String propertiesKey )
    {
        try
        {
            return readProperties( directory, propertiesKey );
        }
        catch ( FileNotFoundException | NoSuchFileException e )
        {
            // ignore and return new properties
        }
        catch ( IOException e )
        {
            // TODO
            log.error( e.getMessage(), e );
        }
        return new Properties();
    }

    private Properties readProperties( File directory, String propertiesKey )
        throws IOException
    {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream( new File( directory, propertiesKey + ".properties" ).toPath() ))
        {

            properties.load( in );
        }
        return properties;
    }

    @Override
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId );

            Properties properties = readOrCreateProperties( directory, PROJECT_METADATA_KEY );

            ProjectMetadata project = null;

            String id = properties.getProperty( "id" );
            if ( id != null )
            {
                project = new ProjectMetadata();
                project.setNamespace( properties.getProperty( "namespace" ) );
                project.setId( id );
            }

            return project;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
            String id = properties.getProperty( "id" );
            ProjectVersionMetadata versionMetadata = null;
            if ( id != null )
            {
                versionMetadata = new ProjectVersionMetadata();
                versionMetadata.setId( id );
                versionMetadata.setName( properties.getProperty( "name" ) );
                versionMetadata.setDescription( properties.getProperty( "description" ) );
                versionMetadata.setUrl( properties.getProperty( "url" ) );
                versionMetadata.setIncomplete( Boolean.valueOf( properties.getProperty( "incomplete", "false" ) ) );

                String scmConnection = properties.getProperty( "scm.connection" );
                String scmDeveloperConnection = properties.getProperty( "scm.developerConnection" );
                String scmUrl = properties.getProperty( "scm.url" );
                if ( scmConnection != null || scmDeveloperConnection != null || scmUrl != null )
                {
                    Scm scm = new Scm();
                    scm.setConnection( scmConnection );
                    scm.setDeveloperConnection( scmDeveloperConnection );
                    scm.setUrl( scmUrl );
                    versionMetadata.setScm( scm );
                }

                String ciSystem = properties.getProperty( "ci.system" );
                String ciUrl = properties.getProperty( "ci.url" );
                if ( ciSystem != null || ciUrl != null )
                {
                    CiManagement ci = new CiManagement();
                    ci.setSystem( ciSystem );
                    ci.setUrl( ciUrl );
                    versionMetadata.setCiManagement( ci );
                }

                String issueSystem = properties.getProperty( "issue.system" );
                String issueUrl = properties.getProperty( "issue.url" );
                if ( issueSystem != null || issueUrl != null )
                {
                    IssueManagement issueManagement = new IssueManagement();
                    issueManagement.setSystem( issueSystem );
                    issueManagement.setUrl( issueUrl );
                    versionMetadata.setIssueManagement( issueManagement );
                }

                String orgName = properties.getProperty( "org.name" );
                String orgUrl = properties.getProperty( "org.url" );
                if ( orgName != null || orgUrl != null )
                {
                    Organization org = new Organization();
                    org.setName( orgName );
                    org.setUrl( orgUrl );
                    versionMetadata.setOrganization( org );
                }

                boolean done = false;
                int i = 0;
                while ( !done )
                {
                    String licenseName = properties.getProperty( "license." + i + ".name" );
                    String licenseUrl = properties.getProperty( "license." + i + ".url" );
                    if ( licenseName != null || licenseUrl != null )
                    {
                        License license = new License();
                        license.setName( licenseName );
                        license.setUrl( licenseUrl );
                        versionMetadata.addLicense( license );
                    }
                    else
                    {
                        done = true;
                    }
                    i++;
                }

                done = false;
                i = 0;
                while ( !done )
                {
                    String mailingListName = properties.getProperty( "mailingList." + i + ".name" );
                    if ( mailingListName != null )
                    {
                        MailingList mailingList = new MailingList();
                        mailingList.setName( mailingListName );
                        mailingList.setMainArchiveUrl( properties.getProperty( "mailingList." + i + ".archive" ) );
                        String p = properties.getProperty( "mailingList." + i + ".otherArchives" );
                        if ( p != null && p.length() > 0 )
                        {
                            mailingList.setOtherArchives( Arrays.asList( p.split( "," ) ) );
                        }
                        else
                        {
                            mailingList.setOtherArchives( Collections.<String>emptyList() );
                        }
                        mailingList.setPostAddress( properties.getProperty( "mailingList." + i + ".post" ) );
                        mailingList.setSubscribeAddress( properties.getProperty( "mailingList." + i + ".subscribe" ) );
                        mailingList.setUnsubscribeAddress(
                            properties.getProperty( "mailingList." + i + ".unsubscribe" ) );
                        versionMetadata.addMailingList( mailingList );
                    }
                    else
                    {
                        done = true;
                    }
                    i++;
                }

                done = false;
                i = 0;
                while ( !done )
                {
                    String dependencyArtifactId = properties.getProperty( "dependency." + i + ".artifactId" );
                    if ( dependencyArtifactId != null )
                    {
                        Dependency dependency = new Dependency();
                        dependency.setArtifactId( dependencyArtifactId );
                        dependency.setGroupId( properties.getProperty( "dependency." + i + ".groupId" ) );
                        dependency.setClassifier( properties.getProperty( "dependency." + i + ".classifier" ) );
                        dependency.setOptional(
                            Boolean.valueOf( properties.getProperty( "dependency." + i + ".optional" ) ) );
                        dependency.setScope( properties.getProperty( "dependency." + i + ".scope" ) );
                        dependency.setSystemPath( properties.getProperty( "dependency." + i + ".systemPath" ) );
                        dependency.setType( properties.getProperty( "dependency." + i + ".type" ) );
                        dependency.setVersion( properties.getProperty( "dependency." + i + ".version" ) );
                        dependency.setOptional(
                            Boolean.valueOf( properties.getProperty( "dependency." + i + ".optional" ) ) );
                        versionMetadata.addDependency( dependency );
                    }
                    else
                    {
                        done = true;
                    }
                    i++;
                }

                String facetIds = properties.getProperty( "facetIds", "" );
                if ( facetIds.length() > 0 )
                {
                    for ( String facetId : facetIds.split( "," ) )
                    {
                        MetadataFacetFactory factory = metadataFacetFactories.get( facetId );
                        if ( factory == null )
                        {
                            log.error( "Attempted to load unknown project version metadata facet: {}", facetId );
                        }
                        else
                        {
                            MetadataFacet facet = factory.createMetadataFacet();
                            Map<String, String> map = new HashMap<>();
                            for ( Object key : new ArrayList( properties.keySet() ) )
                            {
                                String property = (String) key;
                                if ( property.startsWith( facet.getFacetId() ) )
                                {
                                    map.put( property.substring( facet.getFacetId().length() + 1 ),
                                             properties.getProperty( property ) );
                                }
                            }
                            facet.fromProperties( map );
                            versionMetadata.addFacet( facet );
                        }
                    }
                }

                updateProjectVersionFacets( versionMetadata, properties );
            }
            return versionMetadata;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

            Set<String> versions = new HashSet<String>();
            for ( Map.Entry entry : properties.entrySet() )
            {
                String name = (String) entry.getKey();
                if ( name.startsWith( "artifact:version:" ) )
                {
                    versions.add( (String) entry.getValue() );
                }
            }
            return versions;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );

            Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
            int numberOfRefs = Integer.parseInt( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;

            List<ProjectVersionReference> references = new ArrayList<>();
            for ( int i = 0; i < numberOfRefs; i++ )
            {
                ProjectVersionReference reference = new ProjectVersionReference();
                reference.setProjectId( properties.getProperty( "ref:reference." + i + ".projectId" ) );
                reference.setNamespace( properties.getProperty( "ref:reference." + i + ".namespace" ) );
                reference.setProjectVersion( properties.getProperty( "ref:reference." + i + ".projectVersion" ) );
                reference.setReferenceType( ProjectVersionReference.ReferenceType.valueOf(
                    properties.getProperty( "ref:reference." + i + ".referenceType" ) ) );
                references.add( reference );
            }
            return references;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getRootNamespaces( String repoId )
        throws MetadataResolutionException
    {
        return getNamespaces( repoId, null );
    }

    @Override
    public Collection<String> getNamespaces( String repoId, String baseNamespace )
        throws MetadataResolutionException
    {
        try
        {
            List<String> allNamespaces = new ArrayList<>();
            File directory = getDirectory( repoId );
            File[] files = directory.listFiles();
            if ( files != null )
            {
                for ( File namespace : files )
                {
                    if ( new File( namespace, NAMESPACE_METADATA_KEY + ".properties" ).exists() )
                    {
                        allNamespaces.add( namespace.getName() );
                    }
                }
            }

            Set<String> namespaces = new LinkedHashSet<>();
            int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
            for ( String namespace : allNamespaces )
            {
                if ( baseNamespace == null || namespace.startsWith( baseNamespace + "." ) )
                {
                    int i = namespace.indexOf( '.', fromIndex );
                    if ( i >= 0 )
                    {
                        namespaces.add( namespace.substring( fromIndex, i ) );
                    }
                    else
                    {
                        namespaces.add( namespace.substring( fromIndex ) );
                    }
                }
            }
            return new ArrayList<>( namespaces );
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getProjects( String repoId, String namespace )
        throws MetadataResolutionException
    {
        try
        {
            List<String> projects = new ArrayList<>();
            File directory = new File( getDirectory( repoId ), namespace );
            File[] files = directory.listFiles();
            if ( files != null )
            {
                for ( File project : files )
                {
                    if ( new File( project, PROJECT_METADATA_KEY + ".properties" ).exists() )
                    {
                        projects.add( project.getName() );
                    }
                }
            }
            return projects;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        try
        {
            List<String> projectVersions = new ArrayList<>();
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId );
            File[] files = directory.listFiles();
            if ( files != null )
            {
                for ( File projectVersion : files )
                {
                    if ( new File( projectVersion, PROJECT_VERSION_METADATA_KEY + ".properties" ).exists() )
                    {
                        projectVersions.add( projectVersion.getName() );
                    }
                }
            }
            return projectVersions;
        }
        catch ( IOException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public void removeProject( String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        try
        {
            File directory = new File( getDirectory( repositoryId ), namespace + "/" + projectId );
            FileUtils.deleteDirectory( directory );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeProjectVersion( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException
    {
        try
        {
            File directory = new File( getDirectory( repoId ), namespace + "/" + projectId + "/" + projectVersion );
            FileUtils.deleteDirectory( directory );
        }
        catch ( IOException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }

    private void writeProperties( Properties properties, File directory, String propertiesKey )
        throws IOException
    {
        directory.mkdirs();
        try (OutputStream os = Files.newOutputStream( new File( directory, propertiesKey + ".properties" ).toPath() ))
        {
            properties.store( os, null );
        }
    }

    private static class ArtifactComparator
        implements Comparator<ArtifactMetadata>
    {
        @Override
        public int compare( ArtifactMetadata artifact1, ArtifactMetadata artifact2 )
        {
            if ( artifact1.getWhenGathered() == artifact2.getWhenGathered() )
            {
                return 0;
            }
            if ( artifact1.getWhenGathered() == null )
            {
                return 1;
            }
            if ( artifact2.getWhenGathered() == null )
            {
                return -1;
            }
            return artifact1.getWhenGathered().compareTo( artifact2.getWhenGathered() );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( String repoId )
        throws MetadataRepositoryException
    {
        try
        {
            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for ( String ns : getRootNamespaces( repoId ) )
            {
                getArtifacts( artifacts, repoId, ns );
            }
            return artifacts;
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void getArtifacts( List<ArtifactMetadata> artifacts, String repoId, String ns )
        throws MetadataResolutionException
    {
        for ( String namespace : getNamespaces( repoId, ns ) )
        {
            getArtifacts( artifacts, repoId, ns + "." + namespace );
        }

        for ( String project : getProjects( repoId, ns ) )
        {
            for ( String version : getProjectVersions( repoId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repoId, ns, project, version ) )
                {
                    artifacts.add( artifact );
                }
            }
        }
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "searchArtifacts not yet implemented in File backend" );
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( String key, String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException( "searchArtifacts not yet implemented in File backend" );
    }
}
