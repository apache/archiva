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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;

import java.io.File;

/**
 * Find artifacts in the repository that are considered old.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.ArtifactReportProcessor" role-hint="old-artifact"
 * @todo make this configurable from the web interface
 */
public class OldArtifactReportProcessor
    implements ArtifactReportProcessor
{
    private static final String ROLE_HINT = "old-artifact";

    /**
     * The maximum age of an artifact before it is reported old, specified in seconds. The default is 1 year.
     *
     * @plexus.configuration default-value="31536000"
     */
    private int maxAge;

    public void processArtifact( Artifact artifact, Model model, ReportingDatabase reporter )
    {
        ArtifactRepository repository = artifact.getRepository();

        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        adjustDistributionArtifactHandler( artifact );

        String artifactPath = repository.pathOf( artifact );

        //get the location of the artifact itself
        File file = new File( repository.getBasedir(), artifactPath );

        if ( file.exists() )
        {
            if ( System.currentTimeMillis() - file.lastModified() > maxAge * 1000 )
            {
                // TODO: reason could be an i18n key derived from the processor and the problem ID and the
                reporter.addNotice( artifact, ROLE_HINT, "old-artifact",
                                    "The artifact is older than the maximum age of " + maxAge + " seconds." );
            }
        }
        else
        {
            throw new IllegalStateException( "Couldn't find artifact " + file );
        }
    }

    private static void adjustDistributionArtifactHandler( Artifact artifact )
    {
        // need to tweak these as they aren't currently in the known type converters. TODO - add them in Maven
        if ( "distribution-zip".equals( artifact.getType() ) )
        {
            artifact.setArtifactHandler( new DefaultArtifactHandler( "zip" ) );
        }
        else if ( "distribution-tgz".equals( artifact.getType() ) )
        {
            artifact.setArtifactHandler( new DefaultArtifactHandler( "tar.gz" ) );
        }
    }
}
