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
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * ScanConsumer 
 *
 *
 */
public class KnownScanConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private int processCount = 0;

    private List<String> includes = new ArrayList<>();

    private boolean processUnmodified = false;

    @Override
    public List<String> getExcludes()
    {
        return null;
    }

    public void setIncludes( String includesArray[] )
    {
        this.includes.clear();
        this.includes.addAll( Arrays.asList( includesArray ) );
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }

    @Override
    public String getId()
    {
        return "test-scan-consumer";
    }

    @Override
    public String getDescription()
    {
        return "Scan Consumer (for testing)";
    }

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
    public void processFile( String path )
        throws ConsumerException
    {
        logger.info( "Processing {}", path);
        this.processCount++;
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        processFile( path );
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

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }

    @Override
    public boolean isProcessUnmodified()
    {
        return processUnmodified;
    }

    public void setProcessUnmodified( boolean processUnmodified )
    {
        this.processUnmodified = processUnmodified;
    }
}
