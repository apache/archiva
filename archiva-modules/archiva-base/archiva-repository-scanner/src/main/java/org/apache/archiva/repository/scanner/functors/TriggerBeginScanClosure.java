package org.apache.archiva.repository.scanner.functors;

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
import org.apache.commons.collections.Closure;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * TriggerBeginScanClosure 
 *
 *
 */
public class TriggerBeginScanClosure
    implements Closure
{
    private Logger log = LoggerFactory.getLogger( TriggerBeginScanClosure.class );
    
    private ManagedRepository repository;
    
    private Date whenGathered;

    private boolean executeOnEntireRepo = true;

    public TriggerBeginScanClosure( ManagedRepository repository )
    {
        this.repository = repository;
    }
    
    public TriggerBeginScanClosure( ManagedRepository repository, Date whenGathered )
    {
        this( repository );
        this.whenGathered = whenGathered;
    }

    public TriggerBeginScanClosure( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
    {
        this( repository, whenGathered );
        this.executeOnEntireRepo = executeOnEntireRepo;
    }

    @Override
    public void execute( Object input )
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;
                
            try
            {
                consumer.beginScan( repository, whenGathered, executeOnEntireRepo );
            }
            catch ( ConsumerException e )
            {
                log.warn( "Consumer [{}] cannot begin: {}",consumer.getId(), e.getMessage(), e );
            }
        }
    }
}
