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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.storage.StorageMetadataResolver;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Take an artifact off of disk and put it into the metadata repository.
 *
 * @version $Id: ArtifactUpdateDatabaseConsumer.java 718864 2008-11-19 06:33:35Z brett $
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="create-archiva-metadata" instantiation-strategy="per-lookup"
 */
public class ArchivaMetadataCreationConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    /**
     * @plexus.configuration default-value="create-archiva-metadata"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Create basic metadata for Archiva to be able to reference the artifact"
     */
    private String description;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    private Date whenGathered;

    private List<String> includes = new ArrayList<String>();

    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    /**
     * FIXME: this needs to be configurable based on storage type
     *
     * @plexus.requirement role-hint="maven2"
     */
    private StorageMetadataResolver storageResolver;

    private static final Logger log = LoggerFactory.getLogger( ArchivaMetadataCreationConsumer.class );

    private String repoId;

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public boolean isPermanent()
    {
        return true;
    }

    public List<String> getExcludes()
    {
        return getDefaultArtifactExclusions();
    }

    public List<String> getIncludes()
    {
        return this.includes;
    }

    public void beginScan( ManagedRepositoryConfiguration repo, Date whenGathered )
        throws ConsumerException
    {
        repoId = repo.getId();
        this.whenGathered = whenGathered;
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        // note that we do minimal processing including checksums and POM information for performance of
        // the initial scan. Any request for this information will be intercepted and populated on-demand
        // or picked up by subsequent scans

        ArtifactMetadata artifact = storageResolver.getArtifactForPath( repoId, path );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( artifact.getNamespace() );
        project.setId( artifact.getProject() );

        String projectVersion = VersionUtil.getBaseVersion( artifact.getVersion() );
        // TODO: maybe not too efficient since it may have already been read and stored for this artifact
        ProjectVersionMetadata versionMetadata = null;
        try
        {
            versionMetadata = storageResolver.getProjectVersion( repoId, artifact.getNamespace(), artifact.getProject(),
                                                                 projectVersion );
        }
        catch ( MetadataResolutionException e )
        {
            log.warn( "Error occurred resolving POM for artifact: " + path + "; message: " + e.getMessage() );
        }

        boolean createVersionMetadata = false;
        if ( versionMetadata == null )
        {
            log.warn( "Missing or invalid POM for artifact: " + path + "; creating empty metadata" );
            versionMetadata = new ProjectVersionMetadata();
            versionMetadata.setId( projectVersion );
            versionMetadata.setIncomplete( true );
            createVersionMetadata = true;
        }

        try
        {
            // TODO: transaction
            // read the metadata and update it if it is newer or doesn't exist
            artifact.setWhenGathered( whenGathered );
            metadataRepository.updateArtifact( repoId, project.getNamespace(), project.getId(), projectVersion,
                                               artifact );
            if ( createVersionMetadata )
            {
                metadataRepository.updateProjectVersion( repoId, project.getNamespace(), project.getId(),
                                                         versionMetadata );
            }
            metadataRepository.updateProject( repoId, project );
        }
        catch ( MetadataRepositoryException e )
        {
            log.warn( "Error occurred persisting metadata for artifact: " + path + "; message: " + e.getMessage(), e );
        }
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    public void initialize()
        throws InitializationException
    {
        configuration.addChangeListener( this );

        initIncludes();
    }
}
