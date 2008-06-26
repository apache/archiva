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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;

/**
 * ArtifactsByRepositoryConstraintTest
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class ArtifactsByRepositoryConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    public void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, null, "jar" );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "test-repo" );

        return artifact;
    }

    public void testQueryAllArtifactsInRepo()
        throws Exception
    {
        Date whenGathered = Calendar.getInstance().getTime();
        whenGathered.setTime( 123456789 );

        ArchivaArtifact artifact = createArtifact( "org.apache.archiva", "artifact-one", "1.0" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-one", "1.0.1" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-two", "1.0.2" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-one", "2.0" );
        artifact.getModel().setRepositoryId( "different-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        assertConstraint( "Artifacts By Repository", 3, new ArtifactsByRepositoryConstraint( "test-repo" ) );
    }

    public void testQueryArtifactsInRepoWithWhenGathered()
        throws Exception
    {
        Date whenGathered = Calendar.getInstance().getTime();

        ArchivaArtifact artifact = createArtifact( "org.apache.archiva", "artifact-one", "1.0" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-one", "1.0.1" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-one", "1.0.2" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-one", "2.0" );
        artifact.getModel().setRepositoryId( "different-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        Date olderWhenGathered = Calendar.getInstance().getTime();
        olderWhenGathered.setTime( 123456789 );
        
        artifact = createArtifact( "org.apache.archiva", "artifact-two", "1.1-SNAPSHOT" );
        artifact.getModel().setWhenGathered( olderWhenGathered );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.archiva", "artifact-three", "2.0-beta-1" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifactDao.saveArtifact( artifact );

        assertConstraint( "Artifacts By Repository and When Gathered", 4,
                          new ArtifactsByRepositoryConstraint( "test-repo", whenGathered, "repositoryId" ) );
    }

    private void assertConstraint( String msg, int count, ArtifactsByRepositoryConstraint constraint )
        throws Exception
    {
        List results = artifactDao.queryArtifacts( constraint );
        assertNotNull( msg + ": Not Null", results );
        assertEquals( msg + ": Results.size", count, results.size() );
    }
}
