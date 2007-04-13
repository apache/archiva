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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.RepositoryDAO;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.jpox.SchemaTool;

/**
 * IndexerTaskExecutorTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id:$
 */
public class ArchivaScheduledTaskExecutorTest
    extends PlexusTestCase
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

        for ( Iterator it = properties.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        URL jdoFileUrls[] = new URL[] { getClass()
            .getResource( "/org/apache/maven/archiva/model/package.jdo" ) }; 

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

        taskExecutor = (TaskExecutor) lookup( TaskExecutor.class.getName(), "test-archiva-task-executor" );
    }

    public void testExecutor() throws Exception
    {
        RepositoryDAO repoDao = dao.getRepositoryDAO();

        File repoDir = new File( getBasedir(), "src/test/repositories/default-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        String repoUri = "file://" + StringUtils.replace( repoDir.getAbsolutePath(), "\\", "/" );

        
        // Create it
        ArchivaRepository repo =
            repoDao.createRepository( "testRepo", "Test Repository", repoUri );
        assertNotNull( repo );

        // Set some mandatory values
        repo.getModel().setCreationSource( "Test Case" );
        repo.getModel().setLayoutName( "default" );

        // Save it.
        ArchivaRepository repoSaved = repoDao.saveRepository( repo );
        assertNotNull( repoSaved );
        assertNotNull( repoSaved.getModel() );
        assertEquals( "testRepo", JDOHelper.getObjectId( repoSaved.getModel() ).toString() );

        RepositoryTask repoTask = new RepositoryTask();
        
        repoTask.setName( "testTask" );
        repoTask.setRepositoryId( "testRepo" );
        
        taskExecutor.executeTask( repoTask );

    }

}
