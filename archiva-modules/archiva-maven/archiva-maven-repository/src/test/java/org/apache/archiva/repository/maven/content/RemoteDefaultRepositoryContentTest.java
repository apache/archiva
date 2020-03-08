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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.junit.Before;

import javax.inject.Inject;
import java.util.List;

/**
 * RemoteDefaultRepositoryContentTest
 */
public class RemoteDefaultRepositoryContentTest
    extends AbstractRepositoryContentTest
{

    @Inject
    private List<? extends ArtifactMappingProvider> artifactMappingProviders;

    private RemoteRepositoryContent repoContent;

    @Before
    public void setUp()
        throws Exception
    {
        RemoteRepository repository =
            createRemoteRepository( "testRemoteRepo", "Unit Test Remote Repo", "http://repo1.maven.org/maven2/" );

        repoContent = new RemoteDefaultRepositoryContent(artifactMappingProviders);
        //repoContent = (RemoteRepositoryContent) lookup( RemoteRepositoryContent.class, "default" );
        repoContent.setRepository( repository );
    }

    @Override
    protected ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return repoContent.toArtifactReference( path );
    }

    @Override
    protected ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return repoContent.toItemSelector( path );
    }

    @Override
    protected ManagedRepositoryContent getManaged( )
    {
        return null;
    }

    @Override
    protected RepositoryContent getContent( )
    {
        return repoContent;
    }

    @Override
    protected String toPath( ArtifactReference reference )
    {
        return repoContent.toPath( reference );
    }

    @Override
    protected String toPath( ItemSelector selector ) {
        return repoContent.toPath( selector );
    }
}
