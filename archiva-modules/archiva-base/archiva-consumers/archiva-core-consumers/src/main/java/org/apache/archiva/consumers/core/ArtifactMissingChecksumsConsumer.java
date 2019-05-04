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

import org.apache.archiva.checksum.*;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
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
 * ArtifactMissingChecksumsConsumer - Create missing and/or fix invalid checksums for the artifact.
 */
@Service( "knownRepositoryContentConsumer#create-missing-checksums" )
@Scope( "prototype" )
public class ArtifactMissingChecksumsConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
    // it's prototype bean so we assume configuration won't change during a run
    //, RegistryListener
{

    private Logger log = LoggerFactory.getLogger( ArtifactMissingChecksumsConsumer.class );

    private String id = "create-missing-checksums";

    private String description = "Create Missing and/or Fix Invalid Checksum files.";

    private ArchivaConfiguration configuration;

    private FileTypes filetypes;

    private static final String TYPE_CHECKSUM_NOT_FILE = "checksum-bad-not-file";

    private static final String TYPE_CHECKSUM_CANNOT_CALC = "checksum-calc-failure";

    private static final String TYPE_CHECKSUM_CANNOT_CREATE = "checksum-create-failure";

    private Path repositoryDir;

    private List<String> includes = new ArrayList<>( 0 );
    private List<ChecksumAlgorithm> algorithms;

    @Inject
    public ArtifactMissingChecksumsConsumer( ArchivaConfiguration configuration, FileTypes filetypes )
    {
        this.configuration = configuration;
        this.filetypes = filetypes;

        //configuration.addChangeListener( this );

        initIncludes( );
    }

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
    public void beginScan( ManagedRepository repo, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = Paths.get( repo.getLocation( ) );
    }

    @Override
    public void beginScan( ManagedRepository repo, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repo, whenGathered );
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
        return getDefaultArtifactExclusions( );
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
        Path artifactPath = repositoryDir.resolve(path);
        ChecksummedFile csFile = new ChecksummedFile(artifactPath);
        UpdateStatusList result = csFile.fixChecksums(algorithms);
        if (result.getTotalStatus()== UpdateStatus.ERROR) {
            log.warn( "Error accessing file {}. ", path );
            triggerConsumerWarning( TYPE_CHECKSUM_NOT_FILE,
                    "Error accessing file " + path + "." );
        } else {
            result.getStatusList().stream().forEach(st ->
                    triggerInfo(path, st));
        }
    }

    private void triggerInfo(String path, UpdateStatus status) {
        switch (status.getValue()) {
            case UpdateStatus.ERROR:
                log.error( "Cannot create checksum for file {} :", path, status.getError() );
                triggerConsumerError( TYPE_CHECKSUM_CANNOT_CREATE, "Cannot create checksum for file " + path +
                        ": " + status.getError().getMessage( ) );
                break;
            case UpdateStatus.CREATED:
                log.info( "Created missing checksum file {}", path );
                triggerConsumerInfo( "Created missing checksum file " + path );
                break;
            case UpdateStatus.UPDATED:
                log.info( "Fixed checksum file {}", path );
                triggerConsumerInfo( "Fixed checksum file " + path );
                break;

        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }


    /*
    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }


    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // do nothing
    }

    */

    private void initIncludes( )
    {
        includes = new ArrayList<>( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );

    }

    @PostConstruct
    public void initialize( )
    {
        //configuration.addChangeListener( this );

        initIncludes( );
        algorithms = ChecksumUtil.getAlgorithms(configuration.getConfiguration().getArchivaRuntimeConfiguration().getChecksumTypes());
    }
}
