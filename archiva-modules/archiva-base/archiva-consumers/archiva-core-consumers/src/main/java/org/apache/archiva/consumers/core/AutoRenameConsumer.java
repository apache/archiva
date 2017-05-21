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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * AutoRenameConsumer
 *
 *
 */
@Service( "knownRepositoryContentConsumer#auto-rename" )
@Scope( "prototype" )
public class AutoRenameConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private Logger log = LoggerFactory.getLogger( AutoRenameConsumer.class ); 

    private String id = "auto-rename";

    private String description = "Automatically rename common artifact mistakes.";

    private static final String RENAME_FAILURE = "rename_failure";

    private File repositoryDir;

    private List<String> includes = new ArrayList<>( 3 );

    private Map<String, String> extensionRenameMap = new HashMap<>();

    public AutoRenameConsumer()
    {
        includes.add( "**/*.distribution-tgz" );
        includes.add( "**/*.distribution-zip" );
        includes.add( "**/*.plugin" );

        extensionRenameMap.put( ".distribution-tgz", ".tar.gz" );
        extensionRenameMap.put( ".distribution-zip", ".zip" );
        extensionRenameMap.put( ".plugin", ".jar" );
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = new File( repository.getLocation() );
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    @Override
    public void completeScan()
    {
        /* do nothing */
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    @Override
    public List<String> getExcludes()
    {
        return null;
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        File file = new File( this.repositoryDir, path );
        if ( file.exists() )
        {
            Iterator<String> itExtensions = this.extensionRenameMap.keySet().iterator();
            while ( itExtensions.hasNext() )
            {
                String extension = itExtensions.next();
                if ( path.endsWith( extension ) )
                {
                    String fixedExtension = this.extensionRenameMap.get( extension );
                    String correctedPath = path.substring( 0, path.length() - extension.length() ) + fixedExtension;
                    File to = new File( this.repositoryDir, correctedPath );
                    try
                    {
                        // Rename the file.
                        FileUtils.moveFile( file, to );
                    }
                    catch ( IOException e )
                    {
                        log.warn( "Unable to rename {} to {} :", path, correctedPath, e );
                        triggerConsumerWarning( RENAME_FAILURE, "Unable to rename " + path + " to " + correctedPath +
                            ": " + e.getMessage() );
                    }
                }
            }

            log.info( "(Auto) Removing File: {} ", file.getAbsolutePath() );
            triggerConsumerInfo( "(Auto) Removing File: " + file.getAbsolutePath() );
            file.delete();
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }
}
