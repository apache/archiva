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
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        new HashSet( Arrays.asList( new String[]{"jar", "zip", "ejb", "par", "sar", "war", "ear"} ) );

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    public RepositoryIndexRecord createRecord( Artifact artifact )
        throws RepositoryIndexException
    {
        StandardArtifactIndexRecord record = null;

        File file = artifact.getFile();
        // TODO: is this condition really a possibility?
        if ( file != null && file.exists() )
        {
            String md5 = readChecksum( file, Digester.MD5 );
            String sha1 = readChecksum( file, Digester.SHA1 );

            List files = null;
            try
            {
                files = readFilesInArchive( file );
            }
            catch ( IOException e )
            {
                getLogger().error( "Error reading artifact file, omitting from index: " + e.getMessage() );
            }

            // If it's an archive with no files, don't create a record
            if ( !ARCHIVE_TYPES.contains( artifact.getType() ) || files != null )
            {
                record = new StandardArtifactIndexRecord();

                record.setGroupId( artifact.getGroupId() );
                record.setArtifactId( artifact.getArtifactId() );
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
                    populateArchiveEntries( files, record );
                }

                if ( !"pom".equals( artifact.getType() ) )
                {
                    Artifact pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                                  artifact.getArtifactId(),
                                                                                  artifact.getVersion() );
                    File pomFile = new File( artifact.getRepository().getBasedir(),
                                             artifact.getRepository().pathOf( pomArtifact ) );
                    if ( pomFile.exists() )
                    {
                        populatePomEntries( readPom( pomFile ), record );
                    }
                }
                else
                {
                    populatePomEntries( readPom( file ), record );
                }

                if ( "maven-plugin".equals( record.getPackaging() ) )
                {
                    // Typically discovered as a JAR
                    record.setType( record.getPackaging() );

                    RepositoryMetadata metadata = new GroupRepositoryMetadata( artifact.getGroupId() );
                    File metadataFile = new File( artifact.getRepository().getBasedir(),
                                                  artifact.getRepository().pathOfRemoteRepositoryMetadata( metadata ) );
                    if ( metadataFile.exists() )
                    {
                        populatePluginEntries( readMetadata( metadataFile ), record );
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

    private Model readPom( File file )
        throws RepositoryIndexException
    {
        MavenXpp3Reader r = new MavenXpp3Reader();

        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return r.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexException( "Unable to find requested POM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Unable to read POM: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexException( "Unable to parse POM: " + xe.getMessage(), xe );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private Metadata readMetadata( File file )
        throws RepositoryIndexException
    {
        MetadataXpp3Reader r = new MetadataXpp3Reader();

        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return r.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexException( "Unable to find requested metadata: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Unable to read metadata: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexException( "Unable to parse metadata: " + xe.getMessage(), xe );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private void populateArchiveEntries( List files, StandardArtifactIndexRecord record )
    {
        StringBuffer classes = new StringBuffer();
        StringBuffer fileBuffer = new StringBuffer();

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            // ignore directories
            if ( !name.endsWith( "/" ) )
            {
                fileBuffer.append( name ).append( "\n" );

                if ( isClass( name ) )
                {
                    classes.append( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) ).append( "\n" );
                }
            }
        }

        record.setClasses( classes.toString() );
        record.setFiles( fileBuffer.toString() );
    }

    public void populatePluginEntries( Metadata metadata, StandardArtifactIndexRecord record )
    {
        Map prefixes = new HashMap();
        for ( Iterator i = metadata.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            prefixes.put( plugin.getArtifactId(), plugin.getPrefix() );
        }

        if ( record.getGroupId().equals( metadata.getGroupId() ) )
        {
            String prefix = (String) prefixes.get( record.getArtifactId() );
            if ( prefix != null )
            {
                record.setPluginPrefix( prefix );
            }
        }
    }
}
