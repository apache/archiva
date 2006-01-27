package org.apache.maven.repository.converter;

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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.converter.ArtifactPomRewriter;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.reporting.ArtifactReporter;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * Implementation of repository conversion class.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.repository.converter.RepositoryConverter" role-hint="default"
 */
public class DefaultRepositoryConverter
    implements RepositoryConverter
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactPomRewriter rewriter;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean force;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean dryrun;

    /**
     * @plexus.requirement
     */
    private I18N i18n;

    public void convert( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        if ( artifact.getRepository().getUrl().equals( targetRepository.getUrl() ) )
        {
            throw new RepositoryConversionException( getI18NString( "exception.repositories.match" ) );
        }

        if ( copyArtifact( artifact, targetRepository, reporter ) )
        {
            copyPom( artifact, targetRepository, reporter );

            Metadata metadata = createBaseMetadata( artifact );
            Versioning versioning = new Versioning();
            versioning.addVersion( artifact.getBaseVersion() );
            metadata.setVersioning( versioning );
            updateMetadata( new ArtifactRepositoryMetadata( artifact ), targetRepository, metadata );

            metadata = createBaseMetadata( artifact );
            metadata.setVersion( artifact.getBaseVersion() );
            versioning = new Versioning();

            Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
            if ( matcher.matches() )
            {
                Snapshot snapshot = new Snapshot();
                snapshot.setBuildNumber( Integer.valueOf( matcher.group( 3 ) ).intValue() );
                snapshot.setTimestamp( matcher.group( 2 ) );
                versioning.setSnapshot( snapshot );
            }

            // TODO: merge latest/release/snapshot from source instead
            metadata.setVersioning( versioning );
            updateMetadata( new SnapshotArtifactRepositoryMetadata( artifact ), targetRepository, metadata );

            reporter.addSuccess( artifact );
        }
    }

    private static Metadata createBaseMetadata( Artifact artifact )
    {
        Metadata metadata = new Metadata();
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setGroupId( artifact.getGroupId() );
        return metadata;
    }

    private void updateMetadata( ArtifactMetadata artifactMetadata, ArtifactRepository targetRepository,
                                 Metadata newMetadata )
        throws RepositoryConversionException
    {
        File file = new File( targetRepository.getBasedir(),
                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );

        Metadata metadata;
        boolean changed;

        if ( file.exists() )
        {
            MetadataXpp3Reader reader = new MetadataXpp3Reader();
            FileReader fileReader = null;
            try
            {
                fileReader = new FileReader( file );
                metadata = reader.read( fileReader );
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Error reading target metadata", e );
            }
            catch ( XmlPullParserException e )
            {
                throw new RepositoryConversionException( "Error reading target metadata", e );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
            changed = metadata.merge( newMetadata );
        }
        else
        {
            changed = true;
            metadata = newMetadata;
        }

        if ( changed && !dryrun )
        {
            Writer writer = null;
            try
            {
                file.getParentFile().mkdirs();
                writer = new FileWriter( file );

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, metadata );
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Error writing target metadata", e );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
    }

    private void copyPom( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        Artifact pom = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                              artifact.getVersion() );
        pom.setBaseVersion( artifact.getBaseVersion() );
        ArtifactRepository repository = artifact.getRepository();
        File file = new File( repository.getBasedir(), repository.pathOf( pom ) );

        if ( file.exists() )
        {
            // TODO: utility methods in the model converter
            File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pom ) );

            String contents = null;
            boolean checksumsValid = false;
            try
            {
                if ( testChecksums( artifact, file, reporter ) )
                {
                    checksumsValid = true;
                    contents = FileUtils.fileRead( file );
                }
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Unable to read source POM: " + e.getMessage(), e );
            }

            if ( checksumsValid && contents.indexOf( "modelVersion" ) >= 0 )
            {
                // v4 POM
                try
                {
                    boolean matching = false;
                    if ( !force && targetFile.exists() )
                    {
                        String targetContents = FileUtils.fileRead( targetFile );
                        matching = targetContents.equals( contents );
                    }
                    if ( force || !matching )
                    {
                        if ( !dryrun )
                        {
                            targetFile.getParentFile().mkdirs();
                            FileUtils.fileWrite( targetFile.getAbsolutePath(), contents );
                        }
                    }
                }
                catch ( IOException e )
                {
                    throw new RepositoryConversionException( "Unable to write target POM: " + e.getMessage(), e );
                }
            }
            else
            {
                // v3 POM
                StringReader stringReader = new StringReader( contents );
                Writer fileWriter = null;
                try
                {
                    fileWriter = new FileWriter( targetFile );

                    // TODO: this api could be improved - is it worth having or go back to modelConverter?
                    rewriter.rewrite( stringReader, fileWriter, false, artifact.getGroupId(), artifact.getArtifactId(),
                                      artifact.getVersion(), artifact.getType() );

                    List warnings = rewriter.getWarnings();

                    for ( Iterator i = warnings.iterator(); i.hasNext(); )
                    {
                        String message = (String) i.next();
                        reporter.addWarning( artifact, message );
                    }

                    IOUtil.close( fileWriter );
                }
                catch ( Exception e )
                {
                    if ( fileWriter != null )
                    {
                        IOUtil.close( fileWriter );
                        targetFile.delete();
                    }
                    throw new RepositoryConversionException( "Unable to write converted POM", e );
                }
            }
        }
        else
        {
            reporter.addWarning( artifact, getI18NString( "warning.missing.pom" ) );
        }
    }

    private String getI18NString( String key )
    {
        return i18n.getString( getClass().getName(), Locale.getDefault(), key );
    }

    private boolean testChecksums( Artifact artifact, File file, ArtifactReporter reporter )
        throws IOException, RepositoryConversionException
    {
        boolean result = true;

        try
        {
            File md5 = new File( file.getParentFile(), file.getName() + ".md5" );
            if ( md5.exists() )
            {
                String checksum = FileUtils.fileRead( md5 );
                if ( !digester.verifyChecksum( file, checksum, Digester.MD5 ) )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.incorrect.md5" ) );
                    result = false;
                }
            }

            File sha1 = new File( file.getParentFile(), file.getName() + ".sha1" );
            if ( sha1.exists() )
            {
                String checksum = FileUtils.fileRead( sha1 );
                if ( !digester.verifyChecksum( file, checksum, Digester.SHA1 ) )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.incorrect.sha1" ) );
                    result = false;
                }
            }
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryConversionException( "Error copying artifact: " + e.getMessage(), e );
        }
        return result;
    }

    private boolean copyArtifact( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        File sourceFile = artifact.getFile();

        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );

        boolean result = true;
        try
        {
            boolean matching = false;
            if ( !force && targetFile.exists() )
            {
                matching = FileUtils.contentEquals( sourceFile, targetFile );
                if ( !matching )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.target.already.exists" ) );
                    result = false;
                }
            }
            if ( result )
            {
                if ( force || !matching )
                {
                    if ( testChecksums( artifact, sourceFile, reporter ) )
                    {
                        if ( !dryrun )
                        {
                            FileUtils.copyFile( sourceFile, targetFile );
                        }
                    }
                    else
                    {
                        result = false;
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryConversionException( "Error copying artifact", e );
        }
        return result;
    }

    public void convert( List artifacts, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            convert( artifact, targetRepository, reporter );
        }
    }
}
