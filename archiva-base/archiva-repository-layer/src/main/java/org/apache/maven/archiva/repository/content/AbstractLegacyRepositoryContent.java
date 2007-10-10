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
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractLegacyRepositoryContent 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractLegacyRepositoryContent
{
    private static final String DIR_JAVADOC = "javadoc.jars";

    private static final String DIR_JAVA_SOURCE = "java-sources";

    private static final String PATH_SEPARATOR = "/";

    private static final Map<String, String> typeToDirectoryMap;

    static
    {
        typeToDirectoryMap = new HashMap<String, String>();
        typeToDirectoryMap.put( "ejb-client", "ejb" );
        typeToDirectoryMap.put( "distribution-tgz", "distribution" );
        typeToDirectoryMap.put( "distribution-zip", "distribution" );
    }

    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return LegacyPathParser.toArtifactReference( path );
    }

    public String toPath( ArtifactReference reference )
    {
        if ( reference == null )
        {
            throw new IllegalArgumentException( "Artifact reference cannot be null" );
        }

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

            path.append( '.' ).append( ArtifactExtensionMapping.getExtension( type ) );
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
                return DIR_JAVA_SOURCE;
            }

            if ( "javadoc".equals( classifier ) )
            {
                return DIR_JAVADOC;
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
}
