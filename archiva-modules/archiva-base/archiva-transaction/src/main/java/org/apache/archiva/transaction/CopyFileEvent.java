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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Event to copy a file.
 *
 *
 */
public class CopyFileEvent
    extends AbstractTransactionEvent
{
    private final Path source;

    private final Path destination;

    /**
     * 
     * @param source
     * @param destination
     * @param checksumAlgorithms The checksum algorithms
     */
    public CopyFileEvent( Path source, Path destination, List<ChecksumAlgorithm> checksumAlgorithms )
    {
        super( checksumAlgorithms );
        this.source = source;
        this.destination = destination;
    }

    @Override
    public void commit()
        throws IOException
    {
        createBackup( destination );

        mkDirs( destination.getParent() );

        FileUtils.copyFile( source.toFile(), destination.toFile() );

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
        for ( ChecksumAlgorithm checksumAlgorithm : getChecksumAlgorithms() )
        {
            copyChecksum( getChecksumFileExtension( checksumAlgorithm ) );
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
        Path checksumSource = Paths.get( source.toAbsolutePath() + "." + extension );
        if ( Files.exists(checksumSource) )
        {
            Path checksumDestination = Paths.get( destination.toAbsolutePath() + "." + extension );
            FileUtils.copyFile( checksumSource.toFile(), checksumDestination.toFile() );
            return true;
        }
        return false;
    }

    @Override
    public void rollback()
        throws IOException
    {
        Files.deleteIfExists(destination);

        revertFilesCreated();

        revertMkDirs();

        restoreBackups();
    }
}
