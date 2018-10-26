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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Event for creating a file from a string content.
 *
 *
 */
public class CreateFileEvent
    extends AbstractTransactionEvent
{
    private final Path destination;

    private final String content;

    /**
     * 
     * @param content
     * @param destination
     * @param checksumAlgorithms digesters to use for checksumming
     */
    public CreateFileEvent( String content, Path destination, List<ChecksumAlgorithm> checksumAlgorithms )
    {
        super( checksumAlgorithms );
        this.content = content;
        this.destination = destination;
    }

    @Override
    public void commit()
        throws IOException
    {
        createBackup( destination );

        mkDirs( destination.getParent() );

        if ( !Files.exists(destination))
        {
            Files.createFile(destination);
        }

        writeStringToFile( destination, content );

        createChecksums( destination, true );
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
