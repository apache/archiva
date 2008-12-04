package org.apache.maven.archiva.database;

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

import java.util.List;

import org.apache.maven.archiva.database.constraints.RepositoryProblemByArtifactConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.codehaus.plexus.cache.Cache;

/**
 * Process repository management events and respond appropriately.
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.events.RepositoryListener" role-hint="database"
 */
public class RepositoryDatabaseEventListener
    implements RepositoryListener
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArtifactDAO artifactDAO;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private RepositoryProblemDAO repositoryProblemDAO;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ProjectModelDAO projectModelDAO;

    /**
     * @plexus.requirement role-hint="effective-project-cache"
     */
    private Cache effectiveProjectCache;

    public void deleteArtifact( ManagedRepositoryContent repository, ArchivaArtifact artifact )
    {
        try
        {
            ArchivaArtifact queriedArtifact =
                artifactDAO.getArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                         artifact.getClassifier(), artifact.getType() );
            artifactDAO.deleteArtifact( queriedArtifact );
        }
        catch ( ArchivaDatabaseException e )
        {
            // ignored
        }

        try
        {
            // Remove all repository problems related to this artifact
            Constraint artifactConstraint = new RepositoryProblemByArtifactConstraint( artifact );
            List<RepositoryProblem> repositoryProblems =
                repositoryProblemDAO.queryRepositoryProblems( artifactConstraint );

            if ( repositoryProblems != null )
            {
                for ( RepositoryProblem repositoryProblem : repositoryProblems )
                {
                    repositoryProblemDAO.deleteRepositoryProblem( repositoryProblem );
                }
            }
        }
        catch ( ArchivaDatabaseException e )
        {
            // ignored
        }

        if ( "pom".equals( artifact.getType() ) )
        {
            try
            {
                ArchivaProjectModel projectModel =
                    projectModelDAO.getProjectModel( artifact.getGroupId(), artifact.getArtifactId(),
                                                     artifact.getVersion() );

                projectModelDAO.deleteProjectModel( projectModel );

                // Force removal of project model from effective cache
                String projectKey = toProjectKey( projectModel );
                synchronized ( effectiveProjectCache )
                {
                    if ( effectiveProjectCache.hasKey( projectKey ) )
                    {
                        effectiveProjectCache.remove( projectKey );
                    }
                }
            }
            catch ( ArchivaDatabaseException e )
            {
                // ignored
            }
        }
    }

    private String toProjectKey( ArchivaProjectModel project )
    {
        StringBuilder key = new StringBuilder();

        key.append( project.getGroupId() ).append( ":" );
        key.append( project.getArtifactId() ).append( ":" );
        key.append( project.getVersion() );

        return key.toString();
    }
}
