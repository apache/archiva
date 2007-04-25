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

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.UniqueArtifactIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * DefaultRepositoryBrowsing 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.browsing.RepositoryBrowsing"
 *                   role-hint="default"
 */
public class DefaultRepositoryBrowsing
    extends AbstractLogEnabled
    implements RepositoryBrowsing
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    public BrowsingResults getRoot()
    {
        List groups = dao.query( new UniqueGroupIdConstraint() );

        BrowsingResults results = new BrowsingResults();

        results.setGroupIds( GroupIdFilter.filterGroups( groups ) );

        return results;
    }

    public BrowsingResults selectArtifactId( String groupId, String artifactId )
    {
        // List groups = dao.query( new UniqueGroupIdConstraint( groupId ) );
        // List artifacts = dao.query( new UniqueArtifactIdConstraint( groupId ) );
        List versions = dao.query( new UniqueVersionConstraint( groupId, artifactId ) );

        BrowsingResults results = new BrowsingResults( groupId, artifactId );

        // results.setGroupIds( groups );
        // results.setArtifacts( artifacts );
        results.setArtifacts( versions );

        return results;
    }

    public BrowsingResults selectGroupId( String groupId )
    {
        List groups = dao.query( new UniqueGroupIdConstraint( groupId ) );
        List artifacts = dao.query( new UniqueArtifactIdConstraint( groupId ) );

        BrowsingResults results = new BrowsingResults( groupId );
        results.setGroupIds( groups );
        results.setArtifacts( artifacts );

        return results;
    }

    public ArchivaProjectModel selectVersion( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaProjectModel model = dao.getProjectModelDAO().getProjectModel( groupId, artifactId, version );

        // TODO: if the model isn't found. load it from disk, insert into DB, and then return it.

        return model;
    }
}
