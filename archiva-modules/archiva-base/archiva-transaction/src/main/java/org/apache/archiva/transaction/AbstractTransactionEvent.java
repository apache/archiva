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
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Abstract class for the TransactionEvents
 *
 *
 */
public abstract class AbstractTransactionEvent
    implements TransactionEvent
{
    private Map<Path, Path> backups = new HashMap<>();

    private List<Path> createdDirs = new ArrayList<>();

    private List<Path> createdFiles = new ArrayList<>();

    private List<ChecksumAlgorithm> checksumAlgorithms;

    protected AbstractTransactionEvent()
    {
        this( new ArrayList<ChecksumAlgorithm>( 0 ) );
    }

    protected AbstractTransactionEvent( List<ChecksumAlgorithm> checksumAlgorithms)
    {
        this.checksumAlgorithms = checksumAlgorithms;
    }

    protected List<ChecksumAlgorithm> getChecksumAlgorithms()
    {
        return checksumAlgorithms;
    }

    /**
     * Method that creates a directory as well as all the parent directories needed
     *
     * @param dir The File directory to be created
     * @throws IOException when an unrecoverable error occurred
     */
    protected void mkDirs( Path dir )
        throws IOException
    {
        List<Path> createDirs = new ArrayList<>();

        Path parent = dir;
        while ( !Files.exists(parent) || !Files.isDirectory(parent) )
        {
            createDirs.add( parent );

            parent = parent.getParent();
        }

        while ( !createDirs.isEmpty() )
        {
            Path directory = createDirs.remove( createDirs.size() - 1 );
            Files.createDirectories(directory);
            createdDirs.add( directory );
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
                Path dir = createdDirs.remove( 0 );

                if ( Files.isDirectory(dir))
                {
                    try(Stream<Path> str = Files.list(dir)) {
                        if (str.count()==0) {
                            org.apache.archiva.common.utils.FileUtils.deleteDirectory(dir);
                        }
                    }
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
        Iterator<Path> it = createdFiles.iterator();
        while ( it.hasNext() )
        {
            Path file = it.next();
            Files.deleteIfExists(file);
            it.remove();
        }
    }

    protected void createBackup( Path file )
        throws IOException
    {
        if ( Files.exists(file) && Files.isRegularFile(file) )
        {
            Path backup = Files.createTempFile( "temp-", ".backup" );

            FileUtils.copyFile( file.toFile(), backup.toFile() );

            backup.toFile().deleteOnExit();

            backups.put( file, backup );
        }
    }

    protected void restoreBackups()
        throws IOException
    {
        for ( Map.Entry<Path, Path> entry : backups.entrySet() )
        {
            FileUtils.copyFile( entry.getValue().toFile(), entry.getKey().toFile() );
        }
    }

    protected void restoreBackup( Path file )
        throws IOException
    {
        Path backup = backups.get( file );
        if ( backup != null )
        {
            FileUtils.copyFile( backup.toFile(), file.toFile() );
        }
    }

    /**
     * Create checksums of file using all digesters defined at construction time.
     *
     * @param file
     * @param force whether existing checksums should be overwritten or not
     * @throws IOException
     */
    protected void createChecksums( Path file, boolean force )
        throws IOException
    {
        for ( ChecksumAlgorithm checksumAlgorithm : getChecksumAlgorithms() )
        {
            Path checksumFile = Paths.get( file.toAbsolutePath( ) + "." + getChecksumFileExtension( checksumAlgorithm ) );
            if ( Files.exists( checksumFile ) )
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
        }
        ChecksummedFile csFile = new ChecksummedFile( file );
        csFile.fixChecksums( getChecksumAlgorithms() );
    }

    /**
     * TODO: Remove in favor of using FileUtils directly.
     */
    protected void writeStringToFile( Path file, String content )
        throws IOException
    {
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( file, Charset.defaultCharset(), content );
    }

    /**
     * File extension for checksums
     * TODO should be moved to plexus-digester ?
     */
    protected String getChecksumFileExtension( ChecksumAlgorithm algorithm )
    {
        return algorithm.getExt().get(0);
    }

}
