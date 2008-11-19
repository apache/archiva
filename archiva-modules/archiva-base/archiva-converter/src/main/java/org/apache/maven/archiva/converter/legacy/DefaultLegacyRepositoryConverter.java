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

import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.scanner.RepositoryScanner;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DefaultLegacyRepositoryConverter 
 *
 * @version $Id$
 * @plexus.component 
 */
public class DefaultLegacyRepositoryConverter
    implements LegacyRepositoryConverter
{
    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArtifactRepositoryLayout defaultLayout;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer" 
     *                     role-hint="artifact-legacy-to-default-converter"
     */
    private LegacyConverterArtifactConsumer legacyConverterConsumer;

    /**
     * @plexus.requirement
     */
    private RepositoryScanner repoScanner;

    public void convertLegacyRepository( File legacyRepositoryDirectory, File repositoryDirectory,
                                         List fileExclusionPatterns )
        throws RepositoryConversionException
    {
        try
        {
            String defaultRepositoryUrl = PathUtil.toUrl( repositoryDirectory );

            ManagedRepositoryConfiguration legacyRepository = new ManagedRepositoryConfiguration();
            legacyRepository.setId( "legacy");
            legacyRepository.setName( "Legacy Repository" );
            legacyRepository.setLocation( legacyRepositoryDirectory.getAbsolutePath() );
            legacyRepository.setLayout( "legacy" );

            ArtifactRepository repository = artifactRepositoryFactory.createArtifactRepository( "default",
                                                                                                defaultRepositoryUrl,
                                                                                                defaultLayout, null,
                                                                                                null );
            legacyConverterConsumer.setExcludes( fileExclusionPatterns );
            legacyConverterConsumer.setDestinationRepository( repository );

            List knownConsumers = new ArrayList();
            knownConsumers.add( legacyConverterConsumer );

            List invalidConsumers = Collections.EMPTY_LIST;
            List ignoredContent = new ArrayList();
            ignoredContent.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );

            repoScanner.scan( legacyRepository, knownConsumers, invalidConsumers, ignoredContent,
                              RepositoryScanner.FRESH_SCAN );
        }
        catch ( RepositoryException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }
    }
}
