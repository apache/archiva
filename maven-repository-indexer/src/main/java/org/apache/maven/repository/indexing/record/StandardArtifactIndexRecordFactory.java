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
import org.apache.maven.repository.digest.Digester;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    public RepositoryIndexRecord createRecord( Artifact artifact )
    {
        StandardArtifactIndexRecord record = null;

        File file = artifact.getFile();
        if ( file != null && file.exists() )
        {
            String md5 = readChecksum( file, Digester.MD5 );
            String sha1 = readChecksum( file, Digester.SHA1 );

            List files = null;
            try
            {
                if ( ARCHIVE_TYPES.contains( artifact.getType() ) )
                {
                    files = readFilesInArchive( file );
                }
                else
                {
                    files = Collections.EMPTY_LIST;
                }
            }
            catch ( IOException e )
            {
                getLogger().error( "Error reading artifact file, omitting from index: " + e.getMessage() );
            }

            if ( files != null )
            {
                record = new StandardArtifactIndexRecord();

                record.setGroupId( artifact.getGroupId() );
                record.setArtifactId( artifact.getArtifactId() );
                record.setVersion( artifact.getVersion() );
                record.setClassifier( artifact.getClassifier() );
                record.setType( artifact.getType() );
                record.setMd5Checksum( md5 );
                record.setSha1Checksum( sha1 );
                record.setFilename( file.getName() );
                record.setLastModified( file.lastModified() );
                record.setSize( file.length() );
                record.setRepository( artifact.getRepository().getId() );
/* TODO! these come from the POM and metadata, so probably part of an update record method instead
// remember to test parent & inheritence
                record.setPluginPrefix( pluginPrefix );
                record.setPackaging( packaging );
                record.setProjectName( name );
                record.setProjectDescription( description );
                record.setInceptionYear( year );
                */
/* TODO: fields for later
                indexPlugins( doc, FLD_PLUGINS_BUILD, pom.getBuild().getPlugins().iterator() );
                indexReportPlugins( doc, FLD_PLUGINS_REPORT, pom.getReporting().getPlugins().iterator() );
                record.setDependencies( dependencies );
                record.setLicenses( licenses );
*/
                populateArchiveEntries( files, record );
            }
        }

        return record;
    }

    private void populateArchiveEntries( List files, StandardArtifactIndexRecord record )
    {
        StringBuffer classes = new StringBuffer();
        StringBuffer packages = new StringBuffer();
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
                    int idx = name.lastIndexOf( '/' );
                    String classname = name.substring( idx + 1, name.length() - 6 );
                    classes.append( classname ).append( "\n" );

                    if ( idx > 0 )
                    {
                        String packageName = name.substring( 0, idx ).replace( '/', '.' );
                        if ( packages.indexOf( packageName ) < 0 )
                        {
                            packages.append( packageName ).append( "\n" );
                        }
                    }
                }
            }
        }

        record.setClasses( classes.toString() );
        record.setPackages( packages.toString() );
        record.setFiles( fileBuffer.toString() );
    }
}
