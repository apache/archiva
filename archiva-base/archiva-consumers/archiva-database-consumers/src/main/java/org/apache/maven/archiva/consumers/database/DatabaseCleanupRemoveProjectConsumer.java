package org.apache.maven.archiva.consumers.database;

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
import org.apache.maven.archiva.consumers.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.List;

/**
 * DatabaseCleanupRemoveProjectConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseCleanupConsumer"
 *                   role-hint="not-present-remove-db-project"
 *                   instantiation-strategy="per-lookup"
 */
public class DatabaseCleanupRemoveProjectConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    /**
     * @plexus.configuration default-value="not-present-remove-db-project"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Remove project from database if not present on filesystem."
     */
    private String description;

    public void beginScan()
    {
        // TODO Auto-generated method stub

    }

    public void completeScan()
    {
        // TODO Auto-generated method stub

    }

    public List getIncludedTypes()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        // TODO Auto-generated method stub

    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

}
