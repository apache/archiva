package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.model.Model;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;

import java.util.List;
import java.util.Iterator;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultArtifactReportProcessor
    implements ArtifactReportProcessor
{

    // plexus components
    private ArtifactFactory artifactFactory;

    private RepositoryQueryLayer repositoryQueryLayer;

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter, ArtifactRepository repository )
    {
        if ( artifact == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.NULL_ARTIFACT );
        }
        if ( model == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.NULL_MODEL );
        }
        else
        {
            List dependencies = model.getDependencies();
            if ( ( artifact != null ) && ( model != null ) && ( dependencies != null ) )
            {
                if ( repositoryQueryLayer.containsArtifact( artifact ) )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    reporter.addFailure( artifact, ArtifactReporter.ARTIFACT_NOT_FOUND );
                }
                if ( dependencies.size() > 0 )
                {
                    Iterator iterator = dependencies.iterator();
                    while ( iterator.hasNext() )
                    {
                        Dependency dependency = (Dependency) iterator.next();
                        if ( repositoryQueryLayer.containsArtifact( createArtifact( dependency ) ) )
                        {
                            reporter.addSuccess( artifact );
                        }
                        else
                        {
                            reporter.addFailure( artifact, ArtifactReporter.ARTIFACT_NOT_FOUND );
                        }
                    }
                }
            }
        }
    }

    /**
     * Only used for passing a mock object when unit testing
     * @param repositoryQueryLayer
     */
    protected void setRepositoryQueryLayer( RepositoryQueryLayer repositoryQueryLayer )
    {
        this.repositoryQueryLayer = repositoryQueryLayer;
    }

    /**
     * Only used for passing a mock object when unit testing
     * @param artifactFactory
     */
    protected void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    private Artifact createArtifact( Dependency dependency )
    {
        return artifactFactory.createBuildArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                    dependency.getVersion(), "pom" );
    }
}
