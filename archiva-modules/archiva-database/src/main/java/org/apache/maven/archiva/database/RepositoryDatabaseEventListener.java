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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;

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

        // TODO [MRM-37]: re-run the database consumers to clean up
    }
}
