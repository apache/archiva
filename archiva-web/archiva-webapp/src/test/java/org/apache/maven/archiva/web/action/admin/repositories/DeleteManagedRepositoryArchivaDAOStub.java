package org.apache.maven.archiva.web.action.admin.repositories;

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

import java.io.Serializable;
import java.util.List;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.SimpleConstraint;

/**
 * DeleteManagedRepositoryArchivaDAOStub
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class DeleteManagedRepositoryArchivaDAOStub
    implements ArchivaDAO
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ProjectModelDAO projectModelDAO;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArtifactDAO artifactDAO;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private RepositoryContentStatisticsDAO repoContentStatisticsDAO;

    public List query( SimpleConstraint constraint )
    {
        return null;
    }

    public Object save( Serializable obj )
    {
        throw new UnsupportedOperationException( "query not implemented for stub" );
    }

    public ArtifactDAO getArtifactDAO()
    {
        return artifactDAO;
    }

    public ProjectModelDAO getProjectModelDAO()
    {
        return projectModelDAO;
    }

    public RepositoryProblemDAO getRepositoryProblemDAO()
    {
        throw new UnsupportedOperationException( "query not implemented for stub" );
    }

    public RepositoryContentStatisticsDAO getRepositoryContentStatisticsDAO()
    {
        return repoContentStatisticsDAO;
    }
}
