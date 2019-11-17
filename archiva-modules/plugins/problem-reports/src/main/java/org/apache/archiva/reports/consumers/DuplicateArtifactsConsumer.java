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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.RepositoryProblemFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Search the artifact repository of known SHA1 Checksums for potential duplicate artifacts.
 * <p>
 * TODO: no need for this to be a scanner - we can just query the database / content repository to get a full list
 */
@Service ( "knownRepositoryContentConsumer#duplicate-artifacts" )
@Scope ( "prototype" )
public class DuplicateArtifactsConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( DuplicateArtifactsConsumer.class );

    private String id = "duplicate-artifacts";

    private String description = "Check for Duplicate Artifacts via SHA1 Checksums";

    @Inject
    private ArchivaConfiguration configuration;

    @Inject
    private FileTypes filetypes;

    /**
     * FIXME: this could be multiple implementations and needs to be configured.
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private List<String> includes = new ArrayList<>();

    private Path repositoryDir;

    private String repoId;

    /**
     * FIXME: needs to be selected based on the repository in question
     */
    @Inject
    @Named ( value = "repositoryPathTranslator#maven2" )
    private RepositoryPathTranslator pathTranslator;


    private RepositorySession repositorySession;

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }

    @Override
    public List<String> getExcludes()
    {
        return Collections.emptyList();
    }

    @Override
    public void beginScan( ManagedRepository repo, Date whenGathered )
        throws ConsumerException
    {
        repoId = repo.getId();
        this.repositoryDir = Paths.get( repo.getLocation() );
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
    }

    @Override
    public void beginScan( ManagedRepository repo, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repo, whenGathered );
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        Path artifactFile = this.repositoryDir.resolve( path );

        // TODO: would be quicker to somehow make sure it ran after the update database consumer, or as a part of that
        //  perhaps could use an artifact context that is retained for all consumers? First in can set the SHA-1
        //  alternatively this could come straight from the storage resolver, which could populate the artifact metadata
        //  in the later parse call with the desired checksum and use that
        String checksumSha1;
        ChecksummedFile checksummedFile = new ChecksummedFile( artifactFile);
        try
        {
            checksumSha1 = checksummedFile.calculateChecksum( ChecksumAlgorithm.SHA1 );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        MetadataRepository metadataRepository = repositorySession.getRepository();

        Collection<ArtifactMetadata> results;
        try
        {
            results = metadataRepository.getArtifactsByChecksum(repositorySession , repoId, checksumSha1 );
        }
        catch ( MetadataRepositoryException e )
        {
            repositorySession.close();
            throw new ConsumerException( e.getMessage(), e );
        }

        if ( CollectionUtils.isNotEmpty( results ) )
        {
            ArtifactMetadata originalArtifact;
            try
            {
                originalArtifact = pathTranslator.getArtifactForPath( repoId, path );
            }
            catch ( Exception e )
            {
                log.warn( "Not reporting problem for invalid artifact in checksum check: {}", e.getMessage() );
                return;
            }

            for ( ArtifactMetadata dupArtifact : results )
            {
                String id = path.substring( path.lastIndexOf( '/' ) + 1 );
                if ( dupArtifact.getId().equals( id ) && dupArtifact.getNamespace().equals(
                    originalArtifact.getNamespace() ) && dupArtifact.getProject().equals(
                    originalArtifact.getProject() ) && dupArtifact.getVersion().equals(
                    originalArtifact.getVersion() ) )
                {
                    // Skip reference to itself.

                    log.debug( "Not counting duplicate for artifact {} for path {}", dupArtifact, path );

                    continue;
                }

                RepositoryProblemFacet problem = new RepositoryProblemFacet();
                problem.setRepositoryId( repoId );
                problem.setNamespace( originalArtifact.getNamespace() );
                problem.setProject( originalArtifact.getProject() );
                problem.setVersion( originalArtifact.getVersion() );
                problem.setId( id );
                // FIXME: need to get the right storage resolver for the repository the dupe artifact is in, it might be
                //       a different type
                // FIXME: we need the project version here, not the artifact version
                problem.setMessage( "Duplicate Artifact Detected: " + path + " <--> " + pathTranslator.toPath(
                    dupArtifact.getNamespace(), dupArtifact.getProject(), dupArtifact.getVersion(),
                    dupArtifact.getId() ) );
                problem.setProblem( "duplicate-artifact" );

                try
                {
                    metadataRepository.addMetadataFacet(repositorySession , repoId, problem );
                }
                catch ( MetadataRepositoryException e )
                {
                    throw new ConsumerException( e.getMessage(), e );
                }
            }
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
        repositorySession.close();
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
        includes.clear();

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    @PostConstruct
    public void initialize()
    {
        initIncludes();
        configuration.addChangeListener( this );
    }

    public RepositorySessionFactory getRepositorySessionFactory( )
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }
}
