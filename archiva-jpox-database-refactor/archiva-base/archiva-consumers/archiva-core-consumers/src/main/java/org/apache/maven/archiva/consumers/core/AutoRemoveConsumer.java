package org.apache.maven.archiva.consumers.core;

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
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AutoRemoveConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="auto-remove"
 *                   instantiation-strategy="per-lookup"
 */
public class AutoRemoveConsumer
    extends AbstractMonitoredConsumer
    implements RepositoryContentConsumer, RegistryListener, Initializable
{
    /**
     * @plexus.configuration default-value="auto-remove"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Automatically Remove File from Filesystem."
     */
    private String description;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private File repositoryDir;

    private List propertyNameTriggers = new ArrayList();

    private List includes = new ArrayList();

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

    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        if ( !repository.isManaged() )
        {
            throw new ConsumerException( "Consumer requires managed repository." );
        }

        this.repositoryDir = new File( repository.getUrl().getPath() );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File file = new File( this.repositoryDir, path );
        if ( file.exists() )
        {
            triggerConsumerInfo( "(Auto) Removing File: " + file.getAbsolutePath() );
            file.delete();
        }
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
            .getFileTypeById( "auto-remove" );
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
