package org.apache.maven.archiva.repository.scanner;

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

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;

import java.util.List;

/**
 * SampleKnownConsumer 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *                   role-hint="sample-known"
 */
public class SampleKnownConsumer
  extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        /* nothing to do */
    }

    public void completeScan()
    {
        /* nothing to do */
    }

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return null;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        /* nothing to do */
    }

    public String getDescription()
    {
        return "Sample Known Consumer";
    }

    public String getId()
    {
        return "sample-known";
    }

    public boolean isPermanent()
    {
        return false;
    }
}
