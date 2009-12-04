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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsProcessedConstraint;
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.codehaus.plexus.util.FileUtils;
import org.jpox.SchemaTool;

/**
 * ArchivaRepositoryScanningTaskExecutorTest
 *
 * @version $Id$
 */
public class ArchivaRepositoryScanningTaskExecutorTest
    extends PlexusInSpringTestCase
{
    private TaskExecutor taskExecutor;

    protected ArchivaDAO dao;

    private File repoDir;

    private static final String TEST_REPO_ID = "testRepo";

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

        URL jdoFileUrls[] = new URL[]{getClass().getResource( "/org/apache/maven/archiva/model/package.jdo" )};

        if ( ( jdoFileUrls == null ) || ( jdoFileUrls[0] == null ) )
        {
            fail( "Unable to process test " + getName() + " - missing package.jdo." );
        }

        File propsFile = null; // intentional
        boolean verbose = true;

        SchemaTool.deleteSchemaTables( jdoFileUrls, new URL[]{}, propsFile, verbose );
        SchemaTool.createSchemaTables( jdoFileUrls, new URL[]{}, propsFile, verbose, null );

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();

        this.dao = (ArchivaDAO) lookup( ArchivaDAO.class.getName(), "jdo" );

        taskExecutor = (TaskExecutor) lookup( TaskExecutor.class, "test-repository-scanning" );

        File sourceRepoDir = new File( getBasedir(), "src/test/repositories/default-repository" );
        repoDir = new File( getBasedir(), "target/default-repository" );

        repoDir.mkdir();

        FileUtils.copyDirectoryStructure( sourceRepoDir, repoDir );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ArchivaConfiguration archivaConfig = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        assertNotNull( archivaConfig );

        // Create it
        ManagedRepositoryConfiguration repositoryConfiguration = new ManagedRepositoryConfiguration();
        repositoryConfiguration.setId( TEST_REPO_ID );
        repositoryConfiguration.setName( "Test Repository" );
        repositoryConfiguration.setLocation( repoDir.getAbsolutePath() );
        archivaConfig.getConfiguration().getManagedRepositories().clear();
        archivaConfig.getConfiguration().addManagedRepository( repositoryConfiguration );
    }

    protected void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( repoDir );

        assertFalse( repoDir.exists() );

        super.tearDown();
    }

    public void testExecutor()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );

        taskExecutor.executeTask( repoTask );

        ArtifactDAO adao = dao.getArtifactDAO();
        List<ArchivaArtifact> unprocessedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( false ) );

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );
    }

    public void testExecutorScanOnlyNewArtifacts()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        RepositoryContentStatistics stats = new RepositoryContentStatistics();
        stats.setDuration( 1234567 );
        stats.setNewFileCount( 31 );
        stats.setRepositoryId( TEST_REPO_ID );
        stats.setTotalArtifactCount( 8 );
        stats.setTotalFileCount( 31 );
        stats.setTotalGroupCount( 3 );
        stats.setTotalProjectCount( 5 );
        stats.setTotalSize( 38545 );
        stats.setWhenGathered( Calendar.getInstance().getTime() );

        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics( stats );

        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        ArtifactDAO adao = dao.getArtifactDAO();
        List<ArchivaArtifact> unprocessedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( false ) );

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. No new artifacts should have been found.", 0,
                      unprocessedResultList.size() );

        // check correctness of new stats
        List<RepositoryContentStatistics> results =
            (List<RepositoryContentStatistics>) dao.query( new MostRecentRepositoryScanStatistics( TEST_REPO_ID ) );
        RepositoryContentStatistics newStats = results.get( 0 );
        assertEquals( 0, newStats.getNewFileCount() );
        assertEquals( TEST_REPO_ID, newStats.getRepositoryId() );
        assertEquals( 31, newStats.getTotalFileCount() );
        // TODO: can't test these as they weren't stored in the database
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
        assertEquals( 38545, newStats.getTotalSize() );

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );

        FileUtils.copyDirectoryStructure( new File( getBasedir(), "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );

        assertTrue( newArtifactGroup.exists() );

        taskExecutor.executeTask( repoTask );

        unprocessedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( false ) );
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        results =
            (List<RepositoryContentStatistics>) dao.query( new MostRecentRepositoryScanStatistics( TEST_REPO_ID ) );
        RepositoryContentStatistics updatedStats = results.get( 0 );
        assertEquals( 2, updatedStats.getNewFileCount() );
        assertEquals( TEST_REPO_ID, updatedStats.getRepositoryId() );
        assertEquals( 33, updatedStats.getTotalFileCount() );
        // TODO: can't test these as they weren't stored in the database
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
        assertEquals( 43687, updatedStats.getTotalSize() );
    }

    public void testExecutorForceScanAll()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( true );

        RepositoryContentStatistics stats = new RepositoryContentStatistics();
        stats.setDuration( 1234567 );
        stats.setNewFileCount( 8 );
        stats.setRepositoryId( TEST_REPO_ID );
        stats.setTotalArtifactCount( 8 );
        stats.setTotalFileCount( 8 );
        stats.setTotalGroupCount( 3 );
        stats.setTotalProjectCount( 5 );
        stats.setTotalSize( 999999 );
        stats.setWhenGathered( Calendar.getInstance().getTime() );

        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics( stats );

        taskExecutor.executeTask( repoTask );

        ArtifactDAO adao = dao.getArtifactDAO();
        List<ArchivaArtifact> unprocessedResultList = adao.queryArtifacts( new ArtifactsProcessedConstraint( false ) );

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );
    }
}
