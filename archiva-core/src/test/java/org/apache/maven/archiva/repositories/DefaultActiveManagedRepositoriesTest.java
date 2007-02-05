package org.apache.maven.archiva.repositories;

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

import org.apache.maven.archiva.artifact.ManagedArtifact;
import org.apache.maven.archiva.artifact.ManagedEjbArtifact;
import org.apache.maven.archiva.artifact.ManagedJavaArtifact;
import org.codehaus.plexus.PlexusTestCase;

/**
 * DefaultActiveManagedRepositoriesTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultActiveManagedRepositoriesTest
    extends PlexusTestCase
{
    private ActiveManagedRepositories managedRepos;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        managedRepos = (DefaultActiveManagedRepositories) lookup( ActiveManagedRepositories.ROLE );
    }

    /**
     * Test a simple java find artifact with extras (sources / javadoc). 
     */
    public void testFindArtifactJavaWithExtras()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "geronimo", "daytrader-wsappclient", "1.1", "jar" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedJavaArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedJavaArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedJavaArtifact javaArtifact = (ManagedJavaArtifact) artifact;

        assertEquals( "test", javaArtifact.getRepositoryId() );

        String pathPrefix = "geronimo/daytrader-wsappclient/1.1";
        String pathArtifactVersion = "daytrader-wsappclient-1.1";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".jar", javaArtifact.getPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-javadoc.jar", javaArtifact.getJavadocPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-sources.jar", javaArtifact.getSourcesPath() );
    }
    
    /**
     * Test a simple java find artifact with no extras.
     */
    public void testFindArtifactJavaSimple()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "geronimo", "daytrader-streamer", "1.1", "jar" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedJavaArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedJavaArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedJavaArtifact javaArtifact = (ManagedJavaArtifact) artifact;

        assertEquals( "test", javaArtifact.getRepositoryId() );

        String pathPrefix = "geronimo/daytrader-streamer/1.1";
        String pathArtifactVersion = "daytrader-streamer-1.1";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".jar", javaArtifact.getPath() );
        assertNull( "should have no javadoc jar.", javaArtifact.getJavadocPath() );
        assertNull( "should have no sources jar.", javaArtifact.getSourcesPath() );
    }    

    /**
     * Test a java find of a snapshot artifact that uses a timestamp format. 
     */
    public void testFindArtifactJavaSnapshotTimestamp()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "org.apache.geronimo.daytrader", "daytrader-wsappclient",
                                                              "2.0-20070201.183230-5", "jar" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedJavaArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedJavaArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedJavaArtifact javaArtifact = (ManagedJavaArtifact) artifact;

        assertEquals( "test", javaArtifact.getRepositoryId() );

        String pathPrefix = "org/apache/geronimo/daytrader/daytrader-wsappclient/2.0-SNAPSHOT";
        String pathArtifactVersion = "daytrader-wsappclient-2.0-20070201.183230-5";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".jar", javaArtifact.getPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-javadoc.jar", javaArtifact.getJavadocPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-sources.jar", javaArtifact.getSourcesPath() );
    }

    /**
     * Test a java find of a snapshot artifact. 
     */
    public void testFindArtifactJavaSnapshot()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "org.apache.geronimo.daytrader", "daytrader-wsappclient",
                                                              "2.0-SNAPSHOT", "jar" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedJavaArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedJavaArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedJavaArtifact javaArtifact = (ManagedJavaArtifact) artifact;

        assertEquals( "test", javaArtifact.getRepositoryId() );

        String pathPrefix = "org/apache/geronimo/daytrader/daytrader-wsappclient/2.0-SNAPSHOT";
        String pathArtifactVersion = "daytrader-wsappclient-2.0-SNAPSHOT";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".jar", javaArtifact.getPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-javadoc.jar", javaArtifact.getJavadocPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-sources.jar", javaArtifact.getSourcesPath() );
    }

    /**
     * Test a ejb find of a snapshot artifact that also has a client jar available. 
     */
    public void testFindArtifactEjbSnapshot()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "org.apache.geronimo.daytrader", "daytrader-ejb",
                                                              "2.0-SNAPSHOT", "ejb" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedEjbArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedEjbArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedEjbArtifact ejbArtifact = (ManagedEjbArtifact) artifact;

        assertEquals( "test", ejbArtifact.getRepositoryId() );

        String pathPrefix = "org/apache/geronimo/daytrader/daytrader-ejb/2.0-SNAPSHOT";
        String pathArtifactVersion = "daytrader-ejb-2.0-SNAPSHOT";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".jar", ejbArtifact.getPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-client.jar", ejbArtifact.getClientPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-javadoc.jar", ejbArtifact.getJavadocPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-sources.jar", ejbArtifact.getSourcesPath() );
    }
    
    /**
     * Test a simple java find artifact with no extras.
     */
    public void testFindArtifactWar()
    {
        ManagedArtifact artifact = managedRepos.findArtifact( "geronimo", "daytrader-web", "1.1", "war" );
        assertNotNull( artifact );

        if ( !( artifact instanceof ManagedJavaArtifact ) )
        {
            fail( "Expected artifact to be type <" + ManagedJavaArtifact.class.getName() + "> but was actually <"
                + artifact.getClass().getName() + ">." );
        }

        ManagedJavaArtifact warArtifact = (ManagedJavaArtifact) artifact;

        assertEquals( "test", warArtifact.getRepositoryId() );

        String pathPrefix = "geronimo/daytrader-web/1.1";
        String pathArtifactVersion = "daytrader-web-1.1";

        assertEquals( pathPrefix + "/" + pathArtifactVersion + ".war", warArtifact.getPath() );
        assertEquals( pathPrefix + "/" + pathArtifactVersion + "-javadoc.jar", warArtifact.getJavadocPath() );
        assertNull( "should have no sources jar.", warArtifact.getSourcesPath() );
    }    
}
