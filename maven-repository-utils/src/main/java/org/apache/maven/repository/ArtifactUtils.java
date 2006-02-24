package org.apache.maven.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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

/**
 * @author Edwin Punzalan
 */
public class ArtifactUtils
{
    public static Artifact buildArtifact( File repositoryBase, String path, ArtifactRepository repository,
                                          ArtifactFactory artifactFactory )
    {
        Artifact artifact = buildArtifact( path, artifactFactory );

        if ( artifact != null )
        {
            artifact.setRepository( repository );
            artifact.setFile( new File( repositoryBase, path ) );
        }

        return artifact;
    }

    public static Artifact buildArtifact( String path, ArtifactFactory artifactFactory )
    {
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( path, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );

        Artifact artifact = null;
        if ( pathParts.size() >= 4 )
        {
            // maven 2.x path

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
                return null;
            }
            else
            {
                remainingFilename = remainingFilename.substring( artifactId.length() + 1 );

                String classifier = null;

                // TODO: use artifact handler, share with legacy discoverer
                String type;
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
                        return null;
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
                            return null;
                        }
                        else if ( !result.getBaseVersion().equals( version ) )
                        {
                            return null;
                        }
                        else
                        {
                            artifact = result;
                        }
                    }
                    else if ( !remainingFilename.startsWith( version ) )
                    {
                        return null;
                    }
                    else if ( !remainingFilename.equals( version ) )
                    {
                        if ( remainingFilename.charAt( version.length() ) != '-' )
                        {
                            return null;
                        }
                        else
                        {
                            classifier = remainingFilename.substring( version.length() + 1 );
                            artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version,
                                                                                        type, classifier );
                        }
                    }
                    else
                    {
                        artifact = result;
                    }
                }
            }
        }
        else if ( pathParts.size() == 3 )
        {
            //maven 1.x path

            String filename = (String) pathParts.remove( 0 );

            int idx = filename.lastIndexOf( '-' );
            if ( idx > 0 )
            {
                String version = filename.substring( idx + 1 );

                String artifactId = filename.substring( 0, idx );

                String types = (String) pathParts.remove( 0 );

                // remove the "s" in types
                String type = types.substring( 0, types.length() -1 );

                String groupId = (String) pathParts.remove( 0 );

                artifact = artifactFactory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME, type );
            }
        }

        return artifact;
    }
}
