package org.apache.maven.archiva.reporting.artifact;

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

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.OlderArtifactsByAgeConstraint;
import org.apache.maven.archiva.reporting.DataLimits;
import org.apache.maven.archiva.reporting.DynamicReportSource;

import java.util.List;

/**
 * OldArtifactReport 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.DynamicReportSource" 
 *                   role-hint="old-artifacts"
 */
public class OldArtifactReport
    implements DynamicReportSource
{
    /**
     * @plexus.configuration default-value="Old Artifacts Report"
     */
    private String name;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * The maximum age of an artifact before it is reported old, specified in days. The default is 1 year.
     *
     * @plexus.configuration default-value="365"
     */
    private int cutoffDays;

    public List getData()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return dao.getArtifactDAO().queryArtifacts( new OlderArtifactsByAgeConstraint( cutoffDays ) );
    }

    public List getData( DataLimits limits )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return dao.getArtifactDAO().queryArtifacts( new OlderArtifactsByAgeConstraint( cutoffDays ) );
    }

    public String getName()
    {
        return name;
    }
}
