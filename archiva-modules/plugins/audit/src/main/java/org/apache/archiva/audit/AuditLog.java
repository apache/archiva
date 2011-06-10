package org.apache.archiva.audit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AuditLog - Audit Log.
 * 
 * @version $Id$
 * @plexus.component role="org.apache.archiva.audit.AuditListener" role-hint="logging"
 */
@Service("auditListener#logging")
public class AuditLog
    implements AuditListener
{
    public static final Logger logger = LoggerFactory.getLogger( "org.apache.archiva.AuditLog" );

    private static final String NONE = "-";

    private static final char DELIM = ' ';

    /**
     * Creates a log message in the following format ...
     * "{repository_id} {user_id} {remote_ip} \"{resource}\" \"{action}\""
     */
    public void auditEvent( AuditEvent event )
    {
        StringBuilder msg = new StringBuilder();
        msg.append( checkNull( event.getRepositoryId() ) ).append( DELIM );
        msg.append( event.getUserId() ).append( DELIM );
        msg.append( checkNull( event.getRemoteIP() ) ).append( DELIM );
        msg.append( '\"' ).append( checkNull( event.getResource() ) ).append( '\"' ).append( DELIM );
        msg.append( '\"' ).append( event.getAction() ).append( '\"' );

        logger.info( msg.toString() );
    }

    private String checkNull( String s )
    {
        return s != null ? s : NONE;
    }
}
