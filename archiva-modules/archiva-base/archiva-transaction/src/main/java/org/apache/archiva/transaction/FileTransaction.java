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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement commit/rollback semantics for a set of files.
 *
 */
public class FileTransaction
{
    private List<AbstractTransactionEvent> events = new ArrayList<>();

    public void commit()
        throws TransactionException
    {
        List<TransactionEvent> toRollback = new ArrayList<TransactionEvent>( events.size() );

        for ( TransactionEvent event : events )
        {
            try
            {
                event.commit();

                toRollback.add( event );
            }
            catch ( IOException e )
            {
                try
                {
                    rollback( toRollback );

                    throw new TransactionException( "Unable to commit file transaction", e );
                }
                catch ( IOException ioe )
                {
                    throw new TransactionException(
                        "Unable to commit file transaction, and rollback failed with error: '" + ioe.getMessage() + "'",
                        e );
                }
            }
        }
    }

    private void rollback( List<TransactionEvent> toRollback )
        throws IOException
    {
        for ( TransactionEvent event : toRollback )
        {
            event.rollback();
        }
    }

    /**
     * @param source
     * @param destination
     * @param checksumAlgorithms   The checksum algorithms
     */
    public void copyFile(Path source, Path destination, List<ChecksumAlgorithm> checksumAlgorithms )
    {
        events.add( new CopyFileEvent( source, destination, checksumAlgorithms ) );
    }

    /**
     * @param content
     * @param destination
     * @param checksumAlgorithms   Checksum algorithms
     */
    public void createFile( String content, Path destination, List<ChecksumAlgorithm> checksumAlgorithms )
    {
        events.add( new CreateFileEvent( content, destination, checksumAlgorithms ) );
    }
}
