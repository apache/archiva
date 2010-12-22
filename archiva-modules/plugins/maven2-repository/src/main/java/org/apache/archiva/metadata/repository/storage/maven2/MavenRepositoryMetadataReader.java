package org.apache.archiva.metadata.repository.storage.maven2;

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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.dom4j.Element;

import java.io.File;

/**
 * RepositoryMetadataReader - read maven-metadata.xml files.
 *
 * TODO: we should improve on this, ideally using the Maven standard library
 */
public final class MavenRepositoryMetadataReader
{
    private MavenRepositoryMetadataReader()
    {
    }

    /**
     * Read and return the {@link MavenRepositoryMetadata} object from the provided xml file.
     *
     * @param metadataFile the maven-metadata.xml file to read.
     * @return the archiva repository metadata object that represents the provided file contents.
     * @throws org.apache.maven.archiva.xml.XMLException
     *
     */
    public static MavenRepositoryMetadata read( File metadataFile )
        throws XMLException
    {
        XMLReader xml = new XMLReader( "metadata", metadataFile );
        // invoke this to remove namespaces, see MRM-1136
        xml.removeNamespaces();

        MavenRepositoryMetadata metadata = new MavenRepositoryMetadata();

        metadata.setGroupId( xml.getElementText( "//metadata/groupId" ) );
        metadata.setArtifactId( xml.getElementText( "//metadata/artifactId" ) );
        metadata.setVersion( xml.getElementText( "//metadata/version" ) );

        metadata.setLastUpdated( xml.getElementText( "//metadata/versioning/lastUpdated" ) );
        metadata.setLatestVersion( xml.getElementText( "//metadata/versioning/latest" ) );
        metadata.setReleasedVersion( xml.getElementText( "//metadata/versioning/release" ) );
        metadata.setAvailableVersions( xml.getElementListText( "//metadata/versioning/versions/version" ) );

        Element snapshotElem = xml.getElement( "//metadata/versioning/snapshot" );
        if ( snapshotElem != null )
        {
            MavenRepositoryMetadata.Snapshot snapshot = new MavenRepositoryMetadata.Snapshot();
            snapshot.setTimestamp( snapshotElem.elementTextTrim( "timestamp" ) );
            String tmp = snapshotElem.elementTextTrim( "buildNumber" );
            if ( NumberUtils.isNumber( tmp ) )
            {
                snapshot.setBuildNumber( NumberUtils.toInt( tmp ) );
            }
            metadata.setSnapshotVersion( snapshot );
        }

        for ( Element plugin : xml.getElementList( "//metadata/plugins/plugin" ) )
        {
            MavenRepositoryMetadata.Plugin p = new MavenRepositoryMetadata.Plugin();
            p.setPrefix( plugin.elementTextTrim( "prefix" ) );
            p.setArtifactId( plugin.elementTextTrim( "artifactId" ) );
            p.setName( plugin.elementTextTrim( "name" ) );
            metadata.addPlugin( p );
        }

        return metadata;
    }
}