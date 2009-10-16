package org.apache.maven.archiva.web.action.admin.repositories;

import java.util.List;

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.model.RepositoryProblem;

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

/**
 * Stub class for Archiva DAO to avoid having to set up a database for tests.
 *
 * @todo a mock would be better, but that won't play nicely with Plexus injection.
 */
public class RepositoryProblemDAOStub
    implements RepositoryProblemDAO
{
    public List<RepositoryProblem> queryRepositoryProblems( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        throw new UnsupportedOperationException( "not implemented for stub" );
    }

    public RepositoryProblem saveRepositoryProblem( RepositoryProblem problem )
        throws ArchivaDatabaseException
    {
        throw new UnsupportedOperationException( "not implemented for stub" );
    }

    public void deleteRepositoryProblem( RepositoryProblem problem )
        throws ArchivaDatabaseException
    {
        throw new UnsupportedOperationException( "not implemented for stub" );
    }
}