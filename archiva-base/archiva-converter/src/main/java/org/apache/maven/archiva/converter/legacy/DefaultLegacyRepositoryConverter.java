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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.scanner.RepositoryScanner;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DefaultLegacyRepositoryConverter 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
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
     * @plexus.requirement role="org.apache.maven.archiva.consumers.RepositoryContentConsumer" 
     *                     role-hint="artifact-legacy-to-default-converter"
     */
    private LegacyConverterArtifactConsumer legacyConverterConsumer;

    public void convertLegacyRepository( File legacyRepositoryDirectory, File repositoryDirectory,
                                         List fileExclusionPatterns )
        throws RepositoryConversionException
    {
        try
        {
            String legacyRepositoryUrl = PathUtil.toUrl( legacyRepositoryDirectory );
            String defaultRepositoryUrl = PathUtil.toUrl( repositoryDirectory );

            // workaround for spaces non converted by PathUtils in wagon
            // TODO: remove it when PathUtils will be fixed
            if ( legacyRepositoryUrl.indexOf( "%20" ) >= 0 )
            {
                legacyRepositoryUrl = StringUtils.replace( legacyRepositoryUrl, "%20", " " );
            }
            if ( defaultRepositoryUrl.indexOf( "%20" ) >= 0 )
            {
                defaultRepositoryUrl = StringUtils.replace( defaultRepositoryUrl, "%20", " " );
            }

            ArchivaRepository legacyRepository = new ArchivaRepository( "legacy", "Legacy Repository",
                                                                        legacyRepositoryUrl );
            legacyRepository.getModel().setLayoutName( "legacy" );

            ArtifactRepository repository = artifactRepositoryFactory.createArtifactRepository( "default",
                                                                                                defaultRepositoryUrl,
                                                                                                defaultLayout, null,
                                                                                                null );
            legacyConverterConsumer.setExcludes( fileExclusionPatterns );
            legacyConverterConsumer.setDestinationRepository( repository );

            List consumers = new ArrayList();
            consumers.add( legacyConverterConsumer );

            RepositoryScanner scanner = new RepositoryScanner();
            scanner.scan( legacyRepository, consumers, true );
        }
        catch ( RepositoryException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }
    }
}
