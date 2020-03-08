package org.apache.archiva.repository.maven.metadata.storage;

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
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.maven.model.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
@Service( "repositoryPathTranslator#maven2" )
public class Maven2RepositoryPathTranslator
    implements RepositoryPathTranslator
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private static final char GROUP_SEPARATOR = '.';

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile( "([0-9]{8}.[0-9]{6})-([0-9]+).*" );
    

    private static final Pattern MAVEN_PLUGIN_PATTERN = Pattern.compile( "^(maven-.*-plugin)|(.*-maven-plugin)$" );    

    /**
     *
     * see #initialize
     */
    @Inject
    private List<ArtifactMappingProvider> artifactMappingProviders;

    public Maven2RepositoryPathTranslator()
    {
        // noop
    }

    @PostConstruct
    public void initialize()
    {
        //artifactMappingProviders = new ArrayList<ArtifactMappingProvider>(
        //    applicationContext.getBeansOfType( ArtifactMappingProvider.class ).values() );

    }


    public Maven2RepositoryPathTranslator( List<ArtifactMappingProvider> artifactMappingProviders )
    {
        this.artifactMappingProviders = artifactMappingProviders;
    }

    @Override
    public StorageAsset toFile(StorageAsset basedir, String namespace, String projectId, String projectVersion, String filename )
    {
        return basedir.resolve( toPath( namespace, projectId, projectVersion, filename ) );
    }

    @Override
    public StorageAsset toFile( StorageAsset basedir, String namespace, String projectId, String projectVersion )
    {
        return basedir.resolve( toPath( namespace, projectId, projectVersion ) );
    }

    @Override
    public String toPath( String namespace, String projectId, String projectVersion, String filename )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );
        path.append( PATH_SEPARATOR );
        path.append( filename );

        return path.toString();
    }

    private void appendNamespaceToProjectVersion( StringBuilder path, String namespace, String projectId,
                                                  String projectVersion )
    {
        appendNamespaceAndProject( path, namespace, projectId );
        path.append( projectVersion );
    }

    public String toPath( String namespace, String projectId, String projectVersion )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );

        return path.toString();
    }

    public String toPath( String namespace )
    {
        StringBuilder path = new StringBuilder();

        appendNamespace( path, namespace );

        return path.toString();
    }

    @Override
    public String toPath( String namespace, String projectId )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceAndProject( path, namespace, projectId );

        return path.toString();
    }

    private void appendNamespaceAndProject( StringBuilder path, String namespace, String projectId )
    {
        appendNamespace( path, namespace );
        if (StringUtils.isNotEmpty( projectId ))
        {
            path.append( projectId ).append( PATH_SEPARATOR );
        }
    }

    private void appendNamespace( StringBuilder path, String namespace )
    {
        if ( StringUtils.isNotEmpty( namespace ) ) {
            path.append( formatAsDirectory( namespace ) ).append( PATH_SEPARATOR );
        }
    }

    @Override
    public StorageAsset toFile( StorageAsset basedir, String namespace, String projectId )
    {
        return basedir.resolve( toPath( namespace, projectId ) );
    }

    @Override
    public StorageAsset toFile( StorageAsset basedir, String namespace )
    {
        return basedir.resolve( toPath( namespace ) );
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    @Override
    public ArtifactMetadata getArtifactForPath( String repoId, String relativePath )
    {
        String[] parts = relativePath.replace( '\\', '/' ).split( "/" );

        int len = parts.length;
        if ( len < 4 )
        {
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, not enough directories: " + relativePath );
        }

        String id = parts[--len];
        String baseVersion = parts[--len];
        String artifactId = parts[--len];
        StringBuilder groupIdBuilder = new StringBuilder();
        for ( int i = 0; i < len - 1; i++ )
        {
            groupIdBuilder.append( parts[i] );
            groupIdBuilder.append( '.' );
        }
        groupIdBuilder.append( parts[len - 1] );

        return getArtifactFromId( repoId, groupIdBuilder.toString(), artifactId, baseVersion, id );
    }

    @Override
    public ArtifactMetadata getArtifactFromId( String repoId, String namespace, String projectId, String projectVersion,
                                               String id )
    {
        if ( !id.startsWith( projectId + "-" ) )
        {
            throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                                                    + "' doesn't start with artifact ID '" + projectId + "'" );
        }

        MavenArtifactFacet facet = new MavenArtifactFacet();

        int index = projectId.length() + 1;
        String version;
        String idSubStrFromVersion = id.substring( index );
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
                        "Timestamped snapshots must contain the main version, filename was '" + id + "'" );
                }

                Matcher m = TIMESTAMP_PATTERN.matcher( idSubStrFromVersion.substring( mainVersionLength ) );
                m.matches();
                String timestamp = m.group( 1 );
                String buildNumber = m.group( 2 );
                facet.setTimestamp( timestamp );
                facet.setBuildNumber( Integer.parseInt( buildNumber ) );
                version = idSubStrFromVersion.substring( 0, mainVersionLength ) + timestamp + "-" + buildNumber;
            }
            catch ( IllegalStateException e )
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                                                        + "' doesn't contain a timestamped version matching snapshot '"
                                                        + projectVersion + "'", e);
            }
        }
        else
        {
            // invalid
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, filename '" + id + "' doesn't contain version '"
                    + projectVersion + "'" );
        }

        String classifier;
        String ext;
        index += version.length();
        if ( index == id.length() )
        {
            // no classifier or extension
            classifier = null;
            ext = null;
        }
        else
        {
            char c = id.charAt( index );
            if ( c == '-' )
            {
                // classifier up until '.'
                int extIndex = id.indexOf( '.', index );
                if ( extIndex >= 0 )
                {
                    classifier = id.substring( index + 1, extIndex );
                    ext = id.substring( extIndex + 1 );
                }
                else
                {
                    classifier = id.substring( index + 1 );
                    ext = null;
                }
            }
            else if ( c == '.' )
            {
                // rest is the extension
                classifier = null;
                ext = id.substring( index + 1 );
            }
            else
            {
                throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id
                                                        + "' expected classifier or extension but got '"
                                                        + id.substring( index ) + "'" );
            }
        }

        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setId( id );
        metadata.setNamespace( namespace );
        metadata.setProject( projectId );
        metadata.setRepositoryId( repoId );
        metadata.setProjectVersion( projectVersion );
        metadata.setVersion( version );

        facet.setClassifier( classifier );

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
                "Not a valid artifact path in a Maven 2 repository, filename '" + id + "' does not have a type" );
        }

        facet.setType( type );
        metadata.addFacet( facet );

        return metadata;
    }


    public boolean isArtifactIdValidMavenPlugin( String artifactId )
    {
        return MAVEN_PLUGIN_PATTERN.matcher( artifactId ).matches();
    }
}
