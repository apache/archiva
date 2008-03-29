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

package org.apache.maven.archiva.webdav.simple;

import it.could.webdav.DAVException;
import it.could.webdav.DAVMethod;
import it.could.webdav.DAVMultiStatus;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;

import java.io.IOException;
import java.net.URI;

/**
 * HackedMoveMethod - Created to address the needs for inter-repository moves.
 *
 * @author Pier Fumagalli (Original it.could.webdav 0.4 version)
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a> (Hacked Version)
 * @version $Id: HackedMoveMethod.java 6000 2007-03-04 22:01:49Z joakime $
 */
public class HackedMoveMethod
    implements DAVMethod
{

    public HackedMoveMethod()
    {
        super();
    }

    /**
     * <p>Process the <code>MOVE</code> method.</p>
     */
    public void process( DAVTransaction transaction, DAVResource resource )
        throws IOException
    {
        URI target = transaction.getDestination();
        if ( target == null )
            throw new DAVException( 412, "No destination" );

        if ( target.getScheme() == null )
        {
            // This is a relative file system destination target.
            DAVResource dest = resource.getRepository().getResource( target );
            moveWithinRepository( transaction, resource, dest );
        }
        else
        {
            // This is a inter-repository move request.
            URI dest = target;
            moveInterRepository( transaction, resource, dest );
        }
    }

    private void moveInterRepository( DAVTransaction transaction, DAVResource resource, URI dest )
        throws DAVException
    {
        /* TODO: Figure out how to handle a Repository to Repository MOVE of content, and still maintain
         * the security credentials from the original request. (Need to support NTLM, Digest, BASIC)
         * 
         * IDEA: Could support non-secured Webdav Destination using slide client libraries.
         */
        transaction.setStatus( 501 );
        throw new DAVException( 501, "Server side MOVE to external WebDAV instance not supported." );
    }

    private void moveWithinRepository( DAVTransaction transaction, DAVResource resource, DAVResource dest )
        throws IOException
    {
        int depth = transaction.getDepth();
        boolean recursive = false;
        if ( depth == 0 )
        {
            recursive = false;
        }
        else if ( depth == DAVTransaction.INFINITY )
        {
            recursive = true;
        }
        else
        {
            throw new DAVException( 412, "Invalid Depth specified" );
        }

        try
        {
            int status;
            if ( !dest.isNull() && !transaction.getOverwrite() )
            {
                status = 412; // MOVE-on-existing should fail with 412
            }
            else
            {
                resource.copy( dest, transaction.getOverwrite(), recursive );
                resource.delete();

                if ( transaction.getOverwrite() )
                {
                    status = 204; // No Content
                }
                else
                {
                    status = 201; // Created
                }
            }
            transaction.setStatus( status );
        }
        catch ( DAVMultiStatus multistatus )
        {
            multistatus.write( transaction );
        }
    }

}
