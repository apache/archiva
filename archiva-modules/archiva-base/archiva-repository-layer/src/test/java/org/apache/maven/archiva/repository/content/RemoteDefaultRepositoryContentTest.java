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
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * RemoteDefaultRepositoryContentTest 
 *
 * @version $Id$
 */
public class RemoteDefaultRepositoryContentTest
    extends AbstractDefaultRepositoryContentTestCase
{
    @Inject @Named(value = "remoteRepositoryContent#default")
    private RemoteRepositoryContent repoContent;

    @Before
    public void setUp()
        throws Exception
    {
        RemoteRepositoryConfiguration repository = createRemoteRepository( "testRemoteRepo", "Unit Test Remote Repo",
                                                                           "http://repo1.maven.org/maven2/" );

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
    protected String toPath( ArtifactReference reference )
    {
        return repoContent.toPath( reference );
    }
}
