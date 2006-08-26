package org.apache.maven.archiva.converter.transaction;

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

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for the TransactionEvents
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractTransactionEvent
    implements TransactionEvent
{
    private File backup;

    private List createdDirs;

    /**
     * Method that creates a directory as well as all the parent directories needed
     *
     * @param dir The File directory to be created
     * @throws IOException when an unrecoverable error occurred
     */
    protected void mkDirs( File dir )
        throws IOException
    {
        List createDirs = new ArrayList();

        File parent = dir;
        while ( !parent.exists() || !parent.isDirectory() )
        {
            createDirs.add( parent );

            parent = parent.getParentFile();
        }

        createdDirs = new ArrayList();

        while ( !createDirs.isEmpty() )
        {
            File directory = (File) createDirs.remove( createDirs.size() - 1 );

            if ( directory.mkdir() )
            {
                createdDirs.add( directory );
            }
            else
            {
                throw new IOException( "Failed to create directory: " + directory.getAbsolutePath() );
            }
        }
    }

    protected void revertMkDirs()
        throws IOException
    {
        if ( createdDirs != null )
        {
            Collections.reverse( createdDirs );

            while ( !createdDirs.isEmpty() )
            {
                File dir = (File) createdDirs.remove( 0 );

                if ( dir.isDirectory() && dir.list().length == 0 )
                {
                    FileUtils.deleteDirectory( dir.getAbsolutePath() );
                }
                else
                {
                    //cannot rollback created directory if it still contains files
                    break;
                }
            }
        }
    }

    protected void createBackup( File file )
        throws IOException
    {
        if ( file.exists() && file.isFile() )
        {
            backup = File.createTempFile( "temp-", ".backup" );

            FileUtils.copyFile( file, backup );

            backup.deleteOnExit();
        }
    }

    protected void restoreBackup( File file )
        throws IOException
    {
        if ( backup != null )
        {
            FileUtils.copyFile( backup, file );
        }
    }
}
