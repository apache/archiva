package org.apache.archiva.repository;

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

import java.util.ArrayList;
import java.util.List;
import org.apache.archiva.repository.api.RepositoryContext;
import org.apache.archiva.repository.api.interceptor.PostRepositoryInterceptor;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.audit.Auditable;

public class AuditLoggingPostRepositoryInterceptor implements PostRepositoryInterceptor, Auditable
{
    private final List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    public void intercept(RepositoryContext context)
    {
        triggerAuditEvent(context);
    }

    private void triggerAuditEvent( RepositoryContext context )
    {
        AuditEvent event = new AuditEvent( context.getRepositoryId(), context.getPrincipal(), context.getLogicalPath(), context.getRequestType().toString() );
        event.setRemoteIP( context.getRemoteIP() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    public void addAuditListener(AuditListener auditListener)
    {
        auditListeners.add(auditListener);
    }

    public void clearAuditListeners()
    {
        auditListeners.clear();
    }

    public void removeAuditListener(AuditListener auditListener)
    {
        auditListeners.add(auditListener);
    }
}
