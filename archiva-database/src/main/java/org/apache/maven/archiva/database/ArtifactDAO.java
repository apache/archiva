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

import java.util.List;

/**
 * ArtifactDAO 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ArtifactDAO
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

    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String classifier,
                                           String type );

    public ArchivaArtifact getArtifact( String groupId, String artifactId, String version, String classifier,
                                        String type )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public List /*<ArchivaArtifact>*/queryArtifacts( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    public ArchivaArtifact saveArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;

    public void deleteArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;
}
