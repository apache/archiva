package org.apache.archiva.web.mocks;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.ConsumerMonitor;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "InvalidRepositoryContentConsumer#mock" )
public class MockInvalidRepositoryContentConsumer
    implements InvalidRepositoryContentConsumer
{
    @Override
    public String getId()
    {
        return "foo";
    }

    @Override
    public String getDescription()
    {
        return "the foo";
    }

    @Override
    public void addConsumerMonitor( ConsumerMonitor monitor )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeConsumerMonitor( ConsumerMonitor monitor )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getIncludes()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getExcludes()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void completeScan()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isProcessUnmodified()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
