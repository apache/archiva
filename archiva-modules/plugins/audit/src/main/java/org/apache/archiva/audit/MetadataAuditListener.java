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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.repository.events.AuditListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 *
 */
@Service("auditListener#metadata")
public class MetadataAuditListener
    implements AuditListener
{
    private static final Logger log = LoggerFactory.getLogger( MetadataAuditListener.class );

    /**
     *
     */
    @Inject
    private AuditManager auditManager;

    /**
     * FIXME: this could be multiple implementations and needs to be configured.
     *  It also starts a separate session to the originator of the audit event that we may rather want to pass through.
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Override
    public void auditEvent( AuditEvent event )
    {
        // for now we only log upload events, some of the others are quite noisy
        if ( event.getAction().equals( AuditEvent.CREATE_FILE ) || event.getAction().equals( AuditEvent.UPLOAD_FILE ) ||
            event.getAction().equals( AuditEvent.MERGING_REPOSITORIES ) )
        {
            RepositorySession repositorySession = repositorySessionFactory.createSession();
            try
            {
                auditManager.addAuditEvent( repositorySession.getRepository(), event );
                repositorySession.save();
            }
            catch ( MetadataRepositoryException e )
            {
                log.warn( "Unable to write audit event to repository: {}", e.getMessage(), e );
            }
            finally
            {
                repositorySession.close();
            }
        }
    }
}
