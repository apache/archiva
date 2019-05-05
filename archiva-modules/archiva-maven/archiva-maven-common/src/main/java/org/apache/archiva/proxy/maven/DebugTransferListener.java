package org.apache.archiva.proxy.maven;
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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class DebugTransferListener
    implements TransferListener
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Override
    public void transferInitiated( TransferEvent transferEvent )
    {
        log.debug( "transferInitiated for resource {} on repository url {}", transferEvent.getResource().getName(),
                   transferEvent.getWagon().getRepository().getUrl() );
    }

    @Override
    public void transferStarted( TransferEvent transferEvent )
    {
        log.debug( "transferStarted for resource {} on repository url {}", transferEvent.getResource().getName(),
                   transferEvent.getWagon().getRepository().getUrl() );
    }

    @Override
    public void transferProgress( TransferEvent transferEvent, byte[] bytes, int i )
    {
        log.debug( "transferProgress for resource {} on repository url {}", transferEvent.getResource().getName(),
                   transferEvent.getWagon().getRepository().getUrl() );
    }

    @Override
    public void transferCompleted( TransferEvent transferEvent )
    {
        log.debug( "transferCompleted for resource {} on repository url {}", transferEvent.getResource().getName(),
                   transferEvent.getWagon().getRepository().getUrl() );
    }

    @Override
    public void transferError( TransferEvent transferEvent )
    {
        log.debug( "transferError for resource {} on repository url {}", transferEvent.getResource().getName(),
                   transferEvent.getWagon().getRepository().getUrl(), transferEvent.getException() );
    }

    @Override
    public void debug( String s )
    {
        log.debug( "wagon debug {}", s );
    }
}
