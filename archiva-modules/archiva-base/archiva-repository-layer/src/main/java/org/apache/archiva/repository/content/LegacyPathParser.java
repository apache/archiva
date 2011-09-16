package org.apache.archiva.repository.content;

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
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.LegacyArtifactPath;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.layout.LayoutException;

import java.util.Collection;

/**
 * LegacyPathParser is a parser for maven 1 (legacy layout) paths to
 * ArtifactReference.
 *
 * @version $Id$
 */
public class LegacyPathParser
    implements PathParser
{
    private static final String INVALID_ARTIFACT_PATH = "Invalid path to Artifact: ";

    protected ArchivaConfiguration configuration;

    public LegacyPathParser( ArchivaConfiguration configuration )
    {
        this.configuration = configuration;
    }


    /**
     * {@inheritDoc}
     *
     * @see org.apache.archiva.repository.content.PathParser#toArtifactReference(java.lang.String)
     */
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        ArtifactReference artifact = new ArtifactReference();

        // First, look if a custom resolution rule has been set for this artifact
        Collection<LegacyArtifactPath> legacy = configuration.getConfiguration().getLegacyArtifactPaths();
        for ( LegacyArtifactPath legacyPath : legacy )
        {
            if ( legacyPath.match( path ) )
            {
                artifact.setGroupId( legacyPath.getGroupId() );
                artifact.setArtifactId( legacyPath.getArtifactId() );
                artifact.setClassifier( legacyPath.getClassifier() );
                artifact.setVersion( legacyPath.getVersion() );
                artifact.setType( legacyPath.getType() );
                return artifact;
            }
        }

        String normalizedPath = StringUtils.replace( path, "\\", "/" );

        String pathParts[] = StringUtils.split( normalizedPath, '/' );

        /* Always 3 parts. (Never more or less)
         * 
         *   path = "commons-lang/jars/commons-lang-2.1.jar"
         *   path[0] = "commons-lang";          // The Group ID
         *   path[1] = "jars";                  // The Directory Type
         *   path[2] = "commons-lang-2.1.jar";  // The Filename.
         */

        if ( pathParts.length != 3 )
        {
            // Illegal Path Parts Length.
            throw new LayoutException( INVALID_ARTIFACT_PATH
                                           + "legacy paths should only have 3 parts [groupId]/[type]s/[artifactId]-[version].[type], found "
                                           + pathParts.length + " instead." );
        }

        // The Group ID.
        artifact.setGroupId( pathParts[0] );

        // The Expected Type.
        String expectedType = pathParts[1];

        // Sanity Check: expectedType should end in "s".
        if ( !expectedType.endsWith( "s" ) )
        {
            throw new LayoutException( INVALID_ARTIFACT_PATH
                                           + "legacy paths should have an expected type ending in [s] in the second part of the path." );
        }

        // The Filename.
        String filename = pathParts[2];

        FilenameParser parser = new FilenameParser( filename );

        artifact.setArtifactId( parser.nextNonVersion() );

        // Sanity Check: does it have an artifact id?
        if ( StringUtils.isEmpty( artifact.getArtifactId() ) )
        {
            // Special Case: The filename might start with a version id (like "test-arch-1.0.jar").
            int idx = filename.indexOf( '-' );
            if ( idx > 0 )
            {
                parser.reset();
                // Take the first section regardless of content.
                String artifactId = parser.next();

                // Is there anything more that is considered not a version id?
                String moreArtifactId = parser.nextNonVersion();
                if ( StringUtils.isNotBlank( moreArtifactId ) )
                {
                    artifact.setArtifactId( artifactId + "-" + moreArtifactId );
                }
                else
                {
                    artifact.setArtifactId( artifactId );
                }
            }

            // Sanity Check: still no artifact id?
            if ( StringUtils.isEmpty( artifact.getArtifactId() ) )
            {
                throw new LayoutException( INVALID_ARTIFACT_PATH + "no artifact id present." );
            }
        }

        artifact.setVersion( parser.remaining() );

        // Sanity Check: does it have a version?
        if ( StringUtils.isEmpty( artifact.getVersion() ) )
        {
            // Special Case: use last section of artifactId as version.
            String artifactId = artifact.getArtifactId();
            int idx = artifactId.lastIndexOf( '-' );
            if ( idx > 0 )
            {
                artifact.setVersion( artifactId.substring( idx + 1 ) );
                artifact.setArtifactId( artifactId.substring( 0, idx ) );
            }
            else
            {
                throw new LayoutException( INVALID_ARTIFACT_PATH + "no version found." );
            }
        }

        String classifier = ArtifactClassifierMapping.getClassifier( expectedType );
        if ( classifier != null )
        {
            String version = artifact.getVersion();
            if ( !version.endsWith( "-" + classifier ) )
            {
                throw new LayoutException(
                    INVALID_ARTIFACT_PATH + expectedType + " artifacts must use the classifier " + classifier );
            }
            version = version.substring( 0, version.length() - classifier.length() - 1 );
            artifact.setVersion( version );
            artifact.setClassifier( classifier );
        }

        String extension = parser.getExtension();

        // Set Type
        String defaultExtension = expectedType.substring( 0, expectedType.length() - 1 );
        artifact.setType(
            ArtifactExtensionMapping.mapExtensionAndClassifierToType( classifier, extension, defaultExtension ) );

        // Sanity Check: does it have an extension?
        if ( StringUtils.isEmpty( artifact.getType() ) )
        {
            throw new LayoutException( INVALID_ARTIFACT_PATH + "no extension found." );
        }

        // Special Case with Maven Plugins
        if ( StringUtils.equals( "jar", extension ) && StringUtils.equals( "plugins", expectedType ) )
        {
            artifact.setType( ArtifactExtensionMapping.MAVEN_ONE_PLUGIN );
        }
        else
        {
            // Sanity Check: does extension match pathType on path?
            String expectedExtension = ArtifactExtensionMapping.getExtension( artifact.getType() );

            if ( !expectedExtension.equals( extension ) )
            {
                throw new LayoutException(
                    INVALID_ARTIFACT_PATH + "mismatch on extension [" + extension + "] and layout specified type ["
                        + artifact.getType() + "] (which maps to extension: [" + expectedExtension + "]) on path ["
                        + path + "]" );
            }
        }

        return artifact;
    }
}
