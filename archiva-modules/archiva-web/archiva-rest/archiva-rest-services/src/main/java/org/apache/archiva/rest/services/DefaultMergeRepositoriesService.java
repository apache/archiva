package org.apache.archiva.rest.services;
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

import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.archiva.stagerepository.merge.Maven2RepositoryMerger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service ( "mergeRepositoriesService#rest" )
public class DefaultMergeRepositoriesService
    extends AbstractRestService
    implements MergeRepositoriesService
{

    @Inject
    @Named ( value = "repositoryMerger#maven2" )
    private Maven2RepositoryMerger repositoryMerger;


    public List<Artifact> getMergeConflictedArtifacts( String repositoryId )
        throws ArchivaRestServiceException
    {
        String sourceRepoId = repositoryId + "-stage";
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            List<ArtifactMetadata> artifactMetadatas =
                repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), sourceRepoId,
                                                          repositoryId );

            return buildArtifacts( artifactMetadatas, repositoryId );
        }
        catch ( Exception e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }

    }
}
