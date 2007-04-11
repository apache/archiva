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
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.DefaultBidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;

import java.io.File;

/**
 * RepositoryProjectResolver 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryProjectResolver
    implements ProjectModelResolver
{
    private ArchivaRepository repository;

    private ProjectModelReader reader;

    private BidirectionalRepositoryLayout layout;

    public RepositoryProjectResolver( ArchivaRepository repository )
    {
        this.repository = repository;
        this.reader = new ProjectModel400Reader();
        this.layout = new DefaultBidirectionalRepositoryLayout();
    }

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException
    {
        ArchivaArtifact artifact = new ArchivaArtifact( reference.getGroupId(), reference.getArtifactId(), reference
            .getVersion(), "", "pom" );

        String path = layout.toPath( artifact );
        File repoFile = new File( this.repository.getUrl().getPath(), path );

        return reader.read( repoFile );
    }

}
