package org.apache.maven.archiva.repository.project.dependencies;

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

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.DependencyGraphUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolverFactory;
import org.apache.maven.archiva.repository.project.filters.EffectiveProjectModelFilter;

/**
 * ProjectModelBasedGraphBuilder 
 *
 * @version $Id$
 * 
 * @plexus.component 
 *              role="org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder"
 *              role-hint="project-model"
 */
public class ProjectModelBasedGraphBuilder
    implements DependencyGraphBuilder
{
    /**
     * @plexus.requirement
     */
    private ProjectModelResolverFactory resolverFactory;

    /**
     * @plexus.requirement 
     *          role="org.apache.maven.archiva.repository.project.ProjectModelFilter"
     *          role-hint="effective"
     */
    private EffectiveProjectModelFilter effectiveFilter = new EffectiveProjectModelFilter();

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
        ArchivaProjectModel model = resolveModel( fromNode.getArtifact() );

        DependencyGraphUtils.addNodeFromModel( model, graph, fromNode );
    }

    private ArchivaProjectModel resolveModel( ArtifactReference reference )
    {
        VersionedReference projectRef = new VersionedReference();

        projectRef.setGroupId( reference.getGroupId() );
        projectRef.setArtifactId( reference.getArtifactId() );
        projectRef.setVersion( reference.getVersion() );

        ArchivaProjectModel model = resolverFactory.getCurrentResolverStack().findProject( projectRef );

        if ( model == null )
        {
            return createDefaultModel( reference );
        }

        try
        {
            ArchivaProjectModel processedModel = effectiveFilter.filter( model );

            return processedModel;
        }
        catch ( ProjectModelException e )
        {
            e.printStackTrace( System.err );
            return createDefaultModel( reference );
        }
    }

    private ArchivaProjectModel createDefaultModel( ArtifactReference reference )
    {
        ArchivaProjectModel model = new ArchivaProjectModel();

        // Create default (dummy) model
        model = new ArchivaProjectModel();
        model.setGroupId( reference.getGroupId() );
        model.setArtifactId( reference.getArtifactId() );
        model.setVersion( reference.getVersion() );
        model.setPackaging( reference.getType() );
        return model;
    }
}
