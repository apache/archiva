package org.apache.maven.archiva.consumers;

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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiva.configuration.FileTypes;

/**
 * AbstractMonitoredConsumer 
 *
 * @version $Id$
 */
public abstract class AbstractMonitoredConsumer
    implements Consumer
{
    private Set<ConsumerMonitor> monitors = new HashSet<ConsumerMonitor>();
    
    public void addConsumerMonitor( ConsumerMonitor monitor )
    {
        monitors.add( monitor );
    }

    public void removeConsumerMonitor( ConsumerMonitor monitor )
    {
        monitors.remove( monitor );
    }

    protected void triggerConsumerError( String type, String message )
    {
        for ( Iterator<ConsumerMonitor> itmonitors = monitors.iterator(); itmonitors.hasNext(); )
        {
            ConsumerMonitor monitor = itmonitors.next();
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
        for ( Iterator<ConsumerMonitor> itmonitors = monitors.iterator(); itmonitors.hasNext(); )
        {
            ConsumerMonitor monitor = itmonitors.next();
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
        for ( Iterator<ConsumerMonitor> itmonitors = monitors.iterator(); itmonitors.hasNext(); )
        {
            ConsumerMonitor monitor = itmonitors.next();
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
        return FileTypes.DEFAULT_EXCLUSIONS;
    }
    
    
}
