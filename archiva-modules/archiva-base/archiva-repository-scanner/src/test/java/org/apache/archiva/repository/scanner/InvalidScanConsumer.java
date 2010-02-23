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

import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;

/**
 * InvalidScanConsumer 
 *
 * @version $Id$
 */
public class InvalidScanConsumer
    extends AbstractMonitoredConsumer
    implements InvalidRepositoryContentConsumer
{
    /**
     * @plexus.configuration default-value="unset-id"
     */
    private String id = "unset-id";
    
    private int processCount = 0;

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        /* do nothing */
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return null;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        processCount++;
    }

    public String getDescription()
    {
        return "Bad Content Scan Consumer (for testing)";
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }
}
