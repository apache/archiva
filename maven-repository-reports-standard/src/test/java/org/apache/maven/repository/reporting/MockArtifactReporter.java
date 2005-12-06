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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mock implementation of the artifact reporter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class MockArtifactReporter
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

    public void addWarning( Artifact artifact, String reason )
    {
        artifactWarnings.add( new ArtifactResult( artifact, reason ) );
    }

    public void addFailure( RepositoryMetadata metadata, String reason )
    {
        metadataFailures.add( new RepositoryMetadataResult( metadata, reason ) );
    }

    public void addSuccess( RepositoryMetadata metadata )
    {
        metadataSuccesses.add( new RepositoryMetadataResult( metadata ) );
    }

    public void addWarning( RepositoryMetadata metadata, String reason )
    {
        metadataWarnings.add( new RepositoryMetadataResult( metadata, reason ) );
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

    public int getFailures()
    {
        return artifactFailures.size();
    }

    public int getSuccesses()
    {
        return artifactSuccesses.size();
    }

    public int getWarnings()
    {
        return artifactWarnings.size();
    }
}
