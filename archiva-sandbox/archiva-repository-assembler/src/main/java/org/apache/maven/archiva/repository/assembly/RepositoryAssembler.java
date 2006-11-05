package org.apache.maven.archiva.repository.assembly;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.io.File;
import java.util.Set;

/**
 * Component responsible for writing out {@link Set}s of artifacts to a local directory. The resultant repository
 * structure should be suitable for use as a remote repository.
 *
 * @author Jason van Zyl
 */
public interface RepositoryAssembler
{
    String ROLE = RepositoryAssembler.class.getName();

    /**
     * Write out a set of {@link org.apache.maven.artifact.Artifact}s, which are found in a
     * specified local repository and remote repositories, with a given {@link ArtifactRepositoryLayout}
     * to a specified directory.
     *
     * @param artifacts Artifacts to be written out to disk.
     * @param localRepository Local repository to check for artifacts in the provided set.
     * @param remoteRepositories Remote repositories to check for artifacts in the provided set.
     * @param repositoryLayout The repository layout to use for the target repository.
     * @param repositoryDirectory The directory to write out the repository in.
     * @throws RepositoryAssemblyException
     */
    public void assemble( Set artifacts,
                          File localRepository,
                          Set remoteRepositories,
                          ArtifactRepositoryLayout repositoryLayout,
                          File repositoryDirectory )
        throws RepositoryAssemblyException;

    /**
     * Write out a set of {@link org.apache.maven.artifact.Artifact}s, which are found in a
     * specified local repository and remote repositories, with a given {@link ArtifactRepositoryLayout}
     * to a specified directory.
     *
     * @param artifacts Artifacts to be written out to disk.
     * @param localRepository Local repository to check for artifacts in the provided set.
     * @param remoteRepositories Remote repositories to check for artifacts in the provided set.
     * @param artifactFilter Filter to use while processing artifacts. Can change or restrict given artifacts.
     * @param repositoryLayout The repository layout to use for the target repository.
     * @param repositoryDirectory The directory to write out the repository in.
     * @throws RepositoryAssemblyException
     */
    public void assemble( Set artifacts,
                          File localRepository,
                          Set remoteRepositories,
                          ArtifactFilter artifactFilter,
                          ArtifactRepositoryLayout repositoryLayout,
                          File repositoryDirectory )
        throws RepositoryAssemblyException;
}
