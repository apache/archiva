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
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.commons.collections.CollectionUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
     * FIXME: can be of other types
     *
     * @plexus.requirement
     */
    private RepositorySessionFactory repositorySessionFactory;

    private List<String> includes = new ArrayList<String>();

    private File repositoryDir;

    private String repoId;

    /**
     * FIXME: needs to be selected based on the repository in question
     *
     * @plexus.requirement role-hint="maven2"
     */
    private RepositoryPathTranslator pathTranslator;

    private RepositorySession repositorySession;

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
        repoId = repo.getId();
        this.repositoryDir = new File( repo.getLocation() );
        repositorySession = repositorySessionFactory.createSession();
    }

    public void beginScan( ManagedRepositoryConfiguration repo, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repo, whenGathered );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File artifactFile = new File( this.repositoryDir, path );

        // TODO: would be quicker to somehow make sure it ran after the update database consumer, or as a part of that
        //  perhaps could use an artifact context that is retained for all consumers? First in can set the SHA-1
        //  alternatively this could come straight from the storage resolver, which could populate the artifact metadata
        //  in the later parse call with the desired checksum and use that
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

        MetadataRepository metadataRepository = repositorySession.getRepository();

        List<ArtifactMetadata> results;
        try
        {
            results = metadataRepository.getArtifactsByChecksum( repoId, checksumSha1 );
        }
        catch ( MetadataRepositoryException e )
        {
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
                log.warn( "Not reporting problem for invalid artifact in checksum check: " + e.getMessage() );
                return;
            }

            for ( ArtifactMetadata dupArtifact : results )
            {
                String id = path.substring( path.lastIndexOf( "/" ) + 1 );
                if ( dupArtifact.getId().equals( id ) && dupArtifact.getNamespace().equals(
                    originalArtifact.getNamespace() ) && dupArtifact.getProject().equals(
                    originalArtifact.getProject() ) && dupArtifact.getVersion().equals(
                    originalArtifact.getVersion() ) )
                {
                    // Skip reference to itself.
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Not counting duplicate for artifact " + dupArtifact + " for path " + path );
                    }
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
                    metadataRepository.addMetadataFacet( repoId, problem );
                }
                catch ( MetadataRepositoryException e )
                {
                    throw new ConsumerException( e.getMessage(), e );
                }
            }
        }
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    public void completeScan()
    {
        repositorySession.close();
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
        initIncludes();
        configuration.addChangeListener( this );
    }
}
