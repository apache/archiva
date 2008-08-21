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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search the database of known SHA1 Checksums for potential duplicate artifacts.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.ArchivaArtifactConsumer"
 *                   role-hint="duplicate-artifacts"
 */
public class DuplicateArtifactsConsumer
    extends AbstractMonitoredConsumer
    implements ArchivaArtifactConsumer, RegistryListener, Initializable
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

    // TODO: why is this not used? If it should be, what about excludes?
    private List<String> includes = new ArrayList<String>();

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

    public void beginScan()
    {
        /* do nothing */
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List<String> getIncludedTypes()
    {
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        String checksumSha1 = artifact.getModel().getChecksumSHA1();

        List<ArchivaArtifact> results = null;
        try
        {
            results = dao.getArtifactDAO().queryArtifacts( new ArtifactsByChecksumConstraint(
                checksumSha1, ArtifactsByChecksumConstraint.SHA1 ) );
        }
        catch ( ObjectNotFoundException e )
        {
            log.debug( "No duplicates for artifact: " + artifact );
            return;
        }
        catch ( ArchivaDatabaseException e )
        {
            log.warn( "Unable to query DB for potential duplicates with : " + artifact );
            return;
        }

        if ( CollectionUtils.isNotEmpty( results ) )
        {
            if ( results.size() <= 1 )
            {
                // No duplicates detected.
                log.debug( "Found no duplicate artifact results on: " + artifact );
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
                problem.setPath( toPath( dupArtifact ) );
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

    private String toPath( ArchivaArtifact artifact )
    {
        try
        {
            String repoId = artifact.getModel().getRepositoryId();
            ManagedRepositoryContent repo = repositoryFactory.getManagedRepositoryContent( repoId );
            return repo.toPath( artifact );
        }
        catch ( RepositoryException e )
        {
            log.warn( "Unable to calculate path for artifact: " + artifact );
            return "";
        }
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
