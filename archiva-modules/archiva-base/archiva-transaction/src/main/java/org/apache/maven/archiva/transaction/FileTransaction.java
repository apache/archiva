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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.digest.Digester;

/**
 * Implement commit/rollback semantics for a set of files.
 *
 */
public class FileTransaction
{
    private List<AbstractTransactionEvent> events = new ArrayList<AbstractTransactionEvent>();

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
     * @param digesters   {@link List}&lt;{@link org.codehaus.plexus.digest.Digester}> digesters to use for checksumming
     */
    public void copyFile( File source, File destination, List<? extends Digester> digesters )
    {
        events.add( new CopyFileEvent( source, destination, digesters ) );
    }

    /**
     * @param content
     * @param destination
     * @param digesters   {@link List}&lt;{@link org.codehaus.plexus.digest.Digester}> digesters to use for checksumming
     */
    public void createFile( String content, File destination, List<? extends Digester> digesters )
    {
        events.add( new CreateFileEvent( content, destination, digesters ) );
    }
}
