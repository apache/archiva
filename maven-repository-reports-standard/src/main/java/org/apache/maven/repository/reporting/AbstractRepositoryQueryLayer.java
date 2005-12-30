package org.apache.maven.repository.reporting;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 *
 */
public abstract class AbstractRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    protected ArtifactRepository repository;

    public boolean containsArtifact( Artifact artifact )
    {
        File f = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        return f.exists();
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        String artifactPath = getSnapshotArtifactRepositoryPath( artifact, snapshot );
        File artifactFile = new File( artifactPath );
        return artifactFile.exists();
    }

    public List getVersions( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        Metadata metadata = getMetadata( artifact );

        return metadata.getVersioning().getVersions();
    }

    protected String getSnapshotArtifactRepositoryPath( Artifact artifact, Snapshot snapshot )
    {
        File f = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        String snapshotInfo = artifact.getVersion().replaceFirst( "SNAPSHOT", snapshot.getTimestamp() + "-" +
            snapshot.getBuildNumber() + ".pom" );
        File snapshotFile = new File( f.getParentFile(), artifact.getArtifactId() + "-" + snapshotInfo );
        return snapshotFile.getAbsolutePath();
    }

    protected Metadata getMetadata( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        Metadata metadata;

        ArtifactRepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata( artifact );
        String path = repository.pathOfRemoteRepositoryMetadata( repositoryMetadata );
        File metadataFile = new File( repository.getBasedir(), path );
        if ( metadataFile.exists() )
        {
            MetadataXpp3Reader reader = new MetadataXpp3Reader();
            try
            {
                metadata = reader.read( new FileReader( metadataFile ) );
            }
            catch ( Exception e )
            {
                throw new RepositoryQueryLayerException( "Error occurred while attempting to read metadata file", e );
            }
        }
        else
        {
            throw new RepositoryQueryLayerException( "Metadata not found: " + metadataFile.getAbsolutePath() );
        }

        return metadata;
    }
}
