package org.apache.maven.archiva.database.browsing;

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
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Date;

/**
 * RepositoryBrowsingTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryBrowsingTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar" );
        artifact.getModel().setLastModified( new Date() ); // mandatory field.
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void saveTestData()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "commons-lang", "commons-lang", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "commons-lang", "commons-lang", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.test", "test-one", "1.2" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.test.foo", "test-two", "1.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-SNAPSHOT" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-alpha-1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-bar", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.codehaus.modello", "modellong", "3.0" );
        artifactDao.saveArtifact( artifact );
    }

    public RepositoryBrowsing lookupBrowser()
        throws Exception
    {
        RepositoryBrowsing browser = (RepositoryBrowsing) lookup( RepositoryBrowsing.class.getName() );
        assertNotNull( "RepositoryBrowsing should not be null.", browser );
        return browser;
    }

    public void testSimpleBrowse()
        throws Exception
    {
        saveTestData();

        RepositoryBrowsing browser = lookupBrowser();
        BrowsingResults results = browser.getRoot();
        assertNotNull( "Browsing Results should not be null.", results );

        String expectedRootGroupIds[] = new String[] { "commons-lang", "org" };

        assertEquals( "Browsing Results: groupIds on root.", expectedRootGroupIds.length, results.getGroupIds().size() );
    }
}
