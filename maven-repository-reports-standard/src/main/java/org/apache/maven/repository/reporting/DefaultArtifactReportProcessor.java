package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.Iterator;
import java.util.List;

/**
 * 
 */
public class DefaultArtifactReportProcessor
    implements ArtifactReportProcessor
{
    private static final String EMPTY_STRING = "";

    // plexus components
    private ArtifactFactory artifactFactory;

    private RepositoryQueryLayer repositoryQueryLayer;

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
    {
        if ( artifact == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.NULL_ARTIFACT );
        }
        else
        {
            processArtifact( artifact, reporter );
        }

        if ( model == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.NULL_MODEL );
        }
        else
        {
            List dependencies = model.getDependencies();
            processDependencies( dependencies, reporter );
        }
    }

    protected void processArtifact( Artifact artifact, ArtifactReporter reporter )
    {
        boolean hasFailed = false;
        if ( EMPTY_STRING.equals( artifact.getGroupId() ) || artifact.getGroupId() == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.EMPTY_GROUP_ID );
            hasFailed = true;
        }
        if ( EMPTY_STRING.equals( artifact.getArtifactId() ) || artifact.getArtifactId() == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.EMPTY_ARTIFACT_ID );
            hasFailed = true;
        }
        if ( EMPTY_STRING.equals( artifact.getVersion() ) || artifact.getVersion() == null )
        {
            reporter.addFailure( artifact, ArtifactReporter.EMPTY_VERSION );
            hasFailed = true;
        }
        if ( !hasFailed )
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
    }

    protected void processDependencies( List dependencies, ArtifactReporter reporter )
    {
        if ( dependencies.size() > 0 )
        {
            Iterator iterator = dependencies.iterator();
            while ( iterator.hasNext() )
            {
                boolean hasFailed = false;
                Dependency dependency = (Dependency) iterator.next();
                Artifact artifact = createArtifact( dependency );
                if ( EMPTY_STRING.equals( dependency.getGroupId() ) || dependency.getGroupId() == null )
                {
                    reporter.addFailure( artifact, ArtifactReporter.EMPTY_DEPENDENCY_GROUP_ID );
                    hasFailed = true;
                }
                if ( EMPTY_STRING.equals( dependency.getArtifactId() ) || dependency.getArtifactId() == null )
                {
                    reporter.addFailure( artifact, ArtifactReporter.EMPTY_DEPENDENCY_ARTIFACT_ID );
                    hasFailed = true;
                }
                if ( EMPTY_STRING.equals( dependency.getVersion() ) || dependency.getVersion() == null )
                {
                    reporter.addFailure( artifact, ArtifactReporter.EMPTY_DEPENDENCY_VERSION );
                    hasFailed = true;
                }
                if ( !hasFailed )
                {
                    if ( repositoryQueryLayer.containsArtifact( artifact ) )
                    {
                        reporter.addSuccess( artifact );
                    }
                    else
                    {
                        reporter.addFailure( artifact, ArtifactReporter.DEPENDENCY_NOT_FOUND );
                    }
                }
            }
        }

    }

    /**
     * Only used for passing a mock object when unit testing
     *
     * @param repositoryQueryLayer
     */
    protected void setRepositoryQueryLayer( RepositoryQueryLayer repositoryQueryLayer )
    {
        this.repositoryQueryLayer = repositoryQueryLayer;
    }

    /**
     * Only used for passing a mock object when unit testing
     *
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
