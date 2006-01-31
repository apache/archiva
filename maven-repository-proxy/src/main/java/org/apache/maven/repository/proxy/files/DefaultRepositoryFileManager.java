package org.apache.maven.repository.proxy.files;

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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Edwin Punzalan
 */
public class DefaultRepositoryFileManager
{
    /* @plexus.requirement */
    private ArtifactFactory factory;

    public Object getRequestedObjectFromPath( String path )
    {
        if ( path.endsWith( ".pom" ) )
        {
            return getArtifactFromPath( path );
        }
        else if ( path.endsWith( "ar" ) )
        {
            return getArtifactFromPath( path );
        }
        else if ( path.endsWith( ".md5" ) )
        {
            return new Checksum( "MD5" );
        }
        else if ( path.endsWith( ".sha1" ) )
        {
            return new Checksum( "SHA-1" );
        }
        else
        {
            //@todo handle metadata file requests
            return null;
        }
    }

    private Artifact getArtifactFromPath( String path )
    {
        List pathInfo = getReversedPathInfo( path );
        String filename = getPathToken( pathInfo );
        String version = getPathToken( pathInfo );
        String artifactId = getPathToken( pathInfo );
        String groupId = "";
        while ( pathInfo.size() > 0 )
        {
            if ( groupId.length() == 0 )
            {
                groupId = "." + groupId;
            }
            else
            {
                groupId = getPathToken( pathInfo ) + "." + groupId;
            }
        }

        return factory.createBuildArtifact( groupId, artifactId, version, getFileExtension( filename ) );
    }

    private List getReversedPathInfo( String path )
    {
        List reversedPath = new ArrayList();

        StringTokenizer tokenizer = new StringTokenizer( path, "/" );
        while ( tokenizer.hasMoreTokens() )
        {
            reversedPath.add( 0, tokenizer.nextToken() );
        }

        return reversedPath;
    }

    private String getPathToken( List path )
    {
        if ( path.size() > 0 )
        {
            return (String) path.remove( 0 );
        }
        else
        {
            return null;
        }
    }

    private String getFileExtension( String filename )
    {
        int idx = filename.lastIndexOf( '.' );
        return filename.substring( idx + 1 );
    }
}
