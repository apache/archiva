package org.apache.archiva.repository.content.maven2;

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

import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * RemoteDefaultRepositoryContent
 *
 *
 */
@Service( "remoteRepositoryContent#default" )
@Scope( "prototype" )
public class RemoteDefaultRepositoryContent
    extends AbstractDefaultRepositoryContent
    implements RemoteRepositoryContent
{
    private RemoteRepository repository;

    @Override
    public String getId()
    {
        return repository.getId();
    }

    @Override
    public RemoteRepository getRepository()
    {
        return repository;
    }

    @Override
    public RepositoryURL getURL()
    {
        return new RepositoryURL( repository.getUrl() );
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
     * @throws org.apache.archiva.repository.layout.LayoutException if the path cannot be converted to an artifact reference.
     */
    @Override
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( ( path != null ) && path.startsWith( repository.getUrl() ) )
        {
            return super.toArtifactReference( path.substring( repository.getUrl().length() ) );
        }

        return super.toArtifactReference( path );
    }

    @Override
    public RepositoryURL toURL( ArtifactReference reference )
    {
        String url = repository.getUrl() + toPath( reference );
        return new RepositoryURL( url );
    }
}
