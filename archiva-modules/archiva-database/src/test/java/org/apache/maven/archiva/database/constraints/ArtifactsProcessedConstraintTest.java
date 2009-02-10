package org.apache.maven.archiva.database.constraints;

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
import java.util.Iterator;
import java.util.List;

/**
 * ArtifactsProcessedConstraintTest 
 *
 * @version $Id$
 */
public class ArtifactsProcessedConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
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

    public void assertResults( String type, List results, String expectedArtifacts[] )
    {
        assertNotNull( "Results[" + type + "] should not be null.", results );
        assertEquals( "Results[" + type + "].size", expectedArtifacts.length, results.size() );

        for ( int i = 0; i < expectedArtifacts.length; i++ )
        {
            String artifactId = expectedArtifacts[i];

            int found = 0;
            Iterator it = results.iterator();
            while ( it.hasNext() )
            {
                ArchivaArtifact artifact = (ArchivaArtifact) it.next();
                if ( artifactId.equals( artifact.getArtifactId() ) )
                {
                    found++;
                }
            }

            if ( found <= 0 )
            {
                fail( "Results[" + type + "] - Did not find expected artifact ID [" + artifactId + "]" );
            }

            if ( found > 1 )
            {
                fail( "Results[" + type + "] - Expected to find 1 copy of artifact ID [" + artifactId
                    + "], yet found <" + found + "> instead." );
            }
        }
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactDAO adao = dao.getArtifactDAO();
        assertNotNull( "Artifact DAO should not be null.", adao );

        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-common", "1.0-SNAPSHOT", null ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-utils", "1.0-SNAPSHOT",
                                           "2006/08/22 19:01:00" ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-old", "0.1", "2004/02/15 9:01:00" ) );
        adao.saveArtifact( createArtifact( "org.apache.maven.archiva", "archiva-database", "1.0-SNAPSHOT", null ) );
    }

    public void testNotProcessed()
        throws Exception
    {
        List results = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( false ) );
        assertResults( "not-processed", results, new String[] { "archiva-common", "archiva-database" } );
    }

    public void testProcessed()
        throws Exception
    {
        List results = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( true ) );
        assertResults( "processed", results, new String[] { "archiva-utils", "archiva-old" } );
    }

    public void testSinceRecent()
        throws Exception
    {
        Date since = toDate( "2006/01/01 12:00:00" );
        List results = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( since ) );
        assertResults( "processed", results, new String[] { "archiva-utils" } );
    }

    public void testSinceOld()
        throws Exception
    {
        Date since = toDate( "2001/01/01 12:00:00" );
        List results = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( since ) );
        assertResults( "processed", results, new String[] { "archiva-utils", "archiva-old" } );
    }
}
