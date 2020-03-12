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
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Helper class that contains certain maven specific methods
 */
@Service( "MavenContentHelper" )
public class MavenContentHelper
{

    private static final Logger log = LoggerFactory.getLogger( MavenContentHelper.class );
    public static final Pattern UNIQUE_SNAPSHOT_NUMBER_PATTERN = Pattern.compile( "^([0-9]{8}\\.[0-9]{6}-[0-9]+)(.*)" );


    @Inject
    @Named( "metadataReader#maven" )
    MavenMetadataReader metadataReader;

    public static final String METADATA_FILENAME = "maven-metadata.xml";
    public static final String METADATA_REPOSITORY_FILENAME = "maven-metadata-repository.xml";

    public MavenContentHelper() {

    }

    public void setMetadataReader( MavenMetadataReader metadataReader )
    {
        this.metadataReader = metadataReader;
    }

    /**
     * Returns the namespace string for a given path in the repository
     *
     * @param namespacePath the path to the namespace in the directory
     * @return the namespace string that matches the given path.
     */
    public static String getNamespaceFromNamespacePath( final StorageAsset namespacePath) {
        LinkedList<String> names = new LinkedList<>( );
        StorageAsset current = namespacePath;
        while (current.hasParent()) {
            names.addFirst( current.getName() );
            current = current.getParent( );
        }
        return String.join( ".", names );
    }

    /**
     * Returns the artifact version for the given artifact directory and the item selector
     */
    public String getArtifactVersion( StorageAsset artifactDir, ItemSelector selector) {
        if (selector.hasArtifactVersion()) {
            return selector.getArtifactVersion();
        } else if (selector.hasVersion()) {
            if ( VersionUtil.isGenericSnapshot( selector.getVersion() ) ) {
                return getLatestArtifactSnapshotVersion( artifactDir, selector.getVersion( ) );
            } else {
                return selector.getVersion( );
            }
        } else {
            throw new IllegalArgumentException( "No version set on the selector " );
        }
    }


    /**
     *
     * Returns the latest snapshot version that is referenced by the metadata file.
     *
     * @param artifactDir the directory of the artifact
     * @param snapshotVersion the generic snapshot version (must end with '-SNAPSHOT')
     * @return the real version from the metadata
     */
    public String getLatestArtifactSnapshotVersion( StorageAsset artifactDir, String snapshotVersion) {
        final StorageAsset metadataFile = artifactDir.resolve( METADATA_FILENAME );
        StringBuilder version = new StringBuilder( );
        try
        {
            ArchivaRepositoryMetadata metadata = metadataReader.read( metadataFile );

            // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
            SnapshotVersion metadataVersion = metadata.getSnapshotVersion( );
            if ( metadataVersion != null && StringUtils.isNotEmpty( metadataVersion.getTimestamp( ) ) )
            {
                version.append( snapshotVersion, 0, snapshotVersion.length( ) - 8 ); // remove SNAPSHOT from end
                version.append( metadataVersion.getTimestamp( ) ).append( "-" ).append( metadataVersion.getBuildNumber( ) );
                return version.toString( );
            }
        }
        catch ( RepositoryMetadataException e )
        {
            // unable to parse metadata - LOGGER it, and continue with the version as the original SNAPSHOT version
            log.warn( "Invalid metadata: {} - {}", metadataFile, e.getMessage( ) );
        }
        final String baseVersion = StringUtils.removeEnd( snapshotVersion, "-SNAPSHOT" );
        final String prefix = metadataFile.getParent( ).getParent( ).getName( ) + "-"+baseVersion+"-";
        return artifactDir.list( ).stream( ).filter( a -> a.getName( ).startsWith( prefix ) )
            .map( a -> StringUtils.removeStart( a.getName( ), prefix ) )
            .map( n -> UNIQUE_SNAPSHOT_NUMBER_PATTERN.matcher( n ) )
            .filter( m -> m.matches( ) )
            .map( m -> baseVersion+"-"+m.group( 1 ) )
            .sorted( Comparator.reverseOrder() ).findFirst().orElse( snapshotVersion );
    }


