package org.apache.maven.archiva.consumers.lucene;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.updater.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
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
 * IndexArtifactConsumer
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer"
 * role-hint="index-artifact"
 * instantiation-strategy="per-lookup"
 */
public class IndexArtifactConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseUnprocessedArtifactConsumer, RegistryListener, Initializable
{
    private Logger log = LoggerFactory.getLogger( IndexArtifactConsumer.class );
    
    private static final String INDEX_ERROR = "indexing_error";

    /**
     * @plexus.configuration default-value="index-artifact"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Index the artifact checksums for Find functionality."
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
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    private Map<String, IndexedRepositoryDetails> repositoryMap = new HashMap<String, IndexedRepositoryDetails>();

    public void beginScan()
    {
        /* nothing to do here */
    }

    public void completeScan()
    {
        /* nothing to do here */
    }

    public List<String> getIncludedTypes()
    {
        return null; // TODO: define these as a list of artifacts.
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        HashcodesRecord record = new HashcodesRecord();
        record.setRepositoryId( artifact.getModel().getRepositoryId() );
        record.setArtifact( artifact );

        IndexedRepositoryDetails pnl = getIndexedRepositoryDetails( artifact );

        String artifactPath = pnl.repository.toPath( artifact );
        record.setFilename( artifactPath );

        try
        {
            pnl.index.modifyRecord( record );
        }
        catch ( RepositoryIndexException e )
        {
            triggerConsumerError( INDEX_ERROR, "Unable to index hashcodes: " + e.getMessage() );
        }
    }

    private IndexedRepositoryDetails getIndexedRepositoryDetails( ArchivaArtifact artifact )
    {
        String repoId = artifact.getModel().getRepositoryId();
        if ( StringUtils.isBlank( repoId ) )
        {
            throw new IllegalStateException(
                "Unable to process artifact [" + artifact + "] as it has no repository id associated with it." );
        }

        return getIndexedRepositoryDetails( repoId );
    }

    private IndexedRepositoryDetails getIndexedRepositoryDetails( String id )
    {
        return this.repositoryMap.get( id );
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) )
        {
            initRepositoryMap();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    public void initialize()
        throws InitializationException
    {
        initRepositoryMap();
        configuration.addChangeListener( this );
    }

    private void initRepositoryMap()
    {
        synchronized ( this.repositoryMap )
        {
            this.repositoryMap.clear();

            Iterator<ManagedRepositoryConfiguration> it = configuration.getConfiguration().getManagedRepositories().iterator();
            while ( it.hasNext() )
            {
                ManagedRepositoryConfiguration repository = it.next();

                try
                {
                    IndexedRepositoryDetails pnl = new IndexedRepositoryDetails();

                    pnl.repository = repositoryFactory.getManagedRepositoryContent( repository.getId() );

                    pnl.index = indexFactory.createHashcodeIndex( repository );

                    this.repositoryMap.put( repository.getId(), pnl );
                }
                catch ( RepositoryException e )
                {
                    log.error( "Unable to load repository content object: " + e.getMessage(), e );
                }
            }
        }
    }

    class IndexedRepositoryDetails
    {
        public ManagedRepositoryContent repository;

        public RepositoryContentIndex index;
    }
}
