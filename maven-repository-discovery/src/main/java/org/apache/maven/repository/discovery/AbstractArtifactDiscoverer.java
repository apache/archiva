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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

    private static final String POM = ".pom";

    private List scanForArtifactPaths( File repositoryBase, String blacklistedPatterns, long comparisonTimestamp )
    {
        return scanForArtifactPaths( repositoryBase, blacklistedPatterns, null, STANDARD_DISCOVERY_EXCLUDES,
                                     comparisonTimestamp );
    }

    public List discoverArtifacts( ArtifactRepository repository, String operation, String blacklistedPatterns,
                                   boolean includeSnapshots )
        throws DiscovererException
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported" );
        }

        long comparisonTimestamp = readComparisonTimestamp( repository, operation );

        File repositoryBase = new File( repository.getBasedir() );

        List artifacts = new ArrayList();

        List artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns, comparisonTimestamp );

        try
        {
            setLastCheckedTime( repository, operation, new Date() );
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
     * Returns a list of pom packaging artifacts found in a specified repository
     *
     * @param repository          The ArtifactRepository to discover artifacts
     * @param blacklistedPatterns Comma-delimited list of string paths that will be excluded in the discovery
     * @param includeSnapshots    if the repository contains snapshots which should also be included
     * @return list of pom artifacts
     */
    public List discoverStandalonePoms( ArtifactRepository repository, String blacklistedPatterns,
                                        boolean includeSnapshots )
    {
        List artifacts = new ArrayList();

        File repositoryBase = new File( repository.getBasedir() );

        // TODO: if we keep this method, set comparison timestamp properly!
        List artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns, 0 );

        for ( Iterator i = artifactPaths.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();

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

    public void resetLastCheckedTime( ArtifactRepository repository, String operation )
        throws IOException
    {
        // TODO: get these changes into maven-metadata.xml and migrate towards that. The model is further diverging to a different layout at each level so submodels might be a good idea.
        // TODO: maven-artifact probably needs an improved pathOfMetadata to cope with top level metadata
        // TODO: might we need to write this as maven-metadata-local in some circumstances? merge others? Probably best to keep it simple and just use this format at the root. No need to merge anything that I can see
        // TODO: since this metadata isn't meant to be shared, perhaps another file is called for after all.
        // Format is: <repository><lastDiscovery><KEY>yyyyMMddHHmmss</KEY></lastDiscovery></repository> (ie, flat properties)

        File file = new File( repository.getBasedir(), "maven-metadata.xml" );

        Xpp3Dom dom = readDom( file );

        Xpp3Dom lastDiscoveryDom = getLastDiscoveryDom( dom );

        boolean changed = false;

        // do this in reverse so that removing doesn't affect counter
        Xpp3Dom[] children = lastDiscoveryDom.getChildren();
        for ( int i = lastDiscoveryDom.getChildCount() - 1; i >= 0; i-- )
        {
            if ( children[i].getName().equals( operation ) )
            {
                changed = true;
                lastDiscoveryDom.removeChild( i );
            }
        }

        if ( changed )
        {
            saveDom( file, dom );
        }
    }

    private void saveDom( File file, Xpp3Dom dom )
        throws IOException
    {
        FileWriter writer = new FileWriter( file );

        // save metadata
        try
        {
            Xpp3DomWriter.write( writer, dom );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public void setLastCheckedTime( ArtifactRepository repository, String operation, Date date )
        throws IOException
    {
        // see notes in resetLastCheckedTime

        File file = new File( repository.getBasedir(), "maven-metadata.xml" );

        Xpp3Dom dom = readDom( file );

        Xpp3Dom lastDiscoveryDom = getLastDiscoveryDom( dom );

        Xpp3Dom entry = lastDiscoveryDom.getChild( operation );
        if ( entry == null )
        {
            entry = new Xpp3Dom( operation );
            lastDiscoveryDom.addChild( entry );
        }
        entry.setValue( new SimpleDateFormat( DATE_FMT, Locale.US ).format( date ) );

        saveDom( file, dom );
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
