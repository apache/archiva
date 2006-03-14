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
 * Artifact discoverer for the legacy repository layout (Maven 1.x).
 *
 * @author John Casey
 * @author Brett Porter
 * @plexus.component role="org.apache.maven.repository.discovery.ArtifactDiscoverer" role-hint="org.apache.maven.repository.discovery.LegacyArtifactDiscoverer"
 */
public class LegacyArtifactDiscoverer
    extends AbstractArtifactDiscoverer
    implements ArtifactDiscoverer
{
    private final static String POM = ".pom";

    private final static String DELIM = "\\";

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    public List discoverArtifacts( ArtifactRepository repository, String blacklistedPatterns, boolean includeSnapshots )
    {
        List artifacts = new ArrayList();

        File repositoryBase = new File( repository.getBasedir() );
        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = ArtifactUtils.buildArtifactFromLegacyPath( path, artifactFactory );
            if ( artifact != null )
            {
                artifact.setRepository( repository );
                artifact.setFile( new File( repositoryBase, path ) );

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
                Artifact pomArtifact = ArtifactUtils.buildArtifactFromLegacyPath( path, artifactFactory );
                if ( pomArtifact != null )
                {
                    pomArtifact.setFile( new File( repositoryBase, path ) );
                }

                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                String filename = repositoryBase.getAbsolutePath() + DELIM + path;
                try
                {
                    Model model = mavenReader.read( new FileReader( filename ) );
                    if ( ( pomArtifact != null ) && ( "pom".equals( model.getPackaging() ) ) )
                    {
                        if ( convertSnapshots || !pomArtifact.isSnapshot() )
                        {
                            artifacts.add( pomArtifact );
                        }
                    }
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
