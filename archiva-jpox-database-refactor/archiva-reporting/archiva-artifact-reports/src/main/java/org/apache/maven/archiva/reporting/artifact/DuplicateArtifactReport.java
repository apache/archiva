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
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByTypeConstraint;
import org.apache.maven.archiva.reporting.DataLimits;
import org.apache.maven.archiva.reporting.DynamicReportSource;

import java.util.List;

/**
 * DuplicateArtifactReport 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.DynamicReportSource" 
 *                   role-hint="duplicate-artifacts"
 */
public class DuplicateArtifactReport
    implements DynamicReportSource
{
    public static final String PROBLEM_TYPE_DUPLICATE_ARTIFACTS = "duplicate-artifacts";

    /**
     * @plexus.configuration default-value="Duplicate Artifact Report"
     */
    private String name;

    /**
     * @plexus.requirement
     */
    private ArchivaDAO dao;

    private Constraint constraint;

    public DuplicateArtifactReport()
    {
        constraint = new RepositoryProblemByTypeConstraint( PROBLEM_TYPE_DUPLICATE_ARTIFACTS );
    }

    public List getData()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return dao.getRepositoryProblemDAO().queryRepositoryProblems( constraint );
    }

    public List getData( DataLimits limits )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO: implement limits.        
        return dao.getRepositoryProblemDAO().queryRepositoryProblems( constraint );
    }

    public String getName()
    {
        return name;
    }
}
