package org.apache.archiva.consumers.core;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MetadataUpdaterConsumer will create and update the metadata present within the repository.
 *
 *
 */
@Service( "knownRepositoryContentConsumer#metadata-updater" )
@Scope( "prototype" )
public class MetadataUpdaterConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
    // it's prototype bean so we assume configuration won't change during a run
    //, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( MetadataUpdaterConsumer.class );

    /**
     * default-value="metadata-updater"
     */
    private String id = "metadata-updater";

    private String description = "Update / Create maven-metadata.xml files";

    @Inject
    private RepositoryContentFactory repositoryFactory;

    @Inject
    private MetadataTools metadataTools;

    @Inject
    private ArchivaConfiguration configuration;

    @Inject
    private FileTypes filetypes;

    private static final String TYPE_METADATA_BAD_INTERNAL_REF = "metadata-bad-internal-ref";

    private static final String TYPE_METADATA_WRITE_FAILURE = "metadata-write-failure";

    private static final String TYPE_METADATA_IO = "metadata-io-warning";

    private ManagedRepositoryContent repository;

    private File repositoryDir;

    private List<String> includes = new ArrayList<>( 0 );

    private long scanStartTimestamp = 0;

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public String getId()
    {
        return id;
    }

    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }

    @Override
    public void beginScan( ManagedRepository repoConfig, Date whenGathered )
        throws ConsumerException
    {
        try
        {
            this.repository = repositoryFactory.getManagedRepositoryContent( repoConfig.getId() );
            this.repositoryDir = new File( repository.getRepoRoot() );
            this.scanStartTimestamp = System.currentTimeMillis();
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    @Override
    public void completeScan()
    {
        /* do nothing here */
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
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
    public void processFile( String path )
        throws ConsumerException
    {
        // Ignore paths like .index etc
        if ( !path.startsWith( "." ) )
        {
            try
            {
                ArtifactReference artifact = repository.toArtifactReference( path );
                updateVersionMetadata( artifact, path );
                updateProjectMetadata( artifact, path );
            }
            catch ( LayoutException e )
            {
                log.info( "Not processing path that is not an artifact: {} ({})", path, e.getMessage() );
            }
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        processFile( path );
    }

    private void updateProjectMetadata( ArtifactReference artifact, String path )
    {
        ProjectReference projectRef = new ProjectReference();
        projectRef.setGroupId( artifact.getGroupId() );
        projectRef.setArtifactId( artifact.getArtifactId() );

        try
        {
            String metadataPath = this.metadataTools.toPath( projectRef );

            File projectMetadata = new File( this.repositoryDir, metadataPath );

            if ( projectMetadata.exists() && ( projectMetadata.lastModified() >= this.scanStartTimestamp ) )
            {
                // This metadata is up to date. skip it.
                log.debug( "Skipping uptodate metadata: {}", this.metadataTools.toPath( projectRef ) );
                return;
            }

            metadataTools.updateMetadata( this.repository, projectRef );
            log.debug( "Updated metadata: {}", this.metadataTools.toPath( projectRef ) );
        }
        catch ( LayoutException e )
        {
            log.warn( "Unable to convert path [{}] to an internal project reference: ", path, e );
            triggerConsumerWarning( TYPE_METADATA_BAD_INTERNAL_REF,
                                    "Unable to convert path [" + path + "] to an internal project reference: "
                                        + e.getMessage() );
        }
        catch ( RepositoryMetadataException e )
        {
            log.error( "Unable to write project metadat for artifact [{}]:", path, e );
            triggerConsumerError( TYPE_METADATA_WRITE_FAILURE,
                                  "Unable to write project metadata for artifact [" + path + "]: " + e.getMessage() );
        }
        catch ( IOException e )
        {
            log.warn( "Project metadata not written due to IO warning: ", e );
            triggerConsumerWarning( TYPE_METADATA_IO,
                                    "Project metadata not written due to IO warning: " + e.getMessage() );
        }
        catch ( ContentNotFoundException e )
        {
            log.warn( "Project metadata not written because no versions were found to update: ", e );
            triggerConsumerWarning( TYPE_METADATA_IO,
                                    "Project metadata not written because no versions were found to update: "
                                        + e.getMessage() );
        }
    }

    private void updateVersionMetadata( ArtifactReference artifact, String path )
    {
        VersionedReference versionRef = new VersionedReference();
        versionRef.setGroupId( artifact.getGroupId() );
        versionRef.setArtifactId( artifact.getArtifactId() );
        versionRef.setVersion( artifact.getVersion() );

        try
        {
            String metadataPath = this.metadataTools.toPath( versionRef );

            File projectMetadata = new File( this.repositoryDir, metadataPath );

            if ( projectMetadata.exists() && ( projectMetadata.lastModified() >= this.scanStartTimestamp ) )
            {
                // This metadata is up to date. skip it.
                log.debug( "Skipping uptodate metadata: {}", this.metadataTools.toPath( versionRef ) );
                return;
            }

            metadataTools.updateMetadata( this.repository, versionRef );
            log.debug( "Updated metadata: {}", this.metadataTools.toPath( versionRef ) );
        }
        catch ( LayoutException e )
        {
            log.warn( "Unable to convert path [{}] to an internal version reference: ", path, e );
            triggerConsumerWarning( TYPE_METADATA_BAD_INTERNAL_REF,
                                    "Unable to convert path [" + path + "] to an internal version reference: "
                                        + e.getMessage() );
        }
        catch ( RepositoryMetadataException e )
        {
            log.error( "Unable to write version metadata for artifact [{}]: ", path, e ); 
            triggerConsumerError( TYPE_METADATA_WRITE_FAILURE,
                                  "Unable to write version metadata for artifact [" + path + "]: " + e.getMessage() );
        }
        catch ( IOException e )
        {
            log.warn( "Version metadata not written due to IO warning: ", e );
            triggerConsumerWarning( TYPE_METADATA_IO,
                                    "Version metadata not written due to IO warning: " + e.getMessage() );
        }
        catch ( ContentNotFoundException e )
        {
            log.warn( "Version metadata not written because no versions were found to update: ", e ); 
            triggerConsumerWarning( TYPE_METADATA_IO,
                                    "Version metadata not written because no versions were found to update: "
                                        + e.getMessage() );
        }
    }

    /*
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
        // do nothing here
    }
    */

    private void initIncludes()
    {
        includes = new ArrayList<>( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    @PostConstruct
    public void initialize()
    {
        //configuration.addChangeListener( this );

        initIncludes();
    }
}
