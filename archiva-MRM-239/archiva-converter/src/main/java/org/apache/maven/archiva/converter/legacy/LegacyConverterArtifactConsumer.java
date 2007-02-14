package org.apache.maven.archiva.converter.legacy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.converter.RepositoryConverter;
import org.apache.maven.archiva.discoverer.consumers.GenericArtifactConsumer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

/**
 * LegacyConverterArtifactConsumer - convert artifacts as they are found
 * into the destination repository. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.discoverer.DiscovererConsumer"
 *     role-hint="legacy-converter"
 *     instantiation-strategy="per-lookup"
 */
public class LegacyConverterArtifactConsumer
    extends GenericArtifactConsumer
{
    /**
     * @plexus.requirement
     */
    private RepositoryConverter repositoryConverter;

    private ArtifactRepository destinationRepository;

    public void processArtifact( Artifact artifact, File file )
    {
        try
        {
            repositoryConverter.convert( artifact, destinationRepository );
        }
        catch ( RepositoryConversionException e )
        {
            getLogger().error(
                               "Unable to convert artifact " + artifact + " to destination repository "
                                   + destinationRepository, e );
        }
    }

    public void processArtifactBuildFailure( File path, String message )
    {
        getLogger().error( "Artifact Build Failure on " + path + " : " + message );
    }

    public ArtifactRepository getDestinationRepository()
    {
        return destinationRepository;
    }

    public void setDestinationRepository( ArtifactRepository destinationRepository )
    {
        this.destinationRepository = destinationRepository;
    }
}
