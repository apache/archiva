package org.apache.archiva.metadata.maven;
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

import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.xml.XMLException;
import org.apache.archiva.xml.XMLReader;
import org.apache.archiva.xml.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service("metadataReader#maven")
public class MavenMetadataReader implements MetadataReader
{
    public static final String MAVEN_METADATA = "maven-metadata.xml";


    /*
    <?xml version="1.0" encoding="UTF-8"?>
    <metadata modelVersion="1.1.0">
      <groupId>org.apache.archiva</groupId>
      <artifactId>archiva</artifactId>
      <version>1.4-M3-SNAPSHOT</version>
      <versioning>
        <snapshot>
          <timestamp>20120310.230917</timestamp>
          <buildNumber>2</buildNumber>
        </snapshot>
        <lastUpdated>20120310230917</lastUpdated>
        <snapshotVersions>
          <snapshotVersion>
            <extension>pom</extension>
            <value>1.4-M3-20120310.230917-2</value>
            <updated>20120310230917</updated>
          </snapshotVersion>
        </snapshotVersions>
      </versioning>
    </metadata>
    */

    private static final Logger log = LoggerFactory.getLogger( MavenMetadataReader.class );


    /**
     * Read and return the {@link org.apache.archiva.model.ArchivaRepositoryMetadata} object from the provided xml file.
     *
     * @param metadataFile the maven-metadata.xml file to read.
     * @return the archiva repository metadata object that represents the provided file contents.
     * @throws RepositoryMetadataException if the file cannot be read
     */
    public ArchivaRepositoryMetadata read( StorageAsset metadataFile )
            throws RepositoryMetadataException {

        XMLReader xml;
        try
        {
            xml = new XMLReader( "metadata", metadataFile );
        }
        catch ( XMLException e )
        {
            throw new RepositoryMetadataException( "Could not open XML metadata file " + metadataFile, e );
        }
        return read( xml, metadataFile.getModificationTime(), metadataFile.getSize() );

    }

    public ArchivaRepositoryMetadata read( Path metadataFile )
        throws RepositoryMetadataException {

        XMLReader xml;
        try
        {
            xml = new XMLReader( "metadata", metadataFile );
        }
        catch ( XMLException e )
        {
            log.error( "XML error while reading metadata file {}: {}", metadataFile, e.getMessage(), e );
            throw new RepositoryMetadataException( "Could not open XML metadata file " + metadataFile, e );
        }
        try
        {
            return read( xml, Files.getLastModifiedTime( metadataFile ).toInstant(), Files.size( metadataFile ) );
        }
        catch ( IOException e )
        {
            log.error( "IO Error while reading metadata file {}: {}", metadataFile, e.getMessage(), e );
            throw new RepositoryMetadataException( "Could not open XML metadata file " + metadataFile, e );
        }

    }

    private ArchivaRepositoryMetadata read( XMLReader xml, Instant modTime, long fileSize) throws RepositoryMetadataException
    {
        // invoke this to remove namespaces, see MRM-1136
        xml.removeNamespaces();

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();

        try
        {
            metadata.setGroupId( xml.getElementText( "//metadata/groupId" ) );
            metadata.setArtifactId( xml.getElementText( "//metadata/artifactId" ) );
            metadata.setVersion( xml.getElementText( "//metadata/version" ) );
            metadata.setFileLastModified( Date.from(modTime) );
            metadata.setFileSize( fileSize );
            metadata.setLastUpdated( xml.getElementText( "//metadata/versioning/lastUpdated" ) );
            metadata.setLatestVersion( xml.getElementText( "//metadata/versioning/latest" ) );
            metadata.setReleasedVersion( xml.getElementText( "//metadata/versioning/release" ) );
            metadata.setAvailableVersions( xml.getElementListText( "//metadata/versioning/versions/version" ) );

            Element snapshotElem = xml.getElement( "//metadata/versioning/snapshot" );
            if ( snapshotElem != null )
            {
                SnapshotVersion snapshot = new SnapshotVersion( );
                snapshot.setTimestamp( XmlUtil.getChildText( snapshotElem, "timestamp" ) );
                String buildNumber = XmlUtil.getChildText( snapshotElem, "buildNumber" );
                if ( NumberUtils.isCreatable( buildNumber ) )
                {
                    snapshot.setBuildNumber( NumberUtils.toInt( buildNumber ) );
                }
                metadata.setSnapshotVersion( snapshot );
            }

            for ( Node node : xml.getElementList( "//metadata/plugins/plugin" ) )
            {
                if ( node instanceof Element )
                {
                    Element plugin = (Element) node;
                    Plugin p = new Plugin( );
                    String prefix = plugin.getElementsByTagName( "prefix" ).item( 0 ).getTextContent( ).trim( );
                    p.setPrefix( prefix );
                    String artifactId = plugin.getElementsByTagName( "artifactId" ).item( 0 ).getTextContent( ).trim( );
                    p.setArtifactId( artifactId );
                    String name = plugin.getElementsByTagName( "name" ).item( 0 ).getTextContent( ).trim( );
                    p.setName( name );
                    metadata.addPlugin( p );
                }
            }
        } catch ( XMLException e) {
            throw new RepositoryMetadataException( "XML Error while reading metadata file : " + e.getMessage( ), e );
        }
        return metadata;
    }

    @Override
    public boolean isValidMetadataPath( String path )
    {
        if ( StringUtils.isNotEmpty( path ) ) {
            return path.endsWith( MAVEN_METADATA );
        } else {
            return false;
        }
    }

    @Override
    public boolean isValidForType( RepositoryType repositoryType )
    {
        return RepositoryType.MAVEN.equals( repositoryType );
    }
}
