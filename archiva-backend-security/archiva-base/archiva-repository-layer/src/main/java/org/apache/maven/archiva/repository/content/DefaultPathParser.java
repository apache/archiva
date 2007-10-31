package org.apache.maven.archiva.repository.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * DefaultPathParser is a parser for maven 2 (default layout) paths to ArtifactReference. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.content.DefaultPathParser"
 */
public class DefaultPathParser
{
    private static final String INVALID_ARTIFACT_PATH = "Invalid path to Artifact: ";

    /**
     * Convert a path to an ArtifactReference. 
     * 
     * @param path
     * @return
     * @throws LayoutException
     */
    protected static ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( StringUtils.isBlank( path ) )
        {
            throw new LayoutException( "Unable to convert blank path." );
        }

        ArtifactReference artifact = new ArtifactReference();

        String normalizedPath = StringUtils.replace( path, "\\", "/" );
        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        /* Minimum parts.
         *
         *   path = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar"
         *   path[0] = "commons-lang";        // The Group ID
         *   path[1] = "commons-lang";        // The Artifact ID
         *   path[2] = "2.1";                 // The Version
         *   path[3] = "commons-lang-2.1.jar" // The filename.
         */

        if ( pathParts.length < 4 )
        {
            // Illegal Path Parts Length.
            throw new LayoutException( "Not enough parts to the path [" + path
                + "] to construct an ArchivaArtifact from. (Requires at least 4 parts)" );
        }

        // Maven 2.x path.
        int partCount = pathParts.length;
        int filenamePos = partCount - 1;
        int baseVersionPos = partCount - 2;
        int artifactIdPos = partCount - 3;
        int groupIdPos = partCount - 4;

        // Second to last is the baseVersion (the directory version)
        String baseVersion = pathParts[baseVersionPos];

        // Third to last is the artifact Id.
        artifact.setArtifactId( pathParts[artifactIdPos] );

        // Remaining pieces are the groupId.
        for ( int i = 0; i <= groupIdPos; i++ )
        {
            if ( i == 0 )
            {
                artifact.setGroupId( pathParts[i] );
            }
            else
            {
                artifact.setGroupId( artifact.getGroupId() + "." + pathParts[i] );
            }
        }

        try
        {
            // Last part is the filename
            String filename = pathParts[filenamePos];

            // Now we need to parse the filename to get the artifact version Id.
            if ( StringUtils.isBlank( filename ) )
            {
                throw new IllegalArgumentException( INVALID_ARTIFACT_PATH + "Unable to split blank filename." );
            }

            FilenameParser parser = new FilenameParser( filename );

            // Expect the filename to start with the artifactId.
            artifact.setArtifactId( parser.expect( artifact.getArtifactId() ) );

            if ( artifact.getArtifactId() == null )
            {
                throw new LayoutException( INVALID_ARTIFACT_PATH + "filename format is invalid, "
                    + "should start with artifactId as stated in path." );
            }

            // Process the version.
            artifact.setVersion( parser.expect( baseVersion ) );

            if ( artifact.getVersion() == null )
            {
                // We working with a snapshot?
                if ( VersionUtil.isSnapshot( baseVersion ) )
                {
                    artifact.setVersion( parser.nextVersion() );
                    if ( !VersionUtil.isUniqueSnapshot( artifact.getVersion() ) )
                    {
                        throw new LayoutException( INVALID_ARTIFACT_PATH + "filename format is invalid,"
                            + "expected timestamp format in filename." );
                    }
                }
                else
                {
                    throw new LayoutException( INVALID_ARTIFACT_PATH + "filename format is invalid, "
                        + "expected version as stated in path." );
                }
            }

            // Do we have a classifier?
            switch(parser.seperator())
            {
                case '-':
                    // Definately a classifier.
                    artifact.setClassifier( parser.remaining() );
                    
                    // Set the type.
                    artifact.setType( ArtifactExtensionMapping.guessTypeFromFilename( filename ) );
                    break;
                case '.':
                    // We have an dual extension possibility.
                    String extension = parser.remaining() + '.' + parser.getExtension();
                    artifact.setType( extension.replace( '.', '-' ) );
                    break;
                case 0:
                    // End of the filename, only a simple extension left. - Set the type.
                    artifact.setType( ArtifactExtensionMapping.guessTypeFromFilename( filename ) );
                    break;
            }
            
            // Special case for maven plugins
            if ( StringUtils.equals( "jar", artifact.getType() ) && 
                 ArtifactExtensionMapping.isMavenPlugin( artifact.getArtifactId() ) )
            {
                artifact.setType( ArtifactExtensionMapping.MAVEN_PLUGIN );
            }
        }
        catch ( LayoutException e )
        {
            throw e;
        }

        // Sanity Checks.

        // Do we have a snapshot version?
        if ( VersionUtil.isSnapshot( artifact.getVersion() ) )
        {
            // Rules are different for SNAPSHOTS
            if ( !VersionUtil.isGenericSnapshot( baseVersion ) )
            {
                String filenameBaseVersion = VersionUtil.getBaseVersion( artifact.getVersion() );
                throw new LayoutException( "Invalid snapshot artifact location, version directory should be "
                    + filenameBaseVersion );
            }
        }
        else
        {
            // Non SNAPSHOT rules.
            // Do we pass the simple test?
            if ( !StringUtils.equals( baseVersion, artifact.getVersion() ) )
            {
                throw new LayoutException( "Invalid artifact: version declared in directory path does"
                    + " not match what was found in the artifact filename." );
            }
        }

        return artifact;
    }
    
}
