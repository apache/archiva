package org.apache.archiva.consumers.metadata;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.metadata.repository.storage.ReadMetadataRequest;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageRuntimeException;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.repository.ManagedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Take an artifact off of disk and put it into the metadata repository.
 */
@Service ("knownRepositoryContentConsumer#create-archiva-metadata")
@Scope ("prototype")
public class ArchivaMetadataCreationConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    private String id = "create-archiva-metadata";

    private String description = "Create basic metadata for Archiva to be able to reference the artifact";

    @Inject
    private ArchivaConfiguration configuration;

    @Inject
    private FileTypes filetypes;

    private ZonedDateTime whenGathered;

    private List<String> includes = new ArrayList<>( 0 );

    /**
     * FIXME: this could be multiple implementations and needs to be configured.
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    /**
     * FIXME: this needs to be configurable based on storage type - and could also be instantiated per repo. Change to a
     * factory.
     */
    @Inject
    @Named (value = "repositoryStorage#maven2")
    private RepositoryStorage repositoryStorage;

    private static final Logger log = LoggerFactory.getLogger( ArchivaMetadataCreationConsumer.class );

    private String repoId;

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public List<String> getExcludes()
    {
        return getDefaultArtifactExclusions();
    }

    @Override
    public List<String> getIncludes()
    {
        return this.includes;
    }

    @Override
    public void beginScan( ManagedRepository repo, Date whenGathered )
        throws ConsumerException
    {
        repoId = repo.getId();
        this.whenGathered = ZonedDateTime.ofInstant(whenGathered.toInstant(), ZoneId.of("GMT"));
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
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
            // note that we do minimal processing including checksums and POM information for performance of
            // the initial scan. Any request for this information will be intercepted and populated on-demand
            // or picked up by subsequent scans

            ArtifactMetadata artifact = repositoryStorage.readArtifactMetadataFromPath( repoId, path );

            ProjectMetadata project = new ProjectMetadata();
            project.setNamespace( artifact.getNamespace() );
            project.setId( artifact.getProject() );

            String projectVersion = VersionUtil.getBaseVersion( artifact.getVersion() );

            MetadataRepository metadataRepository = repositorySession.getRepository();

            boolean createVersionMetadata = false;

            // FIXME: maybe not too efficient since it may have already been read and stored for this artifact
            ProjectVersionMetadata versionMetadata = null;
            try
            {
                ReadMetadataRequest readMetadataRequest =
                    new ReadMetadataRequest().repositoryId( repoId ).namespace( artifact.getNamespace() ).projectId(
                        artifact.getProject() ).projectVersion( projectVersion );
                versionMetadata = repositoryStorage.readProjectVersionMetadata( readMetadataRequest );
                createVersionMetadata = true;
            }
            catch ( RepositoryStorageMetadataNotFoundException e )
            {
                log.warn( "Missing or invalid POM for artifact:{} (repository:{}); creating empty metadata", path,
                          repoId );

                versionMetadata = new ProjectVersionMetadata();
                versionMetadata.setId( projectVersion );
                versionMetadata.setIncomplete( true );
                createVersionMetadata = true;
            }
            catch ( RepositoryStorageMetadataInvalidException e )
            {
                log.warn( "Error occurred resolving POM for artifact:{} (repository:{}); message: {}",
                          new Object[]{ path, repoId, e.getMessage() } );
            }

            // read the metadata and update it if it is newer or doesn't exist
            artifact.setWhenGathered( whenGathered );
            metadataRepository.updateArtifact(repositorySession , repoId, project.getNamespace(), project.getId(),
                projectVersion, artifact );
            if ( createVersionMetadata )
            {
                metadataRepository.updateProjectVersion(repositorySession , repoId, project.getNamespace(),
                    project.getId(), versionMetadata );
            }
            metadataRepository.updateProject(repositorySession , repoId, project );
            repositorySession.save();
        }
        catch ( MetadataRepositoryException e )
        {
            log.warn(
                "Error occurred persisting metadata for artifact:{} (repository:{}); message: {}" ,
                path, repoId, e.getMessage(), e );
            try {
                repositorySession.revert();
            } catch (MetadataSessionException ex) {
                log.error("Reverting failed {}", ex.getMessage());
            }
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            log.warn(
                "Error occurred persisting metadata for artifact:{} (repository:{}); message: {}",
                path, repoId, e.getMessage(), e );
            try {
                repositorySession.revert();
            } catch (MetadataSessionException ex) {
                log.error("Reverting failed {}", ex.getMessage());
            }
        } catch (MetadataSessionException e) {
            throw new ConsumerException(e.getMessage(), e);
        } finally
        {
            repositorySession.close();
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    @Override
    public void completeScan()
    {
        /* do nothing */
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes = new ArrayList<String>( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    @PostConstruct
    public void initialize()
    {
        configuration.addChangeListener( this );

        initIncludes();
    }
}
