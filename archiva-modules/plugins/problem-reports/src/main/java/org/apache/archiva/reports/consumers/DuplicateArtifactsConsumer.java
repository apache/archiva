package org.apache.archiva.reports.consumers;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search the database of known SHA1 Checksums for potential duplicate artifacts.
 *
 * TODO: no need for this to be a scanner - we can just query the database / content repository to get a full list
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="duplicate-artifacts"
 * instantiation-strategy="per-lookup"
 */
public class DuplicateArtifactsConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    private Logger log = LoggerFactory.getLogger( DuplicateArtifactsConsumer.class );

    /**
     * @plexus.configuration default-value="duplicate-artifacts"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Check for Duplicate Artifacts via SHA1 Checksums"
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

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    private List<String> includes = new ArrayList<String>();

    private File repositoryDir;

    private String repoId;

    private ManagedRepositoryContent repository;

    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public List<String> getExcludes()
    {
        return Collections.emptyList();
    }

    public void beginScan( ManagedRepositoryConfiguration repo, Date whenGathered )
        throws ConsumerException
    {
        try
        {
            repoId = repo.getId();
            repository = repositoryFactory.getManagedRepositoryContent( repoId );
            this.repositoryDir = new File( repository.getRepoRoot() );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File artifactFile = new File( this.repositoryDir, path );

        // TODO: would be quicker to somehow make sure it ran after the update database consumer, or as a part of that
        //  perhaps could use an artifact context that is retained for all consumers? First in can set the SHA-1
        String checksumSha1;
        ChecksummedFile checksummedFile = new ChecksummedFile( artifactFile );
        try
        {
            checksumSha1 = checksummedFile.calculateChecksum( ChecksumAlgorithm.SHA1 );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        List<ArtifactMetadata> results = metadataRepository.getArtifactsByChecksum( repoId, checksumSha1 );

        if ( CollectionUtils.isNotEmpty( results ) )
        {
            if ( results.size() <= 1 )
            {
                // No duplicates detected.
                log.debug( "Found no duplicate artifact results on: " + path + " (repository " + repoId + ")" );
                return;
            }

            ArtifactReference artifactReference;
            try
            {
                artifactReference = repository.toArtifactReference( path );
            }
            catch ( LayoutException e )
            {
                log.warn( "Unable to report problem for path: " + path );
                return;
            }

            for ( ArtifactMetadata dupArtifact : results )
            {
                String id = path.substring( path.lastIndexOf( "/" ) + 1 );
                if ( dupArtifact.getId().equals( id ) &&
                    dupArtifact.getNamespace().equals( artifactReference.getGroupId() ) &&
                    dupArtifact.getProject().equals( artifactReference.getArtifactId() ) &&
                    dupArtifact.getVersion().equals( artifactReference.getVersion() ) )
                {
                    // Skip reference to itself.
                    continue;
                }

                RepositoryProblemFacet problem = new RepositoryProblemFacet();
                problem.setRepositoryId( repoId );
                problem.setNamespace( artifactReference.getGroupId() );
                problem.setProject( artifactReference.getArtifactId() );
                problem.setVersion( artifactReference.getVersion() );
                problem.setId( id );
                // TODO: proper path conversion for new metadata
                problem.setMessage(
                    "Duplicate Artifact Detected: " + path + " <--> " + dupArtifact.getNamespace().replace( '.', '/' ) +
                        "/" + dupArtifact.getProject() + "/" + dupArtifact.getVersion() + "/" + dupArtifact.getId() );
                problem.setProblem( "duplicate-artifact" );

                metadataRepository.addMetadataFacet( repoId, RepositoryProblemFacet.FACET_ID, problem );
            }
        }
    }

    public void completeScan()
    {
        // nothing to do
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
        initIncludes();
        configuration.addChangeListener( this );
    }
}
