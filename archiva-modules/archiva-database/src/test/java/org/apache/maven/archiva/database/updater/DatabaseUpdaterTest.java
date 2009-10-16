package org.apache.maven.archiva.database.updater;

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
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Date;

/**
 * DatabaseUpdaterTest 
 *
 * @version $Id$
 */
public class DatabaseUpdaterTest
    extends AbstractArchivaDatabaseTestCase
{
    private DatabaseUpdater dbupdater;

    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String whenProcessed )
        throws Exception
    {
        ArchivaArtifact artifact = dao.getArtifactDAO().createArtifact( groupId, artifactId, version, "", "jar", "testrepo" );
        assertNotNull( "Artifact should not be null.", artifact );
        Date dateWhenProcessed = null;

        if ( whenProcessed != null )
        {
            dateWhenProcessed = toDate( whenProcessed );
        }

        artifact.getModel().setWhenProcessed( dateWhenProcessed );

        // Satisfy table / column requirements.
        artifact.getModel().setLastModified( new Date() );

        return artifact;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactDAO adao = dao.getArtifactDAO();
        assertNotNull( "Artifact DAO should not be null.", adao );

        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-common", "1.0-SNAPSHOT", null ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-utils", "1.0-SNAPSHOT", null ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-old", "0.1", "2004/02/15 9:01:00" ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-database", "1.0-SNAPSHOT", null ) );

        dbupdater = (DatabaseUpdater) lookup( DatabaseUpdater.class, "jdo" );
        assertNotNull( "DatabaseUpdater should not be null.", dbupdater );
    }

    public void testUpdateUnprocessed()
        throws Exception
    {
        String groupId = "org.apache.maven.archiva";
        String artifactId = "archiva-utils";
        String version = "1.0-SNAPSHOT";
        String classifier = "";
        String type = "jar";
        
        TestDatabaseUnprocessedConsumer consumer = lookupTestUnprocessedConsumer();
        consumer.resetCount();

        // Check the state of the artifact in the DB.
        ArchivaArtifact savedArtifact = dao.getArtifactDAO().getArtifact( groupId, artifactId, version, classifier,
                                                                          type, "testrepo" );
        assertFalse( "Artifact should not be considered processed (yet).", savedArtifact.getModel().isProcessed() );

        // Update the artifact
        dbupdater.updateUnprocessed( savedArtifact );

        // Check the update.
        ArchivaArtifact processed = dao.getArtifactDAO().getArtifact( groupId, artifactId, version, classifier, type, "testrepo" );
        assertTrue( "Artifact should be flagged as processed.", processed.getModel().isProcessed() );

        // Did the unprocessed consumer do it's thing?
        assertEquals( "Processed Count.", 1, consumer.getCountProcessed() );
    }
}
