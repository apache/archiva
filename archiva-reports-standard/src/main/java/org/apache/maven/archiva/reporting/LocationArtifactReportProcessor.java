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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validate the location of the artifact based on the values indicated
 * in its pom (both the pom packaged with the artifact & the pom in the
 * file system).
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.ArtifactReportProcessor" role-hint="artifact-location"
 */
public class LocationArtifactReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * Check whether the artifact is in its proper location. The location of the artifact
     * is validated first against the groupId, artifactId and versionId in the specified model
     * object (pom in the file system). Then unpack the artifact (jar file) and get the model (pom)
     * included in the package. If a model exists inside the package, then check if the artifact's
     * location is valid based on the location specified in the pom. Check if the both the location
     * specified in the file system pom and in the pom included in the package is the same.
     *
     * @param model      Represents the pom in the file system.
     * @param artifact
     * @param reporter
     * @param repository
     */
    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
        throws ReportProcessorException
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        //check if the artifact is located in its proper location based on the info
        //specified in the model object/pom
        Artifact modelArtifact = artifactFactory.createBuildArtifact( model.getGroupId(), model.getArtifactId(),
                                                                      model.getVersion(), model.getPackaging() );

        boolean failed = false;
        String modelPath = repository.pathOf( modelArtifact );
        String artifactPath = repository.pathOf( artifact );
        if ( modelPath.equals( artifactPath ) )
        {
            //get the location of the artifact itself
            File file = new File( repository.getBasedir(), artifactPath );

            if ( file.exists() )
            {
                //unpack the artifact (using the groupId, artifactId & version specified in the artifact object itself
                //check if the pom is included in the package
                Model extractedModel = readArtifactModel( file, artifact.getGroupId(), artifact.getArtifactId() );

                if ( extractedModel != null )
                {
                    Artifact extractedArtifact = artifactFactory.createBuildArtifact( extractedModel.getGroupId(),
                                                                                      extractedModel.getArtifactId(),
                                                                                      extractedModel.getVersion(),
                                                                                      extractedModel.getPackaging() );
                    if ( !repository.pathOf( extractedArtifact ).equals( artifactPath ) )
                    {
                        reporter.addFailure( artifact,
                                             "The artifact is out of place. It does not match the specified location in the packaged pom." );
                        failed = true;
                    }
                }
            }
            else
            {
                reporter.addFailure( artifact,
                                     "The artifact is out of place. It does not exist at the specified location in the repository pom." );
                failed = true;
            }
        }
        else
        {
            reporter.addFailure( artifact,
                                 "The artifact is out of place. It does not match the specified location in the repository pom." );
            failed = true;
        }

        if ( !failed )
        {
            reporter.addSuccess( artifact );
        }
    }

    /**
     * Extract the contents of the artifact/jar file.
     *
     * @param file
     * @param groupId
     * @param artifactId
     */
    private Model readArtifactModel( File file, String groupId, String artifactId )
        throws ReportProcessorException
    {
        Model model = null;

        JarFile jar = null;
        try
        {
            jar = new JarFile( file );

            //Get the entry and its input stream.
            JarEntry entry = jar.getJarEntry( "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml" );

            // If the entry is not null, extract it.
            if ( entry != null )
            {
                model = readModel( jar.getInputStream( entry ) );
            }
        }
        catch ( IOException e )
        {
            // TODO: should just warn and continue?
            throw new ReportProcessorException( "Unable to read artifact to extract model", e );
        }
        catch ( XmlPullParserException e )
        {
            // TODO: should just warn and continue?
            throw new ReportProcessorException( "Unable to read artifact to extract model", e );
        }
        finally
        {
            if ( jar != null )
            {
                //noinspection UnusedCatchParameter
                try
                {
                    jar.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
        return model;
    }

    private Model readModel( InputStream entryStream )
        throws IOException, XmlPullParserException
    {
        Reader isReader = new InputStreamReader( entryStream );

        Model model;
        try
        {
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            model = pomReader.read( isReader );
        }
        finally
        {
            IOUtil.close( isReader );
        }
        return model;
    }

}
