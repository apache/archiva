package org.apache.archiva.repository.mock;

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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.springframework.stereotype.Service;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("remoteRepositoryContent#mock")
public class RemoteRepositoryContentMock implements RemoteRepositoryContent
{
    RemoteRepository repository;

    @Override
    public String getId( )
    {
        return null;
    }

    @Override
    public RemoteRepository getRepository( )
    {
        return null;
    }

    @Override
    public RepositoryURL getURL( )
    {
        return null;
    }

    @Override
    public void setRepository( RemoteRepository repo )
    {
        this.repository = repo;
    }

    @Override
    public ArtifactReference toArtifactReference( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public String toPath( ArtifactReference reference )
    {
        return null;
    }

    @Override
    public String toPath( ItemSelector selector )
    {
        return null;
    }

    @Override
    public ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public RepositoryURL toURL( ArtifactReference reference )
    {
        return null;
    }
}
