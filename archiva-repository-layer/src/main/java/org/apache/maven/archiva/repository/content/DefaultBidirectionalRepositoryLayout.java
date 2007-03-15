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

import org.apache.maven.archiva.repository.ArchivaArtifact;

/**
 * DefaultBidirectionalRepositoryLayout - the layout mechanism for use by Maven 2.x repositories.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.content.BidirectionalRepositoryLayout"
 *                   role-hint="default"
 */
public class DefaultBidirectionalRepositoryLayout implements BidirectionalRepositoryLayout
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

    public ArchivaArtifact toArtifact( String path )
    {
        
        
        return null;
    }
}
