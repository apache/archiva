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
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.content.LegacyArtifactExtensionMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * LegacyBidirectionalRepositoryLayout - the layout mechanism for use by Maven 1.x repositories.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="legacy"
 */
public class LegacyBidirectionalRepositoryLayout
    implements BidirectionalRepositoryLayout
{
    private static final String MAVEN_METADATA = "maven-metadata.xml";

    private static final String PATH_SEPARATOR = "/";

    private LegacyArtifactExtensionMapping extensionMapper = new LegacyArtifactExtensionMapping();

    private Map typeToDirectoryMap;

    public LegacyBidirectionalRepositoryLayout()
    {
        typeToDirectoryMap = new HashMap();
        typeToDirectoryMap.put( "ejb-client", "ejb" );
        typeToDirectoryMap.put( "distribution-tgz", "distribution" );
        typeToDirectoryMap.put( "distribution-zip", "distribution" );
    }

    public String getId()
    {
        return "legacy";
    }

    public String toPath( ArchivaArtifact artifact )
    {
        return toPath( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                       artifact.getClassifier(), artifact.getType() );
    }

    public String toPath( ProjectReference reference )
    {
        StringBuffer path = new StringBuffer();

        path.append( reference.getGroupId() ).append( PATH_SEPARATOR );
        path.append( getDirectory( null, "jar" ) ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public String toPath( VersionedReference reference )
    {
        // NOTE: A legacy repository cannot contain a versioned reference to the metadata.
        StringBuffer path = new StringBuffer();

        path.append( reference.getGroupId() ).append( PATH_SEPARATOR );
        path.append( getDirectory( null, "jar" ) ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );

        return path.toString();
    }

    public String toPath( ArtifactReference reference )
    {
        return toPath( reference.getGroupId(), reference.getArtifactId(), reference.getVersion(), reference
            .getClassifier(), reference.getType() );
    }

    private String toPath( String groupId, String artifactId, String version, String classifier, String type )
    {
        StringBuffer path = new StringBuffer();

        path.append( groupId ).append( PATH_SEPARATOR );
        path.append( getDirectory( classifier, type ) ).append( PATH_SEPARATOR );

        if ( version != null )
        {
            path.append( artifactId ).append( '-' ).append( version );

            if ( StringUtils.isNotBlank( classifier ) )
            {
                path.append( '-' ).append( classifier );
            }

            path.append( '.' ).append( extensionMapper.getExtension( type ) );
        }

        return path.toString();
    }

    private String getDirectory( String classifier, String type )
    {
        // Special Cases involving type + classifier
        if ( "jar".equals( type ) && StringUtils.isNotBlank( classifier ) )
        {
            if ( "sources".equals( classifier ) )
            {
                return "source.jars";
            }

            if ( "javadoc".equals( classifier ) )
            {
                return "javadoc.jars";
            }
        }

        // Special Cases involving only type.
        String dirname = (String) typeToDirectoryMap.get( type );

        if ( dirname != null )
        {
            return dirname + "s";
        }

        // Default process.
        return type + "s";
    }

    class PathReferences
    {
        public String groupId;

        public String pathType;

        public String type;

        public FilenameParts fileParts;
    }

    private PathReferences toPathReferences( String path, boolean parseFilename )
        throws LayoutException
    {
        PathReferences prefs = new PathReferences();

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
            throw new LayoutException( "Invalid number of parts to the path [" + path
                + "] to construct an ArchivaArtifact from. (Required to be 3 parts)" );
        }

        // The Group ID.
        prefs.groupId = pathParts[0];

        // The Expected Type.
        prefs.pathType = pathParts[1];

        if ( parseFilename )
        {
            // The Filename.
            String filename = pathParts[2];

            prefs.fileParts = RepositoryLayoutUtils.splitFilename( filename, null );

            prefs.type = extensionMapper.getType( prefs.pathType, filename );

            // Sanity Checks.
            if ( StringUtils.isEmpty( prefs.fileParts.extension ) )
            {
                throw new LayoutException( "Invalid artifact, no extension." );
            }

            if ( !prefs.type.equals( prefs.fileParts.extension ) )
            {
                throw new LayoutException( "Invalid artifact, mismatch on extension <" + prefs.fileParts.extension
                    + "> and expected layout specified type <" + prefs.type
                    + "> (mapped from actual path provided type <" + prefs.pathType + ">)" );
            }
        }

        return prefs;
    }

    public ProjectReference toProjectReference( String path )
        throws LayoutException
    {
        throw new LayoutException( "Cannot parse legacy paths to a Project Reference." );
    }

    public ArchivaArtifact toArtifact( String path )
        throws LayoutException
    {
        PathReferences pathrefs = toPathReferences( path, true );

        ArchivaArtifact artifact = new ArchivaArtifact( pathrefs.groupId, pathrefs.fileParts.artifactId,
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
        reference.setArtifactId( pathrefs.fileParts.artifactId );
        reference.setVersion( pathrefs.fileParts.version );
        reference.setClassifier( pathrefs.fileParts.classifier );
        reference.setType( pathrefs.type );

        return reference;
    }

    public VersionedReference toVersionedReference( String path )
        throws LayoutException
    {
        return null;
    }

}
