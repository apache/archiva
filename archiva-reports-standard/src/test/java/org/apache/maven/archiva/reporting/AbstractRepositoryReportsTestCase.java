package org.apache.maven.archiva.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * 
 */
public abstract class AbstractRepositoryReportsTestCase
    extends PlexusTestCase
{
    /**
     * This should only be used for the few that can't use the query layer.
     */
    protected ArtifactRepository repository;

    protected static final String remoteRepoUrl = "http://public.planetmirror.com/pub/maven2/";

    protected static final String remoteArtifactGroup = "HTTPClient";

    protected static final String remoteArtifactId = "HTTPClient";

    protected static final String remoteArtifactVersion = "0.3-3";

    protected static final String remoteArtifactScope = "compile";

    protected static final String remoteArtifactType = "jar";

    protected static final String remoteRepoId = "remote-repo";

    protected void setUp()
        throws Exception
    {
        super.setUp();
        File repositoryDirectory = getTestFile( "src/test/repository" );

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        repository = factory.createArtifactRepository( "repository", repositoryDirectory.toURL().toString(), layout,
                                                       null, null );
    }

}
