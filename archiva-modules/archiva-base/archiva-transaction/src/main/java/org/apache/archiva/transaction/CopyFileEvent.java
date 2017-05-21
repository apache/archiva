package org.apache.archiva.transaction;

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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.digest.Digester;

/**
 * Event to copy a file.
 *
 *
 */
public class CopyFileEvent
    extends AbstractTransactionEvent
{
    private final File source;

    private final File destination;

    /**
     * 
     * @param source
     * @param destination
     * @param digesters {@link List}&lt;{@link Digester}&gt; digesters to use for checksumming 
     */
    public CopyFileEvent( File source, File destination, List<? extends Digester> digesters )
    {
        super( digesters );
        this.source = source;
        this.destination = destination;
    }

    @Override
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
        for ( Digester digester : getDigesters() )
        {
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

    @Override
    public void rollback()
        throws IOException
    {
        destination.delete();

        revertFilesCreated();

        revertMkDirs();

        restoreBackups();
    }
}
