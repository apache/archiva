package org.apache.archiva.scheduler.repository;

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

import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service( "knownRepositoryContentConsumer#test-consumer" )
public class TestConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private Set<ArtifactReference> consumed = new HashSet<ArtifactReference>();

    @Inject
    private RepositoryContentFactory factory;

    private ManagedRepositoryContent repository;

    @Override
    public String getId()
    {
        return "test-consumer";
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public List<String> getIncludes()
    {
        return Collections.singletonList( "**/**" );
    }

    @Override
    public List<String> getExcludes()
    {
        return null;
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        consumed.clear();

        this.repository = repository.getContent();
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
        if ( !path.endsWith( ".sha1" ) && !path.endsWith( ".md5" ) )
        {
            try
            {
                consumed.add( repository.toArtifactReference( path ) );
            }
            catch ( LayoutException e )
            {
                throw new ConsumerException( e.getMessage(), e );
            }
        }
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
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public Collection<ArtifactReference> getConsumed()
    {
        return consumed;
    }
}