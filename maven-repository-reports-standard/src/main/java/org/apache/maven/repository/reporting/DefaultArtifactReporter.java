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

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultArtifactReporter
    implements ArtifactReporter
{

    public void addFailure( Artifact artifact, String reason )
    {
    }

    public void addSuccess( Artifact artifact )
    {
    }

    public void addWarning( Artifact artifact, String message )
    {
    }

    public void addWarning(org.apache.maven.artifact.repository.metadata.RepositoryMetadata metadata, String message)
    {
    }

    public void addFailure(org.apache.maven.artifact.repository.metadata.RepositoryMetadata metadata, String reason)
    {
    }

    public void addSuccess(org.apache.maven.artifact.repository.metadata.RepositoryMetadata metadata)
    {
    }
}
