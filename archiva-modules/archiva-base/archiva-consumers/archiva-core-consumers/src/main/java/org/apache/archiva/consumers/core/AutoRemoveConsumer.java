package org.apache.archiva.consumers.core;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.repository.ManagedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AutoRemoveConsumer
 */
@Service( "knownRepositoryContentConsumer#auto-remove" )
@Scope( "prototype" )
public class AutoRemoveConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{

    private Logger log = LoggerFactory.getLogger( AutoRemoveConsumer.class );

    /**
     * default-value="auto-remove"
     */
    private String id = "auto-remove";

    /**
     * default-value="Automatically Remove File from Filesystem."
     */
    private String description = "Automatically Remove File from Filesystem.";

    /**
     *
     */
    @Inject
    private ArchivaConfiguration configuration;

    /**
     *
     */
    @Inject
    private FileTypes filetypes;

    private Path repositoryDir;

    private List<String> includes = new ArrayList<>( 0 );

    @Override
    public String getId( )
    {
        return this.id;
    }

    @Override
    public String getDescription( )
    {
        return this.description;
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = Paths.get( repository.getLocation( ) );
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    @Override
    public void completeScan( )
    {
        /* do nothing */
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan( );
    }

    @Override
    public List<String> getExcludes( )
    {
        return null;
    }

    @Override
    public List<String> getIncludes( )
    {
        return includes;
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        Path file = this.repositoryDir.resolve(path );
        if ( Files.exists(file) )
        {
            log.info( "(Auto) Removing File: {}", file.toAbsolutePath( ) );
            triggerConsumerInfo( "(Auto) Removing File: " + file.toAbsolutePath( ) );
            try
            {
                Files.deleteIfExists( file );
            }
            catch ( IOException e )
            {
                log.error("Could not delete file {}: {}", file, e.getMessage(), e);
                throw new ConsumerException( "Could not delete file "+file );
            }
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes( );
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes( )
    {
        includes = new ArrayList<>( filetypes.getFileTypePatterns( FileTypes.AUTO_REMOVE ) );
    }

    @PostConstruct
    public void initialize( )
    {
        configuration.addChangeListener( this );

        initIncludes( );
    }
}
