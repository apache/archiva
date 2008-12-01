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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.model.RepositoryProblem;

import java.util.List;

/**
 * JdoRepositoryProblemDAO 
 *
 * @version $Id$
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoRepositoryProblemDAO
    implements RepositoryProblemDAO
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoAccess jdo;

    public List queryRepositoryProblems( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.queryObjects( RepositoryProblem.class, constraint );
    }

    public RepositoryProblem saveRepositoryProblem( RepositoryProblem problem )
        throws ArchivaDatabaseException
    {
        return (RepositoryProblem) jdo.saveObject( problem );
    }

    public void deleteRepositoryProblem( RepositoryProblem problem )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( problem );
    }
}
