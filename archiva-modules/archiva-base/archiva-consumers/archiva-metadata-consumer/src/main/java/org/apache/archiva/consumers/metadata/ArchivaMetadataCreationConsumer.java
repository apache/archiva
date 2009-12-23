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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.storage.StorageMetadataResolver;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * @plexus.requirement
     */
    private ManagedRepositoryContent repository;

    private List<String> includes = new ArrayList<String>();

    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    /**
     * FIXME: this needs to be configurable based on storage type, and availability of proxy module
     * ... could be a different type since we need methods to modify the storage metadata, which would also allow more
     * appropriate methods to pass in the already determined repository configuration, for example, instead of the ID
     *
     * @plexus.requirement role-hint="maven2"
     */
    private StorageMetadataResolver storageResolver;

    private static final Logger log = LoggerFactory.getLogger( ArchivaMetadataCreationConsumer.class );

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
        this.repository.setRepository( repo );
        this.whenGathered = whenGathered;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        // note that we do minimal processing including checksums and POM information for performance of
        // the initial scan. Any request for this information will be intercepted and populated on-demand
        // or picked up by subsequent scans
        ArtifactReference artifact;
        try
        {
            artifact = repository.toArtifactReference( path );
        }
        catch ( LayoutException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        File file = new File( repository.getRepoRoot(), path );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( artifact.getGroupId() );
        project.setId( artifact.getArtifactId() );

        String projectVersion = VersionUtil.getBaseVersion( artifact.getVersion() );
        // TODO: maybe not too efficient since it may have already been read and stored for this artifact
        ProjectVersionMetadata versionMetadata =
            storageResolver.getProjectVersion( repository.getId(), artifact.getGroupId(), artifact.getArtifactId(),
                                               projectVersion );

        boolean createVersionMetadata = false;
        if ( versionMetadata == null )
        {
            log.warn( "Missing or invalid POM for artifact: " + path + "; creating empty metadata" );
            versionMetadata = new ProjectVersionMetadata();
            versionMetadata.setId( projectVersion );
            createVersionMetadata = true;
        }

        ArtifactMetadata artifactMeta = new ArtifactMetadata();
        artifactMeta.setRepositoryId( repository.getId() );
        artifactMeta.setNamespace( artifact.getGroupId() );
        artifactMeta.setProject( artifact.getArtifactId() );
        artifactMeta.setId( file.getName() );
        artifactMeta.setFileLastModified( file.lastModified() );
        artifactMeta.setSize( file.length() );
        artifactMeta.setVersion( artifact.getVersion() );
        artifactMeta.setWhenGathered( whenGathered );

        ChecksummedFile checksummedFile = new ChecksummedFile( file );
        try
        {
            artifactMeta.setMd5( checksummedFile.calculateChecksum( ChecksumAlgorithm.MD5 ) );
        }
        catch ( IOException e )
        {
            log.error( "Error attempting to get MD5 checksum for " + file + ": " + e.getMessage() );
        }
        try
        {
            artifactMeta.setSha1( checksummedFile.calculateChecksum( ChecksumAlgorithm.SHA1 ) );
        }
        catch ( IOException e )
        {
            log.error( "Error attempting to get SHA-1 checksum for " + file + ": " + e.getMessage() );
        }

        // TODO: transaction
        // read the metadata and update it if it is newer or doesn't exist
        metadataRepository.updateArtifact( repository.getId(), project.getNamespace(), project.getId(), projectVersion,
                                           artifactMeta );
        if ( createVersionMetadata )
        {
            metadataRepository.updateProjectVersion( repository.getId(), project.getNamespace(), project.getId(),
                                                     versionMetadata );
        }
        metadataRepository.updateProject( repository.getId(), project );
    }

    public void completeScan()
    {
        /* do nothing */
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
