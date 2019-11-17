package $package;

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
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>SimpleArtifactConsumer</code>
 * 
 */
@Service("knownRepositoryContentConsumer#simple")
@Scope("prototype")
public class SimpleArtifactConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{

    private Logger log = LoggerFactory.getLogger( SimpleArtifactConsumer.class );

    /**
     * default-value="simple-artifact-consumer"
     */
    private String id = "simple-artifact-consumer";

    private String description = "Simple consumer to illustrate how to consume the contents of a repository.";

    @Inject
    private FileTypes filetypes;

    @Inject
    private ArchivaConfiguration configuration;

    private List<String> propertyNameTriggers = new ArrayList<>();

    private List<String> includes = new ArrayList<>();

    /** current repository being scanned */
    private ManagedRepository repository;

    @Inject
    @Named( value = "repositoryContentFactory#default" )
    private RepositoryContentFactory repositoryContentFactory;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private RepositorySession repositorySession;

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        beginScan( repository, whenGathered, true );
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        this.repository = repository;
        log.info( "Beginning scan of repository [{}]", this.repository.getId() );

        try
        {
            repositorySession = repositorySessionFactory.createSession( );
        } catch (MetadataRepositoryException e) {
            log.error("Could not create repository session {}", e.getMessage());
            throw new ConsumerException( "Could not create repository session: " + e.getMessage( ), e );
        }
    }

    public void processFile( String path )
        throws ConsumerException
    {
        processFile( path, true );
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        log.info( "Processing entry [{}] from repository [{}]", path, this.repository.getId() );

        try
        {
            ManagedRepositoryContent repositoryContent = repository.getContent();
            ArtifactReference artifact = repositoryContent.toArtifactReference( path );

            repositorySession.getRepository().getArtifacts( repositorySession, repository.getId(), artifact.getGroupId(),
                                                            artifact.getArtifactId(), artifact.getVersion() );
        }
        catch ( LayoutException | MetadataResolutionException  e )
        {
            throw new ConsumerException( e.getLocalizedMessage(), e );
        }
    }

    public void completeScan()
    {
        completeScan( true );
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        log.info( "Finished scan of repository [" + this.repository.getId() + "]" );

        repositorySession.close();
    }


    /**
     * Used by archiva to determine if the consumer wishes to process all of a repository's entries or just those that
     * have been modified since the last scan.
     * 
     * @return boolean true if the consumer wishes to process all entries on each scan, false for only those modified
     *         since the last scan
     */
    public boolean isProcessUnmodified()
    {
        return super.isProcessUnmodified();
    }

    public void afterConfigurationChange( org.apache.archiva.components.registry.Registry registry, String propertyName, Object propertyValue )
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

    @PostConstruct
    public void initialize()
    {
        propertyNameTriggers = new ArrayList<>();
        propertyNameTriggers.add( "repositoryScanning" );
        propertyNameTriggers.add( "fileTypes" );
        propertyNameTriggers.add( "fileType" );
        propertyNameTriggers.add( "patterns" );
        propertyNameTriggers.add( "pattern" );

        configuration.addChangeListener( this );

        initIncludes();
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return this.includes;
    }

    public boolean isPermanent()
    {
        return false;
    }
}
