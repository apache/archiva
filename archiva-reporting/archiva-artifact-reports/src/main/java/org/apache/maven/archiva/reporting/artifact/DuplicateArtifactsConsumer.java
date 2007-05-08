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
import org.apache.maven.archiva.database.constraints.ArtifactsBySha1ChecksumConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Search the database of known SHA1 Checksums for potential duplicate artifacts.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.ArchivaArtifactConsumer"
 *                   role-hint="duplicate-artifacts"
 */
public class DuplicateArtifactsConsumer
    extends AbstractMonitoredConsumer
    implements ArchivaArtifactConsumer, RegistryListener, Initializable
{
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
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    private List includes = new ArrayList();

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

    public List getIncludedTypes()
    {
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        String checksumSha1 = artifact.getModel().getChecksumSHA1();

        List results = null;
        try
        {
            results = dao.getArtifactDAO().queryArtifacts( new ArtifactsBySha1ChecksumConstraint( checksumSha1 ) );
        }
        catch ( ObjectNotFoundException e )
        {
            getLogger().debug( "No duplicates for artifact: " + artifact );
            return;
        }
        catch ( ArchivaDatabaseException e )
        {
            getLogger().warn( "Unable to query DB for potential duplicates with : " + artifact );
            return;
        }

        if ( CollectionUtils.isNotEmpty( results ) )
        {
            if ( results.size() <= 1 )
            {
                // No duplicates detected.
                getLogger().debug( "Found no duplicate artifact results on: " + artifact );
                return;
            }

            Iterator it = results.iterator();
            while ( it.hasNext() )
            {
                ArchivaArtifact dupArtifact = (ArchivaArtifact) it.next();

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
                    getLogger().debug( "Found duplicate artifact: " + problem );
                    dao.getRepositoryProblemDAO().saveRepositoryProblem( problem );
                }
                catch ( ArchivaDatabaseException e )
                {
                    String emsg = "Unable to save problem with duplicate artifact to DB: " + e.getMessage();
                    getLogger().warn( emsg, e );
                    throw new ConsumerException( emsg, e );
                }
            }
        }
    }

    private String toPath( ArchivaArtifact artifact )
    {
        try
        {
            BidirectionalRepositoryLayout layout = layoutFactory.getLayout( artifact );
            return layout.toPath( artifact );
        }
        catch ( LayoutException e )
        {
            getLogger().warn( "Unable to calculate path for artifact: " + artifact );
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
