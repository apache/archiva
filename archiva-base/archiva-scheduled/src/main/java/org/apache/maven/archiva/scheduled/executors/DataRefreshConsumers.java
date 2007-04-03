package org.apache.maven.archiva.scheduled.executors;

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
import java.util.Iterator;
import java.util.List;

/**
 * Mutable list of consumer for the Data Refresh.
 * 
 * NOTE: This class only exists to minimize the requirements of manual component management.
 *       This approach allows for a small and simple component definition in the application.xml
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.scheduler.executors.DataRefreshConsumers"
 */
public class DataRefreshConsumers
{
    /**
     * @plexus.configuration
     */
    private List consumerNames;

    public List getConsumerNames()
    {
        if ( consumerNames == null )
        {
            consumerNames = new ArrayList();
            consumerNames.add( "index-artifact" );
            consumerNames.add( "artifact-health" );
            consumerNames.add( "metadata-health" );
        }

        return consumerNames;
    }

    public Iterator iterator()
    {
        return getConsumerNames().iterator();
    }
}
