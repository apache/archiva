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
import org.apache.maven.archiva.repository.ArchivaArtifact;
import org.apache.maven.archiva.repository.content.ArtifactExtensionMapping;
import org.apache.maven.archiva.repository.content.DefaultArtifactExtensionMapping;

/**
 * DefaultBidirectionalRepositoryLayout - the layout mechanism for use by Maven 2.x repositories.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="default"
 */
public class DefaultBidirectionalRepositoryLayout
    implements BidirectionalRepositoryLayout
{
    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    private static final char ARTIFACT_SEPARATOR = '-';

    private ArtifactExtensionMapping extensionMapper = new DefaultArtifactExtensionMapping();

    public String getId()
    {
        return "default";
    }

    public String pathOf( ArchivaArtifact artifact )
    {
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( artifact.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( artifact.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( artifact.getBaseVersion() ).append( PATH_SEPARATOR );
        path.append( artifact.getArtifactId() ).append( ARTIFACT_SEPARATOR ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( ARTIFACT_SEPARATOR ).append( artifact.getClassifier() );
        }

        path.append( GROUP_SEPARATOR ).append( extensionMapper.getExtension( artifact ) );

        return path.toString();
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    public ArchivaArtifact toArtifact( String path ) throws LayoutException
    {
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

        // Last part is the filename
        String filename = pathParts[partCount - 1];

        // Second to last is the baseVersion (the directory version)
        // (Don't need it) String baseVersion = pathParts[partCount - 2];

        // Third to last is the artifact Id.
        String artifactId = pathParts[partCount - 3];

        // Remaining pieces are the groupId.
        String groupId = "";
        for ( int i = 0; i <= partCount - 4; i++ )
        {
            if ( groupId.length() > 0 )
            {
                groupId += ".";
            }
            groupId += pathParts[i];
        }
        
        // Now we need to parse the filename to get the artifact version Id. 
        String fileParts[] = RepositoryLayoutUtils.splitFilename( filename, artifactId );
        String version = fileParts[1];
        String classifier = fileParts[2];

        String type = extensionMapper.getType( filename );

        return new ArchivaArtifact( groupId, artifactId, version, classifier, type );
    }
}
