package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    private final ArtifactRepository repository;

    public DefaultRepositoryQueryLayer( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public boolean containsArtifact( Artifact artifact )
    {
        File f = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        return f.exists();
    }
}
