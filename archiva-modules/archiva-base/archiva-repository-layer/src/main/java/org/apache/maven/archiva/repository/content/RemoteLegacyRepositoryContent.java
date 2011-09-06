package org.apache.maven.archiva.repository.content;

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

import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.RepositoryURL;
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * RemoteLegacyRepositoryContent
 *
 * @version $Id$
 * @todo no need to be a component once legacy path parser is not
 */
@Service( "remoteRepositoryContent#legacy" )
@Scope( "prototype" )
public class RemoteLegacyRepositoryContent
    extends AbstractLegacyRepositoryContent
    implements RemoteRepositoryContent
{
    private RemoteRepositoryConfiguration repository;

    public String getId()
    {
        return repository.getId();
    }

    public RemoteRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public RepositoryURL getURL()
    {
        return new RepositoryURL( repository.getUrl() );
    }

    public void setRepository( RemoteRepositoryConfiguration repository )
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
        if ( path.startsWith( repository.getUrl() ) )
        {
            return super.toArtifactReference( path.substring( repository.getUrl().length() ) );
        }

        return super.toArtifactReference( path );
    }

    public RepositoryURL toURL( ArtifactReference reference )
    {
        String url = repository.getUrl() + toPath( reference );
        return new RepositoryURL( url );
    }
}
