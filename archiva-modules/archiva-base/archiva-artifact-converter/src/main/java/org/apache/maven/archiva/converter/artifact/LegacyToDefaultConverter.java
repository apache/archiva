package org.apache.maven.archiva.converter.artifact;

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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiva.transaction.FileTransaction;
import org.apache.maven.archiva.transaction.TransactionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.converter.ModelConverter;
import org.apache.maven.model.converter.PomTranslationException;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * LegacyToDefaultConverter
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.converter.artifact.ArtifactConverter"
 * role-hint="legacy-to-default"
 */
@Service( "artifactConverter#legacy-to-default" )
public class LegacyToDefaultConverter
    implements ArtifactConverter
{
    /**
     * {@link List}&lt;{@link Digester}
     * plexus.requirement role="org.codehaus.plexus.digest.Digester"
     */
    private List<Digester> digesters;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    /**
     * plexus.requirement
     */
    private ModelConverter translator;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * plexus.configuration default-value="false"
     */
    private boolean force;

    /**
     * plexus.configuration default-value="false"
     */
    private boolean dryrun;

    private Map<Artifact, List<String>> warnings = new HashMap<Artifact, List<String>>();

    @PostConstruct
    public void initialize()
        throws ComponentLookupException
    {
        this.digesters = plexusSisuBridge.lookupList( Digester.class );
        translator = plexusSisuBridge.lookup( ModelConverter.class );
        artifactFactory = plexusSisuBridge.lookup( ArtifactFactory.class );
        artifactHandlerManager = plexusSisuBridge.lookup( ArtifactHandlerManager.class );
    }

    public void convert( Artifact artifact, ArtifactRepository targetRepository )
        throws ArtifactConversionException
    {
        if ( artifact.getRepository().getUrl().equals( targetRepository.getUrl() ) )
        {
            throw new ArtifactConversionException( Messages.getString( "exception.repositories.match" ) ); //$NON-NLS-1$
        }

        if ( !validateMetadata( artifact ) )
        {
            addWarning( artifact, Messages.getString( "unable.to.validate.metadata" ) ); //$NON-NLS-1$
            return;
        }

        FileTransaction transaction = new FileTransaction();

        if ( !copyPom( artifact, targetRepository, transaction ) )
        {
            addWarning( artifact, Messages.getString( "unable.to.copy.pom" ) ); //$NON-NLS-1$
            return;
        }

        if ( !copyArtifact( artifact, targetRepository, transaction ) )
        {
            addWarning( artifact, Messages.getString( "unable.to.copy.artifact" ) ); //$NON-NLS-1$
            return;
        }

        Metadata metadata = createBaseMetadata( artifact );
        Versioning versioning = new Versioning();
        versioning.addVersion( artifact.getBaseVersion() );
        metadata.setVersioning( versioning );
        updateMetadata( new ArtifactRepositoryMetadata( artifact ), targetRepository, metadata, transaction );

        metadata = createBaseMetadata( artifact );
        metadata.setVersion( artifact.getBaseVersion() );
        versioning = new Versioning();

        Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
        if ( matcher.matches() )
        {
            Snapshot snapshot = new Snapshot();
            snapshot.setBuildNumber( Integer.parseInt( matcher.group( 3 ) ) );
            snapshot.setTimestamp( matcher.group( 2 ) );
            versioning.setSnapshot( snapshot );
        }

        // TODO: merge latest/release/snapshot from source instead
        metadata.setVersioning( versioning );
        updateMetadata( new SnapshotArtifactRepositoryMetadata( artifact ), targetRepository, metadata, transaction );

        if ( !dryrun )
        {
            try
            {
                transaction.commit();
            }
            catch ( TransactionException e )
            {
                throw new ArtifactConversionException( Messages.getString( "transaction.failure", e.getMessage() ),
                                                       e ); //$NON-NLS-1$
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private boolean copyPom( Artifact artifact, ArtifactRepository targetRepository, FileTransaction transaction )
        throws ArtifactConversionException
    {
        Artifact pom = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                              artifact.getVersion() );
        pom.setBaseVersion( artifact.getBaseVersion() );
        ArtifactRepository repository = artifact.getRepository();
        File file = new File( repository.getBasedir(), repository.pathOf( pom ) );

        boolean result = true;
        if ( file.exists() )
        {
            File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pom ) );

            String contents = null;
            boolean checksumsValid = false;
            try
            {
                if ( testChecksums( artifact, file ) )
                {
                    checksumsValid = true;
                }

                // Even if the checksums for the POM are invalid we should still convert the POM
                contents = FileUtils.readFileToString( file, null );
            }
            catch ( IOException e )
            {
                throw new ArtifactConversionException(
                    Messages.getString( "unable.to.read.source.pom", e.getMessage() ), e ); //$NON-NLS-1$
            }

            if ( checksumsValid && contents.indexOf( "modelVersion" ) >= 0 ) //$NON-NLS-1$
            {
                // v4 POM
                try
                {
                    boolean matching = false;
                    if ( !force && targetFile.exists() )
                    {
                        String targetContents = FileUtils.readFileToString( targetFile, null );
                        matching = targetContents.equals( contents );
                    }
                    if ( force || !matching )
                    {
                        transaction.createFile( contents, targetFile, digesters );
                    }
                }
                catch ( IOException e )
                {
                    throw new ArtifactConversionException(
                        Messages.getString( "unable.to.write.target.pom", e.getMessage() ), e ); //$NON-NLS-1$
                }
            }
            else
            {
                // v3 POM
                StringReader stringReader = new StringReader( contents );
                StringWriter writer = null;
                try
                {
                    org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader v3Reader =
                        new org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader();
                    org.apache.maven.model.v3_0_0.Model v3Model = v3Reader.read( stringReader );

                    if ( doRelocation( artifact, v3Model, targetRepository, transaction ) )
                    {
                        Artifact relocatedPom =
                            artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                   artifact.getVersion() );
                        targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( relocatedPom ) );
                    }

                    Model v4Model = translator.translate( v3Model );

                    translator.validateV4Basics( v4Model, v3Model.getGroupId(), v3Model.getArtifactId(),
                                                 v3Model.getVersion(), v3Model.getPackage() );

                    writer = new StringWriter();
                    MavenXpp3Writer Xpp3Writer = new MavenXpp3Writer();
                    Xpp3Writer.write( writer, v4Model );

                    transaction.createFile( writer.toString(), targetFile, digesters );

                    List<String> warnings = translator.getWarnings();

                    for ( String message : warnings )
                    {
                        addWarning( artifact, message );
                    }
                }
                catch ( XmlPullParserException e )
                {
                    addWarning( artifact, Messages.getString( "invalid.source.pom", e.getMessage() ) ); //$NON-NLS-1$
                    result = false;
                }
                catch ( IOException e )
                {
                    throw new ArtifactConversionException( Messages.getString( "unable.to.write.converted.pom" ),
                                                           e ); //$NON-NLS-1$
                }
                catch ( PomTranslationException e )
                {
                    addWarning( artifact, Messages.getString( "invalid.source.pom", e.getMessage() ) ); //$NON-NLS-1$
                    result = false;
                }
                finally
                {
                    IOUtils.closeQuietly( writer );
                }
            }
        }
        else
        {
            addWarning( artifact, Messages.getString( "warning.missing.pom" ) ); //$NON-NLS-1$
        }
        return result;
    }

    private boolean testChecksums( Artifact artifact, File file )
        throws IOException
    {
        boolean result = true;
        for ( Digester digester : digesters )
        {
            result &= verifyChecksum( file, file.getName() + "." + getDigesterFileExtension( digester ), digester,
                                      //$NON-NLS-1$
                                      artifact,
                                      "failure.incorrect." + getDigesterFileExtension( digester ) ); //$NON-NLS-1$
        }
        return result;
    }

    private boolean verifyChecksum( File file, String fileName, Digester digester, Artifact artifact, String key )
        throws IOException
    {
        boolean result = true;

        File checksumFile = new File( file.getParentFile(), fileName );
        if ( checksumFile.exists() )
        {
            String checksum = FileUtils.readFileToString( checksumFile, null );
            try
            {
                digester.verify( file, checksum );
            }
            catch ( DigesterException e )
            {
                addWarning( artifact, Messages.getString( key ) );
                result = false;
            }
        }
        return result;
    }

    /**
     * File extension for checksums
     * TODO should be moved to plexus-digester ?
     */
    private String getDigesterFileExtension( Digester digester )
    {
        return digester.getAlgorithm().toLowerCase().replaceAll( "-", "" ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private boolean copyArtifact( Artifact artifact, ArtifactRepository targetRepository, FileTransaction transaction )
        throws ArtifactConversionException
    {
        File sourceFile = artifact.getFile();

        if ( sourceFile.getAbsolutePath().indexOf( "/plugins/" ) > -1 ) //$NON-NLS-1$
        {
            artifact.setArtifactHandler( artifactHandlerManager.getArtifactHandler( "maven-plugin" ) ); //$NON-NLS-1$
        }

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
                    addWarning( artifact, Messages.getString( "failure.target.already.exists" ) ); //$NON-NLS-1$
                    result = false;
                }
            }
            if ( result )
            {
                if ( force || !matching )
                {
                    if ( testChecksums( artifact, sourceFile ) )
                    {
                        transaction.copyFile( sourceFile, targetFile, digesters );
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
            throw new ArtifactConversionException( Messages.getString( "error.copying.artifact" ), e ); //$NON-NLS-1$
        }
        return result;
    }

    private Metadata createBaseMetadata( Artifact artifact )
    {
        Metadata metadata = new Metadata();
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setGroupId( artifact.getGroupId() );
        return metadata;
    }

    private Metadata readMetadata( File file )
        throws ArtifactConversionException
    {
        Metadata metadata;
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( file );
            metadata = reader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new ArtifactConversionException( Messages.getString( "error.reading.target.metadata" ),
                                                   e ); //$NON-NLS-1$
        }
        catch ( IOException e )
        {
            throw new ArtifactConversionException( Messages.getString( "error.reading.target.metadata" ),
                                                   e ); //$NON-NLS-1$
        }
        catch ( XmlPullParserException e )
        {
            throw new ArtifactConversionException( Messages.getString( "error.reading.target.metadata" ),
                                                   e ); //$NON-NLS-1$
        }
        finally
        {
            IOUtils.closeQuietly( fileReader );
        }
        return metadata;
    }

    private boolean validateMetadata( Artifact artifact )
        throws ArtifactConversionException
    {
        ArtifactRepository repository = artifact.getRepository();

        boolean result = true;

        RepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata( artifact );
        File file =
            new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = validateMetadata( metadata, repositoryMetadata, artifact );
        }

        repositoryMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = result && validateMetadata( metadata, repositoryMetadata, artifact );
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private boolean validateMetadata( Metadata metadata, RepositoryMetadata repositoryMetadata, Artifact artifact )
    {
        String groupIdKey;
        String artifactIdKey = null;
        String snapshotKey = null;
        String versionKey = null;
        String versionsKey = null;

        if ( repositoryMetadata.storedInGroupDirectory() )
        {
            groupIdKey = "failure.incorrect.groupMetadata.groupId"; //$NON-NLS-1$
        }
        else if ( repositoryMetadata.storedInArtifactVersionDirectory() )
        {
            groupIdKey = "failure.incorrect.snapshotMetadata.groupId"; //$NON-NLS-1$
            artifactIdKey = "failure.incorrect.snapshotMetadata.artifactId"; //$NON-NLS-1$
            versionKey = "failure.incorrect.snapshotMetadata.version"; //$NON-NLS-1$
            snapshotKey = "failure.incorrect.snapshotMetadata.snapshot"; //$NON-NLS-1$
        }
        else
        {
            groupIdKey = "failure.incorrect.artifactMetadata.groupId"; //$NON-NLS-1$
            artifactIdKey = "failure.incorrect.artifactMetadata.artifactId"; //$NON-NLS-1$
            versionsKey = "failure.incorrect.artifactMetadata.versions"; //$NON-NLS-1$
        }

        boolean result = true;

        if ( metadata.getGroupId() == null || !metadata.getGroupId().equals( artifact.getGroupId() ) )
        {
            addWarning( artifact, Messages.getString( groupIdKey ) );
            result = false;
        }
        if ( !repositoryMetadata.storedInGroupDirectory() )
        {
            if ( metadata.getGroupId() == null || !metadata.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                addWarning( artifact, Messages.getString( artifactIdKey ) );
                result = false;
            }
            if ( !repositoryMetadata.storedInArtifactVersionDirectory() )
            {
                // artifact metadata

                boolean foundVersion = false;
                if ( metadata.getVersioning() != null )
                {
                    for ( String version : (List<String>) metadata.getVersioning().getVersions() )
                    {
                        if ( version.equals( artifact.getBaseVersion() ) )
                        {
                            foundVersion = true;
                            break;
                        }
                    }
                }

                if ( !foundVersion )
                {
                    addWarning( artifact, Messages.getString( versionsKey ) );
                    result = false;
                }
            }
            else
            {
                // snapshot metadata
                if ( !artifact.getBaseVersion().equals( metadata.getVersion() ) )
                {
                    addWarning( artifact, Messages.getString( versionKey ) );
                    result = false;
                }

                if ( artifact.isSnapshot() )
                {
                    Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                    if ( matcher.matches() )
                    {
                        boolean correct = false;
                        if ( metadata.getVersioning() != null && metadata.getVersioning().getSnapshot() != null )
                        {
                            Snapshot snapshot = metadata.getVersioning().getSnapshot();
                            int build = Integer.parseInt( matcher.group( 3 ) );
                            String ts = matcher.group( 2 );
                            if ( build == snapshot.getBuildNumber() && ts.equals( snapshot.getTimestamp() ) )
                            {
                                correct = true;
                            }
                        }

                        if ( !correct )
                        {
                            addWarning( artifact, Messages.getString( snapshotKey ) );
                            result = false;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void updateMetadata( RepositoryMetadata artifactMetadata, ArtifactRepository targetRepository,
                                 Metadata newMetadata, FileTransaction transaction )
        throws ArtifactConversionException
    {
        File file = new File( targetRepository.getBasedir(),
                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );

        Metadata metadata;
        boolean changed;

        if ( file.exists() )
        {
            metadata = readMetadata( file );
            changed = metadata.merge( newMetadata );
        }
        else
        {
            changed = true;
            metadata = newMetadata;
        }

        if ( changed )
        {
            StringWriter writer = null;
            try
            {
                writer = new StringWriter();

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, metadata );

                transaction.createFile( writer.toString(), file, digesters );
            }
            catch ( IOException e )
            {
                throw new ArtifactConversionException( Messages.getString( "error.writing.target.metadata" ),
                                                       e ); //$NON-NLS-1$
            }
            finally
            {
                IOUtils.closeQuietly( writer );
            }
        }
    }

    private boolean doRelocation( Artifact artifact, org.apache.maven.model.v3_0_0.Model v3Model,
                                  ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Properties properties = v3Model.getProperties();
        if ( properties.containsKey( "relocated.groupId" ) || properties.containsKey( "relocated.artifactId" )
            //$NON-NLS-1$ //$NON-NLS-2$
            || properties.containsKey( "relocated.version" ) ) //$NON-NLS-1$
        {
            String newGroupId = properties.getProperty( "relocated.groupId", v3Model.getGroupId() ); //$NON-NLS-1$
            properties.remove( "relocated.groupId" ); //$NON-NLS-1$

            String newArtifactId =
                properties.getProperty( "relocated.artifactId", v3Model.getArtifactId() ); //$NON-NLS-1$
            properties.remove( "relocated.artifactId" ); //$NON-NLS-1$

            String newVersion = properties.getProperty( "relocated.version", v3Model.getVersion() ); //$NON-NLS-1$
            properties.remove( "relocated.version" ); //$NON-NLS-1$

            String message = properties.getProperty( "relocated.message", "" ); //$NON-NLS-1$ //$NON-NLS-2$
            properties.remove( "relocated.message" ); //$NON-NLS-1$

            if ( properties.isEmpty() )
            {
                v3Model.setProperties( null );
            }

            writeRelocationPom( v3Model.getGroupId(), v3Model.getArtifactId(), v3Model.getVersion(), newGroupId,
                                newArtifactId, newVersion, message, repository, transaction );

            v3Model.setGroupId( newGroupId );
            v3Model.setArtifactId( newArtifactId );
            v3Model.setVersion( newVersion );

            artifact.setGroupId( newGroupId );
            artifact.setArtifactId( newArtifactId );
            artifact.setVersion( newVersion );

            return true;
        }
        else
        {
            return false;
        }
    }

    private void writeRelocationPom( String groupId, String artifactId, String version, String newGroupId,
                                     String newArtifactId, String newVersion, String message,
                                     ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Model pom = new Model();
        pom.setGroupId( groupId );
        pom.setArtifactId( artifactId );
        pom.setVersion( version );

        DistributionManagement dMngt = new DistributionManagement();

        Relocation relocation = new Relocation();
        relocation.setGroupId( newGroupId );
        relocation.setArtifactId( newArtifactId );
        relocation.setVersion( newVersion );
        if ( message != null && message.length() > 0 )
        {
            relocation.setMessage( message );
        }

        dMngt.setRelocation( relocation );

        pom.setDistributionManagement( dMngt );

        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, "pom" ); //$NON-NLS-1$
        File pomFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );

        StringWriter strWriter = new StringWriter();
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write( strWriter, pom );

        transaction.createFile( strWriter.toString(), pomFile, digesters );
    }

    private void addWarning( Artifact artifact, String message )
    {
        List<String> messages = warnings.get( artifact );
        if ( messages == null )
        {
            messages = new ArrayList<String>();
        }
        messages.add( message );
        warnings.put( artifact, messages );
    }

    public void clearWarnings()
    {
        warnings.clear();
    }

    public Map<Artifact, List<String>> getWarnings()
    {
        return warnings;
    }


    public List<Digester> getDigesters()
    {
        return digesters;
    }

    public void setDigesters( List<Digester> digesters )
    {
        this.digesters = digesters;
    }

    public ModelConverter getTranslator()
    {
        return translator;
    }

    public void setTranslator( ModelConverter translator )
    {
        this.translator = translator;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactHandlerManager getArtifactHandlerManager()
    {
        return artifactHandlerManager;
    }

    public void setArtifactHandlerManager( ArtifactHandlerManager artifactHandlerManager )
    {
        this.artifactHandlerManager = artifactHandlerManager;
    }

    public boolean isForce()
    {
        return force;
    }

    public void setForce( boolean force )
    {
        this.force = force;
    }

    public boolean isDryrun()
    {
        return dryrun;
    }

    public void setDryrun( boolean dryrun )
    {
        this.dryrun = dryrun;
    }
}
