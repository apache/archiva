package org.apache.archiva.consumers;

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

import org.apache.archiva.common.FileTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AbstractMonitoredConsumer 
 *
 *
 */
public abstract class AbstractMonitoredConsumer
    implements Consumer
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<ConsumerMonitor> monitors = new HashSet<ConsumerMonitor>();
    
    @Override
    public void addConsumerMonitor( ConsumerMonitor monitor )
    {
        monitors.add( monitor );
    }

    @Override
    public void removeConsumerMonitor( ConsumerMonitor monitor )
    {
        monitors.remove( monitor );
    }

    protected void triggerConsumerError( String type, String message )
    {
        for ( ConsumerMonitor monitor : monitors ) 
        {
            try
            {
                monitor.consumerError( this, type, message );
            }
            catch ( Throwable t )
            {
                /* discard error */
            }
        }
    }

    protected void triggerConsumerWarning( String type, String message )
    {
        for ( ConsumerMonitor monitor : monitors ) 
        {
            try
            {
                monitor.consumerWarning( this, type, message );
            }
            catch ( Throwable t )
            {
                /* discard error */
            }
        }
    }

    protected void triggerConsumerInfo( String message )
    {
        for ( ConsumerMonitor monitor : monitors ) 
        {
            try
            {
                monitor.consumerInfo( this, message );
            }
            catch ( Throwable t )
            {
                /* discard error */
            }
        }
    }

    public boolean isProcessUnmodified()
    {
        return false;
    }

    protected List<String> getDefaultArtifactExclusions()
    {
        return FileTypeUtils.DEFAULT_EXCLUSIONS;
    }
    
    
}
