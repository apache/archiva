package org.apache.archiva.converter.legacy;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.converter.artifact.ArtifactConversionException;
import org.apache.archiva.converter.artifact.ArtifactConverter;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.maven2.ManagedDefaultRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * LegacyConverterArtifactConsumer - convert artifacts as they are found
 * into the destination repository.
 *
 *
 */
@Service( "knownRepositoryContentConsumer#artifact-legacy-to-default-converter" )
@Scope( "prototype" )
public class LegacyConverterArtifactConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private Logger log = LoggerFactory.getLogger( LegacyConverterArtifactConsumer.class );

    @Inject
    @Named("artifactConverter#legacy-to-default")
    private ArtifactConverter artifactConverter;

    private ArtifactFactory artifactFactory;

    private ManagedRepositoryContent managedRepository;

    private ArtifactRepository destinationRepository;

    private List<String> includes;

    private List<String> excludes;

    @Inject
    public LegacyConverterArtifactConsumer( PlexusSisuBridge plexusSisuBridge )
        throws PlexusSisuBridgeException
    {
        includes = new ArrayList<>( 3 );
        includes.add( "**/*.jar" );
        includes.add( "**/*.ear" );
        includes.add( "**/*.war" );
        artifactFactory = plexusSisuBridge.lookup( ArtifactFactory.class );
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.managedRepository = new ManagedDefaultRepositoryContent();
        this.managedRepository.setRepository( repository );
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    @Override
    public void completeScan()
    {
        // no op
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    @Override
    public List<String> getExcludes()
    {
        return excludes;
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }

    @Override
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
            log.warn( "Unable to convert artifact: {} : {}",path , e.getMessage(), e );
        }
        catch ( ArtifactConversionException e )
        {
            log.warn( "Unable to convert artifact: {} : {}",path , e.getMessage(), e );
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        processFile( path );
    }

    @Override
    public String getDescription()
    {
        return "Legacy Artifact to Default Artifact Converter";
    }

    @Override
    public String getId()
    {
        return "artifact-legacy-to-default-converter";
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
