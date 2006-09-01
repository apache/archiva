package org.apache.maven.archiva.discovery;

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
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    private List scanForArtifactPaths( File repositoryBase, List blacklistedPatterns, long comparisonTimestamp )
    {
        return scanForArtifactPaths( repositoryBase, blacklistedPatterns, null, STANDARD_DISCOVERY_EXCLUDES,
                                     comparisonTimestamp );
    }

    public List discoverArtifacts( ArtifactRepository repository, String operation, List blacklistedPatterns,
                                   boolean includeSnapshots )
        throws DiscovererException
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported" );
        }

        Xpp3Dom dom = getLastArtifactDiscoveryDom( readRepositoryMetadataDom( repository ) );
        long comparisonTimestamp = readComparisonTimestamp( repository, operation, dom );

        // Note that last checked time is deliberately set to the start of the process so that anything added
        // mid-discovery and missed by the scanner will get checked next time.
        // Due to this, there must be no negative side-effects of discovering something twice.
        Date newLastCheckedTime = new Date();

        File repositoryBase = new File( repository.getBasedir() );

        List artifacts = new ArrayList();

        List artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns, comparisonTimestamp );

        // Also note that the last check time, while set at the start, is saved at the end, so that if any exceptions
        // occur, then the timestamp is not updated so that the discovery is attempted again
        // TODO: under the list-return behaviour we have now, exceptions might occur later and the timestamp will not be reset - see MRM-83
        try
        {
            setLastCheckedTime( repository, operation, newLastCheckedTime );
        }
        catch ( IOException e )
        {
            throw new DiscovererException( "Error writing metadata: " + e.getMessage(), e );
        }

        for ( Iterator i = artifactPaths.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();

            try
            {
                Artifact artifact = buildArtifactFromPath( path, repository );

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

    public void setLastCheckedTime( ArtifactRepository repository, String operation, Date date )
        throws IOException
    {
        // see notes in resetLastCheckedTime

        File file = new File( repository.getBasedir(), "maven-metadata.xml" );

        Xpp3Dom dom = readDom( file );

        String dateString = new SimpleDateFormat( DATE_FMT, Locale.US ).format( date );

        setEntry( getLastArtifactDiscoveryDom( dom ), operation, dateString );

        saveDom( file, dom );
    }
}
