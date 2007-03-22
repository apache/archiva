package org.apache.maven.archiva.reporting.processor;

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

import org.apache.maven.archiva.layer.RepositoryQueryLayer;
import org.apache.maven.archiva.layer.RepositoryQueryLayerFactory;
import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
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
 * @plexus.component role="org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor" role-hint="dependency"
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

    /**
     * @plexus.requirement
     */
    private ArtifactResultsDatabase database;

    private static final String POM = "pom";

    private static final String ROLE_HINT = "dependency";

    public void processArtifact( Artifact artifact, Model model )
    {
        RepositoryQueryLayer queryLayer = layerFactory.createRepositoryQueryLayer( artifact.getRepository() );
        if ( !queryLayer.containsArtifact( artifact ) )
        {
            // TODO: is this even possible?
            addFailure( artifact, "missing-artifact", "Artifact does not exist in the repository" );
        }

        if ( model != null && POM.equals( artifact.getType() ) )
        {
            List dependencies = model.getDependencies();
            processDependencies( dependencies, queryLayer, artifact );
        }
    }

    private void addFailure( Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        database.addFailure( artifact, ROLE_HINT, problem, reason );
    }

    private void processDependencies( List dependencies, RepositoryQueryLayer repositoryQueryLayer,
                                      Artifact sourceArtifact )
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
                        String reason = MessageFormat
                            .format( "Artifact''s dependency {0} does not exist in the repository",
                                     new String[] { getDependencyString( dependency ) } );
                        addFailure( sourceArtifact, "missing-dependency:" + getDependencyKey( dependency ), reason );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    String reason = MessageFormat.format( "Artifact''s dependency {0} contains an invalid version {1}",
                                                          new String[] {
                                                              getDependencyString( dependency ),
                                                              dependency.getVersion() } );
                    addFailure( sourceArtifact, "bad-version:" + getDependencyKey( dependency ), reason );
                }
            }
        }
    }

    private String getDependencyKey( Dependency dependency )
    {
        String str = dependency.getGroupId();
        str += ":" + dependency.getArtifactId();
        str += ":" + dependency.getVersion();
        str += ":" + dependency.getType();
        if ( dependency.getClassifier() != null )
        {
            str += ":" + dependency.getClassifier();
        }
        return str;
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
                                                         dependency.getType(), dependency.getClassifier(), dependency
                                                             .getScope() );
    }
}
