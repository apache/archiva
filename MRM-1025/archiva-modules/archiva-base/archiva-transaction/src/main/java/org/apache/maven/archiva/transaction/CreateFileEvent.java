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

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Event for creating a file from a string content.
 *
 * @version $Id$
 */
public class CreateFileEvent
    extends AbstractTransactionEvent
{
    private final File destination;

    private final String content;

    /**
     * 
     * @param content
     * @param destination
     * @param digesters {@link List}&lt;{@link Digester}> digesters to use for checksumming 
     */
    public CreateFileEvent( String content, File destination, List digesters )
    {
        super( digesters );
        this.content = content;
        this.destination = destination;
    }

    public void commit()
        throws IOException
    {
        createBackup( destination );

        mkDirs( destination.getParentFile() );

        if ( !destination.exists() && !destination.createNewFile() )
        {
            throw new IOException( "Unable to create new file" );
        }

        writeStringToFile( destination, content );

        createChecksums( destination, true );
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
