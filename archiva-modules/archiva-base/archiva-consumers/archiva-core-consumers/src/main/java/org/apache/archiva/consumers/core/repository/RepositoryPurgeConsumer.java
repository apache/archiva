package org.apache.archiva.consumers.core.repository;

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
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Consumer for removing old snapshots in the repository based on the criteria
 * specified by the user.
 */
@Service( "knownRepositoryContentConsumer#repository-purge" )
@Scope( "prototype" )
public class RepositoryPurgeConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    /**
     * default-value="repository-purge"
     */
    private String id = "repository-purge";

    /**
     * default-value="Purge repository of old snapshots"
     */
    private String description = "Purge repository of old snapshots";

    /**
     *
     */
    @Inject
    @Named( value = "archivaConfiguration#default" )
    private ArchivaConfiguration configuration;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    /**
     *
     */
    @Inject
    @Named( value = "repositoryContentFactory#default" )
    private RepositoryContentFactory repositoryContentFactory;

    /**
     *
     */
    @Inject
    private MetadataTools metadataTools;

    /**
     *
     */
    @Inject
    @Named( value = "fileTypes" )
    private FileTypes filetypes;

    private List<String> includes = new ArrayList<String>();

    private RepositoryPurge repoPurge;

    private RepositoryPurge cleanUp;

    private boolean deleteReleasedSnapshots;

    /**
     *
     */
    @Inject
    private List<RepositoryListener> listeners = Collections.emptyList();

    /**
     * TODO: this could be multiple implementations and needs to be configured.
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private RepositorySession repositorySession;

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

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        ManagedRepositoryContent repositoryContent;
        try
        {
            repositoryContent = repositoryContentFactory.getManagedRepositoryContent( repository.getId() );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new ConsumerException( "Can't run repository purge: " + e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run repository purge: " + e.getMessage(), e );
        }

        repositorySession = repositorySessionFactory.createSession();

        if ( repository.getDaysOlder() != 0 )
        {
            repoPurge = new DaysOldRepositoryPurge( repositoryContent, repository.getDaysOlder(),
                                                    repository.getRetentionCount(), repositorySession, listeners );
        }
        else
        {
            repoPurge =
                new RetentionCountRepositoryPurge( repositoryContent, repository.getRetentionCount(), repositorySession,
                                                   listeners );
        }

        cleanUp = new CleanupReleasedSnapshotsRepositoryPurge( repositoryContent, metadataTools, managedRepositoryAdmin,
                                                               repositoryContentFactory, repositorySession, listeners );

        deleteReleasedSnapshots = repository.isDeleteReleasedSnapshots();
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
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

    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
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

    @PostConstruct
    public void initialize()
    {
        //this.listeners =
        //    new ArrayList<RepositoryListener>( applicationContext.getBeansOfType( RepositoryListener.class ).values() );
        configuration.addChangeListener( this );

        initIncludes();
    }

    public boolean isProcessUnmodified()
    {
        // we need to check all files for deletion, especially if not modified
        return true;
    }

    public ArchivaConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public RepositoryContentFactory getRepositoryContentFactory()
    {
        return repositoryContentFactory;
    }

    public void setRepositoryContentFactory( RepositoryContentFactory repositoryContentFactory )
    {
        this.repositoryContentFactory = repositoryContentFactory;
    }

    public MetadataTools getMetadataTools()
    {
        return metadataTools;
    }

    public void setMetadataTools( MetadataTools metadataTools )
    {
        this.metadataTools = metadataTools;
    }

    public FileTypes getFiletypes()
    {
        return filetypes;
    }

    public void setFiletypes( FileTypes filetypes )
    {
        this.filetypes = filetypes;
    }

    public RepositoryPurge getRepoPurge()
    {
        return repoPurge;
    }

    public void setRepoPurge( RepositoryPurge repoPurge )
    {
        this.repoPurge = repoPurge;
    }

    public RepositoryPurge getCleanUp()
    {
        return cleanUp;
    }

    public void setCleanUp( RepositoryPurge cleanUp )
    {
        this.cleanUp = cleanUp;
    }

    public boolean isDeleteReleasedSnapshots()
    {
        return deleteReleasedSnapshots;
    }

    public void setDeleteReleasedSnapshots( boolean deleteReleasedSnapshots )
    {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    public RepositorySession getRepositorySession()
    {
        return repositorySession;
    }

    public void setRepositorySession( RepositorySession repositorySession )
    {
        this.repositorySession = repositorySession;
    }
}
