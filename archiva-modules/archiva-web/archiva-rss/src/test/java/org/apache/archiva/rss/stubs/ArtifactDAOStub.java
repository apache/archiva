package org.apache.archiva.rss.stubs;

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
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.List;

/**
 * ArtifactDAO stub.
 * 
 * @version * 
 */
public class ArtifactDAOStub
    implements ArtifactDAO
{
    private List<ArchivaArtifact> artifacts;

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
        return artifacts.size();
    }

    public ArchivaArtifact getArtifact( String groupId, String artifactId, String version, String classifier,
                                        String type, String repositoryId )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<ArchivaArtifact> queryArtifacts( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return artifacts;
    }

    public ArchivaArtifact saveArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setArtifacts( List<ArchivaArtifact> artifacts )
    {
        this.artifacts = artifacts;
    }
}
