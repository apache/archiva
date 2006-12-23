package org.apache.maven.archiva.layer;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class DefaultRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    protected ArtifactRepository repository;

    public DefaultRepositoryQueryLayer( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public boolean containsArtifact( Artifact artifact )
    {
        File f = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        return f.exists();
    }

    public List getVersions( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        Metadata metadata = getMetadata( artifact );

        return metadata.getVersioning().getVersions();
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    private Metadata getMetadata( Artifact artifact )
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
            catch ( FileNotFoundException e )
            {
                throw new RepositoryQueryLayerException( "Error occurred while attempting to read metadata file", e );
            }
            catch ( IOException e )
            {
                throw new RepositoryQueryLayerException( "Error occurred while attempting to read metadata file", e );
            }
            catch ( XmlPullParserException e )
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
