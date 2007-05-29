package org.apache.maven.archiva.consumers.database.project;

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
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

/**
 * Resolves a project model from the database. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelResolver"
 *                   role-hint="database"
 */
public class DatabaseProjectModelResolver
    implements ProjectModelResolver
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException
    {
        try
        {
            ArchivaProjectModel model = dao.getProjectModelDAO().getProjectModel( reference.getGroupId(),
                                                                                  reference.getArtifactId(),
                                                                                  reference.getVersion() );
            return model;
        }
        catch ( ObjectNotFoundException e )
        {
            return null;
        }
        catch ( ArchivaDatabaseException e )
        {
            return null;
        }
    }
}
