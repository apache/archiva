package org.apache.maven.archiva.transaction;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for the TransactionEvents
 *
 * @author Edwin Punzalan
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractTransactionEvent
    extends AbstractLogEnabled
    implements TransactionEvent
{
    private Map backups = new HashMap();

    private List createdDirs = new ArrayList();

    private List createdFiles = new ArrayList();

    /**
     * {@link List}&lt;{@link Digester}>
     */
    private List digesters;

    protected AbstractTransactionEvent()
    {
        this( new ArrayList( 0 ) );
    }

    protected AbstractTransactionEvent( List digesters )
    {
        this.digesters = digesters;
    }

    protected List getDigesters()
    {
        return digesters;
    }

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
                    FileUtils.deleteDirectory( dir );
                }
                else
                {
                    //cannot rollback created directory if it still contains files
                    break;
                }
            }
        }
    }

    protected void revertFilesCreated()
        throws IOException
    {
        Iterator it = createdFiles.iterator();
        while ( it.hasNext() )
        {
            File file = (File) it.next();
            file.delete();
            it.remove();
        }
    }

    protected void createBackup( File file )
        throws IOException
    {
        if ( file.exists() && file.isFile() )
        {
            File backup = File.createTempFile( "temp-", ".backup" );

            FileUtils.copyFile( file, backup );

            backup.deleteOnExit();

            backups.put( file, backup );
        }
    }

    protected void restoreBackups()
        throws IOException
    {
        Iterator it = backups.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Map.Entry) it.next();
            FileUtils.copyFile( (File) entry.getValue(), (File) entry.getKey() );
        }
    }

    protected void restoreBackup( File file )
        throws IOException
    {
        File backup = (File) backups.get( file );
        if ( backup != null )
        {
            FileUtils.copyFile( backup, file );
        }
    }

    /**
     * Create checksums of file using all digesters defined at construction time.
     *
     * @param file
     * @param force whether existing checksums should be overwritten or not
     * @throws IOException
     */
    protected void createChecksums( File file, boolean force )
        throws IOException
    {
        Iterator it = getDigesters().iterator();
        while ( it.hasNext() )
        {
            Digester digester = (Digester) it.next();
            File checksumFile = new File( file.getAbsolutePath() + "." + getDigesterFileExtension( digester ) );
            if ( checksumFile.exists() )
            {
                if ( !force )
                {
                    continue;
                }
                createBackup( checksumFile );
            }
            else
            {
                createdFiles.add( checksumFile );
            }

            try
            {
                writeStringToFile( checksumFile, digester.calc( file ) );
            }
            catch ( DigesterException e )
            {
                throw (IOException) e.getCause();
            }
        }
    }

    protected void writeStringToFile( File file, String content )
        throws IOException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream( file );
            IOUtil.copy( content, out );
        }
        finally
        {
            IOUtil.close( out );
        }
    }

    /**
     * File extension for checksums
     * TODO should be moved to plexus-digester ?
     */
    protected String getDigesterFileExtension( Digester digester )
    {
        return digester.getAlgorithm().toLowerCase().replaceAll( "-", "" );
    }

}
