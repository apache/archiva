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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

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

            Artifact artifact = buildArtifact( repositoryBase, path, repository );
            if ( artifact != null )
            {
                if ( includeSnapshots || !artifact.isSnapshot() )
                {
                    artifacts.add( artifact );
                }
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

    /**
     * @noinspection CollectionDeclaredAsConcreteClass
     */
    private Artifact buildArtifact( File repositoryBase, String path, ArtifactRepository repository )
    {
        StringTokenizer tokens = new StringTokenizer( path, "/\\" );

        Artifact result = null;

        int numberOfTokens = tokens.countTokens();

        if ( numberOfTokens != 3 )
        {
            addKickedOutPath( path );
        }
        else
        {
            String groupId = tokens.nextToken();

            String type = tokens.nextToken();

            if ( !type.endsWith( "s" ) )
            {
                addKickedOutPath( path );
            }
            else
            {
                type = type.substring( 0, type.length() - 1 );

                // contains artifactId, version, classifier, and extension.
                String avceGlob = tokens.nextToken();

                LinkedList avceTokenList = new LinkedList();

                StringTokenizer avceTokenizer = new StringTokenizer( avceGlob, "-" );
                while ( avceTokenizer.hasMoreTokens() )
                {
                    avceTokenList.addLast( avceTokenizer.nextToken() );
                }

                String lastAvceToken = (String) avceTokenList.removeLast();

                boolean valid = true;

                // TODO: share with other discoverer, use artifact handlers instead
                if ( lastAvceToken.endsWith( ".tar.gz" ) )
                {
                    type = "distribution-tgz";

                    lastAvceToken = lastAvceToken.substring( 0, lastAvceToken.length() - ".tar.gz".length() );

                    avceTokenList.addLast( lastAvceToken );
                }
                else if ( lastAvceToken.endsWith( "sources.jar" ) )
                {
                    type = "java-source";

                    lastAvceToken = lastAvceToken.substring( 0, lastAvceToken.length() - ".jar".length() );

                    avceTokenList.addLast( lastAvceToken );
                }
                else if ( lastAvceToken.endsWith( ".zip" ) )
                {
                    type = "distribution-zip";

                    lastAvceToken = lastAvceToken.substring( 0, lastAvceToken.length() - ".zip".length() );

                    avceTokenList.addLast( lastAvceToken );
                }
                else
                {
                    int extPos = lastAvceToken.lastIndexOf( '.' );

                    if ( extPos > 0 )
                    {
                        String ext = lastAvceToken.substring( extPos + 1 );
                        if ( type.equals( ext ) )
                        {
                            lastAvceToken = lastAvceToken.substring( 0, extPos );

                            avceTokenList.addLast( lastAvceToken );
                        }
                        else
                        {
                            addKickedOutPath( path );

                            valid = false;
                        }
                    }
                    else
                    {
                        // no extension
                        addKickedOutPath( path );

                        valid = false;
                    }
                }

                if ( valid )
                {
                    // let's discover the version, and whatever's leftover will be either
                    // a classifier, or part of the artifactId, depending on position.
                    // Since version is at the end, we have to move in from the back.
                    Collections.reverse( avceTokenList );

                    // TODO: this is obscene - surely a better way?
                    String validVersionParts = "([Dd][Ee][Vv][_.0-9]*)|" + "([Ss][Nn][Aa][Pp][Ss][Hh][Oo][Tt])|" +
                        "([0-9][_.0-9a-zA-Z]*)|" + "([Gg]?[_.0-9ab]*([Pp][Rr][Ee]|[Rr][Cc]|[Gg]|[Mm])[_.0-9]*)|" +
                        "([Aa][Ll][Pp][Hh][Aa][_.0-9]*)|" + "([Bb][Ee][Tt][Aa][_.0-9]*)|" + "([Rr][Cc][_.0-9]*)|" +
                        "([Tt][Ee][Ss][Tt][_.0-9]*)|" + "([Dd][Ee][Bb][Uu][Gg][_.0-9]*)|" +
                        "([Uu][Nn][Oo][Ff][Ff][Ii][Cc][Ii][Aa][Ll][_.0-9]*)|" + "([Cc][Uu][Rr][Rr][Ee][Nn][Tt])|" +
                        "([Ll][Aa][Tt][Ee][Ss][Tt])|" + "([Ff][Cc][Ss])|" + "([Rr][Ee][Ll][Ee][Aa][Ss][Ee][_.0-9]*)|" +
                        "([Nn][Ii][Gg][Hh][Tt][Ll][Yy])|" + "([AaBb][_.0-9]*)";

                    StringBuffer classifierBuffer = new StringBuffer();
                    StringBuffer versionBuffer = new StringBuffer();

                    boolean firstVersionTokenEncountered = false;
                    boolean firstToken = true;

                    int tokensIterated = 0;
                    for ( Iterator it = avceTokenList.iterator(); it.hasNext(); )
                    {
                        String token = (String) it.next();

                        boolean tokenIsVersionPart = token.matches( validVersionParts );

                        StringBuffer bufferToUpdate;

                        // NOTE: logic in code is reversed, since we're peeling off the back
                        // Any token after the last versionPart will be in the classifier.
                        // Any token UP TO first non-versionPart is part of the version.
                        if ( !tokenIsVersionPart )
                        {
                            if ( firstVersionTokenEncountered )
                            {
                                //noinspection BreakStatement
                                break;
                            }
                            else
                            {
                                bufferToUpdate = classifierBuffer;
                            }
                        }
                        else
                        {
                            firstVersionTokenEncountered = true;

                            bufferToUpdate = versionBuffer;
                        }

                        if ( firstToken )
                        {
                            firstToken = false;
                        }
                        else
                        {
                            bufferToUpdate.insert( 0, '-' );
                        }

                        bufferToUpdate.insert( 0, token );

                        tokensIterated++;
                    }

                    getLogger().debug( "After parsing loop, state of buffers:\no  Version Buffer: \'" + versionBuffer +
                        "\'\no  Classifier Buffer: \'" + classifierBuffer + "\'\no Number of Tokens Iterated: " +
                        tokensIterated );

                    // Now, restore the proper ordering so we can build the artifactId.
                    Collections.reverse( avceTokenList );

                    getLogger().debug(
                        "Before repairing bad version and/or cleaning up used tokens, avce token list is:\n" +
                            avceTokenList );

                    // if we didn't find a version, then punt. Use the last token
                    // as the version, and set the classifier empty.
                    if ( versionBuffer.length() < 1 )
                    {
                        if ( avceTokenList.size() > 1 )
                        {
                            int lastIdx = avceTokenList.size() - 1;

                            versionBuffer.append( avceTokenList.get( lastIdx ) );
                            avceTokenList.remove( lastIdx );
                        }
                        else
                        {
                            getLogger().debug( "Cannot parse version from artifact path: \'" + path + "\'." );
                            getLogger().debug( "artifact-version-classifier-extension remaining tokens is: \'" +
                                avceTokenList + "\'" );
                        }

                        classifierBuffer.setLength( 0 );
                    }
                    else
                    {
                        getLogger().debug( "Removing " + tokensIterated + " tokens from avce token list." );

                        // if everything is kosher, then pop off all the classifier and
                        // version tokens, leaving the naked artifact id in the list.
                        avceTokenList =
                            new LinkedList( avceTokenList.subList( 0, avceTokenList.size() - tokensIterated ) );
                    }

                    getLogger().debug( "Now, remainder of avce token list is:\n" + avceTokenList );

                    StringBuffer artifactIdBuffer = new StringBuffer();

                    firstToken = true;
                    for ( Iterator it = avceTokenList.iterator(); it.hasNext(); )
                    {
                        String token = (String) it.next();

                        if ( firstToken )
                        {
                            firstToken = false;
                        }
                        else
                        {
                            artifactIdBuffer.append( '-' );
                        }

                        artifactIdBuffer.append( token );
                    }

                    String artifactId = artifactIdBuffer.toString();

                    int lastVersionCharIdx = versionBuffer.length() - 1;
                    if ( lastVersionCharIdx > -1 && versionBuffer.charAt( lastVersionCharIdx ) == '-' )
                    {
                        versionBuffer.setLength( lastVersionCharIdx );
                    }

                    String version = versionBuffer.toString();

                    if ( version.length() < 1 )
                    {
                        addKickedOutPath( path );
                    }
                    else
                    {
                        getLogger().debug( "Extracted artifact information from path:\n" + "groupId: \'" + groupId +
                            "\'\n" + "artifactId: \'" + artifactId + "\'\n" + "type: \'" + type + "\'\n" +
                            "version: \'" + version + "\'\n" + "classifier: \'" + classifierBuffer + "\'" );

                        if ( classifierBuffer.length() > 0 )
                        {
                            getLogger().debug( "Creating artifact with classifier." );

                            result = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                                   classifierBuffer.toString() );
                        }
                        else
                        {
                            result = artifactFactory.createArtifact( groupId, artifactId, version,
                                                                     Artifact.SCOPE_RUNTIME, type );
                        }

                        result.setRepository( repository );
                        result.setFile( new File( repositoryBase, path ) );
                    }
                }
            }
        }
        return result;
    }

}
