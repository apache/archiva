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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.plexusbridge.DigesterUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ValidateChecksumConsumer - validate the provided checksum against the file it represents.
 *
 * @version $Id$
 */
@Service( "knownRepositoryContentConsumer#validate-checksum" )
@Scope( "prototype" )
public class ValidateChecksumConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private static final String NOT_VALID_CHECKSUM = "checksum-not-valid";

    private static final String CHECKSUM_NOT_FOUND = "checksum-not-found";

    private static final String CHECKSUM_DIGESTER_FAILURE = "checksum-digester-failure";

    private static final String CHECKSUM_IO_ERROR = "checksum-io-error";

    /**
     * default-value="validate-checksums"
     */
    private String id = "validate-checksums";

    /**
     * default-value="Validate checksums against file."
     */
    private String description = "Validate checksums against file.";

    /**
     *
     */
    private ChecksumFile checksum;

    /**
     *
     */
    private List<Digester> allDigesters;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private DigesterUtils digesterUtils;

    private File repositoryDir;

    private List<String> includes = new ArrayList<String>();

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

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = new File( repository.getLocation() );
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    public void completeScan()
    {
        /* nothing to do */
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return this.includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File checksumFile = new File( this.repositoryDir, path );
        try
        {
            if ( !checksum.isValidChecksum( checksumFile ) )
            {
                triggerConsumerWarning( NOT_VALID_CHECKSUM, "The checksum for " + checksumFile + " is invalid." );
            }
        }
        catch ( FileNotFoundException e )
        {
            triggerConsumerError( CHECKSUM_NOT_FOUND, "File not found during checksum validation: " + e.getMessage() );
        }
        catch ( DigesterException e )
        {
            triggerConsumerError( CHECKSUM_DIGESTER_FAILURE,
                                  "Digester failure during checksum validation on " + checksumFile );
        }
        catch ( IOException e )
        {
            triggerConsumerError( CHECKSUM_IO_ERROR, "Checksum I/O error during validation on " + checksumFile );
        }
    }

    public void processFile( String path, boolean executeOnEntireReDpo )
        throws Exception
    {
        processFile( path );
    }

    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        checksum = plexusSisuBridge.lookup( ChecksumFile.class );
        List<Digester> allDigesters = new ArrayList<Digester>( digesterUtils.getAllDigesters() );
        for ( Digester digester : allDigesters )
        {
            includes.add( "**/*" + digester.getFilenameExtension() );
        }
    }
}
