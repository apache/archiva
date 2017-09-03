package org.apache.archiva.converter;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.converter.legacy.LegacyRepositoryConverter;
import org.apache.commons.io.FileUtils;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;

/**
 * Test the repository converter.
 *
 * @todo what about deletions from the source repository?
 * @todo use artifact-test instead
 * @todo should reject if dependencies are missing - rely on reporting?
 * @todo group metadata
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath:/spring-context.xml"} )
public class RepositoryConverterTest
    extends TestCase
{
    private ArtifactRepository sourceRepository;

    private ManagedRepositoryConfiguration targetRepository;

    @Inject
    @Named(value = "legacyRepositoryConverter#default")
    private LegacyRepositoryConverter repositoryConverter;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactRepositoryFactory factory = plexusSisuBridge.lookup( ArtifactRepositoryFactory.class );
            //(ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "legacy" );
            //(ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        Path sourceBase = Paths.get( "src/test/source-repository" );
        sourceRepository = factory.createArtifactRepository( "source", sourceBase.toUri().toURL().toString(), layout, null,
                                                             null );

        layout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "default" );
            //(ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        Path targetBase = Paths.get( "target/test-target-repository" );
        copyDirectoryStructure( Paths.get( "src/test/target-repository" ), targetBase );

        targetRepository = new ManagedRepositoryConfiguration();
        targetRepository.setId( "target" );
        targetRepository.setName( "Target Repo" );
        targetRepository.setLocation( targetBase.toAbsolutePath().toString() );
        targetRepository.setLayout( "default" );

        //repositoryConverter = (LegacyRepositoryConverter) lookup( LegacyRepositoryConverter.ROLE, "default" );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    private void copyDirectoryStructure( Path sourceDirectory, Path destinationDirectory )
        throws IOException
    {
        if ( !Files.exists(sourceDirectory) )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.toAbsolutePath() + ")." );
        }

        Path[] files = Files.list(sourceDirectory).toArray(Path[]::new);

        String sourcePath = sourceDirectory.toAbsolutePath().toString();

        for ( int i = 0; i < files.length; i++ )
        {
            Path file = files[i];

            String dest = file.toAbsolutePath().toString();

            dest = dest.substring( sourcePath.length() + 1 );

            Path destination = destinationDirectory.resolve( dest );

            if ( Files.isRegularFile(file) )
            {
                destination = destination.getParent();

                FileUtils.copyFileToDirectory( file.toFile(), destination.toFile() );
            }
            else if ( Files.isDirectory(file) )
            {
                if ( !".svn".equals( file.getFileName().toString() ) )
                {
                    if ( !Files.exists(destination))
                    {
                        Files.createDirectories( destination );
                    }
                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.toAbsolutePath() );
            }
        }
    }

    @Test
    public void testLegacyConversion()
        throws IOException, RepositoryConversionException
    {
        Path legacyRepoDir = Paths.get( sourceRepository.getBasedir() );
        Path destRepoDir = Paths.get( targetRepository.getLocation() );
        List<String> excludes = new ArrayList<>();
        repositoryConverter.convertLegacyRepository( legacyRepoDir, destRepoDir, excludes );
    }
}
