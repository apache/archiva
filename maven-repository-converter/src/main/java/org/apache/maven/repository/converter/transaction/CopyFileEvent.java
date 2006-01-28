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
 * Event to copy a file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CopyFileEvent
    implements TransactionEvent
{
    private final File source;

    private final File destination;

    public CopyFileEvent( File source, File destination )
    {
        this.source = source;
        this.destination = destination;
    }

    public void commit()
        throws IOException
    {
        destination.getParentFile().mkdirs();

        FileUtils.copyFile( source, destination );
    }

    public void rollback()
        throws IOException
    {
        // TODO: revert to backup/delete if was created
    }
}
