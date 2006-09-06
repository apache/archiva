package org.apache.maven.archiva.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.layer.RepositoryQueryLayer;
import org.apache.maven.archiva.layer.RepositoryQueryLayerFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.Iterator;
import java.util.List;

/**
 * @plexus.component role="org.apache.maven.archiva.reporting.ArtifactReportProcessor" role-hint="dependency"
 */
public class DependencyArtifactReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryQueryLayerFactory layerFactory;

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
    {
        RepositoryQueryLayer queryLayer = layerFactory.createRepositoryQueryLayer( repository );
        processArtifact( artifact, reporter, queryLayer );

        List dependencies = model.getDependencies();
        processDependencies( dependencies, reporter, queryLayer );
    }

    private void processArtifact( Artifact artifact, ArtifactReporter reporter,
                                  RepositoryQueryLayer repositoryQueryLayer )
    {
        if ( repositoryQueryLayer.containsArtifact( artifact ) )
        {
            reporter.addSuccess( artifact );
        }
        else
        {
            reporter.addFailure( artifact, ArtifactReporter.ARTIFACT_NOT_FOUND );
        }
    }

    private void processDependencies( List dependencies, ArtifactReporter reporter,
                                      RepositoryQueryLayer repositoryQueryLayer )
    {
        if ( dependencies.size() > 0 )
        {
            Iterator iterator = dependencies.iterator();
            while ( iterator.hasNext() )
            {
                Dependency dependency = (Dependency) iterator.next();

                Artifact artifact = null;
                try
                {
                    artifact = createArtifact( dependency );

                    if ( repositoryQueryLayer.containsArtifact( artifact ) )
                    {
                        reporter.addSuccess( artifact );
                    }
                    else
                    {
                        reporter.addFailure( artifact, ArtifactReporter.DEPENDENCY_NOT_FOUND );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    reporter.addFailure( artifact, ArtifactReporter.DEPENDENCY_INVALID_VERSION );
                }
            }
        }
    }

    private Artifact createArtifact( Dependency dependency )
        throws InvalidVersionSpecificationException
    {
        return artifactFactory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                         VersionRange.createFromVersionSpec( dependency.getVersion() ),
                                                         dependency.getType(), dependency.getClassifier(),
                                                         dependency.getScope() );
    }
}
