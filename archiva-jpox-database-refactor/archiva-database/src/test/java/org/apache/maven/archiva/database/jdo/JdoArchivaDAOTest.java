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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaRepositoryModel;

import java.util.List;

/**
 * JdoArchivaDAOTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class JdoArchivaDAOTest extends AbstractArchivaDatabaseTestCase
{
    public void testRepositoryCRUD() throws ArchivaDatabaseException
    {
        ArchivaRepositoryModel repo = dao.createRepository( "testRepo", "http://localhost:8080/repository/foo" );

        assertNotNull( repo );

        repo.setName( "The Test Repostitory." );
        repo.setLayoutName( "default" );

        ArchivaRepositoryModel repoSaved = dao.saveRepository( repo );
        assertNotNull( repoSaved );

        List repos = dao.getRepositories();
        assertNotNull( repos );
        assertEquals( 1, repos.size() );
        
        repoSaved.setName( "Saved Again" );
        dao.saveRepository( repoSaved );
        
        ArchivaRepositoryModel actualRepo = dao.getRepository( "testRepo" );
        assertNotNull( actualRepo );
        assertEquals( "testRepo", actualRepo.getId() );
        assertEquals( "http://localhost:8080/repository/foo", actualRepo.getUrl() );
        assertEquals( "Saved Again", actualRepo.getName() );
        
        assertEquals( 1, dao.getRepositories().size() );
        
        dao.deleteRepository( actualRepo );
        assertEquals( 0, dao.getRepositories().size() );
    }
}

