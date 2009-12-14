package org.apache.maven.archiva.database.jdo;

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

import java.util.List;

import org.apache.maven.archiva.database.ArchivaAuditLogsDao;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaAuditLogs;

/**
 * JdoArchivaAuditLogsDao
 * 
 * @version  
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoArchivaAuditLogsDao
    implements ArchivaAuditLogsDao
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoAccess jdo;

    public void deleteAuditLogs( ArchivaAuditLogs logs )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( logs );
    }

    @SuppressWarnings( "unchecked" )
    public List<ArchivaAuditLogs> queryAuditLogs( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return (List<ArchivaAuditLogs>) jdo.queryObjects( ArchivaAuditLogs.class, constraint );
    }

    public ArchivaAuditLogs saveAuditLogs( ArchivaAuditLogs logs )
    {
        return (ArchivaAuditLogs) jdo.saveObject( logs );
    }
}
