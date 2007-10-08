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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * IndexArtifactConsumer
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer"
 * role-hint="index-artifact"
 * instantiation-strategy="per-lookup"
 */
public class IndexArtifactConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseUnprocessedArtifactConsumer, RegistryListener, Initializable
{
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
     * @plexus.requirement role="org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout"
     */
    private Map bidirectionalLayoutMap;  // TODO: replace with new bidir-repo-layout-factory

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

        String artifactPath = pnl.layout.toPath( artifact );
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

                IndexedRepositoryDetails pnl = new IndexedRepositoryDetails();

                pnl.path = repository.getLocation();
                pnl.layout = (BidirectionalRepositoryLayout) this.bidirectionalLayoutMap.get( repository.getLayout() );

                pnl.index = indexFactory.createHashcodeIndex( repository );

                this.repositoryMap.put( repository.getId(), pnl );
            }
        }
    }

    class IndexedRepositoryDetails
    {
        public String path;

        public BidirectionalRepositoryLayout layout;

        public RepositoryContentIndex index;
    }
}
