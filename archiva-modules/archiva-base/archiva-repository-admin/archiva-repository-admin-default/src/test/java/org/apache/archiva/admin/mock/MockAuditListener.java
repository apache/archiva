package org.apache.archiva.admin.mock;
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
import org.apache.archiva.metadata.audit.AuditListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "auditListener#mock" )
public class MockAuditListener
    implements AuditListener
{

    private List<AuditEvent> auditEvents = new ArrayList<>();

    @Override
    public void auditEvent( AuditEvent event )
    {
        auditEvents.add( event );
    }

    public List<AuditEvent> getAuditEvents()
    {
        return auditEvents;
    }

    public void clearEvents()
    {
        auditEvents.clear();
    }
}
