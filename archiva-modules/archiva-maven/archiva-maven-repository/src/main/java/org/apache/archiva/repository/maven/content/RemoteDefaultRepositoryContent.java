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

import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;

import java.util.List;

/**
 * RemoteDefaultRepositoryContent
 */
public class RemoteDefaultRepositoryContent
    extends AbstractDefaultRepositoryContent
    implements RemoteRepositoryContent
{
    private RemoteRepository repository;


    public RemoteDefaultRepositoryContent( List<? extends ArtifactMappingProvider> artifactMappingProviders ) {
        super(artifactMappingProviders);
    }

    @Override
    public String getId( )
    {
        return repository.getId( );
    }

    @Override
    public RemoteRepository getRepository( )
    {
        return repository;
    }

    @Override
    public RepositoryURL getURL( )
    {
        try
        {
            return new RepositoryURL( repository.getLocation( ).toString( ) );
        }
        catch ( Exception e )
        {
            log.error( "Could not convert location url {}", repository.getLocation( ) );
            return new RepositoryURL( "" );
        }
    }

    @Override
    public void setRepository( RemoteRepository repository )
    {
        this.repository = repository;
    }

    /**
     * Convert a path to an artifact reference.
     *
     * @param path the path to convert. (relative or full url path)
     * @throws LayoutException if the path cannot be converted to an artifact reference.
     */
    @Override
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {

        if ( ( path != null ) && repository.getLocation()!=null && path.startsWith( repository.getLocation().toString() ) )
        {
            return super.toArtifactReference( path.substring( repository.getLocation().toString().length( ) ) );
        }

        return super.toArtifactReference( path );
    }

    @Override
    public RepositoryURL toURL( ArtifactReference reference )
    {
        String url = repository.getLocation( ) + toPath( reference );
        return new RepositoryURL( url );
    }

}
