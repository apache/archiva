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
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsRelatedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RepositoryBrowsingTest 
 *
 * @version $Id$
 */
public class RepositoryBrowsingTest
    extends AbstractArchivaDatabaseTestCase
{
    private static final List<String> GUEST_REPO_IDS;

    private static final String USER_GUEST = "guest";
    
    static
    {
        GUEST_REPO_IDS = new ArrayList<String>();        
        GUEST_REPO_IDS.add( "snapshots" );
        GUEST_REPO_IDS.add( "central" );        
    }
    
    private ArtifactDAO artifactDao;

    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar", "central" );
        artifact.getModel().setLastModified( new Date() ); // mandatory field.
        artifact.getModel().setRepositoryId( "central" );
        return artifact;
    }

    private RepositoryBrowsing lookupBrowser()
        throws Exception
    {
        RepositoryBrowsing browser = (RepositoryBrowsing) lookup( RepositoryBrowsing.class );
        assertNotNull( "RepositoryBrowsing should not be null.", browser );
        return browser;
    }

    private void saveTestData()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "commons-lang", "commons-lang", "2.0" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "commons-lang", "commons-lang", "2.0" );
        
        artifact = createArtifact( "commons-lang", "commons-lang", "2.1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "commons-lang", "commons-lang", "2.1" );

        artifact = createArtifact( "org.apache.maven.test", "test-one", "1.2" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.test", "test-one", "1.2" );

        artifact = createArtifact( "org.apache.maven.test.foo", "test-two", "1.0" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.test.foo", "test-two", "1.0" );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.0" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.0" );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-SNAPSHOT" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.1-SNAPSHOT" );
        
        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-20070522.143249-1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.1-20070522.143249-1" );
        
        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-20070522.153141-2" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.1-20070522.153141-2" );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1.1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.1.1" );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1-alpha-1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-two", "2.1-alpha-1" );

        artifact = createArtifact( "org.apache.maven.shared", "test-bar", "2.1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.maven.shared", "test-bar", "2.1" );

        artifact = createArtifact( "org.codehaus.modello", "modellong", "3.0" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.codehaus.modello", "modellong", "3.0" );
        
        artifact = createArtifact( "org.apache.archiva", "archiva-indexer", "1.0-20070522.143249-1" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.archiva", "archiva-indexer", "1.0-20070522.143249-1" );
        
        artifact = createArtifact( "org.apache.archiva", "archiva-indexer", "1.0-20070522.153141-2" );
        artifactDao.saveArtifact( artifact );
        assertArtifactWasSaved( "org.apache.archiva", "archiva-indexer", "1.0-20070522.153141-2" );
    }

    private void assertArtifactWasSaved(String groupId, String artifactId, String version)
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        Constraint constraint = new ArtifactsRelatedConstraint( groupId, artifactId, version );
        List<ArchivaArtifact> artifacts = artifactDao.queryArtifacts( constraint );
        
        assertFalse( "Artifact '" + groupId + ":" + artifactId + ":" + version + "' should have been found.",
                     artifacts.isEmpty() );
    }

    public void testBrowseIntoGroupWithSubgroups()
        throws Exception
    {
        RepositoryBrowsing browser = lookupBrowser();
        BrowsingResults results = browser.selectGroupId( USER_GUEST, GUEST_REPO_IDS, "org.apache.maven.test" );
        assertNotNull( "Browsing Results should not be null.", results );

        String expectedSubGroupIds[] = new String[] { "org.apache.maven.test.foo" };
        assertGroupIds( "Browsing Results (subgroup org.apache.maven.test)", results.getGroupIds(), expectedSubGroupIds );
    }

    public void testSimpleBrowse()
        throws Exception
    {
        RepositoryBrowsing browser = lookupBrowser();
        BrowsingResults results = browser.getRoot( USER_GUEST, GUEST_REPO_IDS );
        assertNotNull( "Browsing Results should not be null.", results );

        String expectedRootGroupIds[] = new String[] { "commons-lang", "org" };

        assertGroupIds( "Browsing Results (root)", results.getGroupIds(), expectedRootGroupIds );
    }

    public void testViewArtifact()
        throws Exception
    {
        RepositoryBrowsing browser = lookupBrowser();
        ArchivaProjectModel artifact = browser.selectVersion( USER_GUEST, GUEST_REPO_IDS, "commons-lang", "commons-lang", "2.0" );
        assertNotNull( "Artifact should not be null.", artifact );
		assertEquals( "commons-lang", artifact.getGroupId() );
		assertEquals( "commons-lang", artifact.getArtifactId() );
		assertEquals( "2.0", artifact.getVersion() );		
		assertEquals( "jar", artifact.getPackaging() );
	
		// MRM-1278
		String repoId = browser.getRepositoryId( USER_GUEST, GUEST_REPO_IDS, "commons-lang", "commons-lang", "2.0" );
		assertEquals( "central", repoId );
    }    
    
    public void testViewArtifactWithMultipleTimestampedVersions()
        throws Exception
    {   
        RepositoryBrowsing browser = lookupBrowser();
        ArchivaProjectModel artifact = browser.selectVersion( USER_GUEST, GUEST_REPO_IDS, "org.apache.archiva", "archiva-indexer", "1.0-SNAPSHOT" );
        assertNotNull( "Artifact should not be null.", artifact );
        assertEquals( "org.apache.archiva", artifact.getGroupId() );
        assertEquals( "archiva-indexer", artifact.getArtifactId() );
        assertEquals( "1.0-20070522.143249-1", artifact.getVersion() );       
        assertEquals( "jar", artifact.getPackaging() );
        
        String repoId = browser.getRepositoryId( USER_GUEST, GUEST_REPO_IDS, "org.apache.archiva", "archiva-indexer", "1.0-SNAPSHOT" );
        assertEquals( "central", repoId );
    }
    
    public void testSelectArtifactId()
        throws Exception
    {   
        RepositoryBrowsing browser = lookupBrowser();
        BrowsingResults results =
            browser.selectArtifactId( USER_GUEST, GUEST_REPO_IDS, "org.apache.maven.shared", "test-two" );
        assertNotNull( "Browsing results should not be null.", results );
        assertEquals( 4, results.getVersions().size() );
        assertTrue( results.getVersions().contains( "2.0" ) );
        assertTrue( results.getVersions().contains( "2.1-SNAPSHOT" ) );
        assertTrue( results.getVersions().contains( "2.1.1" ) );
        assertTrue( results.getVersions().contains( "2.1-alpha-1" ) ); 
    }
    
    public void testGetOtherSnapshotVersionsRequestedVersionIsGeneric()
        throws Exception
    {
        RepositoryBrowsing browser = lookupBrowser();
        List<String> results =
            browser.getOtherSnapshotVersions( GUEST_REPO_IDS, "org.apache.maven.shared", "test-two", "2.1-SNAPSHOT" );
        assertNotNull( "Returned list of versions should not be null.", results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( "2.1-SNAPSHOT" ) );
        assertTrue( results.contains( "2.1-20070522.143249-1" ) );
        assertTrue( results.contains( "2.1-20070522.153141-2" ) ); 
    }
    
    public void testGetOtherSnapshotVersionsRequestedVersionIsUnique()
        throws Exception
    {
        RepositoryBrowsing browser = lookupBrowser();
        List<String> results =
            browser.getOtherSnapshotVersions( GUEST_REPO_IDS, "org.apache.maven.shared", "test-two", "2.1-20070522.143249-1" );
        assertNotNull( "Returned list of versions should not be null.", results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( "2.1-SNAPSHOT" ) );
        assertTrue( results.contains( "2.1-20070522.143249-1" ) );
        assertTrue( results.contains( "2.1-20070522.153141-2" ) ); 
    }
    
    private void assertGroupIds( String msg, List<String> actualGroupIds, String[] expectedGroupIds )
    {
        assertEquals( msg + ": groupIds.length", expectedGroupIds.length, actualGroupIds.size() );

        for ( int i = 0; i < expectedGroupIds.length; i++ )
        {
            String expectedGroupId = expectedGroupIds[i];
            assertTrue( msg + ": actual groupIds.contains(" + expectedGroupId + ")", actualGroupIds
                .contains( expectedGroupId ) );
        }
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        artifactDao = dao.getArtifactDAO();        
        saveTestData();
    }
}
