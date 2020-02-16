package org.apache.archiva.repository.content.base;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class that gives information about the physical location of artifacts.
 */
@Service( "ArtifactUtil#default" )
public class ArtifactUtil
{

    @Inject
    RepositoryContentFactory repositoryContentFactory;

    /**
     * Returns the physical location of a given artifact in the repository. There is no check for the
     * existence of the returned file.
     *
     * @param repository        The repository, where the artifact is stored.
     * @param artifactReference The artifact reference.
     * @return The absolute path to the artifact.
     * @throws RepositoryException
     */
    public Path getArtifactPath( ManagedRepository repository, ArtifactReference artifactReference ) throws RepositoryException
    {
        final ManagedRepositoryContent content = repositoryContentFactory.getManagedRepositoryContent( repository );
        final String artifactPath = content.toPath( artifactReference );
        return Paths.get( repository.getLocation( ) ).resolve( artifactPath );
    }

    /**
     * Returns the physical location of a given artifact in the repository. There is no check for the
     * existence of the returned file.
     *
     * @param repository        The repository, where the artifact is stored.
     * @param artifactReference The artifact reference.
     * @return The asset representation of the artifact.
     * @throws RepositoryException
     */
    public StorageAsset getArtifactAsset( ManagedRepository repository, ArtifactReference artifactReference ) throws RepositoryException
    {
        final ManagedRepositoryContent content = repositoryContentFactory.getManagedRepositoryContent( repository );
        final String artifactPath = content.toPath( artifactReference );
        return repository.getAsset( artifactPath );
    }

}
