package org.apache.maven.repository.discovery;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.ArtifactUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Artifact discoverer for the new repository layout (Maven 2.0+).
 *
 * @author John Casey
 * @author Brett Porter
 * @plexus.component role="org.apache.maven.repository.discovery.ArtifactDiscoverer" role-hint="org.apache.maven.repository.discovery.DefaultArtifactDiscoverer"
 */
public class DefaultArtifactDiscoverer
    extends AbstractArtifactDiscoverer
    implements ArtifactDiscoverer
{
    private final static String POM = ".pom";

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    public List discoverArtifacts( ArtifactRepository repository, String blacklistedPatterns, boolean includeSnapshots )
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported" );
        }

        File repositoryBase = new File( repository.getBasedir() );

        List artifacts = new ArrayList();

        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = ArtifactUtils.buildArtifact( repositoryBase, path, repository, artifactFactory );

            if ( artifact != null )
            {
                if ( includeSnapshots || !artifact.isSnapshot() )
                {
                    artifacts.add( artifact );
                }
            }
            else
            {
                addKickedOutPath( path );
            }
        }

        return artifacts;
    }

    public List discoverStandalonePoms( ArtifactRepository repository, String blacklistedPatterns,
                                        boolean convertSnapshots )
    {
        List artifacts = new ArrayList();

        File repositoryBase = new File( repository.getBasedir() );

        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            if ( path.toLowerCase().endsWith( POM ) )
            {
                Artifact pomArtifact = ArtifactUtils.buildArtifact( path, artifactFactory );
                if ( pomArtifact != null )
                {
                    pomArtifact.setFile( new File( repositoryBase, path ) );
                }

                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                String filename = repositoryBase.getAbsolutePath() + "/" + path;
                try
                {
                    Model model = mavenReader.read( new FileReader( filename ) );
                    if ( ( model != null ) && ( "pom".equals( model.getPackaging() ) ) )
                    {
                        artifacts.add( model );
                    }
                    /*if ( ( pomArtifact != null ) && ( "pom".equals( model.getPackaging() ) ) )
                    {
                        if ( convertSnapshots || !pomArtifact.isSnapshot() )
                        {
                            artifacts.add( pomArtifact );
                        }
                    }
                    */
                }
                catch ( Exception e )
                {
                    getLogger().info( "error reading file: " + filename );
                    e.printStackTrace();
                }
            }
        }

        return artifacts;
    }
}
