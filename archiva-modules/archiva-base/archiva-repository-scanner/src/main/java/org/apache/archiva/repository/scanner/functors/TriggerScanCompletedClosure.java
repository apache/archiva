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

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TriggerScanCompletedClosure
 */
public class TriggerScanCompletedClosure
    implements Closure
{
    private Logger log = LoggerFactory.getLogger( TriggerScanCompletedClosure.class );

    private final ManagedRepositoryConfiguration repository;

    private boolean executeOnEntireRepo = true;

    public TriggerScanCompletedClosure( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public TriggerScanCompletedClosure( ManagedRepositoryConfiguration repository, boolean executeOnEntireRepo )
    {
        this( repository );
        this.executeOnEntireRepo = executeOnEntireRepo;
    }

    public void execute( Object input )
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;
            consumer.completeScan( executeOnEntireRepo );
            log.debug( "Consumer [{}] completed for repository [{}]", consumer.getId(), repository.getId() );
        }
    }
}
