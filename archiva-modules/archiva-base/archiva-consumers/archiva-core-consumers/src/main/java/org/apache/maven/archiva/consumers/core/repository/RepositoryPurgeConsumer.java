package org.apache.maven.archiva.consumers.core.repository;

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
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Consumer for removing old snapshots in the repository based on the criteria
 * specified by the user.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *      role-hint="repository-purge"
 *      instantiation-strategy="per-lookup"
 */
public class RepositoryPurgeConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    /**
     * @plexus.configuration default-value="repository-purge"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Purge repository of old snapshots"
     */
    private String description;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private MetadataTools metadataTools;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    private List<String> includes = new ArrayList<String>();

    private RepositoryPurge repoPurge;

    private RepositoryPurge cleanUp;

    private boolean deleteReleasedSnapshots;

    /** @plexus.requirement role="org.apache.maven.archiva.repository.events.RepositoryListener" */
    private List<RepositoryListener> listeners = Collections.emptyList();
    
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
        return false;
    }

    public List<String> getExcludes()
    {
        return getDefaultArtifactExclusions();
    }

    public List<String> getIncludes()
    {
        return this.includes;
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        try
        {
            ManagedRepositoryContent repositoryContent = repositoryFactory.getManagedRepositoryContent( repository
                .getId() );

            if ( repository.getDaysOlder() != 0 )
            {
                repoPurge = new DaysOldRepositoryPurge( repositoryContent, repository.getDaysOlder(), 
                                                        repository.getRetentionCount(), listeners );
            }
            else
            {
                repoPurge = new RetentionCountRepositoryPurge( repositoryContent, repository.getRetentionCount(), 
                                                               listeners );
            }
            
            cleanUp =
                new CleanupReleasedSnapshotsRepositoryPurge( repositoryContent, metadataTools, configuration,
                                                             repositoryFactory, listeners );

            deleteReleasedSnapshots = repository.isDeleteReleasedSnapshots();
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new ConsumerException( "Can't run repository purge: " + e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run repository purge: " + e.getMessage(), e );
        }
    }

    public void processFile( String path )
        throws ConsumerException
    {
        try
        {
            if ( deleteReleasedSnapshots )
            {
                cleanUp.process( path );
            }

            repoPurge.process( path );
        }
        catch ( RepositoryPurgeException rpe )
        {
            throw new ConsumerException( rpe.getMessage(), rpe );
        }
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

    public boolean isProcessUnmodified()
    {
        // we need to check all files for deletion, especially if not modified
        return true;
    }
}
