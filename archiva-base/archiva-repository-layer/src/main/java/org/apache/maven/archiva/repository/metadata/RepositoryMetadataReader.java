package org.apache.maven.archiva.repository.metadata;

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

import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.RepositoryContent;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;

import java.io.File;
import java.util.Date;

/**
 * RepositoryMetadataReader - read maven-metadata.xml files.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryMetadataReader
{
    /**
     * Read and return the {@link ArchivaRepositoryMetadata} object from the provided xml file.
     * 
     * @param metadataFile the maven-metadata.xml file to read.
     * @return the archiva repository metadata object that represents the provided file contents.
     * @throws RepositoryMetadataException
     */
    public ArchivaRepositoryMetadata read( File metadataFile ) throws RepositoryMetadataException
    {
        try
        {
            XMLReader xml = new XMLReader( "metadata", metadataFile );

            ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();

            RepositoryContent contentKey = new RepositoryContent();
            contentKey.setGroupId( xml.getElementText( "//metadata/groupId" ) );
            contentKey.setArtifactId( xml.getElementText( "//metadata/artifactId" ) );
            
            metadata.setContentKey( contentKey );

            metadata.setLastModified( new Date( metadataFile.lastModified() ) );
            metadata.setSize( metadataFile.length() );
            metadata.setWhenIndexed( new Date() );

            metadata.setReleasedVersion( xml.getElementText( "//metadata/versioning/release" ) );
            metadata.setAvailableVersions( xml.getElementListText( "//metadata/versioning/versions/version" ) );

            return metadata;
        }
        catch ( XMLException e )
        {
            throw new RepositoryMetadataException( e.getMessage(), e );
        }
    }
}
