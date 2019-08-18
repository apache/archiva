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

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.facets.AbstractMetadataFacetFactory;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service("metadataFacetFactory#org.apache.archiva.audit")
public class AuditEventFactory
    extends AbstractMetadataFacetFactory<AuditEvent>
{
    public AuditEventFactory() {
        super( AuditEvent.class );
    }

    @Override
    public AuditEvent createMetadataFacet()
    {
        throw new UnsupportedOperationException( "Must construct an audit event with a name" );
    }

    @Override
    public AuditEvent createMetadataFacet( String repositoryId, String name )
    {
        return new AuditEvent( name, repositoryId );
    }

    @Override
    public boolean assignsFacet( Class<?> clazz )
    {
        return false;
    }

    @Override
    public Class<AuditEvent> getFacetClass( )
    {
        return null;
    }
}
