package org.apache.maven.archiva.discoverer;

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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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

    private List scanForArtifactPaths( File repositoryBase, List blacklistedPatterns )
    {
        return scanForArtifactPaths( repositoryBase, blacklistedPatterns, null, STANDARD_DISCOVERY_EXCLUDES );
    }

    public List discoverArtifacts( ArtifactRepository repository, List blacklistedPatterns, ArtifactFilter filter )
        throws DiscovererException
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported" );
        }

        File repositoryBase = new File( repository.getBasedir() );

        List artifacts = new ArrayList();

        List artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( Iterator i = artifactPaths.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();

            try
            {
                Artifact artifact = buildArtifactFromPath( path, repository );

                if ( filter.include( artifact ) )
                {
                    artifacts.add( artifact );
                }
                // TODO: else add to excluded? [!]
                // TODO! excluded/kickout tracking should be optional
            }
            catch ( DiscovererException e )
            {
                addKickedOutPath( path, e.getMessage() );
            }
        }

        return artifacts;
    }

    /**
     * Returns an artifact object that is represented by the specified path in a repository
     *
     * @param path       The path that is pointing to an artifact
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
