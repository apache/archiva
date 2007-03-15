package org.apache.maven.archiva.common.consumers;

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

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * DiscovererConsumerFactory - factory for consumers.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.common.consumers.ConsumerFactory"
 */
public class ConsumerFactory
    extends AbstractLogEnabled
    implements Contextualizable
{
    public static final String ROLE = ConsumerFactory.class.getName();

    private PlexusContainer container;

    public Consumer createConsumer( String name )
        throws ConsumerException
    {
        getLogger().info( "Attempting to create consumer [" + name + "]" );

        Consumer consumer;
        try
        {
            consumer = (Consumer) container.lookup( Consumer.ROLE, name, container.getLookupRealm() );
        }
        catch ( Throwable t )
        {
            String emsg = "Unable to create consumer [" + name + "]: " + t.getMessage();
            getLogger().warn( t.getMessage(), t );
            throw new ConsumerException( null, emsg, t );
        }

        getLogger().info( "Created consumer [" + name + "|" + consumer.getName() + "]" );
        return consumer;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
