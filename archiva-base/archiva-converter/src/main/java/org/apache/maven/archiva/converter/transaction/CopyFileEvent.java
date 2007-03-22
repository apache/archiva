package org.apache.maven.archiva.converter.transaction;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.digest.Digester;

/**
 * Event to copy a file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class CopyFileEvent
    extends AbstractTransactionEvent
{
    private final File source;

    private final File destination;

    /**
     * Creates a copy file event with no digesters
     * 
     * @deprecated use other constructors
     * 
     * @param source
     * @param destination
     */
    public CopyFileEvent( File source, File destination )
    {
        this( source, destination, new ArrayList( 0 ) );
    }

    /**
     * 
     * @param source
     * @param destination
     * @param digesters {@link List}&lt;{@link Digester}> digesters to use for checksumming 
     */
    public CopyFileEvent( File source, File destination, List digesters )
    {
        super( digesters );
        this.source = source;
        this.destination = destination;
    }

    public void commit()
        throws IOException
    {
        createBackup( destination );

        mkDirs( destination.getParentFile() );

        FileUtils.copyFile( source, destination );

        createChecksums( destination, true );
        copyChecksums();

        copyChecksum( "asc" );
    }

    /**
     * Copy checksums of source file with all digesters if exist
     * 
     * @throws IOException
     */
    private void copyChecksums()
        throws IOException
    {
        Iterator it = getDigesters().iterator();
        while ( it.hasNext() )
        {
            Digester digester = (Digester) it.next();
            copyChecksum( getDigesterFileExtension( digester ) );
        }
    }

    /**
     * Copy checksum of source file with extension provided if exists
     * 
     * @param extension
     * @return whether the checksum exists or not 
     * @throws IOException
     */
    private boolean copyChecksum( String extension )
        throws IOException
    {
        File checksumSource = new File( source.getAbsolutePath() + "." + extension );
        if ( checksumSource.exists() )
        {
            File checksumDestination = new File( destination.getAbsolutePath() + "." + extension );
            FileUtils.copyFile( checksumSource, checksumDestination );
            return true;
        }
        return false;
    }

    public void rollback()
        throws IOException
    {
        destination.delete();

        revertFilesCreated();

        revertMkDirs();

        restoreBackups();
    }
}
