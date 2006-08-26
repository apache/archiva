package org.apache.maven.repository.indexing.record;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * An index record type for the standard index.
 *
 * @author Edwin Punzalan
 * @author Brett Porter
 * @plexus.component role="org.apache.maven.repository.indexing.record.RepositoryIndexRecordFactory" role-hint="standard"
 */
public class StandardArtifactIndexRecordFactory
    extends AbstractArtifactIndexRecordFactory
{
    /**
     * A list of artifact types to treat as a zip archive.
     *
     * @todo this should be smarter (perhaps use plexus archiver to look for an unarchiver, and make the ones for zip configurable since sar, par, etc can be added at random.
     */
    private static final Set ARCHIVE_TYPES =
        new HashSet( Arrays.asList( new String[]{"jar", "ejb", "par", "sar", "war", "ear", "rar"} ) );

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement role-hint="sha1"
     */
    protected Digester sha1Digester;

    /**
     * @plexus.requirement role-hint="md5"
     */
    protected Digester md5Digester;

    private static final String PLUGIN_METADATA_NAME = "META-INF/maven/plugin.xml";

    private static final String ARCHETYPE_METADATA_NAME = "META-INF/maven/archetype.xml";

    // some current/old archetypes have the archetype.xml at different location.
    private static final String ARCHETYPE_METADATA_NAME_OLD = "META-INF/archetype.xml";

    public RepositoryIndexRecord createRecord( Artifact artifact )
        throws RepositoryIndexException
    {
        StandardArtifactIndexRecord record = null;

        File file = artifact.getFile();
        
        // TODO: is this condition really a possibility?
        if ( file != null && file.exists() )
        {
            String md5 = readChecksum( file, md5Digester );
            String sha1 = readChecksum( file, sha1Digester );
            
            List files = null;
            boolean archive = ARCHIVE_TYPES.contains( artifact.getType() );
            try
            {
                if ( archive )
                {
                    files = readFilesInArchive( file );
                }
            }
            catch ( IOException e )
            {
                getLogger().error( "Error reading artifact file, omitting from index: " + e.getMessage() );
            }

            // If it's an archive with no files, don't create a record
            if ( !archive || files != null )
            {
                record = new StandardArtifactIndexRecord();

                record.setGroupId( artifact.getGroupId() );
                record.setArtifactId( artifact.getArtifactId() );
                record.setBaseVersion( artifact.getBaseVersion() );
                record.setVersion( artifact.getVersion() );
                record.setClassifier( artifact.getClassifier() );
                record.setType( artifact.getType() );
                record.setMd5Checksum( md5 );
                record.setSha1Checksum( sha1 );
                record.setFilename( artifact.getRepository().pathOf( artifact ) );
                record.setLastModified( file.lastModified() );
                record.setSize( file.length() );
                record.setRepository( artifact.getRepository().getId() );
                if ( files != null )
                {
                    populateArchiveEntries( files, record, artifact.getFile() );
                }

                if ( !"pom".equals( artifact.getType() ) )
                {
                    Artifact pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                                  artifact.getArtifactId(),
                                                                                  artifact.getVersion() );
                    pomArtifact.isSnapshot(); // gross hack around bug in maven-artifact
                    File pomFile = new File( artifact.getRepository().getBasedir(),
                                             artifact.getRepository().pathOf( pomArtifact ) );
                    if ( pomFile.exists() )
                    {
                        try
                        {
                            populatePomEntries( readPom( pomArtifact, artifact.getRepository() ), record );
                        }
                        catch ( ProjectBuildingException e )
                        {
                            getLogger().error( "Error reading POM file, not populating in index: " + e.getMessage() );
                        }
                    }
                }
                else
                {
                    Model model;
                    try
                    {
                        model = readPom( artifact, artifact.getRepository() );

                        if ( !"pom".equals( model.getPackaging() ) )
                        {
                            // Don't return a record for a POM that is does not belong on its own
                            record = null;
                        }
                        else
                        {
                            populatePomEntries( model, record );
                        }
                    }
                    catch ( ProjectBuildingException e )
                    {
                        getLogger().error( "Error reading POM file, not populating in index: " + e.getMessage() );
                    }
                }
            }
        }

        return record;
    }

    private void populatePomEntries( Model pom, StandardArtifactIndexRecord record )
    {
        record.setPackaging( pom.getPackaging() );
        record.setProjectName( pom.getName() );
        record.setProjectDescription( pom.getDescription() );
        record.setInceptionYear( pom.getInceptionYear() );

/* TODO: fields for later
                indexPlugins( doc, FLD_PLUGINS_BUILD, pom.getBuild().getPlugins().iterator() );
                indexReportPlugins( doc, FLD_PLUGINS_REPORT, pom.getReporting().getPlugins().iterator() );
                record.setDependencies( dependencies );
                record.setLicenses( licenses );
*/
    }

    private Model readPom( Artifact artifact, ArtifactRepository repository )
        throws RepositoryIndexException, ProjectBuildingException
    {
        // TODO: this can create a -SNAPSHOT.pom when it didn't exist and a timestamped one did. This is harmless, but should be avoided
        // TODO: will this pollute with local repo metadata?
        MavenProject project = projectBuilder.buildFromRepository( artifact, Collections.EMPTY_LIST, repository );
        return project.getModel();
    }

    private void populateArchiveEntries( List files, StandardArtifactIndexRecord record, File artifactFile )
        throws RepositoryIndexException
    {
        List classes = new ArrayList();
        List fileList = new ArrayList();

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            // ignore directories
            if ( !name.endsWith( "/" ) )
            {
                fileList.add( name );

                if ( isClass( name ) )
                {
                    classes.add( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) );
                }
                else if ( PLUGIN_METADATA_NAME.equals( name ) )
                {
                    populatePluginEntries( readXmlMetadataFileInJar( artifactFile, PLUGIN_METADATA_NAME ), record );
                }
                else if ( ARCHETYPE_METADATA_NAME.equals( name ) || ARCHETYPE_METADATA_NAME_OLD.equals( name ) )
                {
                    populateArchetypeEntries( record );
                }
            }
        }

        if ( !classes.isEmpty() )
        {
            record.setClasses( classes );
        }
        if ( !fileList.isEmpty() )
        {
            record.setFiles( fileList );
        }
    }

    private void populateArchetypeEntries( StandardArtifactIndexRecord record )
    {
        // Typically discovered as a JAR
        record.setType( "maven-archetype" );
    }

    private Xpp3Dom readXmlMetadataFileInJar( File file, String name )
        throws RepositoryIndexException
    {
        // TODO: would be more efficient with original ZipEntry still around

        Xpp3Dom xpp3Dom;
        ZipFile zipFile = null;
        try
        {
            zipFile = new ZipFile( file );
            ZipEntry entry = zipFile.getEntry( name );
            xpp3Dom = Xpp3DomBuilder.build( new InputStreamReader( zipFile.getInputStream( entry ) ) );
        }
        catch ( ZipException e )
        {
            throw new RepositoryIndexException( "Unable to read plugin metadata: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Unable to read plugin metadata: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryIndexException( "Unable to read plugin metadata: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( zipFile );
        }
        return xpp3Dom;
    }

    public void populatePluginEntries( Xpp3Dom metadata, StandardArtifactIndexRecord record )
    {
        // Typically discovered as a JAR
        record.setType( "maven-plugin" );

        Xpp3Dom prefix = metadata.getChild( "goalPrefix" );

        if ( prefix != null )
        {
            record.setPluginPrefix( prefix.getValue() );
        }
    }
}
