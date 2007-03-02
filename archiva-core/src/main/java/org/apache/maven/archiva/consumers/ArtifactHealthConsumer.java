package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.common.consumers.GenericArtifactConsumer;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.util.Collections;

/**
 * ArtifactHealthConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 *
 * @plexus.component role="org.apache.maven.archiva.common.consumers.Consumer"
 *     role-hint="artifact-health"
 *     instantiation-strategy="per-lookup"
 */
public class ArtifactHealthConsumer
    extends GenericArtifactConsumer
{
    /**
     * @plexus.requirement
     */
    private ArtifactResultsDatabase database;

    /**
     * @plexus.requirement role-hint="health"
     */
    private ReportGroup health;

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    public void processArtifact( Artifact artifact, BaseFile file )
    {
        Model model = null;
        try
        {
            Artifact pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact
                .getArtifactId(), artifact.getVersion() );
            MavenProject project = projectBuilder.buildFromRepository( pomArtifact, Collections.EMPTY_LIST, repository );

            model = project.getModel();
        }
        catch ( InvalidArtifactRTException e )
        {
            database.addWarning( artifact, "health", "invalid", "Invalid artifact [" + artifact + "] : " + e );
        }
        catch ( ProjectBuildingException e )
        {
            database.addWarning( artifact, "health", "project-build", "Error reading project model: " + e );
        }
        
        database.remove( artifact );
        health.processArtifact( artifact, model );
    }

    public void processFileProblem( BaseFile path, String message )
    {
        /* do nothing here (yet) */
        // TODO: store build failure into database?
    }
    
    public String getName()
    {
        return "Artifact Health Consumer";
    }
}
