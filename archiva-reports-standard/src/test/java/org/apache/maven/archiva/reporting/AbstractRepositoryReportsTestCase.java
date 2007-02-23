package org.apache.maven.archiva.reporting;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.jpox.SchemaTool;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 *
 */
public abstract class AbstractRepositoryReportsTestCase
    extends PlexusTestCase
{
    /**
     * This should only be used for the few that can't use the query layer.
     */
    protected ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private ArtifactRepositoryFactory factory;

    private ArtifactRepositoryLayout layout;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        setupJdoFactory();

        File repositoryDirectory = getTestFile( "src/test/repository" );

        factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        repository = factory.createArtifactRepository( "repository", repositoryDirectory.toURL().toString(), layout,
                                                       null, null );
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    protected void setupJdoFactory()
        throws Exception
    {
        DefaultConfigurableJdoFactory jdoFactory = (DefaultConfigurableJdoFactory) lookup( JdoFactory.ROLE, "archiva" );

        jdoFactory.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" ); //$NON-NLS-1$

        jdoFactory.setDriverName( "org.hsqldb.jdbcDriver" ); //$NON-NLS-1$

        jdoFactory.setUrl( "jdbc:hsqldb:mem:" + getName() ); //$NON-NLS-1$

        jdoFactory.setUserName( "sa" ); //$NON-NLS-1$

        jdoFactory.setPassword( "" ); //$NON-NLS-1$

        jdoFactory.setProperty( "org.jpox.transactionIsolation", "READ_UNCOMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.poid.transactionIsolation", "READ_UNCOMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.autoCreateSchema", "true" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "javax.jdo.PersistenceManagerFactoryClass", "org.jpox.PersistenceManagerFactoryImpl" );

        Properties properties = jdoFactory.getProperties();

        for ( Iterator it = properties.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        SchemaTool.createSchemaTables( new URL[] { getClass()
            .getResource( "/org/apache/maven/archiva/reporting/model/package.jdo" ) }, new URL[] {}, null, false, null ); //$NON-NLS-1$

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();
    }

    protected Artifact createArtifactFromRepository( File repository, String groupId, String artifactId, String version )
        throws Exception
    {
        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );

        artifact.setRepository( factory.createArtifactRepository( "repository", repository.toURL().toString(), layout,
                                                                  null, null ) );

        artifact.isSnapshot();

        return artifact;
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, "jar" );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
        artifact.setRepository( repository );
        artifact.isSnapshot();
        return artifact;
    }

    protected Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                                     String classifier )
    {
        Artifact artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                          classifier );
        artifact.setRepository( repository );
        return artifact;
    }

}
