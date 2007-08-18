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

import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.DefaultBidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.jpox.SchemaTool;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public abstract class AbstractRepositoryPurgeTest
    extends PlexusTestCase
{
    public static final String TEST_REPO_ID = "test-repo";

    public static final String TEST_REPO_NAME = "Test Repository";

    public static final String TEST_REPO_URL = getBasedir() + "/target/test-classes/test-repo/";

    public static final int TEST_RETENTION_COUNT = 2;

    public static final int TEST_DAYS_OLDER = 30;

    private ManagedRepositoryConfiguration config;

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

    public ManagedRepositoryConfiguration getRepoConfiguration()
    {
        if ( config == null )
        {
            config = new ManagedRepositoryConfiguration();
        }

        config.setId( TEST_REPO_ID );
        config.setName( TEST_REPO_NAME );
        config.setDaysOlder( TEST_DAYS_OLDER );
        config.setLocation( TEST_REPO_URL );
        config.setReleases( true );
        config.setSnapshots( true );
        config.setRetentionCount( TEST_RETENTION_COUNT );

        return config;
    }

    public ArchivaRepository getRepository()
    {
        if ( repo == null )
        {
            repo = new ArchivaRepository( TEST_REPO_ID, TEST_REPO_NAME, PathUtil.toUrl( TEST_REPO_URL ) );
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
