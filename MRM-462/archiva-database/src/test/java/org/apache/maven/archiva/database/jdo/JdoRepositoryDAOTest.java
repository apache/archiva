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
import org.apache.maven.archiva.database.RepositoryDAO;
import org.apache.maven.archiva.model.ArchivaRepository;

import java.util.List;

import javax.jdo.JDOHelper;

/**
 * JdoRepositoryDAOTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class JdoRepositoryDAOTest
    extends AbstractArchivaDatabaseTestCase
{
    public void testRepositoryCRUD()
        throws ArchivaDatabaseException
    {
        RepositoryDAO repoDao = dao.getRepositoryDAO();

        // Create it
        ArchivaRepository repo = repoDao.createRepository( "testRepo", "Test Repository",
                                                           "http://localhost:8080/repository/foo" );
        assertNotNull( repo );

        // Set some mandatory values
        repo.getModel().setCreationSource( "Test Case" );
        repo.getModel().setLayoutName( "default" );

        // Save it. 
        ArchivaRepository repoSaved = repoDao.saveRepository( repo );
        assertNotNull( repoSaved );
        assertNotNull( repoSaved.getModel() );
        assertEquals( "testRepo", JDOHelper.getObjectId( repoSaved.getModel() ).toString() );

        // Test that something has been saved.
        List repos = repoDao.getRepositories();
        assertNotNull( repos );
        assertEquals( 1, repos.size() );

        // Test that retreived object is what we expect.
        ArchivaRepository firstRepo = (ArchivaRepository) repos.get( 0 );
        assertNotNull( firstRepo );
        assertEquals( "testRepo", repo.getId() );
        assertEquals( "Test Repository", repo.getModel().getName() );
        assertEquals( "Test Case", repo.getModel().getCreationSource() );
        assertEquals( "default", repo.getModel().getLayoutName() );

        // Change value and save.
        repoSaved.getModel().setCreationSource( "Changed" );
        repoDao.saveRepository( repoSaved );

        // Test that only 1 object is saved.
        assertEquals( 1, repoDao.getRepositories().size() );

        // Get the specific repo.
        ArchivaRepository actualRepo = repoDao.getRepository( "testRepo" );
        assertNotNull( actualRepo );

        // Test expected values.
        assertEquals( "testRepo", actualRepo.getId() );
        assertEquals( "http://localhost:8080/repository/foo", actualRepo.getUrl().toString() );
        assertEquals( "Changed", actualRepo.getModel().getCreationSource() );

        // Test that only 1 object is saved.
        assertEquals( 1, repoDao.getRepositories().size() );

        // Delete object.
        repoDao.deleteRepository( actualRepo );
        assertEquals( 0, repoDao.getRepositories().size() );
    }
}
