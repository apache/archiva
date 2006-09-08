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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This class validates well-formedness of pom xml file.
 *
 * @todo nice to have this a specific, tested report - however it is likely to double up with project building exceptions from IndexerTask. Resolve [!]
 * @plexus.component role="org.apache.maven.archiva.reporting.ArtifactReportProcessor" role-hint="invalid-pom"
 */
public class InvalidPomArtifactReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @param artifact The pom xml file to be validated, passed as an artifact object.
     * @param reporter The artifact reporter object.
     */
    public void processArtifact( Artifact artifact, Model model, ReportingDatabase reporter )
    {
        ArtifactRepository repository = artifact.getRepository();

        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        if ( "pom".equals( artifact.getType().toLowerCase() ) )
        {
            File f = new File( repository.getBasedir(), repository.pathOf( artifact ) );

            if ( !f.exists() )
            {
                reporter.addFailure( artifact, "Artifact not found." );
            }
            else
            {
                Reader reader = null;

                MavenXpp3Reader pomReader = new MavenXpp3Reader();

                try
                {
                    reader = new FileReader( f );
                    pomReader.read( reader );
                }
                catch ( XmlPullParserException e )
                {
                    reporter.addFailure( artifact, "The pom xml file is not well-formed. Error while parsing: " +
                        e.getMessage() );
                }
                catch ( FileNotFoundException e )
                {
                    reporter.addFailure( artifact, "Error while reading the pom xml file: " + e.getMessage() );
                }
                catch ( IOException e )
                {
                    reporter.addFailure( artifact, "Error while reading the pom xml file: " + e.getMessage() );
                }
                finally
                {
                    IOUtil.close( reader );
                }
            }
        }
    }
}
