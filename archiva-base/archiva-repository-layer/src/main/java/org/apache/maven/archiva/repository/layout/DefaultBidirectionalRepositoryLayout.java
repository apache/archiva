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
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.content.DefaultArtifactExtensionMapping;

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
    private static final String MAVEN_METADATA = "maven-metadata.xml";

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

    private DefaultArtifactExtensionMapping extensionMapper = new DefaultArtifactExtensionMapping();

    public String getId()
    {
        return "default";
    }

    public ArchivaArtifact toArtifact( String path )
        throws LayoutException
    {
        PathReferences pathrefs = toPathReferences( path, true );

        ArchivaArtifact artifact = new ArchivaArtifact( pathrefs.groupId, pathrefs.artifactId,
                                                        pathrefs.fileParts.version, pathrefs.fileParts.classifier,
                                                        pathrefs.type );

        return artifact;
    }

    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        PathReferences pathrefs = toPathReferences( path, true );

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
        return toPath( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact
            .getVersion(), artifact.getClassifier(), artifact.getType() );
    }

    public String toPath( ArtifactReference reference )
    {
        String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
        return toPath( reference.getGroupId(), reference.getArtifactId(), baseVersion, reference.getVersion(),
                       reference.getClassifier(), reference.getType() );
    }

    public String toPath( ProjectReference reference )
    {
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public String toPath( VersionedReference reference )
    {
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        if ( reference.getVersion() != null )
        {
            // add the version only if it is present
            path.append( VersionUtil.getBaseVersion( reference.getVersion() ) ).append( PATH_SEPARATOR );
        }
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public ProjectReference toProjectReference( String path )
        throws LayoutException
    {
        if ( !path.endsWith( "/maven-metadata.xml" ) )
        {
            throw new LayoutException(
                "Only paths ending in '/maven-metadata.xml' can be " + "converted to a ProjectReference." );
        }

        PathReferences pathrefs = toPathReferences( path, false );
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( pathrefs.groupId );
        reference.setArtifactId( pathrefs.artifactId );

        return reference;
    }

    public VersionedReference toVersionedReference( String path )
        throws LayoutException
    {
        if ( !path.endsWith( "/maven-metadata.xml" ) )
        {
            throw new LayoutException(
                "Only paths ending in '/maven-metadata.xml' can be " + "converted to a VersionedReference." );
        }

        PathReferences pathrefs = toPathReferences( path, false );

        VersionedReference reference = new VersionedReference();
        reference.setGroupId( pathrefs.groupId );
        reference.setArtifactId( pathrefs.artifactId );
        reference.setVersion( pathrefs.baseVersion );

        return reference;
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

                path.append( GROUP_SEPARATOR ).append( extensionMapper.getExtension( type ) );
            }
        }

        return path.toString();
    }

    public boolean isValidPath( String path )
    {
        try
        {
            toPathReferences( path, false );
            return true;
        }
        catch ( LayoutException e )
        {
            return false;
        }
    }

    private PathReferences toPathReferences( String path, boolean parseFilename )
        throws LayoutException
    {
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
            throw new LayoutException( "Not enough parts to the path [" + path +
                "] to construct an ArchivaArtifact from. (Requires at least 4 parts)" );
        }

        // Maven 2.x path.
        int partCount = pathParts.length;
        int filenamePos = partCount - 1;
        int baseVersionPos = partCount - 2;
        int artifactIdPos = partCount - 3;
        int groupIdPos = partCount - 4;

        // Second to last is the baseVersion (the directory version)
        prefs.baseVersion = pathParts[baseVersionPos];

        if ( "maven-metadata.xml".equals( pathParts[filenamePos] ) )
        {
            if ( !VersionUtil.isVersion( prefs.baseVersion ) )
            {
                // We have a simple path without a version identifier.
                prefs.baseVersion = null;
                artifactIdPos++;
                groupIdPos++;
            }
        }

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
            prefs.fileParts = RepositoryLayoutUtils.splitFilename( filename, prefs.artifactId );

            prefs.type = extensionMapper.getType( filename );
        }
        catch ( LayoutException e )
        {
            if ( parseFilename )
            {
                throw e;
            }
        }

        // Sanity Checks.
        if ( prefs.fileParts != null )
        {
            String artifactBaseVersion = VersionUtil.getBaseVersion( prefs.fileParts.version );
            if ( !artifactBaseVersion.equals( prefs.baseVersion ) )
            {
                throw new LayoutException( "Invalid artifact location, version directory and filename mismatch." );
            }

            if ( !prefs.artifactId.equals( prefs.fileParts.artifactId ) )
            {
                throw new LayoutException( "Invalid artifact Id" );
            }
        }

        return prefs;
    }
}
