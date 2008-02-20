package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.jpox.SchemaTool;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public abstract class AbstractRepositoryPurgeTest
    extends PlexusTestCase
{
    public static final String TEST_REPO_ID = "test-repo";

    public static final String TEST_REPO_NAME = "Test Repository";

    public static final int TEST_RETENTION_COUNT = 2;

    public static final int TEST_DAYS_OLDER = 30;

    public static final String PATH_TO_BY_DAYS_OLD_ARTIFACT = "org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar";

    public static final String PATH_TO_BY_DAYS_OLD_METADATA_DRIVEN_ARTIFACT = "org/codehaus/plexus/plexus-utils/1.4.3-SNAPSHOT/plexus-utils-1.4.3-20070113.163208-4.jar";

    public static final String PATH_TO_BY_RETENTION_COUNT_ARTIFACT = "org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar";

    public static final String PATH_TO_BY_RETENTION_COUNT_POM = "org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom";

    public static final String PATH_TO_RELEASED_SNAPSHOT = "org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar";

    public static final String PATH_TO_HIGHER_SNAPSHOT_EXISTS = "org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar";
    
    public static final String PATH_TO_TEST_ORDER_OF_DELETION = "org/apache/maven/plugins/maven-assembly-plugin/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar";

    private ManagedRepositoryConfiguration config;

    private ManagedRepositoryContent repo;

    protected ArtifactDAO dao;

    protected RepositoryPurge repoPurge;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        DefaultConfigurableJdoFactory jdoFactory = (DefaultConfigurableJdoFactory) lookup( JdoFactory.ROLE, "archiva" );
        assertEquals( DefaultConfigurableJdoFactory.class.getName(), jdoFactory.getClass().getName() );

        jdoFactory.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" );

        jdoFactory.setDriverName( System.getProperty( "jdo.test.driver", "org.hsqldb.jdbcDriver" ) );
        jdoFactory.setUrl( System.getProperty( "jdo.test.url", "jdbc:hsqldb:mem:testdb" ) );

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

        for ( Entry<Object, Object> entry : properties.entrySet() )
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

        dao = (ArtifactDAO) lookup( ArtifactDAO.class.getName(), "jdo" );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        config = null;
        repo = null;
    }

    public ManagedRepositoryConfiguration getRepoConfiguration()
    {
        if ( config == null )
        {
            config = new ManagedRepositoryConfiguration();
        }

        config.setId( TEST_REPO_ID );
        config.setName( TEST_REPO_NAME );
        config.setDaysOlder( TEST_DAYS_OLDER );
        config.setLocation( getTestRepoRoot().getAbsolutePath() );
        config.setReleases( true );
        config.setSnapshots( true );
        config.setDeleteReleasedSnapshots( true );
        config.setRetentionCount( TEST_RETENTION_COUNT );

        return config;
    }

    public ManagedRepositoryContent getRepository()
        throws Exception
    {
        if ( repo == null )
        {
            repo = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "default" );
            repo.setRepository( getRepoConfiguration() );
        }

        return repo;
    }

    protected void populateDb( String groupId, String artifactId, List<String> versions )
        throws ArchivaDatabaseException
    {
        for ( String version : versions )
        {
            ArchivaArtifact artifact = dao.createArtifact( groupId, artifactId, version, "", "jar" );
            assertNotNull( artifact );
            artifact.getModel().setLastModified( new Date() );
            artifact.getModel().setOrigin( "test" );
            ArchivaArtifact savedArtifact = dao.saveArtifact( artifact );
            assertNotNull( savedArtifact );

            //POM
            artifact = dao.createArtifact( groupId, artifactId, version, "", "pom" );
            assertNotNull( artifact );
            artifact.getModel().setLastModified( new Date() );
            artifact.getModel().setOrigin( "test" );
            savedArtifact = dao.saveArtifact( artifact );
            assertNotNull( savedArtifact );
        }
    }

    protected void assertDeleted( String path )
    {
        assertFalse( "File should have been deleted: " + path, new File( path ).exists() );
    }

    protected void assertExists( String path )
    {
        assertTrue( "File should exist: " + path, new File( path ).exists() );
    }
    
    protected File getTestRepoRoot()
    {
        return getTestFile( "target/test-" + getName() + "/test-repo" );
    }

    protected String prepareTestRepo()
        throws IOException
    {
        File testDir = getTestRepoRoot();
        FileUtils.deleteDirectory( testDir );
        FileUtils.copyDirectory( getTestFile( "target/test-classes/test-repo" ), testDir );
        
        return testDir.getAbsolutePath();
    }
    
    protected void populateDbForTestOrderOfDeletion()
        throws Exception
    {
        List<String> versions = new ArrayList<String>();
        versions.add( "1.1.2-20070427.065136-1" );
        versions.add( "1.1.2-20070506.163513-2" );
        versions.add( "1.1.2-20070615.105019-3" );
    
        populateDb( "org.apache.maven.plugins", "maven-assembly-plugin", versions );
    }
}
