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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.converter.artifact.ArtifactConversionException;
import org.apache.maven.archiva.converter.artifact.ArtifactConverter;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LegacyConverterArtifactConsumer - convert artifacts as they are found
 * into the destination repository. 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *     role-hint="artifact-legacy-to-default-converter"
 *     instantiation-strategy="per-lookup"
 */
public class LegacyConverterArtifactConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private Logger log = LoggerFactory.getLogger( LegacyConverterArtifactConsumer.class );
    
    /**
     * @plexus.requirement role-hint="legacy-to-default"
     */
    private ArtifactConverter artifactConverter;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    private ManagedRepositoryContent managedRepository;
    
    private ArtifactRepository destinationRepository;

    private List<String> includes;

    private List<String> excludes;

    public LegacyConverterArtifactConsumer()
    {
        includes = new ArrayList<String>();
        includes.add( "**/*.jar" );
        includes.add( "**/*.ear" );
        includes.add( "**/*.war" );
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        this.managedRepository = new ManagedDefaultRepositoryContent();
        this.managedRepository.setRepository( repository );
    }

    public void completeScan()
    {

    }

    public List<String> getExcludes()
    {
        return excludes;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        try
        {
            ArtifactReference reference = managedRepository.toArtifactReference( path );
            Artifact artifact = artifactFactory.createArtifact( reference.getGroupId(), reference.getArtifactId(),
                                                                reference.getVersion(), reference.getClassifier(),
                                                                reference.getType() );
            artifactConverter.convert( artifact, destinationRepository );
        }
        catch ( LayoutException e )
        {
            log.warn( "Unable to convert artifact: " + path + " : " + e.getMessage(), e );
        }
        catch ( ArtifactConversionException e )
        {
            log.warn( "Unable to convert artifact: " + path + " : " + e.getMessage(), e );
        }
    }

    public String getDescription()
    {
        return "Legacy Artifact to Default Artifact Converter";
    }

    public String getId()
    {
        return "artifact-legacy-to-default-converter";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
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
