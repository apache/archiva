package org.apache.maven.archiva.web.rss;

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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.DeclarativeConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Stub used for RssFeedServlet unit test.
 * 
 * @version
 */
public class ArtifactDAOStub
    implements ArtifactDAO
{

    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String classifier,
                                           String type, String repositoryId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub

    }

    public long countArtifacts( DeclarativeConstraint constraint )
    {
        return queryArtifacts( constraint ).size();
    }

    public ArchivaArtifact getArtifact( String groupId, String artifactId, String version, String classifier,
                                        String type, String repositoryId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<ArchivaArtifact> queryArtifacts( Constraint constraint )
    {
        List<ArchivaArtifact> artifacts = new ArrayList<ArchivaArtifact>();

        Date whenGathered = Calendar.getInstance().getTime();
        whenGathered.setTime( 123456789 );

        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.0", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.1", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "2.0", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.1", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.2", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.3-SNAPSHOT", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-three", "2.0-SNAPSHOT", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-four", "1.1-beta-2", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        return artifacts;
    }

    public ArchivaArtifact saveArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
