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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksumReference;
import org.apache.archiva.checksum.ChecksumValidationException;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.apache.archiva.checksum.ChecksumValidationException.ValidationError.*;

/**
 * ValidateChecksumConsumer - validate the provided checksum against the file it represents.
 */
@Service( "knownRepositoryContentConsumer#validate-checksums" )
@Scope( "prototype" )
public class ValidateChecksumConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private Logger log = LoggerFactory.getLogger( ValidateChecksumConsumer.class );

    private static final String NOT_VALID_CHECKSUM = "checksum-not-valid";

    private static final String CHECKSUM_NOT_FOUND = "checksum-not-found";

    private static final String CHECKSUM_DIGESTER_FAILURE = "checksum-digester-failure";

    private static final String CHECKSUM_IO_ERROR = "checksum-io-error";

    private String id = "validate-checksums";

    private String description = "Validate checksums against file.";

    private Path repositoryDir;

    private List<String> includes;

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
        /* nothing to do */
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
        return this.includes;
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        Path checksumFile = this.repositoryDir.resolve( path );
        try
        {
            ChecksumReference cf = ChecksummedFile.getFromChecksumFile( checksumFile );
            if ( !cf.getFile().isValidChecksum( cf.getAlgorithm(), true )  )
            {
                log.warn( "The checksum for {} is invalid.", checksumFile );
                triggerConsumerWarning( NOT_VALID_CHECKSUM, "The checksum for " + checksumFile + " is invalid." );
            }
        }
        catch ( ChecksumValidationException e )
        {
            if (e.getErrorType()==READ_ERROR) {
                log.error( "Checksum read error during validation on {}", checksumFile );
                triggerConsumerError( CHECKSUM_IO_ERROR, "Checksum I/O error during validation on " + checksumFile );
            } else if (e.getErrorType()==INVALID_FORMAT || e.getErrorType()==DIGEST_ERROR) {
                log.error( "Digester failure during checksum validation on {}", checksumFile );
                triggerConsumerError( CHECKSUM_DIGESTER_FAILURE,
                    "Digester failure during checksum validation on " + checksumFile );
            } else if (e.getErrorType()==FILE_NOT_FOUND) {
                log.error( "File not found during checksum validation: ", e );
                triggerConsumerError( CHECKSUM_NOT_FOUND, "File not found during checksum validation: " + e.getMessage( ) );
            }
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireReDpo )
        throws Exception
    {
        processFile( path );
    }

    @PostConstruct
    public void initialize( )
    {
        Set<String> extensions = ChecksumAlgorithm.getAllExtensions();
        includes = new ArrayList<>( extensions.size() );
        for ( String ext : extensions )
        {
            includes.add( "**/*." + ext );
        }
    }
}
