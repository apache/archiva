package org.apache.maven.repository.configuration;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create artifact repositories from a configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.repository.configuration.ConfiguredRepositoryFactory"
 */
public class DefaultConfiguredRepositoryFactory
    implements ConfiguredRepositoryFactory
{
    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repoFactory;

    public ArtifactRepository createRepository( RepositoryConfiguration configuration )
    {
        File repositoryDirectory = new File( configuration.getDirectory() );
        String repoDir = repositoryDirectory.toURI().toString();

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) repositoryLayouts.get( configuration.getLayout() );
        return repoFactory.createArtifactRepository( configuration.getId(), repoDir, layout, null, null );
    }

    public List createRepositories( Configuration configuration )
    {
        List repositories = new ArrayList( configuration.getRepositories().size() );

        for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
        {
            repositories.add( createRepository( (RepositoryConfiguration) i.next() ) );
        }

        return repositories;
    }
}
