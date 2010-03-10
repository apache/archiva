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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.DefaultArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.Maven2RepositoryPathTranslator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.util.Collections;

/**
 * DefaultPathParser is a parser for maven 2 (default layout) paths to ArtifactReference.
 *
 * TODO: remove in favour of path translator, this is just delegating for the most part
 *
 * @version $Id$
 */
public class DefaultPathParser implements PathParser
{
    private static final String INVALID_ARTIFACT_PATH = "Invalid path to Artifact: ";

    private RepositoryPathTranslator pathTranslator = new Maven2RepositoryPathTranslator(
        Collections.<ArtifactMappingProvider>singletonList( new DefaultArtifactMappingProvider() ) );

    /**
     * {@inheritDoc}
     * @see org.apache.maven.archiva.repository.content.PathParser#toArtifactReference(java.lang.String)
     */
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( StringUtils.isBlank( path ) )
        {
            throw new LayoutException( "Unable to convert blank path." );
        }

        ArtifactMetadata metadata;
        try
        {
            metadata = pathTranslator.getArtifactForPath( null, path );
        }
        catch ( IllegalArgumentException e )
        {
            throw new LayoutException( e.getMessage(), e );
        }

        ArtifactReference artifact = new ArtifactReference();
        artifact.setGroupId( metadata.getNamespace() );
        artifact.setArtifactId( metadata.getProject() );
        artifact.setVersion( metadata.getVersion() );

        // TODO: use Maven facet instead
        String filename = metadata.getId();
        FilenameParser parser = new FilenameParser( filename );
        artifact.setArtifactId( parser.expect( artifact.getArtifactId() ) );
        if ( artifact.getArtifactId() == null )
        {
            throw new LayoutException( INVALID_ARTIFACT_PATH + "filename format is invalid, "
                + "should start with artifactId as stated in path." );
        }
        String baseVersion = VersionUtil.getBaseVersion( metadata.getVersion() );
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
        switch(parser.seperator())
        {
            case '-':
                // Definately a classifier.
                artifact.setClassifier( parser.remaining() );

                // Set the type.
                artifact.setType( ArtifactExtensionMapping.mapExtensionAndClassifierToType( artifact.getClassifier(), parser.getExtension() ) );
                break;
            case '.':
                // We have an dual extension possibility.
                String extension = parser.remaining() + '.' + parser.getExtension();
                artifact.setType( extension );
                break;
            case 0:
                // End of the filename, only a simple extension left. - Set the type.
                String type = ArtifactExtensionMapping.mapExtensionToType( parser.getExtension() );
                if ( type == null )
                {
                    throw new LayoutException( "Invalid artifact: no type was specified" );
                }
                artifact.setType( type );
                break;
        }
        if ( StringUtils.equals( "jar", artifact.getType() ) &&
             ArtifactExtensionMapping.isMavenPlugin( artifact.getArtifactId() ) )
        {
            artifact.setType( ArtifactExtensionMapping.MAVEN_PLUGIN );
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

        return artifact;
    }

}
