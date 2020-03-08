package org.apache.archiva.repository.maven.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.archiva.repository.maven.metadata.storage.Maven2RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.PathParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AbstractDefaultRepositoryContent - common methods for working with default (maven 2) layout.
 */
public abstract class AbstractDefaultRepositoryContent implements RepositoryContent
{


    protected Logger log = LoggerFactory.getLogger( getClass() );

    public static final String MAVEN_METADATA = "maven-metadata.xml";

    protected static final char PATH_SEPARATOR = '/';

    protected static final char GROUP_SEPARATOR = '.';

    protected static final char ARTIFACT_SEPARATOR = '-';

    private RepositoryPathTranslator pathTranslator = new Maven2RepositoryPathTranslator();

    private PathParser defaultPathParser = new DefaultPathParser();


    PathParser getPathParser() {
        return defaultPathParser;
    }



    /**
     *
     */
    protected List<? extends ArtifactMappingProvider> artifactMappingProviders;

    AbstractDefaultRepositoryContent(List<? extends ArtifactMappingProvider> artifactMappingProviders) {
        this.artifactMappingProviders = artifactMappingProviders;
    }

    public void setArtifactMappingProviders(List<? extends ArtifactMappingProvider> artifactMappingProviders) {
        this.artifactMappingProviders = artifactMappingProviders;
    }

    @Override
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return defaultPathParser.toArtifactReference( path );
    }

    @Override
    public ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return defaultPathParser.toItemSelector( path );
    }

    public String toPath ( ProjectReference reference) {
        final StringBuilder path = new StringBuilder();
        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId( ) );
        return path.toString( );
    }

    @Override
    public String toPath ( ItemSelector selector ) {
        if (selector==null) {
            throw new IllegalArgumentException( "ItemSelector must not be null." );
        }
        String projectId;
        // Initialize the project id if not set
        if (selector.hasProjectId()) {
            projectId = selector.getProjectId( );
        } else if (selector.hasArtifactId()) {
            // projectId same as artifact id, if set
            projectId = selector.getArtifactId( );
        } else {
            // we arrive here, if projectId && artifactId not set
            return pathTranslator.toPath( selector.getNamespace(), "");
        }
        if ( !selector.hasArtifactId( )) {
            return pathTranslator.toPath( selector.getNamespace( ), projectId );
        }
        // this part only, if projectId && artifactId is set
        String artifactVersion = "";
        String version = "";
        if (selector.hasVersion() && selector.hasArtifactVersion() ) {
            artifactVersion = selector.getArtifactVersion();
            version = VersionUtil.getBaseVersion( selector.getVersion( ) );
        } else if (!selector.hasVersion() && selector.hasArtifactVersion()) {
            // we try to retrieve the base version, if artifact version is only set
            version = VersionUtil.getBaseVersion( selector.getArtifactVersion( ) );
            artifactVersion = selector.getArtifactVersion( );
        } else if (selector.hasVersion() && !selector.hasArtifactVersion()) {
            artifactVersion = selector.getVersion();
            version = VersionUtil.getBaseVersion( selector.getVersion( ) );
        }

        return pathTranslator.toPath( selector.getNamespace(), projectId, version,
                constructId( selector.getArtifactId(), artifactVersion, selector.getClassifier(), selector.getType() ) );

    }

    public String toMetadataPath( ProjectReference reference )
    {
        final StringBuilder path = new StringBuilder();
        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( MAVEN_METADATA );
        return path.toString();
    }

    public String toPath( String namespace )
    {
        return formatAsDirectory( namespace );
    }

    public String toPath( VersionedReference reference )
    {
        final StringBuilder path = new StringBuilder();
        path.append( formatAsDirectory( reference.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( reference.getArtifactId() ).append( PATH_SEPARATOR );
        if ( reference.getVersion() != null )
        {
            // add the version only if it is present
            path.append( VersionUtil.getBaseVersion( reference.getVersion() ) );
        }
        return path.toString();
    }

    public String toMetadataPath( VersionedReference reference )
    {
        StringBuilder path = new StringBuilder();

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

    public String toPath( ArchivaArtifact reference )
    {
        if ( reference == null )
        {
            throw new IllegalArgumentException( "ArchivaArtifact cannot be null" );
        }

        String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
        return toPath( reference.getGroupId(), reference.getArtifactId(), baseVersion, reference.getVersion(),
                       reference.getClassifier(), reference.getType() );
    }

    @Override
    public String toPath( ArtifactReference reference )
    {
        if ( reference == null )
        {
            throw new IllegalArgumentException( "Artifact reference cannot be null" );
        }
        if ( reference.getVersion() != null )
        {
            String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
            return toPath( reference.getGroupId(), reference.getArtifactId(), baseVersion, reference.getVersion(),
                           reference.getClassifier(), reference.getType() );
        }
        return toPath( reference.getGroupId(), reference.getArtifactId(), null, null,
                       reference.getClassifier(), reference.getType() );
    }

    protected String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    private String toPath( String groupId, String artifactId, String baseVersion, String version, String classifier,
                           String type )
    {
        if ( baseVersion != null )
        {
            return pathTranslator.toPath( groupId, artifactId, baseVersion,
                                          constructId( artifactId, version, classifier, type ) );
        }
        else
        {
            return pathTranslator.toPath( groupId, artifactId );
        }
    }

    // TODO: move into the Maven Artifact facet when refactoring away the caller - the caller will need to have access
    //       to the facet or filename (for the original ID)
    private String constructId( String artifactId, String version, String classifier, String type )
    {
        String ext = null;
        for ( ArtifactMappingProvider provider : artifactMappingProviders )
        {
            ext = provider.mapTypeToExtension( type );
            if ( ext != null )
            {
                break;
            }
        }
        if ( ext == null )
        {
            ext = type;
        }

        StringBuilder id = new StringBuilder();
        if ( ( version != null ) && ( type != null ) )
        {
            id.append( artifactId ).append( ARTIFACT_SEPARATOR ).append( version );

            if ( StringUtils.isNotBlank( classifier ) )
            {
                id.append( ARTIFACT_SEPARATOR ).append( classifier );
            }

            id.append( "." ).append( ext );
        }
        return id.toString();
    }
}
