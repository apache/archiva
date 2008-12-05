package org.apache.maven.archiva.database.updater;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestDatabaseUnprocessedConsumer 
 *
 * @version $Id$
 */
public class TestDatabaseUnprocessedConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseUnprocessedArtifactConsumer
{
    private Logger log = LoggerFactory.getLogger( TestDatabaseUnprocessedConsumer.class );
    
    private int countBegin = 0;

    private int countComplete = 0;

    private int countProcessed = 0;

    public void resetCount()
    {
        countBegin = 0;
        countProcessed = 0;
        countComplete = 0;
    }

    public void beginScan()
    {
        countBegin++;
    }

    public void completeScan()
    {
        countComplete++;
    }

    public List getIncludedTypes()
    {
        List types = new ArrayList();
        types.add( "pom" );
        types.add( "jar" );
        return types;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        log.info( "Processing Artifact: " + artifact );
        countProcessed++;
    }

    public String getDescription()
    {
        return "Test Consumer for Database Unprocessed";
    }

    public String getId()
    {
        return "test-db-unprocessed";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public int getCountBegin()
    {
        return countBegin;
    }

    public int getCountComplete()
    {
        return countComplete;
    }

    public int getCountProcessed()
    {
        return countProcessed;
    }
}
