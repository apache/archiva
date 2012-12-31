package org.apache.archiva.policies;

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

import org.apache.archiva.common.ArchivaException;

import java.util.Collections;
import java.util.Map;

/**
 * One or more exceptions occurred downloading from a remote repository during the proxy phase.
 */
public class ProxyDownloadException
    extends ArchivaException
{
    /**
     * A list of failures keyed by repository ID.
     */
    private final Map<String, Exception> failures;

    public ProxyDownloadException( String message, String repositoryId, Exception cause )
    {
        super( constructMessage( message, Collections.singletonMap( repositoryId, cause ) ), cause );

        failures = Collections.singletonMap( repositoryId, cause );
    }

    public ProxyDownloadException( String message, Map<String, Exception> failures )
    {
        super( constructMessage( message, failures ) );

        this.failures = failures;
    }

    private static String constructMessage( String message, Map<String, Exception> failures )
    {
        StringBuilder msg = new StringBuilder( message );
        msg.append( ":" );
        for ( Map.Entry<String, Exception> entry : failures.entrySet() )
        {
            msg.append( "\n\t" ).append( entry.getKey() ).append( ": " ).append( entry.getValue().getMessage() );
        }
        return msg.toString();
    }

    public Map<String, Exception> getFailures()
    {
        return failures;
    }
}
