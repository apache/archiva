package org.apache.maven.repository.converter.transaction;

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

/**
 * Event for creating a file from a string content.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CreateFileEvent
    extends AbstractTransactionEvent
{
    private final File destination;

    private final String content;

    public CreateFileEvent( String content, File destination )
    {
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

        FileUtils.fileWrite( destination.getAbsolutePath(), content );
    }

    public void rollback()
        throws IOException
    {
        FileUtils.fileDelete( destination.getAbsolutePath() );

        revertMkDirs();

        restoreBackup( destination );
    }
}
