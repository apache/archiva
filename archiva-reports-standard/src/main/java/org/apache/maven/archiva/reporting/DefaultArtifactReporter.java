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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @plexus.component role="org.apache.maven.archiva.reporting.ArtifactReporter"
 */
public class DefaultArtifactReporter
    implements ArtifactReporter
{
    private List artifactFailures = new ArrayList();

    private List artifactSuccesses = new ArrayList();

    private List artifactWarnings = new ArrayList();

    private List metadataFailures = new ArrayList();

    private List metadataSuccesses = new ArrayList();

    private List metadataWarnings = new ArrayList();

    public void addFailure( Artifact artifact, String reason )
    {
        artifactFailures.add( new ArtifactResult( artifact, reason ) );
    }

    public void addSuccess( Artifact artifact )
    {
        artifactSuccesses.add( new ArtifactResult( artifact ) );
    }

    public void addWarning( Artifact artifact, String message )
    {
        artifactWarnings.add( new ArtifactResult( artifact, message ) );
    }

    public void addFailure( RepositoryMetadata metadata, String reason )
    {
        metadataFailures.add( new RepositoryMetadataResult( metadata, reason ) );
    }

    public void addSuccess( RepositoryMetadata metadata )
    {
        metadataSuccesses.add( new RepositoryMetadataResult( metadata ) );
    }

    public void addWarning( RepositoryMetadata metadata, String message )
    {
        metadataWarnings.add( new RepositoryMetadataResult( metadata, message ) );
    }

    public Iterator getArtifactFailureIterator()
    {
        return artifactFailures.iterator();
    }

    public Iterator getArtifactSuccessIterator()
    {
        return artifactSuccesses.iterator();
    }

    public Iterator getArtifactWarningIterator()
    {
        return artifactWarnings.iterator();
    }

    public Iterator getRepositoryMetadataFailureIterator()
    {
        return metadataFailures.iterator();
    }

    public Iterator getRepositoryMetadataSuccessIterator()
    {
        return metadataSuccesses.iterator();
    }

    public Iterator getRepositoryMetadataWarningIterator()
    {
        return metadataWarnings.iterator();
    }

    public int getNumFailures()
    {
        return artifactFailures.size() + metadataFailures.size();
    }

    public int getNumSuccesses()
    {
        return artifactSuccesses.size() + metadataSuccesses.size();
    }

    public int getNumWarnings()
    {
        return artifactWarnings.size() + metadataWarnings.size();
    }
}
