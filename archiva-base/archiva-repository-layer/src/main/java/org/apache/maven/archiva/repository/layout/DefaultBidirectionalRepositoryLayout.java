package org.apache.maven.archiva.repository.layout;

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
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.content.ArtifactExtensionMapping;

/**
 * DefaultBidirectionalRepositoryLayout - the layout mechanism for use by Maven 2.x repositories.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role-hint="default"
 */
public class DefaultBidirectionalRepositoryLayout
    implements BidirectionalRepositoryLayout
{
    class PathReferences
    {
        public String groupId;

        public String artifactId;

        public String baseVersion;

        public String type;

        public FilenameParts fileParts;

        public void appendGroupId( String part )
        {
            if ( groupId == null )
            {
                groupId = part;
                return;
            }

            groupId += "." + part;
        }
    }

    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    private static final char ARTIFACT_SEPARATOR = '-';

    public String getId()
    {
        return "default";
    }

    public ArchivaArtifact toArtifact( String path )
        throws LayoutException
    {
        PathReferences pathrefs = toPathReferences( path );

        ArchivaArtifact artifact = new ArchivaArtifact( pathrefs.groupId, pathrefs.artifactId,
                                                        pathrefs.fileParts.version, pathrefs.fileParts.classifier,
                                                        pathrefs.type );

        return artifact;
    }

    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        PathReferences pathrefs = toPathReferences( path );

        ArtifactReference reference = new ArtifactReference();
        reference.setGroupId( pathrefs.groupId );
        reference.setArtifactId( pathrefs.artifactId );
        reference.setVersion( pathrefs.fileParts.version );
        reference.setClassifier( pathrefs.fileParts.classifier );
        reference.setType( pathrefs.type );

        return reference;
    }

    public String toPath( ArchivaArtifact artifact )
    {
        if ( artifact == null )
        {
            throw new IllegalArgumentException( "Artifact cannot be null" );
        }

        return toPath( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact
            .getVersion(), artifact.getClassifier(), artifact.getType() );
    }

    public String toPath( ArtifactReference reference )
    {
        if ( reference == null )
        {
            throw new IllegalArgumentException( "Artifact reference cannot be null" );
        }

        String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
        return toPath( reference.getGroupId(), reference.getArtifactId(), baseVersion, reference.getVersion(),
                       reference.getClassifier(), reference.getType() );
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    private String toPath( String groupId, String artifactId, String baseVersion, String version, String classifier,
                           String type )
    {
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( groupId ) ).append( PATH_SEPARATOR );
        path.append( artifactId ).append( PATH_SEPARATOR );

        if ( baseVersion != null )
        {
            path.append( baseVersion ).append( PATH_SEPARATOR );
            if ( ( version != null ) && ( type != null ) )
            {
                path.append( artifactId ).append( ARTIFACT_SEPARATOR ).append( version );

                if ( StringUtils.isNotBlank( classifier ) )
                {
                    path.append( ARTIFACT_SEPARATOR ).append( classifier );
                }

                path.append( GROUP_SEPARATOR ).append( ArtifactExtensionMapping.getExtension( type ) );
            }
        }

        return path.toString();
    }

    public boolean isValidPath( String path )
    {
        try
        {
            toPathReferences( path );
            return true;
        }
        catch ( LayoutException e )
        {
            return false;
        }
    }

    private PathReferences toPathReferences( String path )
        throws LayoutException
    {
        if ( StringUtils.isBlank( path ) )
        {
            throw new LayoutException( "Unable to convert blank path." );
        }

        PathReferences prefs = new PathReferences();

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
        prefs.baseVersion = pathParts[baseVersionPos];

        // Third to last is the artifact Id.
        prefs.artifactId = pathParts[artifactIdPos];

        // Remaining pieces are the groupId.
        for ( int i = 0; i <= groupIdPos; i++ )
        {
            prefs.appendGroupId( pathParts[i] );
        }

        try
        {
            // Last part is the filename
            String filename = pathParts[filenamePos];

            // Now we need to parse the filename to get the artifact version Id.
            prefs.fileParts = RepositoryLayoutUtils.splitFilename( filename, prefs.artifactId, prefs.baseVersion );

            /* If classifier is discovered, see if it deserves to be.
             *
             * Filenames like "comm-3.0-u1.jar" might be identified as having a version of "3.0"
             * and a classifier of "u1".
             *
             * This routine will take the version + classifier and compare it to the prefs.baseVersion and
             * move the classifierensure that
             *
             * javax/comm/3.0-u1/comm-3.0-u1.jar
             */
            if ( StringUtils.isNotBlank( prefs.fileParts.classifier ) )
            {
                String conjoinedVersion = prefs.fileParts.version + "-" + prefs.fileParts.classifier;

                if( StringUtils.equals( prefs.baseVersion, conjoinedVersion ) )
                {
                    prefs.fileParts.version = conjoinedVersion;
                    prefs.fileParts.classifier = null;
                }
            }

            prefs.type = ArtifactExtensionMapping.guessTypeFromFilename( filename );
        }
        catch ( LayoutException e )
        {
            throw e;
        }

        // Sanity Checks.
        if ( prefs.fileParts != null )
        {
            /* Compare artifact version to path baseversion.
             *
             * Version naming in the wild can be strange at times.
             * Sometimes what is seen as a classifier is actually part of the version id.
             *
             * To compensate for this, the path is checked against the artifact.version and
             *  the concatenation of the artifact.version + "-" + artifact.classifier
             */
            String pathVersion = prefs.baseVersion;
            String artifactVersion = prefs.fileParts.version;

            // Do we have a snapshot version?
            if ( VersionUtil.isSnapshot( artifactVersion ) )
            {
                // Rules are different for SNAPSHOTS
                if ( !VersionUtil.isGenericSnapshot( pathVersion ) )
                {
                    String baseVersion = VersionUtil.getBaseVersion( prefs.fileParts.version );
                    throw new LayoutException( "Invalid snapshot artifact location, version directory should be "
                        + baseVersion );
                }
            }
            else
            {
                // Non SNAPSHOT rules.
                // Do we pass the simple test?
                if ( !StringUtils.equals( pathVersion, artifactVersion ) )
                {
                    // Do we have a classifier?  If so, test the conjoined case.
                    if ( StringUtils.isNotBlank( prefs.fileParts.classifier ) )
                    {
                        String artifactLongVersion = artifactVersion + "-" + prefs.fileParts.classifier;
                        if ( !StringUtils.equals( pathVersion, artifactLongVersion ) )
                        {
                            throw new LayoutException( "Invalid artifact: version declared in directory path does"
                                + " not match what was found in the artifact filename." );
                        }
                    }
                    else
                    {
                        throw new LayoutException( "Invalid artifact: version declared in directory path does"
                            + " not match what was found in the artifact filename." );
                    }
                }
            }

            // Test if the artifactId present on the directory path is the same as the artifactId filename.
            if ( !prefs.artifactId.equals( prefs.fileParts.artifactId ) )
            {
                throw new LayoutException( "Invalid artifact Id" );
            }
        }

        return prefs;
    }
}
