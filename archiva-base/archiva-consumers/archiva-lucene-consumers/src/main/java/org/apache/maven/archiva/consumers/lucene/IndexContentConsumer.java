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
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IndexContentConsumer - generic full file content indexing consumer. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.RepositoryContentConsumer"
 *                   role-hint="index-content"
 *                   instantiation-strategy="per-lookup"
 */
public class IndexContentConsumer
    extends AbstractMonitoredConsumer
    implements RepositoryContentConsumer, RegistryListener, Initializable
{
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
    private RepositoryContentIndexFactory indexFactory;

    private List propertyNameTriggers = new ArrayList();

    private List includes = new ArrayList();

    private RepositoryContentIndex index;

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

        this.repositoryDir = new File( repository.getUrl().getPath() );
        this.index = indexFactory.createFileContentIndex( repository );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        FileContentRecord record = new FileContentRecord();
        try
        {
            File file = new File( repositoryDir, path );
            record.setFile( file );
            record.setContents( FileUtils.readFileToString( file, null ) );

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

        FileType artifactTypes = configuration.getConfiguration().getRepositoryScanning()
            .getFileTypeById( "indexable-content" );
        if ( artifactTypes != null )
        {
            includes.addAll( artifactTypes.getPatterns() );
        }
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

        if ( includes.isEmpty() )
        {
            throw new InitializationException( "Unable to use " + getId() + " due to empty includes list." );
        }
    }
}
