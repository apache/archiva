package org.apache.maven.archiva.discoverer;

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

import org.apache.maven.artifact.Artifact;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Artifact discoverer for the legacy repository layout (Maven 1.x).
 * Method used to build an artifact object using a relative path from a repository base directory.  An artifactId
 * having the words "DEV", "PRE", "RC", "ALPHA", "BETA", "DEBUG", "UNOFFICIAL", "CURRENT", "LATEST", "FCS",
 * "RELEASE", "NIGHTLY", "SNAPSHOT" and "TEST" (not case-sensitive) will most likely make this method fail as
 * they are reserved for version usage.
 *
 * @author John Casey
 * @author Brett Porter
 * @plexus.component role="org.apache.maven.archiva.discoverer.ArtifactDiscoverer" role-hint="legacy"
 */
public class LegacyArtifactDiscoverer
    extends AbstractArtifactDiscoverer
{
    /**
     * @see org.apache.maven.archiva.discoverer.ArtifactDiscoverer#buildArtifact(String)
     */
    public Artifact buildArtifact( String path )
        throws DiscovererException
    {
        StringTokenizer tokens = new StringTokenizer( path, "/\\" );

        Artifact result;

        int numberOfTokens = tokens.countTokens();

        if ( numberOfTokens == 3 )
        {
            String groupId = tokens.nextToken();

            String type = tokens.nextToken();

            if ( type.endsWith( "s" ) )
            {
                type = type.substring( 0, type.length() - 1 );

                // contains artifactId, version, classifier, and extension.
                String avceGlob = tokens.nextToken();

                //noinspection CollectionDeclaredAsConcreteClass
                LinkedList avceTokenList = new LinkedList();

                StringTokenizer avceTokenizer = new StringTokenizer( avceGlob, "-" );
                while ( avceTokenizer.hasMoreTokens() )
                {
                    avceTokenList.addLast( avceTokenizer.nextToken() );
                }

                String lastAvceToken = (String) avceTokenList.removeLast();

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
                else if ( lastAvceToken.endsWith( "javadoc.jar" ) )
                {
                    type = "javadoc.jar";

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
                        if ( type.equals( ext ) || "plugin".equals( type ) )
                        {
                            lastAvceToken = lastAvceToken.substring( 0, extPos );

                            avceTokenList.addLast( lastAvceToken );
                        }
                        else
                        {
                            throw new DiscovererException( "Path type does not match the extension" );
                        }
                    }
                    else
                    {
                        throw new DiscovererException( "Path filename does not have an extension" );
                    }
                }

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
                    "([Nn][Ii][Gg][Hh][Tt][Ll][Yy])|" + "[Ff][Ii][Nn][Aa][Ll]|" + "([AaBb][_.0-9]*)";

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

                // Now, restore the proper ordering so we can build the artifactId.
                Collections.reverse( avceTokenList );

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

                    classifierBuffer.setLength( 0 );
                }
                else
                {
                    // if everything is kosher, then pop off all the classifier and
                    // version tokens, leaving the naked artifact id in the list.
                    avceTokenList = new LinkedList( avceTokenList.subList( 0, avceTokenList.size() - tokensIterated ) );
                }

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

                if ( artifactId.length() > 0 )
                {
                    int lastVersionCharIdx = versionBuffer.length() - 1;
                    if ( lastVersionCharIdx > -1 && versionBuffer.charAt( lastVersionCharIdx ) == '-' )
                    {
                        versionBuffer.setLength( lastVersionCharIdx );
                    }

                    String version = versionBuffer.toString();

                    if ( version.length() > 0 )
                    {
                        if ( classifierBuffer.length() > 0 )
                        {
                            result = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                                   classifierBuffer.toString() );
                        }
                        else
                        {
                            result = artifactFactory.createArtifact( groupId, artifactId, version,
                                                                     Artifact.SCOPE_RUNTIME, type );
                        }
                    }
                    else
                    {
                        throw new DiscovererException( "Path filename version is empty" );
                    }
                }
                else
                {
                    throw new DiscovererException( "Path filename artifactId is empty" );
                }
            }
            else
            {
                throw new DiscovererException( "Path artifact type does not corresspond to an artifact type" );
            }
        }
        else
        {
            throw new DiscovererException( "Path does not match a legacy repository path for an artifact" );
        }

        return result;
    }
}
