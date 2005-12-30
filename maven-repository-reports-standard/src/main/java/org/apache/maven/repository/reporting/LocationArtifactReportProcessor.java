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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validate the location of the artifact based on the values indicated
 * in its pom (both the pom packaged with the artifact & the pom in the
 * file system).
 */
public class LocationArtifactReportProcessor
    implements ArtifactReportProcessor
{
    private boolean isLocal = true;

    private InputStream is;

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
    {
        boolean fsPomLocation = false, pkgPomLocation = false;
        String repositoryUrl = "", modelArtifactLocation = "";

        if ( !repository.getProtocol().equals( "file" ) )
        {
            isLocal = false;
            repositoryUrl = repository.getUrl();
        }
        else
        {
            repositoryUrl = repository.getBasedir();
        }

        //check if the artifact is located in its proper location based on the info
        //specified in the model object/pom
        modelArtifactLocation = repositoryUrl + model.getGroupId() + "/" + model.getArtifactId() + "/" +
            model.getVersion() + "/" + model.getArtifactId() + "-" + model.getVersion() + "." + model.getPackaging();
        fsPomLocation = validateArtifactLocation( modelArtifactLocation );

        //get the location of the artifact itself
        String artifactLocation = repositoryUrl + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." +
            artifact.getType();

        //unpack the artifact (using the groupId, artifactId & version specified in the artifact object itself
        //check if the pom is included in the package
        Model extractedModel =
            unpackArtifact( artifactLocation, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );

        if ( extractedModel != null )
        {

            String pkgPomArtifactLocation = repositoryUrl + extractedModel.getGroupId() + "/" +
                extractedModel.getArtifactId() + "/" + extractedModel.getVersion() + "/" +
                extractedModel.getArtifactId() + "-" + extractedModel.getVersion() + "." +
                extractedModel.getPackaging();
            pkgPomLocation = validateArtifactLocation( pkgPomArtifactLocation );

            //check the conditions
            if ( fsPomLocation == true && pkgPomLocation == true )
            {
                reporter.addSuccess( artifact );

            }
            else if ( fsPomLocation == false && pkgPomLocation == true )
            {
                reporter
                    .addFailure( artifact,
                                 "The artifact is out of place. It does not match the specified location in the file system pom." );

            }
            else if ( fsPomLocation == true && pkgPomLocation == false )
            {
                reporter
                    .addFailure( artifact,
                                 "The artifact is out of place. It does not match the specified location in the packaged pom." );

            }
            else if ( fsPomLocation == false && pkgPomLocation == false )
            {
                reporter.addFailure( artifact, "The artifact is out of place." );
            }

        }
        else
        {

            if ( fsPomLocation )
            {
                reporter.addSuccess( artifact );

            }
            else
            {
                reporter.addFailure( artifact, "The artifact is out of place." );
            }
        }
    }

    /**
     * Validate the if the artifact exists in the specified location.
     *
     * @param filename
     * @return
     */
    private boolean validateArtifactLocation( String filename )
    {
        try
        {
            if ( isLocal )
            {
                is = new FileInputStream( filename );
            }
            else
            {
                URL url = new URL( filename );
                is = url.openStream();
            }

            is.close();
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Extract the contents of the artifact/jar file.
     *
     * @param filename
     * @param groupId
     * @param artifactId
     * @param version
     */
    private Model unpackArtifact( String filename, String groupId, String artifactId, String version )
    {
        String basedir = "";
        Model modelObj = null;

        basedir = System.getProperty( "basedir" );
        File f = new File( basedir + "/" + "temp" );
        boolean b = f.mkdirs();

        try
        {
            JarFile jar = new JarFile( filename );

            try
            {
                //Get the entry and its input stream.
                JarEntry entry = jar.getJarEntry( "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml" );

                // If the entry is not null, extract it.
                if ( entry != null )
                {
                    InputStream entryStream = jar.getInputStream( entry );

                    try
                    {

                        //Create the output file (clobbering the file if it exists).
                        FileOutputStream file = new FileOutputStream( basedir + "/temp/pom.xml" );

                        try
                        {
                            byte[] buffer = new byte[1024];
                            int bytesRead;

                            while ( ( bytesRead = entryStream.read( buffer ) ) != -1 )
                            {
                                file.write( buffer, 0, bytesRead );
                            }

                        }
                        finally
                        {
                            file.close();
                        }
                        InputStream inputStream = new FileInputStream( basedir + "/temp/pom.xml" );
                        Reader isReader = new InputStreamReader( inputStream );

                        try
                        {
                            MavenXpp3Reader pomReader = new MavenXpp3Reader();
                            modelObj = pomReader.read( isReader );
                        }
                        finally
                        {
                            isReader.close();
                            inputStream.close();
                        }

                    }
                    finally
                    {
                        entryStream.close();
                    }
                }
                else
                {
                    return modelObj;
                }

            }
            finally
            {
                jar.close();
            }

        }
        catch ( Exception e )
        {
            return modelObj;

        }
        finally
        {
            try
            {
                FileUtils.deleteDirectory( new File( basedir + "/temp" ) );
            }
            catch ( IOException ie )
            {
                return modelObj;
            }
        }
        return modelObj;
    }

}
