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

import java.util.HashMap;
import java.util.Map;

/**
 * LegacyBidirectionalRepositoryLayout - the layout mechanism for use by Maven 1.x repositories.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.content.BidirectionalRepositoryLayout"
 *                   role-hint="legacy"
 */
public class LegacyBidirectionalRepositoryLayout implements BidirectionalRepositoryLayout
{
    private static final String PATH_SEPARATOR = "/";

    private ArtifactExtensionMapping extensionMapper = new LegacyArtifactExtensionMapping();

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

    public String pathOf( ArchivaArtifact artifact )
    {
        StringBuffer path = new StringBuffer();

        path.append( artifact.getGroupId() ).append( PATH_SEPARATOR );
        path.append( getDirectory( artifact ) ).append( PATH_SEPARATOR );
        path.append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        path.append( '.' ).append( extensionMapper.getExtension( artifact ) );

        return path.toString();
    }

    private String getDirectory( ArchivaArtifact artifact )
    {
        // Special Cases involving classifiers and type.
        if ( "jar".equals( artifact.getType() ) && "sources".equals( artifact.getClassifier() ) )
        {
            return "javadoc.jars";
        }

        // Special Cases involving only type.
        String dirname = (String) typeToDirectoryMap.get( artifact.getType() );

        if ( dirname != null )
        {
            return dirname + "s";
        }

        // Default process.
        return artifact.getType() + "s";
    }

    public ArchivaArtifact toArtifact( String path )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
