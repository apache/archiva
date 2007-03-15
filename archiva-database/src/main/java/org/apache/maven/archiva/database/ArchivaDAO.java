package org.apache.maven.archiva.database;

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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContent;

import java.util.List;

/**
 * ArchivaDAO - The interface for all content within the database.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ArchivaDAO
{
    /* NOTE TO ARCHIVA DEVELOPERS.
     * 
     * Please keep this interface clean and lean.
     * We don't want a repeat of the Continuum Store.
     * You should have the following methods per object type ...
     * 
     *   (Required Methods)
     * 
     *    DatabaseObject .createDatabaseObject( Required Params ) ;
     *    List           .queryDatabaseObject( Constraint )       throws ObjectNotFoundException, DatabaseException;
     *    DatabaseObject .saveDatabaseObject( DatabaseObject )    throws DatabaseException;
     *    
     *   (Optional Methods)
     *   
     *    DatabaseObject .getDatabaseObject( Id )                 throws ObjectNotFoundException, DatabaseException;
     *    List           .getDatabaseObjects()                    throws ObjectNotFoundException, DatabaseException;
     *    void           .deleteDatabaseObject( DatabaseObject )  throws DatabaseException;
     *    
     * This is the only list of options created in this DAO.
     */

    /* .\ Archiva Repository \.____________________________________________________________ */

    public ArchivaRepository createRepository( String id, String url );

    public List /*<ArchivaRepository>*/getRepositories()
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public ArchivaRepository getRepository( String id )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public List queryRepository( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public ArchivaRepository saveRepository( ArchivaRepository repository )
        throws ArchivaDatabaseException;

    public void deleteRepository( ArchivaRepository repository )
        throws ArchivaDatabaseException;

    /* .\ Repository Content \.____________________________________________________________ */

    public RepositoryContent createRepositoryContent( String groupId, String artifactId, String version,
                                                      String repositoryId );

    public RepositoryContent getRepositoryContent( String groupId, String artifactId, String version,
                                                   String repositoryId )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public List /*<RepositoryContent>*/queryRepositoryContents( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public RepositoryContent saveRepositoryContent( RepositoryContent repoContent )
        throws ArchivaDatabaseException;

    public void deleteRepositoryContent( RepositoryContent repoContent )
        throws ArchivaDatabaseException;

    /* .\ Archiva Artifact \. _____________________________________________________________ */

    public ArchivaArtifact createArtifact( RepositoryContent repoContent, String classifier, String type );

    public ArchivaArtifact getArtifact( RepositoryContent repoContent, String classifier, String type )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public List /*<ArchivaArtifact>*/queryArtifacts( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public ArchivaArtifact saveArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;

    public void deleteArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;

}
