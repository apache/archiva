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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.managed.ManagedRepository;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DefaultLegacyRepositoryConverter
 *
 * @version $Id$
 */
@Service( "legacyRepositoryConverter#default" )
public class DefaultLegacyRepositoryConverter
    implements LegacyRepositoryConverter
{
    /**
     *
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     *
     */
    private ArtifactRepositoryLayout defaultLayout;

    /**
     *
     */
    @Inject
    @Named( value = "knownRepositoryContentConsumer#artifact-legacy-to-default-converter" )
    private LegacyConverterArtifactConsumer legacyConverterConsumer;

    /**
     *
     */
    @Inject
    private RepositoryScanner repoScanner;

    @Inject
    public DefaultLegacyRepositoryConverter( PlexusSisuBridge plexusSisuBridge )
        throws PlexusSisuBridgeException
    {
        artifactRepositoryFactory = plexusSisuBridge.lookup( ArtifactRepositoryFactory.class );
        defaultLayout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "default" );
    }

    public void convertLegacyRepository( File legacyRepositoryDirectory, File repositoryDirectory,
                                         List<String> fileExclusionPatterns )
        throws RepositoryConversionException
    {
        try
        {
            String defaultRepositoryUrl = PathUtil.toUrl( repositoryDirectory );

            ManagedRepository legacyRepository = new ManagedRepository();
            legacyRepository.setId( "legacy" );
            legacyRepository.setName( "Legacy Repository" );
            legacyRepository.setLocation( legacyRepositoryDirectory.getAbsolutePath() );
            legacyRepository.setLayout( "legacy" );

            ArtifactRepository repository =
                artifactRepositoryFactory.createArtifactRepository( "default", defaultRepositoryUrl, defaultLayout,
                                                                    null, null );
            legacyConverterConsumer.setExcludes( fileExclusionPatterns );
            legacyConverterConsumer.setDestinationRepository( repository );

            List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<KnownRepositoryContentConsumer>();
            knownConsumers.add( legacyConverterConsumer );

            List<InvalidRepositoryContentConsumer> invalidConsumers = Collections.emptyList();
            List<String> ignoredContent = new ArrayList<String>();
            ignoredContent.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );

            repoScanner.scan( legacyRepository, knownConsumers, invalidConsumers, ignoredContent,
                              RepositoryScanner.FRESH_SCAN );
        }
        catch ( RepositoryScannerException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }
    }
}