    /**
     * Returns a artifact filename that corresponds to the given data.
     * @param artifactId the selector data
     * @param artifactVersion the artifactVersion
     * @param classifier the artifact classifier
     * @param extension the file extension
     */
    static String getArtifactFileName( String artifactId, String artifactVersion,
                                       String classifier, String extension )
    {
        StringBuilder fileName = new StringBuilder( artifactId ).append( "-" );
        fileName.append( artifactVersion );
        if ( !StringUtils.isEmpty( classifier ) )
        {
            fileName.append( "-" ).append( classifier );
        }
        fileName.append( "." ).append( extension );
        return fileName.toString( );
    }

    /**
     * Returns the classifier for a given selector. If the selector has no classifier, but
     * a type set. The classifier is generated from the type.
     *
     * @param selector the artifact selector
     * @return the classifier or empty string if no classifier was found
     */
    static String getClassifier( ItemSelector selector )
    {
        if ( selector.hasClassifier( ) )
        {
            return selector.getClassifier( );
        }
        else if ( selector.hasType( ) )
        {
            return getClassifierFromType( selector.getType( ) );
        }
        else
        {
            return "";
        }
    }

    /**
     * Returns a classifier for a given type. It returns only classifier for the maven default types
     * that are known.
     *
     * @param type the type of the artifact
     * @return the classifier if one was found, otherwise a empty string
     */
    static String getClassifierFromType( final String type )
    {
        String testType = type.trim( ).toLowerCase( );
        switch (testType.length( ))
        {
            case 7:
                if ("javadoc".equals(testType)) {
                    return "javadoc";
                }
            case 8:
                if ("test-jar".equals(testType))
                {
                    return "tests";
                }
            case 10:
                if ("ejb-client".equals(testType)) {
                    return "client";
                }
            case 11:
                if ("java-source".equals(testType)) {
                    return "sources";
                }
            default:
                return "";
        }

    }

    /**
     * Returns the type that matches the given classifier and extension
     *
     * @param classifierArg the classifier
     * @param extensionArg the extension
     * @return the type that matches the combination of classifier and extension
     */
    static String getTypeFromClassifierAndExtension( String classifierArg, String extensionArg )
    {
        String extension = extensionArg.toLowerCase( ).trim( );
        String classifier = classifierArg.toLowerCase( ).trim( );
        if ( StringUtils.isEmpty( extension ) )
        {
            return "";
        }
        else if ( StringUtils.isEmpty( classifier ) )
        {
            return extension;
        }
        else if ( classifier.equals( "tests" ) && extension.equals( "jar" ) )
        {
            return "test-jar";
        }
        else if ( classifier.equals( "client" ) && extension.equals( "jar" ) )
        {
            return "ejb-client";
        }
        else if ( classifier.equals( "sources" ) && extension.equals( "jar" ) )
        {
            return "java-source";
        }
        else if ( classifier.equals( "javadoc" ) && extension.equals( "jar" ) )
        {
            return "javadoc";
        }
        else
        {
            return extension;
        }
    }

    /**
     * If the selector defines a type and no extension, the extension can be derived from
     * the type.
     *
     * @param selector the item selector
     * @return the extension that matches the type or the default extension "jar" if the type is not known
     */
    static String getArtifactExtension( ItemSelector selector )
    {
        if ( selector.hasExtension( ) )
        {
            return selector.getExtension( );
        }
        else if ( selector.hasType( ) )
        {
            final String type = selector.getType( ).trim().toLowerCase( );
            switch (type.length()) {
                case 3:
                    if ("pom".equals(type) || "war".equals(type) || "ear".equals(type) || "rar".equals(type)) {
                        return type;
                    }
                default:
                    return "jar";

            }
        }
        else
        {
            return "jar";
        }
    }
}
