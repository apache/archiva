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

import javax.jdo.JDOHelper;

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
        // Create it
        ArchivaRepositoryModel repo = dao.createRepository( "testRepo", "http://localhost:8080/repository/foo" );
        assertNotNull( repo );

        // Set some mandatory values
        repo.setName( "The Test Repository." );
        repo.setCreationSource( "Test Case" );
        repo.setLayoutName( "default" );

        // Save it. 
        ArchivaRepositoryModel repoSaved = dao.saveRepository( repo );
        assertNotNull( repoSaved );
        assertEquals( "testRepo", JDOHelper.getObjectId( repoSaved ).toString() );

        // Test that something has been saved.
        List repos = dao.getRepositories();
        assertNotNull( repos );
        assertEquals( 1, repos.size() );

        // Test that retreived object is what we expect.
        ArchivaRepositoryModel firstRepo = (ArchivaRepositoryModel) repos.get( 0 );
        assertNotNull( firstRepo );
        assertEquals( "testRepo", repo.getId() );
        assertEquals( "The Test Repository.", repo.getName() );
        assertEquals( "Test Case", repo.getCreationSource() );
        assertEquals( "default", repo.getLayoutName() );

        // Change value and save.
        repoSaved.setName( "Saved Again" );
        dao.saveRepository( repoSaved );

        // Test that only 1 object is saved.
        assertEquals( 1, dao.getRepositories().size() );

        // Get the specific repo.
        ArchivaRepositoryModel actualRepo = dao.getRepository( "testRepo" );
        assertNotNull( actualRepo );

        // Test expected values.
        assertEquals( "testRepo", actualRepo.getId() );
        assertEquals( "http://localhost:8080/repository/foo", actualRepo.getUrl() );
        assertEquals( "Saved Again", actualRepo.getName() );

        // Test that only 1 object is saved.
        assertEquals( 1, dao.getRepositories().size() );

        // Delete object.
        dao.deleteRepository( actualRepo );
        assertEquals( 0, dao.getRepositories().size() );
    }
}
