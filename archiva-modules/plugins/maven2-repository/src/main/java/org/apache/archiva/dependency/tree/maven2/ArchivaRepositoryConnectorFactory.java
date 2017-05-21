package org.apache.archiva.dependency.tree.maven2;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;

import java.util.Collection;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class ArchivaRepositoryConnectorFactory
    extends FileRepositoryConnectorFactory
{
    public ArchivaRepositoryConnectorFactory()
    {
        // no op but empty constructor needed by aether
    }

    @Override
    public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
        throws NoRepositoryConnectorException
    {
        try
        {
            return super.newInstance( session, repository );
        }
        catch ( NoRepositoryConnectorException e )
        {

        }

        return new RepositoryConnector()
        {

            private Logger log = LoggerFactory.getLogger( getClass() );

            @Override
            public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                             Collection<? extends MetadataDownload> metadataDownloads )
            {
                log.debug( "get" );
            }

            @Override
            public void put( Collection<? extends ArtifactUpload> artifactUploads,
                             Collection<? extends MetadataUpload> metadataUploads )
            {
                log.debug( "put" );
            }

            @Override
            public void close()
            {
                log.debug( "close" );
            }
        };
    }
}
