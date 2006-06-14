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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for artifact discoverers.
 *
 * @author John Casey
 * @author Brett Porter
 */
public abstract class AbstractArtifactDiscoverer
    extends AbstractDiscoverer
    implements ArtifactDiscoverer
{
    /**
     * Standard patterns to exclude from discovery as they are not artifacts.
     */
    private static final String[] STANDARD_DISCOVERY_EXCLUDES = {"bin/**", "reports/**", ".maven/**", "**/*.md5",
        "**/*.MD5", "**/*.sha1", "**/*.SHA1", "**/*snapshot-version", "*/website/**", "*/licenses/**", "*/licences/**",
        "**/.htaccess", "**/*.html", "**/*.asc", "**/*.txt", "**/*.xml", "**/README*", "**/CHANGELOG*", "**/KEYS*"};

    private static final String POM = ".pom";

    /**
     * Scan the repository for artifact paths.
     */
    private String[] scanForArtifactPaths( File repositoryBase, String blacklistedPatterns )
    {
        return scanForArtifactPaths( repositoryBase, blacklistedPatterns, null, STANDARD_DISCOVERY_EXCLUDES );
    }

    /**
     * Return a list of artifacts found in a specified repository
     *
     * @param repository The ArtifactRepository to discover artifacts
     * @param blacklistedPatterns Comma-delimited list of string paths that will be excluded in the discovery
     * @param includeSnapshots if the repository contains snapshots which should also be included
     * @return list of artifacts
     */
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

            Artifact artifact;
            try
            {
                artifact = buildArtifactFromPath( path, repository );

                if ( includeSnapshots || !artifact.isSnapshot() )
                {
                    artifacts.add( artifact );
                }
            }
            catch ( DiscovererException e )
            {
                addKickedOutPath( path, e.getMessage() );
            }
        }

        return artifacts;
    }

    /**
     * Returns a list of pom packaging artifacts found in a specified repository
     *
     * @param repository The ArtifactRepository to discover artifacts
     * @param blacklistedPatterns Comma-delimited list of string paths that will be excluded in the discovery
     * @param includeSnapshots if the repository contains snapshots which should also be included
     * @return list of pom artifacts
     */
    public List discoverStandalonePoms( ArtifactRepository repository, String blacklistedPatterns,
                                        boolean includeSnapshots )
    {
        List artifacts = new ArrayList();

        File repositoryBase = new File( repository.getBasedir() );

        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            String filename = repositoryBase.getAbsolutePath() + "/" + path;

            if ( path.toLowerCase().endsWith( POM ) )
            {
                try
                {
                    Artifact pomArtifact = buildArtifactFromPath( path, repository );

                    MavenXpp3Reader mavenReader = new MavenXpp3Reader();

                    Model model = mavenReader.read( new FileReader( filename ) );
                    if ( pomArtifact != null && "pom".equals( model.getPackaging() ) )
                    {
                        if ( includeSnapshots || !pomArtifact.isSnapshot() )
                        {
                            artifacts.add( model );
                        }
                    }
                }
                catch ( FileNotFoundException e )
                {
                    // this should never happen
                    getLogger().error( "Error finding file during POM discovery: " + filename, e );
                }
                catch ( IOException e )
                {
                    getLogger().error( "Error reading file during POM discovery: " + filename + ": " + e );
                }
                catch ( XmlPullParserException e )
                {
                    getLogger().error(
                        "Parse error reading file during POM discovery: " + filename + ": " + e.getMessage() );
                }
                catch ( DiscovererException e )
                {
                    getLogger().error( e.getMessage() );
                }
            }
        }

        return artifacts;
    }

    /**
     * Returns an artifact object that is represented by the specified path in a repository
     *
     * @param path The path that is pointing to an artifact
     * @param repository The repository of the artifact
     * @return Artifact
     * @throws DiscovererException when the specified path does correspond to an artifact
     */
    public Artifact buildArtifactFromPath( String path, ArtifactRepository repository )
        throws DiscovererException
    {
        Artifact artifact = buildArtifact( path );

        if ( artifact != null )
        {
            artifact.setRepository( repository );
            artifact.setFile( new File( repository.getBasedir(), path ) );
        }

        return artifact;
    }
}
