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

import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Base class for the index record factories.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractArtifactIndexRecordFactory
    extends AbstractLogEnabled
    implements RepositoryIndexRecordFactory
{
    protected String readChecksum( File file, Digester digester )
    {
        String checksum;
        try
        {
            checksum = digester.calc( file ).toLowerCase();
        }
        catch ( DigesterException e )
        {
            getLogger().error( "Error getting checksum for artifact file, leaving empty in index: " + e.getMessage() );
            checksum = null;
        }
        return checksum;
    }

    protected List readFilesInArchive( File file )
        throws IOException
    {
        ZipFile zipFile = new ZipFile( file );
        List files;
        try
        {
            files = new ArrayList( zipFile.size() );

            for ( Enumeration entries = zipFile.entries(); entries.hasMoreElements(); )
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                files.add( entry.getName() );
            }
        }
        finally
        {
            closeQuietly( zipFile );
        }
        return files;
    }

    protected static boolean isClass( String name )
    {
        // TODO: verify if class is public or protected (this might require the original ZipEntry)
        return name.endsWith( ".class" ) && name.lastIndexOf( "$" ) < 0;
    }

    protected static void closeQuietly( ZipFile zipFile )
    {
        try
        {
            if ( zipFile != null )
            {
                zipFile.close();
            }
        }
        catch ( IOException e )
        {
            // ignored
        }
    }
}
