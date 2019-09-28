package org.apache.archiva.maven2.metadata;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.xml.XMLException;
import org.apache.archiva.xml.XMLReader;
import org.apache.archiva.xml.XmlUtil;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class MavenMetadataReader
{
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

    public static ArchivaRepositoryMetadata read(StorageAsset metadataFile) throws XMLException, IOException {
        if (metadataFile.isFileBased()) {
            return read(metadataFile.getFilePath());
        } else {
            throw new IOException("StorageAsset is not file based");
        }
    }

    /**
     * Read and return the {@link org.apache.archiva.model.ArchivaRepositoryMetadata} object from the provided xml file.
     *
     * @param metadataFile the maven-metadata.xml file to read.
     * @return the archiva repository metadata object that represents the provided file contents.
     * @throws XMLException
     */
    public static ArchivaRepositoryMetadata read( Path metadataFile )
            throws XMLException {

        XMLReader xml = new XMLReader( "metadata", metadataFile );
        // invoke this to remove namespaces, see MRM-1136
        xml.removeNamespaces();

        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();

        metadata.setGroupId( xml.getElementText( "//metadata/groupId" ) );
        metadata.setArtifactId( xml.getElementText( "//metadata/artifactId" ) );
        metadata.setVersion( xml.getElementText( "//metadata/version" ) );
        Date modTime;
        try {
            modTime = new Date(Files.getLastModifiedTime(metadataFile).toMillis());
        } catch (IOException e) {
            modTime = new Date();
            log.error("Could not read modification time of {}", metadataFile);
        }
        metadata.setFileLastModified( modTime );
        try {
            metadata.setFileSize(Files.size(metadataFile));
        } catch (IOException e) {
            metadata.setFileSize( 0 );
            log.error("Could not read file size of {}", metadataFile);
        }
        metadata.setLastUpdated( xml.getElementText( "//metadata/versioning/lastUpdated" ) );
        metadata.setLatestVersion( xml.getElementText( "//metadata/versioning/latest" ) );
        metadata.setReleasedVersion( xml.getElementText( "//metadata/versioning/release" ) );
        metadata.setAvailableVersions( xml.getElementListText( "//metadata/versioning/versions/version" ) );

        Element snapshotElem = xml.getElement( "//metadata/versioning/snapshot" );
        if ( snapshotElem != null )
        {
            SnapshotVersion snapshot = new SnapshotVersion();
            snapshot.setTimestamp(XmlUtil.getChildText(snapshotElem, "timestamp"));
            String buildNumber = XmlUtil.getChildText(snapshotElem, "buildNumber");
            if ( NumberUtils.isCreatable( buildNumber ) )
            {
                snapshot.setBuildNumber( NumberUtils.toInt( buildNumber ) );
            }
            metadata.setSnapshotVersion( snapshot );
        }

        for ( Node node : xml.getElementList( "//metadata/plugins/plugin" ) )
        {
            if (node instanceof Element) {
                Element plugin = (Element) node;
                Plugin p = new Plugin();
                String prefix = plugin.getElementsByTagName("prefix").item(0).getTextContent().trim();
                p.setPrefix(prefix);
                String artifactId = plugin.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                p.setArtifactId(artifactId);
                String name = plugin.getElementsByTagName("name").item(0).getTextContent().trim();
                p.setName(name);
                metadata.addPlugin(p);
            }
        }

        return metadata;

    }
}
