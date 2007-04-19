package org.apache.maven.archiva.consumers.database;

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
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ArtifactUpdateDatabaseConsumer - Take an artifact off of disk and put it into the repository.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.RepositoryContentConsumer"
 *                   role-hint="update-db-artifact"
 *                   instantiation-strategy="per-lookup"
 */
public class ArtifactUpdateDatabaseConsumer
    extends AbstractMonitoredConsumer
    implements RepositoryContentConsumer, RegistryListener, Initializable
{
    private static final String TYPE_NOT_ARTIFACT = "file-not-artifact";

    private static final String DB_ERROR = "db-error";

    private static final String CHECKSUM_CALCULATION = null;

    /**
     * @plexus.configuration default-value="update-db-artifact"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Update the Artifact in the Database"
     */
    private String description;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    /**
     * @plexus.requirement role-hint="md5";
     */
    private Digester digestMd5;

    private ArchivaRepository repository;

    private File repositoryDir;

    private BidirectionalRepositoryLayout layout;

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
        return true;
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
        this.repositoryDir = new File( repository.getUrl().getPath() );

        try
        {
            this.layout = layoutFactory.getLayout( repository.getModel().getLayoutName() );
        }
        catch ( LayoutException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    public void processFile( String path )
        throws ConsumerException
    {
        try
        {
            ArchivaArtifact artifact = layout.toArtifact( path );

            artifact.getModel().setRepositoryId( this.repository.getId() );

            // Calculate the hashcodes.
            File artifactFile = new File( this.repositoryDir, path );
            try
            {
                artifact.getModel().setChecksumMD5( digestMd5.calc( artifactFile ) );
            }
            catch ( DigesterException e )
            {
                triggerConsumerWarning( CHECKSUM_CALCULATION, "Unable to calculate the MD5 checksum: " + e.getMessage() );
            }

            try
            {
                artifact.getModel().setChecksumSHA1( digestSha1.calc( artifactFile ) );
            }
            catch ( DigesterException e )
            {
                triggerConsumerWarning( CHECKSUM_CALCULATION, "Unable to calculate the SHA1 checksum: "
                    + e.getMessage() );
            }

            artifact.getModel().setLastModified( new Date( artifactFile.lastModified() ) );
            artifact.getModel().setSize( artifactFile.length() );
            artifact.getModel().setOrigin( "FileSystem" );

            dao.getArtifactDAO().saveArtifact( artifact );
        }
        catch ( LayoutException e )
        {
            triggerConsumerError( TYPE_NOT_ARTIFACT, "Path " + path + " cannot be converted to artifact: "
                + e.getMessage() );
        }
        catch ( ArchivaDatabaseException e )
        {
            triggerConsumerError( DB_ERROR, "Unable to save artifact to database: " + e.getMessage() );
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

        FileType artifactTypes = configuration.getConfiguration().getRepositoryScanning().getFileTypeById( "artifacts" );
        if ( artifactTypes != null )
        {
            includes.addAll( artifactTypes.getPatterns() );
        }
    }

    public void initialize()
        throws InitializationException
    {
        configuration.addChangeListener( this );

        initIncludes();

        if ( includes.isEmpty() )
        {
            throw new InitializationException( "Unable to use " + getId() + " due to empty includes list." );
        }
    }
}
