package org.apache.maven.archiva.reporting.processor;

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
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validate the location of the artifact based on the values indicated
 * in its pom (both the pom packaged with the artifact & the pom in the
 * file system).
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor" role-hint="artifact-location"
 */
public class LocationArtifactReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    // TODO: share with other code with the same
    private static final Set JAR_FILE_TYPES =
        new HashSet( Arrays.asList( new String[]{"jar", "war", "par", "ejb", "ear", "rar", "sar"} ) );

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    private static final String POM = "pom";

    private static final String ROLE_HINT = "artifact-location";

    /**
     * Check whether the artifact is in its proper location. The location of the artifact
     * is validated first against the groupId, artifactId and versionId in the specified model
     * object (pom in the file system). Then unpack the artifact (jar file) and get the model (pom)
     * included in the package. If a model exists inside the package, then check if the artifact's
     * location is valid based on the location specified in the pom. Check if the both the location
     * specified in the file system pom and in the pom included in the package is the same.
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

        adjustDistributionArtifactHandler( artifact );

        String artifactPath = repository.pathOf( artifact );

        if ( model != null )
        {
            // only check if it is a standalone POM, or an artifact other than a POM
            // ie, don't check the location of the POM for another artifact matches that of the artifact
            if ( !POM.equals( artifact.getType() ) || POM.equals( model.getPackaging() ) )
            {
                //check if the artifact is located in its proper location based on the info
                //specified in the model object/pom
                Artifact modelArtifact = artifactFactory.createArtifactWithClassifier( model.getGroupId(),
                                                                                       model.getArtifactId(),
                                                                                       model.getVersion(),
                                                                                       artifact.getType(),
                                                                                       artifact.getClassifier() );

                adjustDistributionArtifactHandler( modelArtifact );
                String modelPath = repository.pathOf( modelArtifact );
                if ( !modelPath.equals( artifactPath ) )
                {
                    addFailure( reporter, artifact, "repository-pom-location",
                                "The artifact is out of place. It does not match the specified location in the repository pom: " +
                                    modelPath );
                }
            }
        }

        // get the location of the artifact itself
        File file = new File( repository.getBasedir(), artifactPath );

        if ( file.exists() )
        {
            if ( JAR_FILE_TYPES.contains( artifact.getType() ) )
            {
                //unpack the artifact (using the groupId, artifactId & version specified in the artifact object itself
                //check if the pom is included in the package
                Model extractedModel = readArtifactModel( file, artifact, reporter );

                if ( extractedModel != null )
                {
                    Artifact extractedArtifact = artifactFactory.createBuildArtifact( extractedModel.getGroupId(),
                                                                                      extractedModel.getArtifactId(),
                                                                                      extractedModel.getVersion(),
                                                                                      extractedModel.getPackaging() );
                    if ( !repository.pathOf( extractedArtifact ).equals( artifactPath ) )
                    {
                        addFailure( reporter, artifact, "packaged-pom-location",
                                    "The artifact is out of place. It does not match the specified location in the packaged pom." );
                    }
                }
            }
        }
        else
        {
            addFailure( reporter, artifact, "missing-artifact", "The artifact file [" + file + "] cannot be found for metadata." );
        }
    }

    private static void addFailure( ReportingDatabase reporter, Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        reporter.addFailure( artifact, ROLE_HINT, problem, reason );
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

    private Model readArtifactModel( File file, Artifact artifact, ReportingDatabase reporter )
    {
        Model model = null;

        JarFile jar = null;
        try
        {
            jar = new JarFile( file );

            //Get the entry and its input stream.
            JarEntry entry = jar.getJarEntry(
                "META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.xml" );

            // If the entry is not null, extract it.
            if ( entry != null )
            {
                model = readModel( jar.getInputStream( entry ) );

                if ( model.getGroupId() == null )
                {
                    model.setGroupId( model.getParent().getGroupId() );
                }
                if ( model.getVersion() == null )
                {
                    model.setVersion( model.getParent().getVersion() );
                }
            }
        }
        catch ( IOException e )
        {
            addWarning( reporter, artifact, "Unable to read artifact to extract model: " + e );
        }
        catch ( XmlPullParserException e )
        {
            addWarning( reporter, artifact, "Unable to parse extracted model: " + e );
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

    private static void addWarning( ReportingDatabase reporter, Artifact artifact, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        reporter.addWarning( artifact, ROLE_HINT, null, reason );
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
