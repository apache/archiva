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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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

    private final static String DELIM = "\\";

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
                Artifact pomArtifact = buildArtifact( path );

                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                String filename = repositoryBase.getAbsolutePath() + DELIM + path;
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
                    System.out.println( "error reading file: " + filename );
                    e.printStackTrace();
                }
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( String path )
    {
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( path, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );

        Artifact finalResult = null;
        if ( pathParts.size() < 4 )
        {
            addKickedOutPath( path );
        }
        else
        {
            // the actual artifact filename.
            String filename = (String) pathParts.remove( 0 );

            // the next one is the version.
            String version = (String) pathParts.remove( 0 );

            // the next one is the artifactId.
            String artifactId = (String) pathParts.remove( 0 );

            // the remaining are the groupId.
            Collections.reverse( pathParts );
            String groupId = StringUtils.join( pathParts.iterator(), "." );

            String remainingFilename = filename;
            if ( !remainingFilename.startsWith( artifactId + "-" ) )
            {
                addKickedOutPath( path );
            }
            else
            {
                remainingFilename = remainingFilename.substring( artifactId.length() + 1 );

                String classifier = null;

                // TODO: use artifact handler, share with legacy discoverer
                String type = null;
                if ( remainingFilename.endsWith( ".tar.gz" ) )
                {
                    type = "distribution-tgz";
                    remainingFilename =
                        remainingFilename.substring( 0, remainingFilename.length() - ".tar.gz".length() );
                }
                else if ( remainingFilename.endsWith( ".zip" ) )
                {
                    type = "distribution-zip";
                    remainingFilename = remainingFilename.substring( 0, remainingFilename.length() - ".zip".length() );
                }
                else if ( remainingFilename.endsWith( "-sources.jar" ) )
                {
                    type = "java-source";
                    classifier = "sources";
                    remainingFilename =
                        remainingFilename.substring( 0, remainingFilename.length() - "-sources.jar".length() );
                }
                else
                {
                    int index = remainingFilename.lastIndexOf( "." );
                    if ( index < 0 )
                    {
                        addKickedOutPath( path );
                    }
                    else
                    {
                        type = remainingFilename.substring( index + 1 );
                        remainingFilename = remainingFilename.substring( 0, index );
                    }
                }

                if ( type != null )
                {
                    Artifact result;

                    if ( classifier == null )
                    {
                        result = artifactFactory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME,
                                                                 type );
                    }
                    else
                    {
                        result = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                               classifier );
                    }

                    if ( result.isSnapshot() )
                    {
                        // version is XXX-SNAPSHOT, filename is XXX-yyyyMMdd.hhmmss-b
                        int classifierIndex = remainingFilename.indexOf( '-', version.length() + 8 );
                        if ( classifierIndex >= 0 )
                        {
                            classifier = remainingFilename.substring( classifierIndex + 1 );
                            remainingFilename = remainingFilename.substring( 0, classifierIndex );
                            result = artifactFactory.createArtifactWithClassifier( groupId, artifactId,
                                                                                   remainingFilename, type,
                                                                                   classifier );
                        }
                        else
                        {
                            result = artifactFactory.createArtifact( groupId, artifactId, remainingFilename,
                                                                     Artifact.SCOPE_RUNTIME, type );
                        }

                        // poor encapsulation requires we do this to populate base version
                        if ( !result.isSnapshot() )
                        {
                            addKickedOutPath( path );
                        }
                        else if ( !result.getBaseVersion().equals( version ) )
                        {
                            addKickedOutPath( path );
                        }
                        else
                        {
                            finalResult = result;
                        }
                    }
                    else if ( !remainingFilename.startsWith( version ) )
                    {
                        addKickedOutPath( path );
                    }
                    else if ( !remainingFilename.equals( version ) )
                    {
                        if ( remainingFilename.charAt( version.length() ) != '-' )
                        {
                            addKickedOutPath( path );
                        }
                        else
                        {
                            classifier = remainingFilename.substring( version.length() + 1 );
                            finalResult = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version,
                                                                                        type, classifier );
                        }
                    }
                    else
                    {
                        finalResult = result;
                    }
                }
            }
        }

        if ( finalResult != null )
        {
            finalResult.setFile( new File( path ) );
        }

        return finalResult;
    }
}
