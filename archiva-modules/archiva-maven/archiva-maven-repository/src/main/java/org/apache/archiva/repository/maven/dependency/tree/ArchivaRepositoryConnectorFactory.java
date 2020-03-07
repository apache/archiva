package org.apache.archiva.repository.maven.dependency.tree;
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

import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

import java.util.Collection;

/**
 *
 * Creates a dummy connector, if the default connectory factory fails to create one.
 *
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class ArchivaRepositoryConnectorFactory
    implements RepositoryConnectorFactory
{

    private BasicRepositoryConnectorFactory delegate = new BasicRepositoryConnectorFactory();

    public ArchivaRepositoryConnectorFactory()
    {
        // no op but empty constructor needed by aether
        delegate.setRepositoryLayoutProvider(new DefaultRepositoryLayoutProvider());
    }

    @Override
    public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
        throws NoRepositoryConnectorException
    {
        try
        {
            return delegate.newInstance( session, repository );
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

    @Override
    public float getPriority( )
    {
        return 0;
    }
}
