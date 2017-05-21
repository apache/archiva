package org.apache.archiva.cli;

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
import org.apache.archiva.consumers.RepositoryContentConsumer;

import java.util.Date;

/**
 * AbstractProgressConsumer 
 *
 *
 */
public abstract class AbstractProgressConsumer
    extends AbstractMonitoredConsumer
    implements RepositoryContentConsumer
{
    private int count = 0;

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.count = 0;
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
        count++;
        if ( ( count % 1000 ) == 0 )
        {
            System.out.println( "Files Processed: " + count );
        }

    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );
    }

    @Override
    public void completeScan()
    {
        System.out.println( "Final Count of Artifacts processed by " + getId() + ": " + count );
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

}
