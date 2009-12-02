package org.apache.maven.archiva.reporting.artifact;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
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
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    private List<String> includes = new ArrayList<String>();

    private File repositoryDir;

    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    private String repoId;

    private ManagedRepositoryContent repository;

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
        try
        {
            checksumSha1 = digestSha1.calc( artifactFile );
        }
        catch ( DigesterException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        List<ArchivaArtifact> results;
        try
        {
            results = dao.getArtifactDAO().queryArtifacts(
                new ArtifactsByChecksumConstraint( checksumSha1, ArtifactsByChecksumConstraint.SHA1 ) );
        }
        catch ( ObjectNotFoundException e )
        {
            log.debug( "No duplicates for artifact: " + path + " (repository " + repoId + ")" );
            return;
        }
        catch ( ArchivaDatabaseException e )
        {
            log.warn( "Unable to query DB for potential duplicates with: " + path + " (repository " + repoId + "): " + e.getMessage(), e );
            return;
        }

        if ( CollectionUtils.isNotEmpty( results ) )
        {
            if ( results.size() <= 1 )
            {
                // No duplicates detected.
                log.debug( "Found no duplicate artifact results on: " + path + " (repository " + repoId + ")" );
                return;
            }

            ArchivaArtifact artifact;
            try
            {
                artifact = new ArchivaArtifact( repository.toArtifactReference( path ), repoId );
            }
            catch ( LayoutException e )
            {
                log.warn( "Unable to report problem for path: " + path );
                return;
            }
            for ( ArchivaArtifact dupArtifact : results )
            {
                if ( dupArtifact.equals( artifact ) )
                {
                    // Skip reference to itself.
                    continue;
                }

                RepositoryProblem problem = new RepositoryProblem();
                problem.setRepositoryId( dupArtifact.getModel().getRepositoryId() );
                problem.setPath( path );
                problem.setGroupId( artifact.getGroupId() );
                problem.setArtifactId( artifact.getArtifactId() );
                problem.setVersion( artifact.getVersion() );
                problem.setType( DuplicateArtifactReport.PROBLEM_TYPE_DUPLICATE_ARTIFACTS );
                problem.setOrigin( getId() );
                problem.setMessage( "Duplicate Artifact Detected: " + artifact + " <--> " + dupArtifact );

                try
                {
                    log.debug( "Found duplicate artifact: " + problem );
                    dao.getRepositoryProblemDAO().saveRepositoryProblem( problem );
                }
                catch ( ArchivaDatabaseException e )
                {
                    String emsg = "Unable to save problem with duplicate artifact to DB: " + e.getMessage();
                    log.warn( emsg, e );
                    throw new ConsumerException( emsg, e );
                }
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
