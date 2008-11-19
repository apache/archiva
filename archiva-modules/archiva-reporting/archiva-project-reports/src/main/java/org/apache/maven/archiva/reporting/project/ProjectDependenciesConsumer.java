package org.apache.maven.archiva.reporting.project;

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
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.List;

/**
 * ProjectDependenciesConsumer 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.ArchivaArtifactConsumer"
 *                   role-hint="missing-dependencies"
 */
public class ProjectDependenciesConsumer
    extends AbstractMonitoredConsumer
    implements ArchivaArtifactConsumer
{
    /**
     * @plexus.configuration default-value="missing-dependencies"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Check for missing dependencies."
     */
    private String description;

    private List includes;

    public ProjectDependenciesConsumer()
    {
        this.includes = new ArrayList();
        this.includes.add( "pom" );
    }

    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan()
    {
        /* do nothing */
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List getIncludedTypes()
    {
        return includes;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        // TODO: consider loading this logic into the 'update-db-project' consumer. 
        
        // TODO: Load the ArchivaProjectModel.
        // TODO: Attach a monitor for missing parent poms to resolvers / filters.
        // TODO: Attach a monitor for missing dependencies to resolvers / filters.
        // TODO: Fully resolve the ArchivaProjectModel and listen on monitors.
    }
}
