package org.apache.maven.repository.discovery;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Artifact discoverer for the new repository layout (Maven 2.0+).
 *
 * @author John Casey
 * @author Brett Porter
 */
public class DefaultArtifactDiscoverer
    extends AbstractArtifactDiscoverer
{
    private ArtifactFactory artifactFactory;

    public List discoverArtifacts( File repositoryBase, String blacklistedPatterns, boolean convertSnapshots )
    {
        List artifacts = new ArrayList();

        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = buildArtifact( path );

            if ( artifact != null )
            {
                if ( convertSnapshots || !artifact.isSnapshot() )
                {
                    artifacts.add( artifact );
                }
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( String path )
    {
        Artifact result;

        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( path, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );

        if ( pathParts.size() < 4 )
        {
            addKickedOutPath( path );

            return null;
        }

        // the actual artifact filename.
        String filename = (String) pathParts.remove( 0 );

        // the next one is the version.
        String version = (String) pathParts.remove( 0 );

        // the next one is the artifactId.
        String artifactId = (String) pathParts.remove( 0 );

        // the remaining are the groupId.
        Collections.reverse( pathParts );
        String groupId = StringUtils.join( pathParts.iterator(), "." );

        result = artifactFactory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar" );

        String remainingFilename = filename;
        if ( !remainingFilename.startsWith( artifactId + "-" ) )
        {
            addKickedOutPath( path );

            return null;
        }

        remainingFilename = remainingFilename.substring( artifactId.length() + 1 );
        if ( result.isSnapshot() )
        {
            result = artifactFactory.createArtifact( groupId, artifactId,
                                                     remainingFilename.substring( 0, remainingFilename.length() - 4 ),
                                                     Artifact.SCOPE_RUNTIME, "jar" );
            // poor encapsulation requires we do this to populate base version
            if ( !result.isSnapshot() )
            {
                addKickedOutPath( path );

                return null;
            }
            if ( !result.getBaseVersion().equals( version ) )
            {
                addKickedOutPath( path );

                return null;
            }
        }
        else if ( !remainingFilename.startsWith( version ) )
        {
            addKickedOutPath( path );

            return null;
        }

        result.setFile( new File( path ) );

        return result;
    }
}
