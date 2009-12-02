package org.apache.maven.archiva.database;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.jpox.SchemaTool;

/**
 * AbstractArchivaDatabaseTestCase 
 *
 * @version $Id$
 */
public abstract class AbstractArchivaDatabaseTestCase
    extends PlexusInSpringTestCase
{
    private static final String TIMESTAMP = "yyyy/MM/dd HH:mm:ss";

    protected ArchivaDAO dao;

    @Override
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

        for ( Map.Entry<Object,Object> entry : properties.entrySet() )
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
    }

    protected Date toDate( String txt )
        throws Exception
    {
        SimpleDateFormat sdf = new SimpleDateFormat( TIMESTAMP );
        return sdf.parse( txt );
    }

    protected String fromDate( Date date )
        throws Exception
    {
        SimpleDateFormat sdf = new SimpleDateFormat( TIMESTAMP );
        return sdf.format( date );
    }

    protected VersionedReference toVersionedReference( String id )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( id, ':' );
        assertEquals( "Should have 3 parts [" + id + "]", 3, parts.length );
    
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );
    
        assertTrue( "Group ID should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getGroupId() ) );
        assertTrue( "Artifact ID should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getArtifactId() ) );
        assertTrue( "Version should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getVersion() ) );
    
        return ref;
    }

    protected ArtifactReference toArtifactReference( String id )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( id, ':' );
        assertEquals( "Should have 6 parts [" + id + "]", 6, parts.length );
    
        ArtifactReference ref = new ArtifactReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );
        ref.setClassifier( parts[3] );
        ref.setType( parts[4] );
        
        assertTrue( "Group ID should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getGroupId() ) );
        assertTrue( "Artifact ID should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getArtifactId() ) );
        assertTrue( "Version should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getVersion() ) );
        // Blank string is ok for classifier, NULL is not.
        assertNotNull( "Classifier should not be null [" + id + "]", ref.getClassifier() );
        assertTrue( "Type should not be blank [" + id + "]", StringUtils.isNotBlank( ref.getType() ) );
    
        return ref;
    }
}
