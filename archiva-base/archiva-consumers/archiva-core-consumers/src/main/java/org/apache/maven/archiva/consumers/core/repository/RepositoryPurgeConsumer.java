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

import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.List;
import java.util.ArrayList;

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

    private ArchivaRepository repository;

    private BidirectionalRepositoryLayout repositoryLayout;

    private List includes = new ArrayList();

    private List propertyNameTriggers = new ArrayList();

    private RepositoryPurge repoPurge;

    private RepositoryContentIndex index;

    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

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

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return this.includes;
    }

    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        if ( !repository.isManaged() )
        {
            throw new ConsumerException( "Consumer requires managed repository." );
        }

        this.repository = repository;
        this.index = indexFactory.createFileContentIndex( repository );

        try
        {
            this.repositoryLayout = layoutFactory.getLayout( this.repository.getLayoutType() );
        }
        catch ( LayoutException e )
        {
            throw new ConsumerException(
                "Unable to initialize consumer due to unknown repository layout: " + e.getMessage(), e );
        }

        // @todo check the repo configuration first which purge was set by the user
        // temporarily default to DaysOldRepositoryPurge
        repoPurge = new DaysOldRepositoryPurge();
        repoPurge.setLayout( repositoryLayout );
        repoPurge.setRepository( repository );
        repoPurge.setIndex( index );
        repoPurge.setArtifactDao( dao.getArtifactDAO() );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        try
        {
            repoPurge.process( path, configuration.getConfiguration() );

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
        propertyNameTriggers = new ArrayList();
        propertyNameTriggers.add( "repositoryScanning" );
        propertyNameTriggers.add( "fileTypes" );
        propertyNameTriggers.add( "fileType" );
        propertyNameTriggers.add( "patterns" );
        propertyNameTriggers.add( "pattern" );

        configuration.addChangeListener( this );

        initIncludes();
    }

}
