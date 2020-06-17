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
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile( "([0-9]{8}.[0-9]{6})-([0-9]+).*" );
    private static final Pattern MAVEN_PLUGIN_PATTERN = Pattern.compile( "^(maven-.*-plugin)|(.*-maven-plugin)$" );

    private RepositoryPathTranslator pathTranslator;
    private List<? extends ArtifactMappingProvider> artifactMappingProviders;


    AbstractDefaultRepositoryContent() {
    }

    public RepositoryPathTranslator getPathTranslator( )
    {
        return pathTranslator;
    }

    public void setPathTranslator( RepositoryPathTranslator pathTranslator )
    {
        this.pathTranslator = pathTranslator;
    }

    public void setArtifactMappingProviders(List<? extends ArtifactMappingProvider> artifactMappingProviders) {
        this.artifactMappingProviders = artifactMappingProviders;
    }

    public ArchivaItemSelector.Builder getArtifactFromFilename( String namespace, String projectId, String projectVersion,
                                                                String artifactFileName )
    {
        if ( !artifactFileName.startsWith( projectId + "-" ) )
        {
            throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + artifactFileName
                + "' doesn't start with artifact ID '" + projectId + "'" );
        }

        int index = projectId.length() + 1;
        String version;
        String idSubStrFromVersion = artifactFileName.substring( index );
        if ( idSubStrFromVersion.startsWith( projectVersion ) && !VersionUtil.isUniqueSnapshot( projectVersion ) )
        {
            // non-snapshot versions, or non-timestamped snapshot versions
            version = projectVersion;
        }
        else if ( VersionUtil.isGenericSnapshot( projectVersion ) )
        {
            // timestamped snapshots
            try
            {
                int mainVersionLength = projectVersion.length() - 8; // 8 is length of "SNAPSHOT"
                if ( mainVersionLength == 0 )
                {
                    throw new IllegalArgumentException(
                        "Timestamped snapshots must contain the main version, filename was '" + artifactFileName + "'" );
                }

                Matcher m = TIMESTAMP_PATTERN.matcher( idSubStrFromVersion.substring( mainVersionLength ) );
                m.matches();
                String timestamp = m.group( 1 );
                String buildNumber = m.group( 2 );
                version = idSubStrFromVersion.substring( 0, mainVersionLength ) + timestamp + "-" + buildNumber;
            }
            catch ( IllegalStateException e )
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + artifactFileName
                    + "' doesn't contain a timestamped version matching snapshot '"
                    + projectVersion + "'", e);
            }
        }
        else
        {
            // invalid
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, filename '" + artifactFileName + "' doesn't contain version '"
                    + projectVersion + "'" );
        }

        String classifier;
        String ext;
        index += version.length();
        if ( index == artifactFileName.length() )
        {
            // no classifier or extension
            classifier = null;
            ext = null;
        }
        else
        {
            char c = artifactFileName.charAt( index );
            if ( c == '-' )
            {
                // classifier up until '.'
                int extIndex = artifactFileName.indexOf( '.', index );
                if ( extIndex >= 0 )
                {
                    classifier = artifactFileName.substring( index + 1, extIndex );
                    ext = artifactFileName.substring( extIndex + 1 );
                }
                else
                {
                    classifier = artifactFileName.substring( index + 1 );
                    ext = null;
                }
            }
            else if ( c == '.' )
            {
                // rest is the extension
                classifier = null;
                ext = artifactFileName.substring( index + 1 );
            }
            else
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + artifactFileName
                    + "' expected classifier or extension but got '"
                    + artifactFileName.substring( index ) + "'" );
            }
        }

        ArchivaItemSelector.Builder selectorBuilder = ArchivaItemSelector.builder( )
            .withNamespace( namespace )
            .withProjectId( projectId )
            .withArtifactId( projectId )
            .withVersion( projectVersion )
            .withArtifactVersion( version )
            .withClassifier( classifier );


        // we use our own provider here instead of directly accessing Maven's artifact handlers as it has no way
        // to select the correct order to apply multiple extensions mappings to a preferred type
        // TODO: this won't allow the user to decide order to apply them if there are conflicts or desired changes -
        //       perhaps the plugins could register missing entries in configuration, then we just use configuration
        //       here?

        String type = null;
        for ( ArtifactMappingProvider mapping : artifactMappingProviders )
        {
            type = mapping.mapClassifierAndExtensionToType( classifier, ext );
            if ( type != null )
            {
                break;
            }
        }

        // TODO: this is cheating! We should check the POM metadata instead
        if ( type == null && "jar".equals( ext ) && isArtifactIdValidMavenPlugin( projectId ) )
        {
            type = "maven-plugin";
        }

        // use extension as default
        if ( type == null )
        {
            type = ext;
        }

        // TODO: should we allow this instead?
        if ( type == null )
        {
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, filename '" + artifactFileName + "' does not have a type" );
        }

        selectorBuilder.withType( type );


        return selectorBuilder;
    }

    public boolean isArtifactIdValidMavenPlugin( String artifactId )
    {
        return MAVEN_PLUGIN_PATTERN.matcher( artifactId ).matches();
    }

    private ArchivaItemSelector getArtifactForPath( String relativePath )
    {
        String[] parts = relativePath.replace( '\\', '/' ).split( "/" );

        int len = parts.length;
        if ( len < 4 )
        {
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, not enough directories: " + relativePath );
        }

        String fileName = parts[--len];
        String baseVersion = parts[--len];
        String artifactId = parts[--len];
        StringBuilder namespaceBuilder = new StringBuilder();
        for ( int i = 0; i < len - 1; i++ )
        {
            namespaceBuilder.append( parts[i] );
            namespaceBuilder.append( '.' );
        }
        namespaceBuilder.append( parts[len - 1] );

        return getArtifactFromFilename( namespaceBuilder.toString(), artifactId, baseVersion, fileName ).build();
    }

    @Override
    public ItemSelector toItemSelector( String path ) throws LayoutException
    {
        if ( StringUtils.isBlank( path ) )
        {
            throw new LayoutException( "Unable to convert blank path." );
        }
        try
        {

            return getArtifactForPath( path );
        }
        catch ( IllegalArgumentException e )
        {
            throw new LayoutException( e.getMessage(), e );
        }

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


    public String toPath( String namespace )
    {
        return formatAsDirectory( namespace );
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
