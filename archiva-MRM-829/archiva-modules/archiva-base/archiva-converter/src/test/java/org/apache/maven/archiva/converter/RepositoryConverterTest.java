package org.apache.maven.archiva.converter;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.converter.legacy.LegacyRepositoryConverter;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the repository converter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo what about deletions from the source repository?
 * @todo use artifact-test instead
 * @todo should reject if dependencies are missing - rely on reporting?
 * @todo group metadata
 */
public class RepositoryConverterTest
    extends PlexusInSpringTestCase
{
    private ArtifactRepository sourceRepository;

    private ManagedRepositoryConfiguration targetRepository;

    private LegacyRepositoryConverter repositoryConverter;

    private ArtifactFactory artifactFactory;

    private static final int SLEEP_MILLIS = 100;

    private I18N i18n;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        File sourceBase = getTestFile( "src/test/source-repository" );
        sourceRepository = factory.createArtifactRepository( "source", sourceBase.toURL().toString(), layout, null,
                                                             null );

        layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File targetBase = getTestFile( "target/test-target-repository" );
        copyDirectoryStructure( getTestFile( "src/test/target-repository" ), targetBase );

        targetRepository = new ManagedRepositoryConfiguration();
        targetRepository.setId( "target" );
        targetRepository.setName( "Target Repo" );
        targetRepository.setLocation( targetBase.getAbsolutePath() );
        targetRepository.setLayout( "default" );

        repositoryConverter = (LegacyRepositoryConverter) lookup( LegacyRepositoryConverter.ROLE, "default" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        i18n = (I18N) lookup( I18N.ROLE );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    private void copyDirectoryStructure( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destinationDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !".svn".equals( file.getName() ) )
                {
                    if ( !destination.exists() && !destination.mkdirs() )
                    {
                        throw new IOException( "Could not create destination directory '"
                            + destination.getAbsolutePath() + "'." );
                    }
                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    public void testLegacyConversion()
        throws IOException, RepositoryConversionException
    {
        File legacyRepoDir = new File( sourceRepository.getBasedir() );
        File destRepoDir = new File( targetRepository.getLocation() );
        List excludes = new ArrayList();
        repositoryConverter.convertLegacyRepository( legacyRepoDir, destRepoDir, excludes );
    }
}
