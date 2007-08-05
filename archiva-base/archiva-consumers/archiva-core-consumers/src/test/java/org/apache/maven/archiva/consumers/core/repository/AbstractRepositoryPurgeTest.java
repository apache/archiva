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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.DefaultBidirectionalRepositoryLayout;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.jpox.SchemaTool;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.PersistenceManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Iterator;
import java.util.Map;
import java.net.URL;
import java.io.File;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class AbstractRepositoryPurgeTest
    extends PlexusTestCase
{
    public static final String TEST_REPO_ID = "test-repo";

    public static final String TEST_REPO_NAME = "Test Repository";

    public static final String TEST_REPO_URL = "file://" + getBasedir() + "/target/test-classes/test-repo/";

    public static final int TEST_RETENTION_COUNT = 2;

    public static final int TEST_DAYS_OLDER = 30;

    private Configuration config;

    private ArchivaRepository repo;

    private BidirectionalRepositoryLayout layout;

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

        for ( Iterator it = properties.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

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

        dao = (ArtifactDAO) lookup( ArtifactDAO.class.getName(), "jdo" );
    }

    public void lookupRepositoryPurge( String role )
        throws Exception
    {
        repoPurge = (RepositoryPurge) lookup( RepositoryPurge.class.getName(), role );

        repoPurge.setArtifactDao( dao );

        repoPurge.setRepository( getRepository() );

        repoPurge.setLayout( getLayout() );
    }

    public Configuration getRepoConfiguration()
    {
        if ( config == null )
        {
            config = new Configuration();
        }

        RepositoryConfiguration repoConfig = new RepositoryConfiguration();
        repoConfig.setId( TEST_REPO_ID );
        repoConfig.setName( TEST_REPO_NAME );
        repoConfig.setDaysOlder( TEST_DAYS_OLDER );
        repoConfig.setUrl( TEST_REPO_URL );
        repoConfig.setReleases( true );
        repoConfig.setSnapshots( true );
        repoConfig.setRetentionCount( TEST_RETENTION_COUNT );

        List repos = new ArrayList();
        repos.add( repoConfig );

        config.setRepositories( repos );

        return config;
    }

    public ArchivaRepository getRepository()
    {
        if ( repo == null )
        {
            repo = new ArchivaRepository( TEST_REPO_ID, TEST_REPO_NAME, TEST_REPO_URL );
        }

        return repo;
    }

    public BidirectionalRepositoryLayout getLayout()
        throws LayoutException
    {
        if ( layout == null )
        {
            layout = new DefaultBidirectionalRepositoryLayout();
        }

        return layout;
    }
}
