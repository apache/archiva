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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * IndexContentConsumer - generic full file content indexing consumer.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="index-content"
 * instantiation-strategy="per-lookup"
 */
public class IndexContentConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    private Logger log = LoggerFactory.getLogger( IndexContentConsumer.class );
    
    private static final String READ_CONTENT = "read_content";

    private static final String INDEX_ERROR = "indexing_error";

    /**
     * @plexus.configuration default-value="index-content"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Text and XML file contents indexing"
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
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;
    
    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    private List<String> propertyNameTriggers = new ArrayList<String>();

    private List<String> includes = new ArrayList<String>();

    private RepositoryContentIndex index;

    private ManagedRepositoryContent repository;

    private File repositoryDir;

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

    public void beginScan( ManagedRepositoryConfiguration repo, Date whenGathered )
        throws ConsumerException
    {
        try
        {
            this.repository = repositoryFactory.getManagedRepositoryContent( repo.getId() );
            this.repositoryDir = new File( repository.getRepoRoot() );
            this.index = indexFactory.createFileContentIndex( repository.getRepository() );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Unable to start IndexContentConsumer: " + e.getMessage(), e );
        }
    }

    public void processFile( String path )
        throws ConsumerException
    {
        if ( path.endsWith( "/" + MetadataTools.MAVEN_METADATA ) )
        {
            log.debug( "File is a metadata file. Not indexing." );
            return;
        }
        
        FileContentRecord record = new FileContentRecord();
        try
        {
            File file = new File( repositoryDir, path );
            record.setRepositoryId( this.repository.getId() );
            record.setFilename( path );
            record.setContents( FileUtils.readFileToString( file, null ) );

            // Test for possible artifact reference syntax.
            try
            {
                ArtifactReference ref = repository.toArtifactReference( path );
                ArchivaArtifact artifact = new ArchivaArtifact( ref );
                artifact.getModel().setRepositoryId( repository.getId() );
                record.setArtifact( artifact );                
            }
            catch ( LayoutException e )
            {
                // Not an artifact.
            }

            index.modifyRecord( record );
        }
        catch ( IOException e )
        {
            triggerConsumerError( READ_CONTENT, "Unable to read file contents: " + e.getMessage() );
        }
        catch ( RepositoryIndexException e )
        {
            triggerConsumerError( INDEX_ERROR, "Unable to index file contents: " + e.getMessage() );
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

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.INDEXABLE_CONTENT ) );
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
