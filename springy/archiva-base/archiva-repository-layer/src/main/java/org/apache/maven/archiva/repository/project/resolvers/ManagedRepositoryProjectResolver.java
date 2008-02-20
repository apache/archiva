package org.apache.maven.archiva.repository.project.resolvers;

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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

import java.io.File;

/**
 * Resolve Project from managed repository. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedRepositoryProjectResolver
    implements ProjectModelResolver, FilesystemBasedResolver
{
    private ManagedRepositoryContent repository;

    private ProjectModelReader reader;

    public ManagedRepositoryProjectResolver( ManagedRepositoryContent repository, ProjectModelReader reader )
    {
        this.repository = repository;
        this.reader = reader;
    }

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException
    {
        ArchivaArtifact artifact = new ArchivaArtifact( reference.getGroupId(), reference.getArtifactId(), reference
            .getVersion(), "", "pom" );

        File repoFile = repository.toFile( artifact );

        return reader.read( repoFile );
    }

}
