package org.apache.archiva.repository.scanner;

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

import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * InvalidScanConsumer 
 *
 *
 */
public class InvalidScanConsumer
    extends AbstractMonitoredConsumer
    implements InvalidRepositoryContentConsumer
{
    /**
     * default-value="unset-id"
     */
    private String id = "unset-id";

    private Logger logger = LoggerFactory.getLogger( getClass() );
    
    private int processCount = 0;

    private List<String> paths = new ArrayList<>( );

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        /* do nothing */
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
        return null;
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        logger.info( "processFile: {}", path );
        paths.add( path );
        processCount++;
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    @Override
    public String getDescription()
    {
        return "Bad Content Scan Consumer (for testing)";
    }

    @Override
    public String getId()
    {
        return id;
    }

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public List<String> getPaths()
    {
        return paths;
    }
}
