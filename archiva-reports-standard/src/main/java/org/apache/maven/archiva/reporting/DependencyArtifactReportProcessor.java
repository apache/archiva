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
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.text.MessageFormat;
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

    private static final String POM = "pom";

    public void processArtifact( Artifact artifact, Model model, ReportingDatabase reporter )
    {
        RepositoryQueryLayer queryLayer = layerFactory.createRepositoryQueryLayer( artifact.getRepository() );
        processArtifact( artifact, reporter, queryLayer );

        if ( model != null && POM.equals( artifact.getType() ) )
        {
            List dependencies = model.getDependencies();
            processDependencies( dependencies, reporter, queryLayer, artifact );
        }
    }

    private void processArtifact( Artifact artifact, ReportingDatabase reporter,
                                  RepositoryQueryLayer repositoryQueryLayer )
    {
        if ( !repositoryQueryLayer.containsArtifact( artifact ) )
        {
            reporter.addFailure( artifact, "Artifact does not exist in the repository" );
        }
    }

    private void processDependencies( List dependencies, ReportingDatabase reporter,
                                      RepositoryQueryLayer repositoryQueryLayer, Artifact sourceArtifact )
    {
        if ( dependencies.size() > 0 )
        {
            Iterator iterator = dependencies.iterator();
            while ( iterator.hasNext() )
            {
                Dependency dependency = (Dependency) iterator.next();

                try
                {
                    Artifact artifact = createArtifact( dependency );

                    // TODO: handle ranges properly. We should instead be mapping out all the artifacts in the
                    // repository and mapping out the graph

                    if ( artifact.getVersion() == null )
                    {
                        // it was a range, for now presume it exists
                        continue;
                    }

                    if ( !repositoryQueryLayer.containsArtifact( artifact ) )
                    {
                        String reason = MessageFormat.format(
                            "Artifact''s dependency {0} does not exist in the repository",
                            new String[]{getDependencyString( dependency )} );
                        reporter.addFailure( sourceArtifact, reason );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    String reason = MessageFormat.format( "Artifact''s dependency {0} contains an invalid version {1}",
                                                          new String[]{getDependencyString( dependency ),
                                                              dependency.getVersion()} );
                    reporter.addFailure( sourceArtifact, reason );
                }
            }
        }
    }

    static String getDependencyString( Dependency dependency )
    {
        String str = "(group=" + dependency.getGroupId();
        str += ", artifact=" + dependency.getArtifactId();
        str += ", version=" + dependency.getVersion();
        str += ", type=" + dependency.getType();
        if ( dependency.getClassifier() != null )
        {
            str += ", classifier=" + dependency.getClassifier();
        }
        str += ")";
        return str;
    }

    private Artifact createArtifact( Dependency dependency )
        throws InvalidVersionSpecificationException
    {
        VersionRange spec = VersionRange.createFromVersionSpec( dependency.getVersion() );

        if ( spec == null )
        {
            throw new InvalidVersionSpecificationException( "Dependency version was null" );
        }

        return artifactFactory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(), spec,
                                                         dependency.getType(), dependency.getClassifier(),
                                                         dependency.getScope() );
    }
}
