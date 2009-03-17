package org.apache.maven.archiva.scheduled.executors;

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

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsProcessedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.jpox.SchemaTool;

/**
 * ArchivaDatabaseUpdateTaskExecutorTest
 *
 * @version $Id:$
 */
public class ArchivaDatabaseUpdateTaskExecutorTest
    extends PlexusInSpringTestCase
{
    private TaskExecutor taskExecutor;

    protected ArchivaDAO dao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        DefaultConfigurableJdoFactory jdoFactory = (DefaultConfigurableJdoFactory) lookup( JdoFactory.ROLE, "archiva" );
        assertEquals( DefaultConfigurableJdoFactory.class.getName(), jdoFactory.getClass().getName() );

        jdoFactory.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" );

        /* derby version
         File derbyDbDir = new File( "target/plexus-home/testdb" );
         if ( derbyDbDir.exists() )
         {
         FileUtils.deleteDirectory( derbyDbDir );
         }

         jdoFactory.setDriverName( System.getProperty( "jdo.test.driver", "org.apache.derby.jdbc.EmbeddedDriver" ) );   
         jdoFactory.setUrl( System.getProperty( "jdo.test.url", "jdbc:derby:" + derbyDbDir.getAbsolutePath() + ";create=true" ) );
         */

        jdoFactory.setDriverName( System.getProperty( "jdo.test.driver", "org.hsqldb.jdbcDriver" ) );
        jdoFactory.setUrl( System.getProperty( "jdo.test.url", "jdbc:hsqldb:mem:" + getName() ) );

        jdoFactory.setUserName( System.getProperty( "jdo.test.user", "sa" ) );

        jdoFactory.setPassword( System.getProperty( "jdo.test.pass", "" ) );

        jdoFactory.setProperty( "org.jpox.transactionIsolation", "READ_COMMITTED" );

        jdoFactory.setProperty( "org.jpox.poid.transactionIsolation", "READ_COMMITTED" );

        jdoFactory.setProperty( "org.jpox.autoCreateSchema", "true" );

        jdoFactory.setProperty( "javax.jdo.option.RetainValues", "true" );

        jdoFactory.setProperty( "javax.jdo.option.RestoreValues", "true" );

        // jdoFactory.setProperty( "org.jpox.autoCreateColumns", "true" );

        jdoFactory.setProperty( "org.jpox.validateTables", "true" );

        jdoFactory.setProperty( "org.jpox.validateColumns", "true" );

        jdoFactory.setProperty( "org.jpox.validateConstraints", "true" );

        Properties properties = jdoFactory.getProperties();

        for ( Map.Entry<Object, Object> entry : properties.entrySet() )
        {
            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        URL jdoFileUrls[] = new URL[] { getClass().getResource( "/org/apache/maven/archiva/model/package.jdo" ) };

        if ( ( jdoFileUrls == null ) || ( jdoFileUrls[0] == null ) )
        {
            fail( "Unable to process test " + getName() + " - missing package.jdo." );
        }

        File propsFile = null; // intentional
        boolean verbose = true;

        SchemaTool.deleteSchemaTables( jdoFileUrls, new URL[] {}, propsFile, verbose );
        SchemaTool.createSchemaTables( jdoFileUrls, new URL[] {}, propsFile, verbose, null );

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();

        this.dao = (ArchivaDAO) lookup( ArchivaDAO.class.getName(), "jdo" );

        taskExecutor = (TaskExecutor) lookup( TaskExecutor.class, "test-database-update" );
    }

    public void testExecutor()
        throws Exception
    {
        File repoDir = new File( getBasedir(), "src/test/repositories/default-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ManagedRepositoryConfiguration repo = createRepository( "testRepo", "Test Repository", repoDir );
        assertNotNull( repo );

        ArtifactDAO adao = dao.getArtifactDAO();

        ArchivaArtifact sqlArtifact = adao.createArtifact( "javax.sql", "jdbc", "2.0", "", "jar", repo.getId() );
        sqlArtifact.getModel().setLastModified( new Date() );
        sqlArtifact.getModel().setSize( 1234 );
        sqlArtifact.getModel().setOrigin( "testcase" );
        sqlArtifact.getModel().setWhenProcessed( null );

        adao.saveArtifact( sqlArtifact );

        ArchivaArtifact artifact = adao.getArtifact( "javax.sql", "jdbc", "2.0", null, "jar", repo.getId() );

        assertNotNull( artifact );

        // Test for artifact existance.
        List<ArchivaArtifact> artifactList = adao.queryArtifacts( null );
        assertNotNull( "Artifact list should not be null.", artifactList );
        assertEquals( "Artifact list size", 1, artifactList.size() );
        
        // Test for unprocessed artifacts.
        List<ArchivaArtifact> unprocessedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( false ) );
        assertNotNull( "Unprocessed Results should not be null.", unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 1, unprocessedResultList.size() );

        // Execute the database task.
        DatabaseTask dataTask = new DatabaseTask();

        dataTask.setName( "testDataTask" );

        taskExecutor.executeTask( dataTask );
        
        // Test for artifact existance.
        artifactList = adao.queryArtifacts( null );
        assertNotNull( "Artifact list should not be null.", artifactList );
        assertEquals( "Artifact list size", 1, artifactList.size() );
        
        // Test for processed artifacts.
        List<ArchivaArtifact> processedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( true ) );
        assertNotNull( "Processed Results should not be null.", processedResultList );
        assertEquals( "Incorrect number of processed artifacts detected.", 1, processedResultList.size() );
    }
    
    protected ManagedRepositoryConfiguration createRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }
}
