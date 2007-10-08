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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumer for removing old snapshots in the repository based on the criteria
 * specified by the user.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="repository-purge"
 * instantiation-strategy="per-lookup
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
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    private List<String> includes = new ArrayList<String>();

    private List<String> propertyNameTriggers = new ArrayList<String>();

    private RepositoryPurge repoPurge;

    private RepositoryPurge cleanUp;

    private boolean deleteReleasedSnapshots;

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
        return null;
    }

    public List<String> getIncludes()
    {
        return this.includes;
    }

    public void beginScan( ManagedRepositoryConfiguration repository )
        throws ConsumerException
    {
        BidirectionalRepositoryLayout repositoryLayout;
        try
        {
            repositoryLayout = layoutFactory.getLayout( repository.getLayout() );
        }
        catch ( LayoutException e )
        {
            throw new ConsumerException(
                "Unable to initialize consumer due to unknown repository layout: " + e.getMessage(), e );
        }

        ManagedRepositoryConfiguration repoConfig =
            configuration.getConfiguration().findManagedRepositoryById( repository.getId() );

        if ( repoConfig.getDaysOlder() != 0 )
        {
            repoPurge = new DaysOldRepositoryPurge( repository, repositoryLayout, dao.getArtifactDAO(),
                                                    repoConfig.getDaysOlder() );
        }
        else
        {
            repoPurge = new RetentionCountRepositoryPurge( repository, repositoryLayout, dao.getArtifactDAO(),
                                                           repoConfig.getRetentionCount() );
        }

        cleanUp = new CleanupReleasedSnapshotsRepositoryPurge( repository, repositoryLayout, dao.getArtifactDAO() );

        deleteReleasedSnapshots = repoConfig.isDeleteReleasedSnapshots();
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
            throw new ConsumerException( rpe.getMessage() );
        }
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyNameTriggers.contains( propertyName ) )
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
        propertyNameTriggers = new ArrayList<String>();
        propertyNameTriggers.add( "repositoryScanning" );
        propertyNameTriggers.add( "fileTypes" );
        propertyNameTriggers.add( "fileType" );
        propertyNameTriggers.add( "patterns" );
        propertyNameTriggers.add( "pattern" );

        configuration.addChangeListener( this );

        initIncludes();
    }

}
