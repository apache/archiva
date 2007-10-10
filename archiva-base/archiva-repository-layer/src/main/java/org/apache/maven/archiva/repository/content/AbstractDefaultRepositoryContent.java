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
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * AbstractDefaultRepositoryContent - common methods for working with default (maven 2) layout. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractDefaultRepositoryContent
{
    public static final String MAVEN_METADATA = "maven-metadata.xml";

    protected static final char PATH_SEPARATOR = '/';

    protected static final char GROUP_SEPARATOR = '.';

    protected static final char ARTIFACT_SEPARATOR = '-';

    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return DefaultPathParser.toArtifactReference( path );
    }

    public String toMetadataPath( ProjectReference reference )
    {
        StringBuffer path = new StringBuffer();
    
        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );
    
        return path.toString();
    }

    public String toMetadataPath( VersionedReference reference )
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
}
