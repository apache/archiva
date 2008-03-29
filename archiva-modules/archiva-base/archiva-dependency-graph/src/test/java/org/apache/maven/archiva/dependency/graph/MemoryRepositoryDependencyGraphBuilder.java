package org.apache.maven.archiva.dependency.graph;

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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * MemoryRepositoryProjectResolver 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MemoryRepositoryDependencyGraphBuilder
    implements DependencyGraphBuilder
{
    private MemoryRepository memoryRepository;

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
    {
        ArtifactReference artifact = new ArtifactReference();
        artifact.setGroupId( reference.getGroupId() );
        artifact.setArtifactId( reference.getArtifactId() );
        artifact.setVersion( reference.getVersion() );
        artifact.setType( "pom" );

        return resolveProjectModel( artifact );
    }

    public ArchivaProjectModel resolveProjectModel( ArtifactReference reference )
    {
        ArchivaProjectModel model = memoryRepository
            .getProjectModel( reference.getGroupId(), reference.getArtifactId(), reference.getVersion() );

        if ( model == null )
        {
            throw new NullPointerException( "Unable to find model for " + DependencyGraphKeys.toKey( reference ) );
        }

        if ( model.getParentProject() != null )
        {
            ArchivaProjectModel parentModel = resolveProjectModel( model.getParentProject() );

            model.getDependencies().addAll( parentModel.getDependencies() );
            model.getDependencyManagement().addAll( parentModel.getDependencyManagement() );
        }

        return model;
    }

    public MemoryRepository getMemoryRepository()
    {
        return memoryRepository;
    }

    public void setMemoryRepository( MemoryRepository memoryRepository )
    {
        this.memoryRepository = memoryRepository;
    }

    public DependencyGraph createGraph( VersionedReference versionedProjectReference )
    {
        String groupId = versionedProjectReference.getGroupId();
        String artifactId = versionedProjectReference.getArtifactId();
        String version = versionedProjectReference.getVersion();

        DependencyGraph graph = new DependencyGraph( groupId, artifactId, version );
        return graph;
    }

    public void resolveNode( DependencyGraph graph, DependencyGraphNode fromNode,
                             VersionedReference versionedProjectReference )
    {
        ArchivaProjectModel model = resolveProjectModel( fromNode.getArtifact() );

        DependencyGraphUtils.addNodeFromModel( model, graph, fromNode );
    }
}
