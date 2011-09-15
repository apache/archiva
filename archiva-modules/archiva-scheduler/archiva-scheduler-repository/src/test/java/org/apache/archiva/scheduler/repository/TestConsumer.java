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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.LayoutException;
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

    public String getId()
    {
        return "test-consumer";
    }

    public String getDescription()
    {
        return null;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public List<String> getIncludes()
    {
        return Collections.singletonList( "**/**" );
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        consumed.clear();

        try
        {
            this.repository = factory.getManagedRepositoryContent( repository.getId() );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

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

    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        processFile( path );
    }

    public void completeScan()
    {
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public Collection<ArtifactReference> getConsumed()
    {
        return consumed;
    }
}