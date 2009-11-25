package org.apache.archiva.metadata.repository.storage.maven2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Collection;

import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataResolver" role-hint="maven2"
 */
public class Maven2RepositoryMetadataResolver
    implements MetadataResolver
{
    /**
     * @plexus.requirement
     */
    private ModelBuilder builder;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role-hint="maven2"
     */
    private RepositoryPathTranslator pathTranslator;

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        throw new UnsupportedOperationException();
    }

    public ProjectBuildMetadata getProjectBuild( String repoId, String namespace, String projectId, String buildId )
    {
        ManagedRepositoryConfiguration repositoryConfiguration =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );

        File basedir = new File( repositoryConfiguration.getLocation() );
        File file = pathTranslator.toFile( basedir, namespace, projectId, buildId, projectId + "-" + buildId + ".pom" );

        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins( false );
        req.setPomFile( file );
        req.setModelResolver( new RepositoryModelResolver( basedir, pathTranslator ) );

        Model model;
        try
        {
            model = builder.build( req ).getEffectiveModel();
        }
        catch ( ModelBuildingException e )
        {
            // TODO: handle it
            throw new RuntimeException( e );
        }

        MavenProjectFacet facet = new MavenProjectFacet();
        facet.setGroupId( model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId() );
        facet.setArtifactId( model.getArtifactId() );
        facet.setPackaging( model.getPackaging() );
        if ( model.getParent() != null )
        {
            MavenProjectParent parent = new MavenProjectParent();
            parent.setGroupId( model.getParent().getGroupId() );
            parent.setArtifactId( model.getParent().getArtifactId() );
            parent.setVersion( model.getParent().getVersion() );
            facet.setParent( parent );
        }
        ProjectBuildMetadata metadata = new ProjectBuildMetadata();
        metadata.setUrl( model.getUrl() );
        metadata.addFacet( facet );
        // TODO: convert project

        return metadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId, String buildId )
    {
        throw new UnsupportedOperationException();
    }
}
